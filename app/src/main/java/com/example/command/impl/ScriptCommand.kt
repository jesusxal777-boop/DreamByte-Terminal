package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import kotlinx.coroutines.delay

class ScriptCommand : Command {
    override val name: String = "sh"
    override val description: String = "DreamShell command language interpreter"
    override val aliases: List<String> = listOf("bash", "dreamshell")
    override val usage: String = "sh [-c \"commands\"] | [script.sh] [args...] | [demo]"
    override val category: CommandCategory = CommandCategory.SHELL

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "DreamShell Shell Script Interpreter",
        synopsis = "sh [-c \"commands\"] | [script.sh] [args...]",
        options = listOf(
            "-c <string>" to "Execute commands read from string",
            "--help" to "Display interpreter usage"
        ),
        description = "Native shell language interpreter for DreamByte OS M. Supports pipelines ('|'), input/output redirections ('>', '>>', '<'), exit code verification, variables, and batch script interpretation.",
        examples = listOf(
            "sh -c \"echo hello > test.txt && cat test.txt\"" to "Run inline pipeline with redirection",
            "sh script.sh" to "Execute shell script file from disk",
            "sh demo" to "Run built-in diagnostic and telemetry script"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.success(
                "DreamShell v1.2 (Android Native Sandbox)\n" +
                "Usage: sh [-c \"command\"] | [script.sh] [args...]\n" +
                "Type 'help sh' for more details."
            )
        }

        val scriptContent: String
        val scriptArgs: List<String>

        when {
            args[0] == "-c" && args.size > 1 -> {
                scriptContent = args.drop(1).joinToString(" ")
                scriptArgs = emptyList()
            }
            args[0] == "demo" -> {
                scriptContent = """
                # DreamByte Core Diagnostics Script
                echo [1/4] Checking host environment...
                sysinfo
                echo [2/4] Verifying active user and process...
                whoami
                echo [3/4] Package manager repository query...
                pkg list
                echo [4/4] Active DreamByte theme state...
                theme
                echo Diagnostics completed with exit code 0.
                """.trimIndent()
                scriptArgs = emptyList()
            }
            else -> {
                val scriptPath = args[0]
                val file = context.shellEnv.resolvePath(scriptPath)
                if (!file.exists() || file.isDirectory) {
                    return CommandResult.error("sh: cannot open '$scriptPath': No such file or directory")
                }
                if (!context.shellEnv.isPathWithinSandbox(file)) {
                    return CommandResult.error("sh: access restricted by Android sandbox.")
                }
                scriptContent = file.readText()
                scriptArgs = args.drop(1)
            }
        }

        // Set script positional parameters
        scriptArgs.forEachIndexed { idx, arg ->
            context.shellEnv.setVariable((idx + 1).toString(), arg)
        }
        context.shellEnv.setVariable("@", scriptArgs.joinToString(" "))

        val lines = scriptContent.lines()
        val sb = StringBuilder()

        for ((idx, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val res = context.executeNestedCommand(line)
            if (res.output.isNotBlank()) {
                sb.appendLine(res.output)
            }
            if (res.exitCode != 0) {
                sb.appendLine("[sh]: Error on line ${idx + 1}: command exited with code ${res.exitCode}")
                return CommandResult.error(sb.toString().trimEnd(), exitCode = res.exitCode)
            }
            delay(15)
        }

        return CommandResult.success(sb.toString().trimEnd())
    }
}
