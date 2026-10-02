package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult

class DpmCommand : Command {
    override val name: String = "dpm"
    override val description: String = "DreamByte Package Manager for system packages"
    override val aliases: List<String> = listOf("pkg", "apt")
    override val usage: String = "dpm [install | update | remove] <pkg>"
    override val category: CommandCategory = CommandCategory.ADMIN
    override val requiresOsPrivileges: Boolean = true

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (context.settings.safeMode || !context.settings.systemTerminalEnabled) {
            return CommandResult.error(
                """
                Permission denied.
                This operation requires DreamByte OS system privileges.
                """.trimIndent()
            )
        }

        context.requestSystemOperation(
            "SYSTEM PACKAGE INSTALL",
            "This command will modify protected\nDreamByte OS system files.",
            "dpm ${args.joinToString(" ")}"
        )

        return CommandResult(
            output = "Requesting DreamByte OS package authorization...",
            action = CommandAction.REQUEST_SYSTEM_OPERATION(
                operationName = "SYSTEM PACKAGE INSTALL",
                details = "This command will modify protected\nDreamByte OS system files.",
                command = "dpm ${args.joinToString(" ")}"
            )
        )
    }
}
