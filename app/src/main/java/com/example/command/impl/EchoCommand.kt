package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult

class EchoCommand : Command {
    override val name: String = "echo"
    override val description: String = "Prints arguments back to the terminal"
    override val usage: String = "echo [text...]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val output = args.joinToString(" ")
        return CommandResult.success(output)
    }
}
