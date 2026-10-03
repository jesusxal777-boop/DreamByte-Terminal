package com.example.command.impl

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import java.io.File
import java.util.Locale

class SysInfoCommand : Command {
    override val name: String = "sysinfo"
    override val description: String = "Displays real system, hardware, memory and telemetry diagnostics"
    override val aliases: List<String> = listOf("uname")
    override val usage: String = "sysinfo [-a | --all]"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "System Hardware and Telemetry Diagnostics",
        synopsis = "sysinfo [-a | --all]",
        options = listOf(
            "-a, --all" to "Print compact uname-style architecture and kernel string",
            "--help" to "Display usage help"
        ),
        description = "Queries real Android hardware, memory, CPU, storage and battery telemetry directly through standard non-privileged Android APIs.",
        examples = listOf(
            "sysinfo" to "Full telemetry and hardware report",
            "sysinfo -a" to "Compact OS and kernel string"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val osVersion = System.getProperty("os.version") ?: "unknown"
        val arch = System.getProperty("os.arch") ?: "unknown"
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")

        if (args.contains("-a") || args.contains("--all")) {
            return CommandResult.success("Linux dreambyte $osVersion $arch Android API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL})")
        }

        val ctx = context.androidContext
        val actMgr = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMgr?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = totalRamMb - availRamMb
        val ramUsagePercent = if (totalRamMb > 0) (usedRamMb * 100 / totalRamMb) else 0

        // Real internal storage statistics
        val dataStat = StatFs(ctx.filesDir.absolutePath)
        val totalStorageMb = (dataStat.blockCountLong * dataStat.blockSizeLong) / (1024 * 1024)
        val freeStorageMb = (dataStat.availableBlocksLong * dataStat.blockSizeLong) / (1024 * 1024)
        val usedStorageMb = totalStorageMb - freeStorageMb

        // Real battery metrics
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val batteryStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                         batteryStatus == BatteryManager.BATTERY_STATUS_FULL

        // CPU cores
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Display metrics
        val metrics = ctx.resources.displayMetrics
        val screenRes = "${metrics.widthPixels}x${metrics.heightPixels} @ ${metrics.densityDpi}dpi"

        val sb = StringBuilder()
        sb.appendLine("DREAMBYTE OS M - HARDWARE & SYSTEM TELEMETRY")
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("DEVICE HARDWARE")
        sb.appendLine("  Manufacturer : ${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}")
        sb.appendLine("  Model        : ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("  Board        : ${Build.BOARD} [${Build.HARDWARE}]")
        sb.appendLine("  Display      : $screenRes")
        sb.appendLine("  CPU Cores    : $cpuCores active cores")
        sb.appendLine("  Architecture : $arch (ABIs: $abis)")

        sb.appendLine("\nOPERATING SYSTEM")
        sb.appendLine("  Platform     : DreamByte OS M (Mobile Edition)")
        sb.appendLine("  Host OS      : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("  Security Pkg : ${Build.VERSION.SECURITY_PATCH ?: "N/A"}")
        sb.appendLine("  Kernel       : Linux $osVersion")
        sb.appendLine("  Sandbox Mode : Active (${ctx.packageName})")

        sb.appendLine("\nMEMORY & STORAGE")
        sb.appendLine("  RAM Total    : $totalRamMb MB")
        sb.appendLine("  RAM Used     : $usedRamMb MB ($ramUsagePercent%) [Free: $availRamMb MB]")
        sb.appendLine("  Storage Total: ${totalStorageMb / 1024} GB (${totalStorageMb} MB)")
        sb.appendLine("  Storage Free : ${freeStorageMb / 1024} GB (${freeStorageMb} MB)")

        sb.appendLine("\nPOWER & THERMAL")
        val chargeStr = if (isCharging) "Charging" else "Discharging"
        sb.appendLine("  Battery      : ${if (batteryPct >= 0) "$batteryPct%" else "Unavailable"} [$chargeStr]")
        sb.appendLine("  Low Memory   : ${if (memInfo.lowMemory) "YES (Warning)" else "Normal"}")

        return CommandResult.success(sb.toString().trimEnd())
    }
}
