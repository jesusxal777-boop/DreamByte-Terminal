package com.example.command.impl

import android.content.Intent
import android.content.pm.PackageManager
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsCommand : Command {
    override val name: String = "apps"
    override val description: String = "Lists launchable apps detected via standard Android APIs"
    override val aliases: List<String> = listOf("packages", "launch")
    override val usage: String = "apps [list | launch <package>]"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Android Application Inspection and Launcher",
        synopsis = "apps [launch <package_name>]",
        options = listOf(
            "list" to "List launchable applications detected via Android PackageManager",
            "launch <pkg>" to "Launch the specified package by its package name"
        ),
        description = "Uses the standard Android Intent-based activity query mechanism to inspect applications available on the device, respecting Android permissions.",
        examples = listOf(
            "apps" to "List installed launchable apps",
            "apps launch com.android.settings" to "Launch Android settings"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.androidContext.packageManager

        if (args.isNotEmpty() && (args[0] == "launch" || args[0] == "open")) {
            if (args.size < 2) {
                return@withContext CommandResult.error("Usage: apps launch <package_name>")
            }
            val pkgName = args[1]
            val launchIntent = pm.getLaunchIntentForPackage(pkgName)
            return@withContext if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.androidContext.startActivity(launchIntent)
                CommandResult.success("Launched application: $pkgName")
            } else {
                CommandResult.error("Unable to launch '$pkgName'. Application not found or not launchable.")
            }
        }

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        if (resolveInfos.isEmpty()) {
            return@withContext CommandResult.warning(
                "No launchable applications returned by system query."
            )
        }

        val appEntries = resolveInfos.map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val version = try {
                val pkgInfo = pm.getPackageInfo(packageName, 0)
                pkgInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "unknown"
            }
            Triple(label, packageName, version)
        }.sortedBy { it.first.lowercase() }

        val sb = StringBuilder()
        sb.appendLine("INSTALLED APPLICATIONS (${appEntries.size} total)")
        sb.appendLine("───────────────────────────────────────────")
        appEntries.take(35).forEachIndexed { idx, (label, pkg, ver) ->
            val num = (idx + 1).toString().padStart(2, ' ')
            sb.appendLine("[$num] ${label.padEnd(24)} v${ver.padEnd(10)} ($pkg)")
        }

        if (appEntries.size > 35) {
            sb.appendLine("... and ${appEntries.size - 35} more.")
        }
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("To launch: apps launch <package_name>")

        CommandResult.success(sb.toString().trimEnd())
    }
}
