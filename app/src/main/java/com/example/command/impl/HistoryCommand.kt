package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.history.CommandHistoryManager
import com.example.model.CommandResult

class HistoryCommand(private val historyManager: CommandHistoryManager) : Command {
    override val name: String = "history"
    override val description: String = "Shows or clears the command execution history"
    override val usage: String = "history [clear]"
    override val category: CommandCategory = CommandCategory.UTILITY

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.isNotEmpty() && args[0].lowercase() == "clear") {
            historyManager.clear()
            return CommandResult.success("Command history cleared.")
        }

        val list = historyManager.history.value
        if (list.isEmpty()) {
            return CommandResult.success("No commands in history.")
        }

        val sb = StringBuilder()
        sb.appendLine("COMMAND HISTORY (${list.size} entries)")
        sb.appendLine("───────────────────────────────────────────")
        list.takeLast(50).forEachIndexed { idx, cmd ->
            val num = (idx + 1).toString().padStart(3, ' ')
            sb.appendLine("  $num  $cmd")
        }
        return CommandResult.success(sb.toString())
    }
}
