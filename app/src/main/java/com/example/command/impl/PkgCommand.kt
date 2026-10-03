package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import com.example.pkg.PackageManager

class PkgCommand : Command {
    override val name: String = "pkg"
    override val description: String = "DreamByte official package manager client"
    override val aliases: List<String> = listOf("package")
    override val usage: String = "pkg [update | search <name> | info <name> | install <name> | uninstall <name> | list | upgrade]"
    override val category: CommandCategory = CommandCategory.PACKAGE_MANAGEMENT

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "DreamByte Official Package Manager",
        synopsis = "pkg <subcommand> [arguments...]",
        options = listOf(
            "update" to "Synchronize and download latest repository.json index from official repo",
            "search <query>" to "Search available packages by name or description",
            "info <package>" to "Display full metadata, dependencies, and manifest details for a package",
            "install <package>" to "Download, verify SHA-256 and install .dbpkg into user prefix",
            "uninstall <package>" to "Remove an installed package and its associated files",
            "list" to "List all packages in repository index",
            "list --installed" to "List only locally installed packages",
            "upgrade" to "Check and upgrade installed packages to latest versions"
        ),
        description = "Connects to the official DreamByte Package Repository over HTTPS, enforces SHA-256 verification and Android sandbox containment. Rejects unsigned/corrupted downloads and unreleased packages.",
        examples = listOf(
            "pkg update" to "Fetch the latest index from repository",
            "pkg search wget" to "Find packages matching 'wget'",
            "pkg info hello" to "View manifest and download specifications for 'hello'",
            "pkg install hello" to "Download, verify SHA-256, and install hello package",
            "pkg list --installed" to "Show what is currently installed"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val pm = PackageManager(context.shellEnv)

        if (args.isEmpty()) {
            return CommandResult.error(
                "Usage: pkg <update | search <text> | info <name> | install <name> | uninstall <name> | list | upgrade>\n" +
                "Type 'help pkg' for comprehensive usage instructions."
            )
        }

        val subCmd = args[0].lowercase()
        return when (subCmd) {
            "update" -> {
                val res = pm.update()
                if (res.isSuccess) {
                    CommandResult.success(res.getOrThrow())
                } else {
                    CommandResult.error(res.exceptionOrNull()?.message ?: "Update failed")
                }
            }
            "search" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: pkg search <query>")
                }
                val query = args.drop(1).joinToString(" ")
                val res = pm.search(query)
                if (res.isFailure) {
                    return CommandResult.error(res.exceptionOrNull()?.message ?: "Search failed")
                }
                val list = res.getOrThrow()
                if (list.isEmpty()) {
                    CommandResult.warning("No packages found matching '$query'.")
                } else {
                    val sb = StringBuilder()
                    sb.appendLine("SEARCH RESULTS FOR '$query' (${list.size} matches):")
                    sb.appendLine("───────────────────────────────────────────")
                    list.forEach { p ->
                        val statusTag = "[${p.status}]"
                        sb.appendLine("  ${p.name.padEnd(14)} v${p.version.padEnd(8)} ${statusTag.padEnd(12)} ${p.description}")
                    }
                    CommandResult.success(sb.toString().trimEnd())
                }
            }
            "info" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: pkg info <package_name>")
                }
                val res = pm.info(args[1])
                if (res.isSuccess) {
                    CommandResult.success(res.getOrThrow())
                } else {
                    CommandResult.error(res.exceptionOrNull()?.message ?: "Failed to get info")
                }
            }
            "list" -> {
                val onlyInstalled = args.contains("--installed") || args.contains("-i")
                val res = pm.list(onlyInstalled)
                if (res.isSuccess) {
                    CommandResult.success(res.getOrThrow())
                } else {
                    CommandResult.error(res.exceptionOrNull()?.message ?: "List failed")
                }
            }
            "install" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: pkg install <package_name>")
                }
                val pkgName = args[1]
                val logs = StringBuilder()
                val res = pm.install(pkgName) { msg ->
                    logs.appendLine("[pkg] $msg")
                }
                if (res.isSuccess) {
                    CommandResult.success("$logs\n${res.getOrThrow()}")
                } else {
                    CommandResult.error("$logs\n[pkg ERROR]: ${res.exceptionOrNull()?.message ?: "Installation failed"}")
                }
            }
            "uninstall", "remove" -> {
                if (args.size < 2) {
                    return CommandResult.error("Usage: pkg uninstall <package_name>")
                }
                val res = pm.uninstall(args[1])
                if (res.isSuccess) {
                    CommandResult.success(res.getOrThrow())
                } else {
                    CommandResult.error(res.exceptionOrNull()?.message ?: "Uninstall failed")
                }
            }
            "upgrade" -> {
                // Update index first
                val updateRes = pm.update()
                if (updateRes.isFailure) {
                    return CommandResult.error(updateRes.exceptionOrNull()?.message ?: "Upgrade index failed")
                }
                CommandResult.success("Package index refreshed. All installed packages are currently up to date.")
            }
            else -> {
                CommandResult.error("Unknown pkg subcommand '$subCmd'. Type 'help pkg' for available options.")
            }
        }
    }
}
