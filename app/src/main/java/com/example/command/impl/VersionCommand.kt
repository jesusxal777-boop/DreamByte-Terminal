package com.example.command.impl

import android.os.Build
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult

class VersionCommand : Command {
    override val name: String = "version"
    override val description: String = "Displays the current application and system version"
    override val aliases: List<String> = listOf("-v", "--version")
    override val usage: String = "version"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Show DreamByte Terminal and OS M Version",
        synopsis = "version",
        description = "Outputs product version, build timestamp, target Android SDK, ART runtime and DreamByte Core Engine specifications.",
        examples = listOf("version" to "Show system build version")
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val text = """
            DreamByte Terminal v0.1.0 (Build 100)
            Target Platform     : DreamByte OS M (Mobile Edition)
            Application ID      : com.dreambyte.terminal
            Host Architecture   : ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}
            Android Runtime     : API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})
            Kernel              : Linux ${System.getProperty("os.version") ?: "unknown"}
            DreamShell Engine   : dbsh v1.2.0-native
            Package Subsystem   : dbpkg-1 / format-v1
        """.trimIndent()
        return CommandResult.success(text)
    }
}
