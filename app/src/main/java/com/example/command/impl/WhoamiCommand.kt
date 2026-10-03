package com.example.command.impl

import android.os.Process
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandAction
import com.example.model.CommandResult

class WhoamiCommand : Command {
    override val name: String = "whoami"
    override val description: String = "Displays or modifies the active terminal user"
    override val aliases: List<String> = listOf("user", "id")
    override val usage: String = "whoami [set <new_username> | -v]"
    override val category: CommandCategory = CommandCategory.GENERAL

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Print User and Process Identity",
        synopsis = "whoami [set <new_username> | -v]",
        options = listOf(
            "set <name>" to "Dynamically change the terminal session prompt user",
            "-v, --verbose" to "Show real Android Linux UID, GID, PID process details"
        ),
        description = "Shows the active shell session username and process identity within Android's sandboxed environment.",
        examples = listOf(
            "whoami" to "Show username",
            "whoami -v" to "Show UID, PID and security context",
            "whoami set alice" to "Change prompt user to alice"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val uid = Process.myUid()
        val pid = Process.myPid()

        if (args.isEmpty()) {
            return CommandResult.success(context.settings.username)
        }

        if (args.contains("-v") || args.contains("--verbose") || args.contains("-a")) {
            return CommandResult.success(
                """
                uid=$uid(${context.settings.username}) gid=$uid(${context.settings.username}) pid=$pid
                Host    : ${context.settings.hostname}
                Context : u:r:untrusted_app:s0 (Android Application Sandbox)
                """.trimIndent()
            )
        }

        val targetUser = if (args[0] == "set" && args.size > 1) {
            args[1]
        } else {
            args[0]
        }

        val sanitized = targetUser.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        if (sanitized.isEmpty()) {
            return CommandResult.error("Invalid username. Use alphanumeric characters.")
        }

        context.onUserChange(sanitized)
        context.shellEnv.setVariable("USER", sanitized)
        return CommandResult(
            output = "User changed to '$sanitized'. Terminal prompt updated.",
            action = CommandAction.CHANGE_USER(sanitized)
        )
    }
}
