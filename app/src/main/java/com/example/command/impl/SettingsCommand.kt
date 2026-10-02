package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult

class SettingsCommand : Command {
    override val name: String = "settings"
    override val description: String = "Opens visual terminal settings or displays current config"
    override val aliases: List<String> = listOf("config", "preferences")
    override val usage: String = "settings [open]"
    override val category: CommandCategory = CommandCategory.GENERAL

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty() || args[0].lowercase() == "open") {
            context.openSettings()
            return CommandResult(
                output = "Opening DreamByte Settings panel...",
                action = CommandAction.OPEN_SETTINGS
            )
        }

        val s = context.settings
        val text = """
            DREAMBYTE TERMINAL CONFIGURATION
            ───────────────────────────────────────────
            Theme              : ${s.theme.displayName} (${s.theme.id})
            Font Size          : ${s.fontSize.displayName}
            Glass Transparency : ${(s.terminalTransparency * 100).toInt()}%
            Sound Effects      : ${if (s.soundEffectsEnabled) "Enabled" else "Disabled"}
            Haptic Feedback    : ${if (s.hapticFeedbackEnabled) "Enabled" else "Disabled"}
            Show Timestamps    : ${if (s.showTimestamps) "Yes" else "No"}
            Active User        : ${s.username}
            Host Machine       : ${s.hostname}
            System Terminal    : ${if (s.systemTerminalEnabled) "Active" else "Inactive"}
            Safe Mode (OS M)   : ${if (s.safeMode) "Enforced" else "Off"}
            ───────────────────────────────────────────
            Use 'settings open' to open the graphical configuration UI.
        """.trimIndent()
        return CommandResult.success(text)
    }
}
