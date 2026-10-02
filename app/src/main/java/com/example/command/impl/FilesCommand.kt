package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult
import java.io.File

class FilesCommand : Command {
    override val name: String = "files"
    override val description: String = "Opens system file manager or inspects app storage safely"
    override val aliases: List<String> = listOf("filemanager", "explorer")
    override val usage: String = "files [open | list | cat <file> | write <file> <text>]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty() || args[0] == "open") {
            context.openSystemFiles()
            return CommandResult(
                output = "Opening system file explorer...",
                action = CommandAction.OPEN_FILES
            )
        }

        val subCmd = args[0].lowercase()
        val filesDir = context.androidContext.filesDir

        return when (subCmd) {
            "list", "ls" -> {
                val files = filesDir.listFiles() ?: emptyArray()
                if (files.isEmpty()) {
                    CommandResult.success("Sandbox directory (${filesDir.name}/): Empty")
                } else {
                    val sb = StringBuilder()
                    sb.appendLine("SANDBOX FILES (${files.size} items in /${filesDir.name})")
                    sb.appendLine("───────────────────────────────────────────")
                    files.forEach { file ->
                        val size = file.length()
                        val type = if (file.isDirectory) "DIR " else "FILE"
                        sb.appendLine("  [$type] ${file.name.padEnd(20)} ${size}B")
                    }
                    CommandResult.success(sb.toString())
                }
            }
            "cat", "read" -> {
                if (args.size < 2) {
                    CommandResult.error("Usage: files cat <filename>")
                } else {
                    val targetFile = File(filesDir, args[1])
                    if (!targetFile.exists() || targetFile.isDirectory) {
                        CommandResult.error("File '${args[1]}' does not exist in sandbox.")
                    } else {
                        CommandResult.success(targetFile.readText())
                    }
                }
            }
            "write", "create" -> {
                if (args.size < 3) {
                    CommandResult.error("Usage: files write <filename> <content...>")
                } else {
                    val filename = args[1]
                    val content = args.drop(2).joinToString(" ")
                    val targetFile = File(filesDir, filename)
                    targetFile.writeText(content)
                    CommandResult.success("Saved content to sandbox file: $filename (${content.length} bytes)")
                }
            }
            else -> {
                CommandResult.error("Unknown files subcommand '$subCmd'. Usage: files [open | list | cat <file> | write <file> <text>]")
            }
        }
    }
}
