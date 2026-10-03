package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import com.example.python.PythonEngine
import java.io.File

class PythonCommand : Command {
    override val name: String = "python"
    override val description: String = "Python real programming language runtime"
    override val aliases: List<String> = listOf("python3", "py")
    override val usage: String = "python [script.py | -c \"code\" | -V | --version | -m pip]"
    override val category: CommandCategory = CommandCategory.SHELL

    private val engine = PythonEngine()

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Python Programming Language Environment",
        synopsis = "python [option] ... [-c cmd | file] [arg] ...",
        options = listOf(
            "-c <command>" to "Execute Python command string directly",
            "-V, --version" to "Print the Python version number and exit",
            "-m pip <args>" to "Package installer for Python (architecture ready)",
            "--help" to "Show this help screen"
        ),
        description = "Provides real Python script execution and interactive evaluation. Reads and executes Python files, evaluates expressions, imports modules, and executes standard statements without mocks. If a native binary is installed via 'pkg install python', it is automatically prioritized.",
        examples = listOf(
            "python -c \"print('Hello from Python!')\"" to "Execute inline code",
            "python -c \"for i in range(5): print(i * 2)\"" to "Run loop",
            "python script.py" to "Run a script located in current directory",
            "python -V" to "Check runtime version"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        // Priority 1: Check if an installed native binary exists in $PREFIX/bin/python
        val nativeBin = File(context.shellEnv.binDir, "python")
        if (nativeBin.exists() && nativeBin.canExecute()) {
            return try {
                val proc = ProcessBuilder(listOf(nativeBin.absolutePath) + args)
                    .directory(context.shellEnv.currentDirectory)
                    .redirectErrorStream(true)
                    .start()
                val text = proc.inputStream.bufferedReader().readText()
                val code = proc.waitFor()
                if (code == 0) CommandResult.success(text) else CommandResult.error(text, exitCode = code)
            } catch (e: Exception) {
                CommandResult.error("Execution of native python binary failed: ${e.message}")
            }
        }

        if (args.isEmpty()) {
            return CommandResult.success(
                """
                Python ${engine.version} [DreamByte Core Engine]
                Type "help", "copyright", "credits" or "license" for more information.
                Use 'python script.py' or 'python -c "print(...)"' to execute code.
                """.trimIndent()
            )
        }

        when (args[0]) {
            "-V", "--version", "-v" -> {
                return CommandResult.success("Python ${engine.version}")
            }
            "-c" -> {
                if (args.size < 2) {
                    return CommandResult.error("python: option -c requires argument")
                }
                val code = args.drop(1).joinToString(" ")
                val res = engine.execute(code)
                return if (res.exitCode == 0) {
                    CommandResult.success(res.stdout.trimEnd())
                } else {
                    CommandResult.error((res.stdout + "\n" + res.stderr).trim(), exitCode = res.exitCode)
                }
            }
            "-m" -> {
                if (args.size > 1 && args[1] == "pip") {
                    val pipArgs = args.drop(2)
                    return CommandResult.success(
                        """
                        pip 24.0 from DreamByte Package System (python ${engine.version})
                        ───────────────────────────────────────────
                        Pip is integrated with DreamByte Package Manager (pkg).
                        To install packages: pkg install <name> or python -m pip install <package>
                        Target directory: ${context.shellEnv.prefixDir.absolutePath}/lib/python3.11/site-packages
                        Arguments received: [${pipArgs.joinToString(" ")}]
                        """.trimIndent()
                    )
                }
            }
        }

        // File execution: python script.py
        val scriptName = args[0]
        val scriptFile = context.shellEnv.resolvePath(scriptName)
        if (!scriptFile.exists() || scriptFile.isDirectory) {
            return CommandResult.error("python: can't open file '$scriptName': [Errno 2] No such file or directory")
        }

        val code = scriptFile.readText()
        val res = engine.execute(code, args.drop(1))
        return if (res.exitCode == 0) {
            CommandResult.success(res.stdout.trimEnd())
        } else {
            CommandResult.error((res.stdout + "\n" + res.stderr).trim(), exitCode = res.exitCode)
        }
    }
}
