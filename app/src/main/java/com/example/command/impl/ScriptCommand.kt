package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult
import kotlinx.coroutines.delay
import java.io.File

class ScriptCommand : Command {
    override val name: String = "sh"
    override val description: String = "Executes shell script commands or script files"
    override val aliases: List<String> = listOf("bash", "script")
    override val usage: String = "sh [-c \"commands\"] | [file.sh] | [demo]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult.error("Usage: sh [-c \"commands\"] | [file.sh] | [demo]")
        }

        val scriptContent: String = when {
            args[0] == "-c" && args.size > 1 -> {
                args.drop(1).joinToString(" ")
            }
            args[0] == "demo" -> {
                """
                # DreamByte OS M Diagnostic Script
                echo [1/3] Initializing DreamByte Core Diagnostics...
                dream version
                echo [2/3] Checking system telemetry...
                dream info
                echo [3/3] Checking theme status...
                theme
                echo Diagnostics completed successfully!
                """.trimIndent()
            }
            else -> {
                val fileName = args[0]
                val file = File(context.androidContext.filesDir, fileName)
                if (!file.exists()) {
                    return CommandResult.error("Script file not found: $fileName (Use 'files write $fileName <content>' to create it)")
                }
                file.readText()
            }
        }

        val lines = scriptContent.lines()
        val sb = StringBuilder()
        sb.appendLine("Executing script (${lines.size} lines)...")
        sb.appendLine("───────────────────────────────────────────")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            sb.appendLine("> $trimmed")
            val res = context.executeNestedCommand(trimmed)
            if (res.output.isNotBlank()) {
                sb.appendLine(res.output)
            }
            if (res.exitCode != 0) {
                sb.appendLine("[SCRIPT HALTED]: Exit code ${res.exitCode}")
                return CommandResult.error(sb.toString(), exitCode = res.exitCode)
            }
            delay(50) // Non-blocking smooth execution delay
        }

        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("[SCRIPT FINISHED]: All commands executed successfully.")
        return CommandResult.success(sb.toString())
    }
}
