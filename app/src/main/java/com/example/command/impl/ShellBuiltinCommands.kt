package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import java.io.File

class PwdCommand : Command {
    override val name: String = "pwd"
    override val description: String = "Prints the current working directory"
    override val category: CommandCategory = CommandCategory.SHELL
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Print Working Directory",
        synopsis = "pwd",
        description = "Outputs the absolute path of the current working directory.",
        examples = listOf("pwd" to "Print active directory")
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        return CommandResult.success(context.shellEnv.currentDirectory.absolutePath)
    }
}

class CdCommand : Command {
    override val name: String = "cd"
    override val description: String = "Changes the active working directory"
    override val usage: String = "cd [dir]"
    override val category: CommandCategory = CommandCategory.SHELL
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Change Directory",
        synopsis = "cd [directory]",
        description = "Changes current directory within the Android sandbox. Supports '~', '..', and relative paths.",
        examples = listOf(
            "cd ~" to "Go to home directory",
            "cd .." to "Move up one directory level",
            "cd scripts" to "Enter scripts folder"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val target = if (args.isEmpty()) "~" else args[0]
        val res = context.shellEnv.changeDirectory(target)
        return if (res.isSuccess) {
            CommandResult.success("")
        } else {
            CommandResult.error(res.exceptionOrNull()?.message ?: "cd failed")
        }
    }
}

class GrepCommand : Command {
    override val name: String = "grep"
    override val description: String = "Filters text lines matching a regular expression or string"
    override val usage: String = "grep [-i] [-v] <pattern> [file...]"
    override val category: CommandCategory = CommandCategory.UTILITY
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Pattern Search Filter",
        synopsis = "grep [-i] [-v] <pattern> [file...]",
        options = listOf(
            "-i" to "Ignore case distinctions in pattern and input",
            "-v, --invert-match" to "Invert match: select non-matching lines"
        ),
        description = "Searches for matching lines from standard input (via pipe '|') or from files. Compatible with DreamShell pipelines.",
        examples = listOf(
            "pkg list | grep beta" to "Filter packages in beta status",
            "cat notes.txt | grep -i dreambyte" to "Find occurrences of dreambyte"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        var ignoreCase = false
        var invert = false
        var pattern: String? = null
        val files = mutableListOf<String>()

        for (arg in args) {
            when {
                arg == "-i" -> ignoreCase = true
                arg == "-v" || arg == "--invert-match" -> invert = true
                pattern == null && !arg.startsWith("-") -> pattern = arg
                !arg.startsWith("-") -> files.add(arg)
            }
        }

        if (pattern == null) {
            return CommandResult.error("grep: missing search pattern\nUsage: grep [-i] [-v] <pattern> [file...]")
        }

        val inputLines = if (files.isEmpty()) {
            context.stdin.lines()
        } else {
            val lines = mutableListOf<String>()
            for (f in files) {
                val resolved = context.shellEnv.resolvePath(f)
                if (resolved.exists() && resolved.isFile) {
                    lines.addAll(resolved.readLines())
                } else {
                    return CommandResult.error("grep: $f: No such file or directory")
                }
            }
            lines
        }

        val matched = inputLines.filter { line ->
            val contains = line.contains(pattern, ignoreCase = ignoreCase)
            if (invert) !contains else contains
        }

        return if (matched.isEmpty()) {
            CommandResult(output = "", exitCode = 1)
        } else {
            CommandResult.success(matched.joinToString("\n"))
        }
    }
}

class CatCommand : Command {
    override val name: String = "cat"
    override val description: String = "Concatenates files and prints on the standard output"
    override val usage: String = "cat [file...]"
    override val category: CommandCategory = CommandCategory.UTILITY
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Concatenate and Display Files",
        synopsis = "cat [file...]",
        description = "Reads files sequentially and writes them to standard output. If no files are given, prints standard input.",
        examples = listOf(
            "cat file.txt" to "Display contents of file.txt",
            "cat file1.txt file2.txt > combined.txt" to "Merge two files into combined.txt"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.success(context.stdin)
        }

        val sb = StringBuilder()
        for (arg in args) {
            val file = context.shellEnv.resolvePath(arg)
            if (!file.exists() || file.isDirectory) {
                return CommandResult.error("cat: $arg: No such file or directory")
            }
            if (!context.shellEnv.isPathWithinSandbox(file)) {
                return CommandResult.error("cat: $arg: Permission denied by sandbox")
            }
            sb.append(file.readText())
        }
        return CommandResult.success(sb.toString().trimEnd())
    }
}

class WcCommand : Command {
    override val name: String = "wc"
    override val description: String = "Prints newline, word, and byte counts for text"
    override val usage: String = "wc [-l] [-w] [-c] [file...]"
    override val category: CommandCategory = CommandCategory.UTILITY
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Word, Line, Character Counter",
        synopsis = "wc [options] [file...]",
        options = listOf(
            "-l" to "Print newline count",
            "-w" to "Print word count",
            "-c" to "Print byte count"
        ),
        description = "Computes text metrics from standard input or files.",
        examples = listOf("pkg list | wc -l" to "Count number of package lines")
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val flags = args.filter { it.startsWith("-") }
        val fileArgs = args.filter { !it.startsWith("-") }

        val text = if (fileArgs.isEmpty()) {
            context.stdin
        } else {
            val file = context.shellEnv.resolvePath(fileArgs[0])
            if (!file.exists()) return CommandResult.error("wc: ${fileArgs[0]}: No such file or directory")
            file.readText()
        }

        val lines = if (text.isEmpty()) 0 else text.lines().size
        val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        val bytes = text.toByteArray().size

        val showAll = flags.isEmpty()
        val sb = StringBuilder()
        if (showAll || flags.contains("-l")) sb.append("$lines ")
        if (showAll || flags.contains("-w")) sb.append("$words ")
        if (showAll || flags.contains("-c")) sb.append("$bytes ")

        return CommandResult.success(sb.toString().trim())
    }
}

class ExportCommand : Command {
    override val name: String = "export"
    override val description: String = "Sets and exports DreamShell environment variables"
    override val usage: String = "export [NAME=value...]"
    override val category: CommandCategory = CommandCategory.SHELL
    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Export Shell Variables",
        synopsis = "export [NAME=value]",
        description = "Sets environment variables for the DreamShell session. If invoked without arguments, lists all variables.",
        examples = listOf(
            "export MY_VAR=hello" to "Set MY_VAR to hello",
            "export" to "List all active variables"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            val vars = context.shellEnv.getAllVariables()
            val text = vars.entries.sortedBy { it.key }
                .joinToString("\n") { "export ${it.key}=\"${it.value}\"" }
            return CommandResult.success(text)
        }

        for (arg in args) {
            if (arg.contains("=")) {
                val key = arg.substringBefore("=").trim()
                val value = arg.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                context.shellEnv.setVariable(key, value)
            }
        }
        return CommandResult.success("")
    }
}
