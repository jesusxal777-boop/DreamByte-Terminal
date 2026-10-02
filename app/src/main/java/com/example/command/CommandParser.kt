package com.example.command

import com.example.model.TerminalSettings

data class ParsedCommand(
    val commandName: String,
    val arguments: List<String>,
    val rawLine: String
)

data class ExecutionPipeline(
    val commands: List<PipelineStep>
)

data class PipelineStep(
    val parsed: ParsedCommand,
    val operator: PipelineOperator
)

enum class PipelineOperator {
    THEN,      // ;
    AND_THEN,  // &&
    END
}

object CommandParser {

    /**
     * Splits a multi-command line (handling ; and &&) into structured pipeline steps.
     */
    fun parseLine(input: String, settings: TerminalSettings): ExecutionPipeline {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return ExecutionPipeline(emptyList())
        }

        // Tokenize considering quotes
        val tokens = tokenizeRawLine(trimmed)
        val steps = mutableListOf<PipelineStep>()

        var currentArgs = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                ";" -> {
                    if (currentArgs.isNotEmpty()) {
                        steps.add(createStep(currentArgs, PipelineOperator.THEN, settings))
                        currentArgs = mutableListOf()
                    }
                }
                "&&" -> {
                    if (currentArgs.isNotEmpty()) {
                        steps.add(createStep(currentArgs, PipelineOperator.AND_THEN, settings))
                        currentArgs = mutableListOf()
                    }
                }
                else -> {
                    currentArgs.add(token)
                }
            }
            i++
        }

        if (currentArgs.isNotEmpty()) {
            steps.add(createStep(currentArgs, PipelineOperator.END, settings))
        }

        return ExecutionPipeline(steps)
    }

    private fun createStep(
        tokens: List<String>,
        operator: PipelineOperator,
        settings: TerminalSettings
    ): PipelineStep {
        val expandedTokens = tokens.map { expandVariables(it, settings) }
        val cmdName = expandedTokens.first().lowercase()
        val args = expandedTokens.drop(1)
        return PipelineStep(
            parsed = ParsedCommand(
                commandName = cmdName,
                arguments = args,
                rawLine = expandedTokens.joinToString(" ")
            ),
            operator = operator
        )
    }

    /**
     * Tokenizes a command line supporting single quotes and double quotes.
     */
    fun tokenizeRawLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inDoubleQuotes = false
        var inSingleQuotes = false
        var escape = false

        var i = 0
        while (i < line.length) {
            val c = line[i]

            if (escape) {
                sb.append(c)
                escape = false
                i++
                continue
            }

            if (c == '\\') {
                escape = true
                i++
                continue
            }

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes
                i++
                continue
            }

            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes
                i++
                continue
            }

            if (!inDoubleQuotes && !inSingleQuotes) {
                // Check operators ; or &&
                if (c == ';') {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    tokens.add(";")
                    i++
                    continue
                }
                if (c == '&' && i + 1 < line.length && line[i + 1] == '&') {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    tokens.add("&&")
                    i += 2
                    continue
                }
                if (c.isWhitespace()) {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    i++
                    continue
                }
            }

            sb.append(c)
            i++
        }

        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }

        return tokens
    }

    private fun expandVariables(token: String, settings: TerminalSettings): String {
        return token
            .replace("\$USER", settings.username)
            .replace("\$HOST", settings.hostname)
            .replace("\$THEME", settings.theme.id)
            .replace("\$VERSION", "0.1.0")
            .replace("\$OS", "DreamByte OS M")
    }
}
