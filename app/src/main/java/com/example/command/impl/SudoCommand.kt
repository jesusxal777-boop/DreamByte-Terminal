package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult

class SudoCommand : Command {
    override val name: String = "sudo"
    override val description: String = "Executes command with DreamByte OS M elevated privileges"
    override val usage: String = "sudo <command>"
    override val category: CommandCategory = CommandCategory.ADMIN
    override val requiresOsPrivileges: Boolean = true

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("usage: sudo <command>")
        }

        val targetCmd = args.joinToString(" ")

        // If Safe Mode is enabled (standard Android environment protection)
        if (context.settings.safeMode || !context.settings.systemTerminalEnabled) {
            return CommandResult.error(
                """
                Permission denied.
                This operation requires DreamByte OS system privileges.
                """.trimIndent()
            )
        }

        // System Terminal is enabled and Safe Mode is unlocked: Request OS Confirmation Dialog
        context.requestSystemOperation(
            "SYSTEM OPERATION",
            "This command will modify protected\nDreamByte OS system files.",
            "sudo $targetCmd"
        )

        return CommandResult(
            output = "Requesting DreamByte OS elevated authorization...",
            action = CommandAction.REQUEST_SYSTEM_OPERATION(
                operationName = "SYSTEM OPERATION",
                details = "This command will modify protected\nDreamByte OS system files.",
                command = "sudo $targetCmd"
            )
        )
    }
}
