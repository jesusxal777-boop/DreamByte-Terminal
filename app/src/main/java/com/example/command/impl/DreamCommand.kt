package com.example.command.impl

import android.os.BatteryManager
import android.os.Build
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandAction
import com.example.model.CommandResult

class DreamCommand : Command {
    override val name: String = "dream"
    override val description: String = "DreamByte OS core namespace commands"
    override val aliases: List<String> = listOf("dreambyte", "db")
    override val usage: String = "dream [help | about | info | version | mascot]"
    override val category: CommandCategory = CommandCategory.DREAMBYTE

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "DreamByte OS Core Namespace",
        synopsis = "dream <subcommand>",
        options = listOf(
            "help" to "List core DreamByte namespace commands",
            "about" to "Display DreamByte Studios vision and philosophy",
            "info" to "Display live device telemetry, memory, battery and status",
            "version" to "Display DreamByte OS M preview edition specifications",
            "mascot" to "Showcase the official DreamByte Cloud Robot mascot"
        ),
        description = "Provides access to the proprietary subsystem namespace of DreamByte OS M and DreamByte Studios.",
        examples = listOf(
            "dream help" to "Show subcommands",
            "dream info" to "Check system diagnostics",
            "dream mascot" to "View the official robot mascot card"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty() || args[0].lowercase() == "help") {
            val helpText = """
                DreamByte commands:
                  dream about
                  dream info
                  dream version
                  dream mascot
            """.trimIndent()
            return CommandResult.success(helpText)
        }

        return when (args[0].lowercase()) {
            "about" -> {
                val text = """
                    DreamByte Studios
                    ───────────────────────────────────────────
                    Vision: Creating the next generation of 
                    holographic, liquid-smooth mobile operating
                    environments where human creativity meets 
                    crystalline computational elegance.

                    Status: DreamByte OS M - Core Development
                    Design: Liquid Glass & Electric Hologram
                    Official Mascot: DreamByte Cloud Bot ☁️🤖
                """.trimIndent()
                CommandResult.success(text)
            }
            "info" -> {
                val runtime = Runtime.getRuntime()
                val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val totalMemoryMB = runtime.maxMemory() / (1024 * 1024)
                val bm = context.androidContext.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

                val text = """
                    DREAMBYTE OS M - TELEMETRY & DIAGNOSTICS
                    ───────────────────────────────────────────
                    Device Model   : ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}
                    Hardware       : ${Build.HARDWARE} (${Build.BOARD})
                    JVM Memory     : $usedMemoryMB MB used / $totalMemoryMB MB max
                    Battery Level  : ${if (batteryLevel >= 0) "$batteryLevel%" else "Unknown"}
                    Core State     : NOMINAL
                    Security Layer : Sandbox Enforced (Safe Mode: ${if (context.settings.safeMode) "ACTIVE" else "OFF"})
                    Active Theme   : ${context.settings.theme.displayName}
                """.trimIndent()
                CommandResult.success(text)
            }
            "version" -> {
                val text = """
                    DreamByte OS M Preview Edition
                    Kernel Interface: Android Native Subsystem v36
                    Architecture: 64-bit ARM/x86_64 Unified
                    Build ID: DB-2026.10-M01-RELEASE
                """.trimIndent()
                CommandResult.success(text)
            }
            "mascot", "bot", "robot" -> {
                context.showMascotDialog()
                val asciiArt = """
                     .-----.
                   /  o   o  \   DreamByte Cloud Bot
                  |   .___.   |  Official System Mascot
                  |  [ .-. ]  |  "Floating dreams in every byte!"
                   \   '-'   /
                    '-...---'
                """.trimIndent()
                CommandResult(
                    output = "$asciiArt\nOpening mascot visual card...",
                    action = CommandAction.SHOW_MASCOT
                )
            }
            else -> {
                CommandResult.error(
                    "Unknown dream command '${args[0]}'. Type 'dream help' for available subcommands."
                )
            }
        }
    }
}
