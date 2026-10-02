package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult

class ClearCommand : Command {
    override val name: String = "clear"
    override val description: String = "Clears the terminal screen buffer"
    override val aliases: List<String> = listOf("cls")
    override val usage: String = "clear"
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        context.onClearScreen()
        return CommandResult(action = CommandAction.CLEAR_SCREEN)
    }
}
