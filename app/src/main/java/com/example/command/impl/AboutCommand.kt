package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.model.CommandResult

class AboutCommand : Command {
    override val name: String = "about"
    override val description: String = "Displays official DreamByte Studios and terminal information"
    override val aliases: List<String> = listOf("info")
    override val usage: String = "about"
    override val category: CommandCategory = CommandCategory.DREAMBYTE

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        val text = """
            DreamByte Terminal
            DreamByte Studios
            Android Edition
            Prototype

            ───────────────────────────────────────────
            Platform : DreamByte OS M (Mobile)
            Version  : 0.1.0-alpha
            Engine   : DreamByte Core Engine v1.0
            Security : Android Native Sandbox (Rootless)
            Mascot   : Cloud Robot (Type 'dream mascot')
            ───────────────────────────────────────────
            Crafted with liquid glass aesthetics & high-tech
            holographic precision by DreamByte Studios.
        """.trimIndent()
        return CommandResult.success(text)
    }
}
