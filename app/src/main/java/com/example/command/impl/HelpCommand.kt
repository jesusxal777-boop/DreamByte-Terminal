package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult

class HelpCommand : Command {
    override val name: String = "help"
    override val description: String = "Shows available commands or details for a specific command"
    override val aliases: List<String> = listOf("?", "commands")
    override val usage: String = "help [command]"
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isNotEmpty()) {
            val target = args[0].lowercase()
            val cmd = context.registry.findCommand(target)
            return if (cmd != null) {
                val privilegesText = if (cmd.requiresOsPrivileges) " (Requires DreamByte OS Privileges)" else ""
                CommandResult.success(
                    """
                    COMMAND: ${cmd.name}$privilegesText
                    Category: ${cmd.category.label}
                    Usage:    ${cmd.usage}
                    Description: ${cmd.description}
                    Aliases:  ${if (cmd.aliases.isEmpty()) "None" else cmd.aliases.joinToString(", ")}
                    """.trimIndent()
                )
            } else {
                CommandResult.error("help: no manual entry for '$target'")
            }
        }

        val sb = StringBuilder()
        sb.appendLine("DREAMBYTE TERMINAL - Available Commands")
        sb.appendLine("───────────────────────────────────────────")

        val grouped = context.registry.getAllCommands().groupBy { it.category }
        for (category in CommandCategory.entries) {
            val list = grouped[category] ?: continue
            sb.appendLine("\n[ ${category.label} ]")
            list.sortedBy { it.name }.forEach { cmd ->
                val pad = cmd.name.padEnd(12, ' ')
                val priv = if (cmd.requiresOsPrivileges) " 🔒" else ""
                sb.appendLine("  $pad$priv- ${cmd.description}")
            }
        }

        sb.appendLine("\nTip: Type 'help <command>' for specific syntax or 'dream help' for DreamByte OS commands.")
        return CommandResult.success(sb.toString())
    }
}
