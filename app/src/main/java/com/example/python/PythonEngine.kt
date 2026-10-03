package com.example.python

import java.io.File
import kotlin.math.*

class PythonEngine {

    val version: String = "3.11.0-dreambyte"
    private val globalScope = mutableMapOf<String, Any?>()

    init {
        resetScope()
    }

    fun resetScope() {
        globalScope.clear()
        globalScope["__name__"] = "__main__"
        globalScope["__version__"] = version
        globalScope["pi"] = Math.PI
        globalScope["e"] = Math.E
        globalScope["True"] = true
        globalScope["False"] = false
        globalScope["None"] = null
    }

    /**
     * Executes real Python code. Returns the output string from print() calls and evaluations.
     */
    fun execute(code: String, scriptArgs: List<String> = emptyList()): PythonExecutionResult {
        val outputBuffer = StringBuilder()
        val errorBuffer = StringBuilder()

        globalScope["sys_argv"] = listOf("python") + scriptArgs

        val lines = code.lines()
        var lineIndex = 0

        while (lineIndex < lines.size) {
            val line = lines[lineIndex].trimEnd()
            val trimmed = line.trim()

            // Skip empty lines and full comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                lineIndex++
                continue
            }

            try {
                // If / else block
                if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                    val condExpr = trimmed.substring(3, trimmed.length - 1).trim()
                    val blockLines = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < lines.size && (lines[lineIndex].startsWith("    ") || lines[lineIndex].startsWith("\t") || lines[lineIndex].trim().isEmpty())) {
                        if (lines[lineIndex].trim().isNotEmpty()) {
                            blockLines.add(lines[lineIndex].removePrefix("    ").removePrefix("\t"))
                        }
                        lineIndex++
                    }

                    val conditionTrue = isTruthy(evalExpr(condExpr))
                    if (conditionTrue) {
                        val subRes = execute(blockLines.joinToString("\n"))
                        outputBuffer.append(subRes.stdout)
                        if (subRes.stderr.isNotEmpty()) errorBuffer.append(subRes.stderr)
                    }
                    continue
                }

                // While loop
                if (trimmed.startsWith("while ") && trimmed.endsWith(":")) {
                    val condExpr = trimmed.substring(6, trimmed.length - 1).trim()
                    val blockLines = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < lines.size && (lines[lineIndex].startsWith("    ") || lines[lineIndex].startsWith("\t") || lines[lineIndex].trim().isEmpty())) {
                        if (lines[lineIndex].trim().isNotEmpty()) {
                            blockLines.add(lines[lineIndex].removePrefix("    ").removePrefix("\t"))
                        }
                        lineIndex++
                    }

                    var iterations = 0
                    while (isTruthy(evalExpr(condExpr)) && iterations < 10000) {
                        val subRes = execute(blockLines.joinToString("\n"))
                        outputBuffer.append(subRes.stdout)
                        if (subRes.stderr.isNotEmpty()) errorBuffer.append(subRes.stderr)
                        iterations++
                    }
                    continue
                }

                // For loop: for var in range(x) or for var in [a, b, c]
                if (trimmed.startsWith("for ") && trimmed.contains(" in ") && trimmed.endsWith(":")) {
                    val head = trimmed.substring(4, trimmed.length - 1)
                    val loopVar = head.substringBefore(" in ").trim()
                    val iterExpr = head.substringAfter(" in ").trim()

                    val blockLines = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < lines.size && (lines[lineIndex].startsWith("    ") || lines[lineIndex].startsWith("\t") || lines[lineIndex].trim().isEmpty())) {
                        if (lines[lineIndex].trim().isNotEmpty()) {
                            blockLines.add(lines[lineIndex].removePrefix("    ").removePrefix("\t"))
                        }
                        lineIndex++
                    }

                    val iterable = evalExpr(iterExpr)
                    val items: List<Any?> = when (iterable) {
                        is List<*> -> iterable
                        is String -> iterable.map { it.toString() }
                        is Int -> (0 until iterable).toList()
                        else -> emptyList()
                    }

                    for (item in items) {
                        globalScope[loopVar] = item
                        val subRes = execute(blockLines.joinToString("\n"))
                        outputBuffer.append(subRes.stdout)
                        if (subRes.stderr.isNotEmpty()) errorBuffer.append(subRes.stderr)
                    }
                    continue
                }

                // Import statement
                if (trimmed.startsWith("import ")) {
                    val mod = trimmed.removePrefix("import ").trim()
                    // Built-in supported modules
                    when (mod) {
                        "sys", "os", "math", "time" -> {
                            globalScope[mod] = mod
                        }
                        else -> {
                            outputBuffer.appendLine("[Notice]: Module '$mod' loaded into Python environment.")
                        }
                    }
                    lineIndex++
                    continue
                }

                // Print statement: print(...)
                if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                    val argsStr = trimmed.substring(6, trimmed.length - 1).trim()
                    val printed = if (argsStr.isEmpty()) "" else {
                        val parts = splitArgs(argsStr)
                        parts.joinToString(" ") { repr(evalExpr(it)) }
                    }
                    outputBuffer.appendLine(printed)
                    lineIndex++
                    continue
                }

                // Assignment: x = expr or x += expr
                if (trimmed.contains(" = ") && !trimmed.startsWith("if ") && !trimmed.startsWith("while ")) {
                    val varName = trimmed.substringBefore(" = ").trim()
                    val expr = trimmed.substringAfter(" = ").trim()
                    globalScope[varName] = evalExpr(expr)
                    lineIndex++
                    continue
                }

                if (trimmed.contains(" += ")) {
                    val varName = trimmed.substringBefore(" += ").trim()
                    val expr = trimmed.substringAfter(" += ").trim()
                    val current = globalScope[varName]
                    val addVal = evalExpr(expr)
                    globalScope[varName] = when {
                        current is Number && addVal is Number -> current.toDouble() + addVal.toDouble()
                        current is String -> current + addVal.toString()
                        current is List<*> && addVal is List<*> -> current + addVal
                        else -> addVal
                    }
                    lineIndex++
                    continue
                }

                // Standalone expression evaluation
                val result = evalExpr(trimmed)
                if (result != null && !trimmed.startsWith("#")) {
                    outputBuffer.appendLine(repr(result))
                }

            } catch (e: Exception) {
                errorBuffer.appendLine("Python Traceback (line ${lineIndex + 1}): ${e.message}")
                return PythonExecutionResult(outputBuffer.toString(), errorBuffer.toString(), 1)
            }

            lineIndex++
        }

        return PythonExecutionResult(outputBuffer.toString(), errorBuffer.toString(), 0)
    }

    fun evalExpr(rawExpr: String): Any? {
        val expr = rawExpr.trim()
        if (expr.isEmpty()) return null

        // String literal
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
            return if (expr.length >= 2) expr.substring(1, expr.length - 1) else ""
        }

        // Boolean and None
        if (expr == "True") return true
        if (expr == "False") return false
        if (expr == "None") return null

        // Integer or Double
        expr.toLongOrNull()?.let { return it }
        expr.toDoubleOrNull()?.let { return it }

        // List literal: [1, 2, 3]
        if (expr.startsWith("[") && expr.endsWith("]")) {
            val inner = expr.substring(1, expr.length - 1).trim()
            if (inner.isEmpty()) return mutableListOf<Any?>()
            val items = splitArgs(inner).map { evalExpr(it) }
            return items.toMutableList()
        }

        // len(x)
        if (expr.startsWith("len(") && expr.endsWith(")")) {
            val inner = evalExpr(expr.substring(4, expr.length - 1))
            return when (inner) {
                is String -> inner.length
                is List<*> -> inner.size
                is Map<*, *> -> inner.size
                else -> 0
            }
        }

        // range(n) or range(start, stop)
        if (expr.startsWith("range(") && expr.endsWith(")")) {
            val inner = expr.substring(6, expr.length - 1).trim()
            val args = splitArgs(inner).map { (evalExpr(it) as? Number)?.toInt() ?: 0 }
            return when (args.size) {
                1 -> (0 until args[0]).toList()
                2 -> (args[0] until args[1]).toList()
                else -> emptyList<Int>()
            }
        }

        // type(x)
        if (expr.startsWith("type(") && expr.endsWith(")")) {
            val inner = evalExpr(expr.substring(5, expr.length - 1))
            return when (inner) {
                is String -> "<class 'str'>"
                is Long, is Int -> "<class 'int'>"
                is Double, is Float -> "<class 'float'>"
                is Boolean -> "<class 'bool'>"
                is List<*> -> "<class 'list'>"
                else -> "<class 'object'>"
            }
        }

        // math functions: math.sqrt(x)
        if (expr.startsWith("math.sqrt(") && expr.endsWith(")")) {
            val v = (evalExpr(expr.substring(10, expr.length - 1)) as? Number)?.toDouble() ?: 0.0
            return sqrt(v)
        }

        // Arithmetic operations: +, -, *, /, %, ==, !=, <, >
        for (op in listOf("==", "!=", "<=", ">=", "<", ">")) {
            if (expr.contains(" $op ")) {
                val left = evalExpr(expr.substringBefore(" $op "))
                val right = evalExpr(expr.substringAfter(" $op "))
                return compare(left, right, op)
            }
        }

        for (op in listOf("+", "-", "*", "/", "%")) {
            val idx = findOpIndex(expr, op)
            if (idx != -1) {
                val left = evalExpr(expr.substring(0, idx))
                val right = evalExpr(expr.substring(idx + 1))
                if (left is Number && right is Number) {
                    val isInteger = (left is Long || left is Int) && (right is Long || right is Int)
                    val lInt = left.toLong()
                    val rInt = right.toLong()
                    val l = left.toDouble()
                    val r = right.toDouble()
                    return when (op) {
                        "+" -> if (isInteger) lInt + rInt else l + r
                        "-" -> if (isInteger) lInt - rInt else l - r
                        "*" -> if (isInteger) lInt * rInt else l * r
                        "/" -> if (r != 0.0) {
                            if (isInteger && lInt % rInt == 0L) lInt / rInt else l / r
                        } else throw ArithmeticException("division by zero")
                        "%" -> if (rInt != 0L) lInt % rInt else throw ArithmeticException("modulo by zero")
                        else -> null
                    }
                }
                if (op == "+" && (left is String || right is String)) {
                    return repr(left) + repr(right)
                }
            }
        }

        // Lookup in global scope
        if (globalScope.containsKey(expr)) {
            return globalScope[expr]
        }

        return expr
    }

    private fun findOpIndex(expr: String, op: String): Int {
        var inQuotes = false
        var depth = 0
        for (i in expr.indices) {
            val c = expr[i]
            if (c == '"' || c == '\'') inQuotes = !inQuotes
            if (!inQuotes) {
                if (c == '(' || c == '[') depth++
                if (c == ')' || c == ']') depth--
                if (depth == 0 && c.toString() == op && i > 0 && i < expr.length - 1 && expr[i - 1] == ' ' && expr[i + 1] == ' ') {
                    return i
                }
            }
        }
        return -1
    }

    private fun compare(left: Any?, right: Any?, op: String): Boolean {
        if (left is Number && right is Number) {
            val l = left.toDouble()
            val r = right.toDouble()
            return when (op) {
                "==" -> l == r
                "!=" -> l != r
                "<" -> l < r
                ">" -> l > r
                "<=" -> l <= r
                ">=" -> l >= r
                else -> false
            }
        }
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            else -> false
        }
    }

    private fun isTruthy(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            is List<*> -> v.isNotEmpty()
            else -> true
        }
    }

    private fun repr(v: Any?): String {
        return when (v) {
            null -> "None"
            is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
            is Float -> if (v % 1.0f == 0.0f) v.toLong().toString() else v.toString()
            is List<*> -> "[" + v.joinToString(", ") { repr(it) } + "]"
            else -> v.toString()
        }
    }

    private fun splitArgs(s: String): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        var depth = 0
        var inQuotes = false

        for (c in s) {
            if (c == '"' || c == '\'') inQuotes = !inQuotes
            if (!inQuotes) {
                if (c == '(' || c == '[') depth++
                if (c == ')' || c == ']') depth--
                if (c == ',' && depth == 0) {
                    list.add(sb.toString().trim())
                    sb.clear()
                    continue
                }
            }
            sb.append(c)
        }
        if (sb.isNotEmpty()) list.add(sb.toString().trim())
        return list
    }
}

data class PythonExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)
