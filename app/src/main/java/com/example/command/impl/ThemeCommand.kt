package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandAction
import com.example.model.CommandResult
import com.example.model.ThemeMode

class ThemeCommand : Command {
    override val name: String = "theme"
    override val description: String = "Queries or changes the visual theme"
    override val usage: String = "theme [liquid | holo | retro]"
    override val category: CommandCategory = CommandCategory.DREAMBYTE

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            val current = context.settings.theme
            return CommandResult.success(
                """
                Current Theme: ${current.displayName} (${current.id})

                Available Themes:
                  • liquid : Liquid Glass (translucent glossy panels, cyan highlights)
                  • holo   : Holo (high-tech neon holographic cyberpunk interface)
                  • retro  : Retro Terminal (phosphor amber-green CRT console)

                Use 'theme <name>' to switch.
                """.trimIndent()
            )
        }

        val target = args[0].lowercase()
        val matched = when (target) {
            "liquid", "glass", "liquid_glass" -> ThemeMode.LIQUID_GLASS
            "holo", "holographic", "cyber" -> ThemeMode.HOLO
            "retro", "crt", "vintage" -> ThemeMode.RETRO_TERMINAL
            else -> null
        }

        return if (matched != null) {
            context.onThemeChange(matched)
            CommandResult(
                output = "Theme switched to ${matched.displayName}.",
                action = CommandAction.CHANGE_THEME(matched)
            )
        } else {
            CommandResult.error(
                "Invalid theme '$target'. Choose from: liquid, holo, retro"
            )
        }
    }
}
