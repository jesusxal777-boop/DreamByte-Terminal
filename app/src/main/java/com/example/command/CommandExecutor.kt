package com.example.command

import com.example.model.CommandResult
import com.example.model.LineType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandExecutor(
    private val registry: CommandRegistry
) {
    suspend fun executeInput(
        rawInput: String,
        context: CommandContext
    ): List<TerminalLine> = withContext(Dispatchers.Default) {
        val resultLines = mutableListOf<TerminalLine>()
        val trimmed = rawInput.trim()

        if (trimmed.isEmpty()) {
            return@withContext emptyList()
        }

        val pipeline = CommandParser.parseLine(trimmed, context.settings)
        if (pipeline.commands.isEmpty()) {
            return@withContext emptyList()
        }

        var shouldContinue = true

        for (step in pipeline.commands) {
            if (!shouldContinue) break

            val parsed = step.parsed
            val command = registry.findCommand(parsed.commandName)

            val cmdResult: CommandResult = if (command == null) {
                CommandResult.error("${parsed.commandName}: command not found. Type 'help' for available commands.")
            } else {
                try {
                    command.execute(context, parsed.arguments)
                } catch (e: Exception) {
                    CommandResult.error("Execution error: ${e.localizedMessage ?: "Unknown error"}")
                }
            }

            // Append output lines
            if (cmdResult.output.isNotBlank()) {
                val lines = cmdResult.output.lines()
                lines.forEach { line ->
                    resultLines.add(
                        TerminalLine(
                            text = line,
                            type = cmdResult.type
                        )
                    )
                }
            }

            // Check chaining operators
            when (step.operator) {
                PipelineOperator.AND_THEN -> {
                    if (cmdResult.exitCode != 0) {
                        shouldContinue = false
                    }
                }
                PipelineOperator.THEN -> {
                    // Always continue
                    shouldContinue = true
                }
                PipelineOperator.END -> {
                    // Last step
                }
            }
        }

        return@withContext resultLines
    }
}
