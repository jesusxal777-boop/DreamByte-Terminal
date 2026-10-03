package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult

class HelpCommand : Command {
    override val name: String = "help"
    override val description: String = "Displays command reference and detailed manual pages"
    override val aliases: List<String> = listOf("?", "man")
    override val usage: String = "help [command]"
    override val category: CommandCategory = CommandCategory.GENERAL

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "DreamByte Terminal Help System",
        synopsis = "help [command]",
        options = listOf(
            "[command]" to "The specific command name to inspect (e.g. pkg, files, sh, wget, python)"
        ),
        description = "Provides comprehensive manuals with syntax, options, descriptions, and runnable examples for every built-in and installed command.",
        examples = listOf(
            "help pkg" to "Display full package manager manual",
            "help wget" to "Display network downloader options",
            "help files" to "Display sandboxed filesystem operations"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isNotEmpty()) {
            val target = args[0].lowercase()
            val cmd = context.registry.findCommand(target)
            return if (cmd != null) {
                val help = cmd.detailedHelp
                val sb = StringBuilder()
                sb.appendLine("DREAMBYTE MANUAL: ${cmd.name.uppercase()}")
                sb.appendLine("───────────────────────────────────────────")
                sb.appendLine("NAME")
                sb.appendLine("    ${cmd.name} - ${help?.summary ?: cmd.description}")
                sb.appendLine("\nSYNOPSIS")
                sb.appendLine("    ${help?.synopsis ?: cmd.usage}")

                sb.appendLine("\nDESCRIPTION")
                val desc = help?.description ?: cmd.description
                desc.lines().forEach { line -> sb.appendLine("    $line") }

                if (help != null && help.options.isNotEmpty()) {
                    sb.appendLine("\nOPTIONS & FLAGS")
                    help.options.forEach { (opt, optDesc) ->
                        sb.appendLine("    ${opt.padEnd(22)} $optDesc")
                    }
                }

                if (help != null && help.examples.isNotEmpty()) {
                    sb.appendLine("\nEXAMPLES")
                    help.examples.forEach { (eg, explanation) ->
                        sb.appendLine("    $ $eg")
                        sb.appendLine("        -> $explanation")
                    }
                }

                if (cmd.aliases.isNotEmpty()) {
                    sb.appendLine("\nALIASES: ${cmd.aliases.joinToString(", ")}")
                }

                CommandResult.success(sb.toString().trimEnd())
            } else {
                CommandResult.error("help: no manual entry for '$target'. Type 'help' to view all commands.")
            }
        }

        val sb = StringBuilder()
        sb.appendLine("DREAMBYTE TERMINAL - System Reference")
        sb.appendLine("───────────────────────────────────────────")

        val grouped = context.registry.getAllCommands().groupBy { it.category }
        for (category in CommandCategory.entries) {
            val list = grouped[category] ?: continue
            sb.appendLine("\n[ ${category.label} ]")
            list.sortedBy { it.name }.forEach { cmd ->
                val pad = cmd.name.padEnd(12, ' ')
                val priv = if (cmd.requiresOsPrivileges) " 🔒" else ""
                sb.appendLine("  $pad$priv - ${cmd.description}")
            }
        }

        sb.appendLine("\n───────────────────────────────────────────")
        sb.appendLine("Type 'help <command>' for manual pages (e.g. 'help pkg', 'help files', 'help wget', 'help python').")
        return CommandResult.success(sb.toString().trimEnd())
    }
}
