package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
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

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val sdf = SimpleDateFormat("HH:mm:ss z (Z)", Locale.getDefault())
        val tz = TimeZone.getDefault().displayName
        return CommandResult.success("${sdf.format(Date())} [$tz]")
    }
}
