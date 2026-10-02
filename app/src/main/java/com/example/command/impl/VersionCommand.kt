package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult

class VersionCommand : Command {
    override val name: String = "version"
    override val description: String = "Displays the current application and system version"
    override val aliases: List<String> = listOf("-v", "--version")
    override val usage: String = "version"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val text = """
            DreamByte Terminal v0.1.0 (Build 100)
            Target Architecture : DreamByte OS M Mobile Preview
            Runtime Environment : Android API ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})
            Kernel              : Linux ${System.getProperty("os.version") ?: "unknown"}
            Dreambyte Shell     : dbsh v1.0.2
        """.trimIndent()
        return CommandResult.success(text)
    }
}
