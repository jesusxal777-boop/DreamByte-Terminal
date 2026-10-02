package com.example.command.impl

import android.os.Build
import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult

class SysInfoCommand : Command {
    override val name: String = "sysinfo"
    override val description: String = "Displays low-level system and hardware information"
    override val aliases: List<String> = listOf("uname")
    override val usage: String = "sysinfo [-a]"
    override val category: CommandCategory = CommandCategory.SYSTEM

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        if (args.contains("-a") || args.contains("--all")) {
            val osVersion = System.getProperty("os.version") ?: "unknown"
            return CommandResult.success("Linux dreambyte $osVersion ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"} Android/DreamByteOS")
        }

        val text = """
            System Architecture:
              OS           : DreamByte OS M (Native Android Host)
              Android SDK  : API ${Build.VERSION.SDK_INT} (${Build.VERSION.CODENAME})
              Device       : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})
              ABIs         : ${Build.SUPPORTED_ABIS.joinToString(", ")}
              Fingerprint  : ${Build.FINGERPRINT.take(40)}...
              Security     : SELinux Enforcing (Restricted App Sandbox)
        """.trimIndent()
        return CommandResult.success(text)
    }
}
