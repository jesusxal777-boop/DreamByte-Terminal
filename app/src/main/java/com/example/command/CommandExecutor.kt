package com.example.command

import com.example.model.CommandResult
import com.example.model.LineType
import com.example.model.TerminalLine
import com.example.shell.DreamShellParser
import com.example.shell.NodeOperator
import com.example.shell.RedirectionType
import com.example.shell.SingleCommandSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CommandExecutor(
    private val registry: CommandRegistry
) {
    suspend fun executeInput(
        rawInput: String,
        context: CommandContext
    ): List<TerminalLine> = withContext(Dispatchers.Default) {
        val resultLines = mutableListOf<TerminalLine>()
        val trimmed = rawInput.trim()

        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return@withContext emptyList()
        }

        val nodes = DreamShellParser.parse(trimmed, context.shellEnv)
        if (nodes.isEmpty()) {
            return@withContext emptyList()
        }

        var shouldExecuteNext = true

        for (node in nodes) {
            if (!shouldExecuteNext) break

            val pipeline = node.pipeline
            var currentPipeStdin = ""
            var lastPipelineResult: CommandResult = CommandResult.success("")

            for ((cmdIndex, cmdSpec) in pipeline.commands.withIndex()) {
                if (cmdSpec.commandName.isEmpty()) continue

                // Check for input redirection '<'
                var effectiveStdin = currentPipeStdin
                val inputRedir = cmdSpec.redirections.find { it.type == RedirectionType.READ_INPUT }
                if (inputRedir != null) {
                    val inputFile = context.shellEnv.resolvePath(inputRedir.target)
                    if (inputFile.exists() && inputFile.isFile && context.shellEnv.isPathWithinSandbox(inputFile)) {
                        effectiveStdin = inputFile.readText()
                    } else {
                        val err = "dbsh: ${inputRedir.target}: No such file or directory"
                        resultLines.add(TerminalLine(text = err, type = LineType.ERROR))
                        context.shellEnv.lastExitCode = 1
                        break
                    }
                }

                // Execute single command with effectiveStdin
                val cmdResult = executeSingleCommand(cmdSpec, effectiveStdin, context)
                lastPipelineResult = cmdResult
                context.shellEnv.lastExitCode = cmdResult.exitCode

                // If in middle of pipe, pass output to next command
                if (cmdIndex < pipeline.commands.size - 1) {
                    currentPipeStdin = cmdResult.output
                } else {
                    // Last command in pipe: handle output redirections '>' and '>>'
                    val writeRedir = cmdSpec.redirections.find { it.type == RedirectionType.WRITE }
                    val appendRedir = cmdSpec.redirections.find { it.type == RedirectionType.APPEND }

                    if (writeRedir != null) {
                        val outFile = context.shellEnv.resolvePath(writeRedir.target)
                        if (context.shellEnv.isPathWithinSandbox(outFile)) {
                            outFile.parentFile?.mkdirs()
                            outFile.writeText(cmdResult.output + "\n")
                        } else {
                            resultLines.add(TerminalLine(text = "dbsh: ${writeRedir.target}: Permission denied by sandbox", type = LineType.ERROR))
                        }
                    } else if (appendRedir != null) {
                        val outFile = context.shellEnv.resolvePath(appendRedir.target)
                        if (context.shellEnv.isPathWithinSandbox(outFile)) {
                            outFile.parentFile?.mkdirs()
                            outFile.appendText(cmdResult.output + "\n")
                        } else {
                            resultLines.add(TerminalLine(text = "dbsh: ${appendRedir.target}: Permission denied by sandbox", type = LineType.ERROR))
                        }
                    } else {
                        // Normal terminal display
                        if (cmdResult.output.isNotBlank()) {
                            val lines = cmdResult.output.lines()
                            lines.forEach { line ->
                                resultLines.add(TerminalLine(text = line, type = cmdResult.type))
                            }
                        }
                    }
                }

                // If error occurred in pipe, stop chain
                if (cmdResult.exitCode != 0 && cmdIndex < pipeline.commands.size - 1) {
                    break
                }
            }

            // Determine if next node should execute based on operator
            when (node.operator) {
                NodeOperator.AND_THEN -> {
                    shouldExecuteNext = (lastPipelineResult.exitCode == 0)
                }
                NodeOperator.OR_THEN -> {
                    shouldExecuteNext = (lastPipelineResult.exitCode != 0)
                }
                NodeOperator.THEN -> {
                    shouldExecuteNext = true
                }
                NodeOperator.END -> {
                    shouldExecuteNext = false
                }
            }
        }

        return@withContext resultLines
    }

    private suspend fun executeSingleCommand(
        spec: SingleCommandSpec,
        stdin: String,
        context: CommandContext
    ): CommandResult {
        val cmdName = spec.commandName
        val args = spec.arguments

        // 1. Variable assignment check (e.g. VAR=value)
        if (cmdName.contains("=") && args.isEmpty()) {
            val key = cmdName.substringBefore("=").trim()
            val value = cmdName.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
            context.shellEnv.setVariable(key, value)
            return CommandResult.success("")
        }

        // 2. Built-in command lookup
        val registeredCommand = registry.findCommand(cmdName)
        if (registeredCommand != null) {
            val cmdCtx = context.copy(stdin = stdin)
            return try {
                registeredCommand.execute(cmdCtx, args)
            } catch (e: Exception) {
                CommandResult.error("Execution error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }

        // 3. External executable in $PATH or relative path (e.g. installed by pkg into $PREFIX/bin)
        val externalExecutable = findExecutable(cmdName, context)
        if (externalExecutable != null && externalExecutable.exists()) {
            return executeExternalExecutable(externalExecutable, args, stdin, context)
        }

        return CommandResult.error("dbsh: $cmdName: command not found. Type 'help' for available commands.", exitCode = 127)
    }

    private fun findExecutable(name: String, context: CommandContext): File? {
        // Direct relative or absolute path
        if (name.startsWith("./") || name.startsWith("/") || name.startsWith("../")) {
            val f = context.shellEnv.resolvePath(name)
            if (f.exists() && f.isFile) return f
        }

        // Search in $PREFIX/bin and $PATH
        val pathVar = context.shellEnv.getVariable("PATH") ?: context.shellEnv.binDir.absolutePath
        val searchDirs = pathVar.split(":")
        for (dirStr in searchDirs) {
            val dir = File(dirStr)
            if (dir.exists() && dir.isDirectory) {
                val candidate = File(dir, name)
                if (candidate.exists() && candidate.isFile) {
                    return candidate
                }
            }
        }
        return null
    }

    private suspend fun executeExternalExecutable(
        file: File,
        args: List<String>,
        stdin: String,
        context: CommandContext
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            // Check shebang
            val firstLine = file.useLines { it.firstOrNull() } ?: ""
            if (firstLine.startsWith("#!")) {
                if (firstLine.contains("python")) {
                    val pyCmd = registry.findCommand("python")
                    if (pyCmd != null) {
                        return@withContext pyCmd.execute(context, listOf(file.absolutePath) + args)
                    }
                }
                if (firstLine.contains("sh") || firstLine.contains("bash")) {
                    val shCmd = registry.findCommand("sh")
                    if (shCmd != null) {
                        return@withContext shCmd.execute(context, listOf(file.absolutePath) + args)
                    }
                }
            }

            // Execute native binary within app sandbox
            if (!file.canExecute()) {
                file.setExecutable(true, false)
            }

            val processBuilder = ProcessBuilder(listOf(file.absolutePath) + args)
                .directory(context.shellEnv.currentDirectory)
                .redirectErrorStream(true)

            // Inject shell environment variables into process
            val procEnv = processBuilder.environment()
            context.shellEnv.getAllVariables().forEach { (k, v) ->
                procEnv[k] = v
            }

            val proc = processBuilder.start()

            if (stdin.isNotEmpty()) {
                proc.outputStream.bufferedWriter().use { writer ->
                    writer.write(stdin)
                }
            }

            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()

            return@withContext if (exitCode == 0) {
                CommandResult.success(output.trimEnd())
            } else {
                CommandResult.error(output.trimEnd(), exitCode = exitCode)
            }
        } catch (e: Exception) {
            CommandResult.error("dbsh: cannot execute '${file.name}': ${e.localizedMessage}", exitCode = 126)
        }
    }
}
