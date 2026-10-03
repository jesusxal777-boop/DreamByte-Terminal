package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandAction
import com.example.model.CommandResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilesCommand : Command {
    override val name: String = "files"
    override val description: String = "Inspects and manages files safely inside Android application sandbox"
    override val aliases: List<String> = listOf("filemanager", "fs")
    override val usage: String = "files [list | read <file> | write <file> <text> | delete <file> | mkdir <dir> | open]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Sandboxed File Management Utility",
        synopsis = "files <subcommand> [path] [arguments...]",
        options = listOf(
            "list [path]" to "List files in directory with size and modification date",
            "read <file>" to "Display text contents of the specified file",
            "write <file> <content>" to "Write text content into the specified file",
            "delete <file>" to "Delete the specified file or empty directory",
            "mkdir <dir>" to "Create a new directory inside sandbox",
            "open" to "Trigger Android system document/file picker intent"
        ),
        description = "Provides real filesystem operations within the Android application sandbox. Prevents directory traversal outside allowed paths and interacts directly with internal storage.",
        examples = listOf(
            "files list" to "List contents of active directory",
            "files write notes.txt 'DreamByte OS'" to "Create notes.txt with text",
            "files read notes.txt" to "Read notes.txt",
            "files mkdir scripts" to "Create a new subdirectory",
            "files delete notes.txt" to "Remove notes.txt"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty() || args[0].lowercase() == "open") {
            context.openSystemFiles()
            return CommandResult(
                output = "Opening system file explorer via Android Intent...",
                action = CommandAction.OPEN_FILES
            )
        }

        val subCmd = args[0].lowercase()
        val shellEnv = context.shellEnv

        return when (subCmd) {
            "list", "ls" -> {
                val targetDir = if (args.size > 1) {
                    shellEnv.resolvePath(args[1])
                } else {
                    shellEnv.currentDirectory
                }

                if (!targetDir.exists()) {
                    return CommandResult.error("files list: cannot access '${targetDir.name}': No such file or directory")
                }
                if (!targetDir.isDirectory) {
                    return CommandResult.error("files list: '${targetDir.name}' is not a directory")
                }
                if (!shellEnv.isPathWithinSandbox(targetDir)) {
                    return CommandResult.error("files list: access restricted by Android sandbox.")
                }

                val files = targetDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val sb = StringBuilder()
                sb.appendLine("DIRECTORY: ${targetDir.absolutePath} (${files.size} items)")
                sb.appendLine("───────────────────────────────────────────")
                if (files.isEmpty()) {
                    sb.appendLine("  (empty)")
                } else {
                    files.forEach { f ->
                        val type = if (f.isDirectory) "DIR " else "FILE"
                        val sizeStr = if (f.isDirectory) "-" else "${f.length()} B"
                        val dateStr = sdf.format(Date(f.lastModified()))
                        sb.appendLine("  [$type] ${f.name.padEnd(24)} ${sizeStr.padStart(10)}  $dateStr")
                    }
                }
                CommandResult.success(sb.toString().trimEnd())
            }

            "read", "cat" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: files read <filename>")
                }
                val file = shellEnv.resolvePath(args[1])
                if (!file.exists() || file.isDirectory) {
                    return CommandResult.error("files read: '${args[1]}': No such file or directory")
                }
                if (!shellEnv.isPathWithinSandbox(file)) {
                    return CommandResult.error("files read: access restricted by Android sandbox.")
                }
                CommandResult.success(file.readText())
            }

            "write" -> {
                if (args.size < 3) {
                    return CommandResult.error("Usage: files write <filename> <content...>")
                }
                val file = shellEnv.resolvePath(args[1])
                if (!shellEnv.isPathWithinSandbox(file)) {
                    return CommandResult.error("files write: access restricted by Android sandbox.")
                }
                val content = args.drop(2).joinToString(" ")
                file.parentFile?.mkdirs()
                file.writeText(content)
                CommandResult.success("Saved content to '${file.name}' (${content.length} characters written).")
            }

            "delete", "rm" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: files delete <filename>")
                }
                val file = shellEnv.resolvePath(args[1])
                if (!file.exists()) {
                    return CommandResult.error("files delete: '${args[1]}': No such file or directory")
                }
                if (!shellEnv.isPathWithinSandbox(file)) {
                    return CommandResult.error("files delete: access restricted by Android sandbox.")
                }

                val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (deleted) {
                    CommandResult.success("Removed '${file.name}'.")
                } else {
                    CommandResult.error("files delete: failed to remove '${file.name}'.")
                }
            }

            "mkdir" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: files mkdir <directory_name>")
                }
                val dir = shellEnv.resolvePath(args[1])
                if (!shellEnv.isPathWithinSandbox(dir)) {
                    return CommandResult.error("files mkdir: access restricted by Android sandbox.")
                }
                if (dir.exists()) {
                    return CommandResult.error("files mkdir: cannot create directory '${args[1]}': File exists")
                }
                if (dir.mkdirs()) {
                    CommandResult.success("Created directory '${dir.name}' at ${dir.absolutePath}.")
                } else {
                    CommandResult.error("files mkdir: failed to create directory '${args[1]}'.")
                }
            }

            else -> {
                CommandResult.error(
                    "files: unknown subcommand '$subCmd'.\n" +
                    "Usage: files [list | read <file> | write <file> <content> | delete <file> | mkdir <dir> | open]\n" +
                    "Type 'help files' for details."
                )
            }
        }
    }
}
