package com.example.command.impl

import android.content.Intent
import android.content.pm.PackageManager
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsCommand : Command {
    override val name: String = "apps"
    override val description: String = "Lists launchable apps detected via standard Android APIs"
    override val aliases: List<String> = listOf("packages", "launch")
    override val usage: String = "apps [launch <package>]"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.androidContext.packageManager

        // Subcommand: launch
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

        // List launchable applications
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        if (resolveInfos.isEmpty()) {
            return@withContext CommandResult.warning(
                "No launchable applications found or query restrictions applied."
            )
        }

        val appEntries = resolveInfos.map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            Pair(label, packageName)
        }.sortedBy { it.first.lowercase() }

        val sb = StringBuilder()
        sb.appendLine("DETECTED APPLICATIONS (${appEntries.size} total)")
        sb.appendLine("───────────────────────────────────────────")
        appEntries.take(30).forEachIndexed { idx, (label, pkg) ->
            val num = (idx + 1).toString().padStart(2, ' ')
            sb.appendLine("[$num] $label ($pkg)")
        }

        if (appEntries.size > 30) {
            sb.appendLine("... and ${appEntries.size - 30} more.")
        }
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("To launch an app: apps launch <package_name>")

        CommandResult.success(sb.toString())
    }
}
