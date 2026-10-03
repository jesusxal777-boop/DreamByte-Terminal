package com.example.command

import com.example.model.CommandResult

/**
 * Base interface for all DreamByte Terminal commands.
 * Highly modular to allow seamless extension for DreamByte OS M and third-party packages.
 */
interface Command {
    val name: String
    val description: String
    val aliases: List<String> get() = emptyList()
    val usage: String get() = name
    val category: CommandCategory get() = CommandCategory.GENERAL
    val requiresOsPrivileges: Boolean get() = false

    val detailedHelp: CommandHelp? get() = null

    suspend fun execute(context: CommandContext, args: List<String>): CommandResult
}

enum class CommandCategory(val label: String) {
    GENERAL("General Commands"),
    SHELL("DreamShell & Scripting"),
    PACKAGE_MANAGEMENT("Package Manager"),
    SYSTEM("System & OS"),
    DREAMBYTE("DreamByte Studios"),
    UTILITY("Utilities"),
    ADMIN("DreamByte OS M Privileged")
}
