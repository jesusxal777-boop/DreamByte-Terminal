package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult

class WhoamiCommand : Command {
    override val name: String = "whoami"
    override val description: String = "Displays or modifies the active terminal user"
    override val aliases: List<String> = listOf("user")
    override val usage: String = "whoami [set <new_username>]"
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.success(context.settings.username)
        }

        val targetUser = if (args[0] == "set" && args.size > 1) {
            args[1]
        } else {
            args[0]
        }

        val sanitized = targetUser.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        if (sanitized.isEmpty()) {
            return CommandResult.error("Invalid username. Use alphanumeric characters.")
        }

        context.onUserChange(sanitized)
        return CommandResult(
            output = "User changed to '$sanitized'. Terminal prompt updated.",
            action = CommandAction.CHANGE_USER(sanitized)
        )
    }
}
