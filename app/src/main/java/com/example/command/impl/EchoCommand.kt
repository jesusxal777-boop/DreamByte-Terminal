package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult

class EchoCommand : Command {
    override val name: String = "echo"
    override val description: String = "Prints arguments back to the terminal"
    override val usage: String = "echo [text...]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Display Message or Variable",
        synopsis = "echo [arg ...]",
        description = "Outputs the string representations of arguments separated by spaces. Supports environment variables (e.g. \$USER, \$PWD, \$OS) and redirection (e.g. echo hello > file.txt).",
        examples = listOf(
            "echo Hello DreamByte" to "Prints Hello DreamByte",
            "echo Active User: \$USER" to "Expands \$USER variable",
            "echo Status code: \$?" to "Expands last exit code"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val output = args.joinToString(" ")
        return CommandResult.success(output)
    }
}
