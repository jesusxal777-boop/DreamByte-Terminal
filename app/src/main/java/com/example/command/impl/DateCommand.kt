package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateCommand : Command {
    override val name: String = "date"
    override val description: String = "Displays the current system date"
    override val usage: String = "date"
    override val category: CommandCategory = CommandCategory.UTILITY

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Display Current Date",
        synopsis = "date",
        description = "Shows the active local date formatted with day of week, month, day and year.",
        examples = listOf("date" to "Print today's date")
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return CommandResult.success(sdf.format(Date()))
    }
}
