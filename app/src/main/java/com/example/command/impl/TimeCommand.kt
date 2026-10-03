package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TimeCommand : Command {
    override val name: String = "time"
    override val description: String = "Displays the current system time and timezone"
    override val usage: String = "time"
    override val category: CommandCategory = CommandCategory.UTILITY

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Display Current Time and Timezone",
        synopsis = "time",
        description = "Shows the high-precision system time, seconds, and standard timezone indicator.",
        examples = listOf("time" to "Print current clock time")
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val sdf = SimpleDateFormat("HH:mm:ss z (Z)", Locale.getDefault())
        val tz = TimeZone.getDefault().displayName
        return CommandResult.success("${sdf.format(Date())} [$tz]")
    }
}
