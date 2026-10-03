package com.example.shell

data class ShellRedirection(
    val type: RedirectionType,
    val target: String
)

enum class RedirectionType {
    WRITE,       // >
    APPEND,      // >>
    READ_INPUT   // <
}

data class SingleCommandSpec(
    val commandName: String,
    val arguments: List<String>,
    val redirections: List<ShellRedirection> = emptyList(),
    val rawString: String = ""
)

data class PipelineSpec(
    val commands: List<SingleCommandSpec> // commands connected by |
)

data class ExecutionNode(
    val pipeline: PipelineSpec,
    val operator: NodeOperator
)

enum class NodeOperator {
    THEN,     // ;
    AND_THEN, // &&
    OR_THEN,  // ||
    END
}

object DreamShellParser {

    /**
     * Parses a full DreamShell command string into a list of piped execution nodes.
     */
    fun parse(input: String, env: DreamShellEnvironment): List<ExecutionNode> {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return emptyList()
        }

        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) return emptyList()

        val nodes = mutableListOf<ExecutionNode>()
        var currentPipelineTokens = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                ";" -> {
                    if (currentPipelineTokens.isNotEmpty()) {
                        nodes.add(ExecutionNode(parsePipeline(currentPipelineTokens, env), NodeOperator.THEN))
                        currentPipelineTokens = mutableListOf()
                    }
                }
                "&&" -> {
                    if (currentPipelineTokens.isNotEmpty()) {
                        nodes.add(ExecutionNode(parsePipeline(currentPipelineTokens, env), NodeOperator.AND_THEN))
                        currentPipelineTokens = mutableListOf()
                    }
                }
                "||" -> {
                    if (currentPipelineTokens.isNotEmpty()) {
                        nodes.add(ExecutionNode(parsePipeline(currentPipelineTokens, env), NodeOperator.OR_THEN))
                        currentPipelineTokens = mutableListOf()
                    }
                }
                else -> {
                    currentPipelineTokens.add(token)
                }
            }
            i++
        }

        if (currentPipelineTokens.isNotEmpty()) {
            nodes.add(ExecutionNode(parsePipeline(currentPipelineTokens, env), NodeOperator.END))
        }

        return nodes
    }

    private fun parsePipeline(tokens: List<String>, env: DreamShellEnvironment): PipelineSpec {
        val commands = mutableListOf<SingleCommandSpec>()
        var currentCmdTokens = mutableListOf<String>()

        for (token in tokens) {
            if (token == "|") {
                if (currentCmdTokens.isNotEmpty()) {
                    commands.add(parseSingleCommand(currentCmdTokens, env))
                    currentCmdTokens = mutableListOf()
                }
            } else {
                currentCmdTokens.add(token)
            }
        }

        if (currentCmdTokens.isNotEmpty()) {
            commands.add(parseSingleCommand(currentCmdTokens, env))
        }

        return PipelineSpec(commands)
    }

    private fun parseSingleCommand(tokens: List<String>, env: DreamShellEnvironment): SingleCommandSpec {
        val rawArgs = mutableListOf<String>()
        val redirections = mutableListOf<ShellRedirection>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                ">" -> {
                    if (i + 1 < tokens.size) {
                        redirections.add(ShellRedirection(RedirectionType.WRITE, expandVariables(tokens[i + 1], env)))
                        i += 2
                        continue
                    }
                }
                ">>" -> {
                    if (i + 1 < tokens.size) {
                        redirections.add(ShellRedirection(RedirectionType.APPEND, expandVariables(tokens[i + 1], env)))
                        i += 2
                        continue
                    }
                }
                "<" -> {
                    if (i + 1 < tokens.size) {
                        redirections.add(ShellRedirection(RedirectionType.READ_INPUT, expandVariables(tokens[i + 1], env)))
                        i += 2
                        continue
                    }
                }
                else -> {
                    rawArgs.add(expandVariables(token, env))
                }
            }
            i++
        }

        if (rawArgs.isEmpty()) {
            return SingleCommandSpec("", emptyList(), redirections)
        }

        val cmdName = rawArgs.first()
        val args = rawArgs.drop(1)
        return SingleCommandSpec(cmdName, args, redirections, rawArgs.joinToString(" "))
    }

    /**
     * Tokenizer that respects single quotes, double quotes, and shell operators.
     */
    fun tokenize(line: String): List<String> {
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

            if (c == '\\' && !inSingleQuotes) {
                escape = true
                i++
                continue
            }

            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes
                sb.append(c) // preserve quote tag for expander
                i++
                continue
            }

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes
                sb.append(c) // preserve quote tag for expander
                i++
                continue
            }

            if (!inDoubleQuotes && !inSingleQuotes) {
                // Two-character operators
                if (c == '&' && i + 1 < line.length && line[i + 1] == '&') {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
                    tokens.add("&&")
                    i += 2
                    continue
                }
                if (c == '|' && i + 1 < line.length && line[i + 1] == '|') {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
                    tokens.add("||")
                    i += 2
                    continue
                }
                if (c == '>' && i + 1 < line.length && line[i + 1] == '>') {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
                    tokens.add(">>")
                    i += 2
                    continue
                }

                // Single character operators
                if (c == ';' || c == '|' || c == '>' || c == '<') {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
                    tokens.add(c.toString())
                    i++
                    continue
                }

                if (c.isWhitespace()) {
                    if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
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

    /**
     * Expands variables like $NAME or ${NAME}. Handles single quotes (literal) vs double quotes.
     */
    fun expandVariables(token: String, env: DreamShellEnvironment): String {
        // If wrapped in single quotes, remove quotes and do not expand
        if (token.startsWith("'") && token.endsWith("'") && token.length >= 2) {
            return token.substring(1, token.length - 1)
        }

        val content = if (token.startsWith("\"") && token.endsWith("\"") && token.length >= 2) {
            token.substring(1, token.length - 1)
        } else {
            token
        }

        if (!content.contains("$")) {
            return content
        }

        val result = StringBuilder()
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '$' && i + 1 < content.length) {
                val next = content[i + 1]
                if (next == '?') {
                    result.append(env.lastExitCode.toString())
                    i += 2
                    continue
                }
                if (next == '{') {
                    val closeIndex = content.indexOf('}', i + 2)
                    if (closeIndex != -1) {
                        val varName = content.substring(i + 2, closeIndex)
                        result.append(env.getVariable(varName) ?: "")
                        i = closeIndex + 1
                        continue
                    }
                }
                if (next.isLetter() || next == '_') {
                    var j = i + 1
                    while (j < content.length && (content[j].isLetterOrDigit() || content[j] == '_')) {
                        j++
                    }
                    val varName = content.substring(i + 1, j)
                    result.append(env.getVariable(varName) ?: "")
                    i = j
                    continue
                }
            }
            result.append(c)
            i++
        }

        return result.toString()
    }
}
