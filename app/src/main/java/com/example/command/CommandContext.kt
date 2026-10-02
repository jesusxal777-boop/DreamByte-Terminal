package com.example.command

import android.content.Context
import com.example.model.CommandResult
import com.example.model.TerminalSettings
import com.example.model.ThemeMode

data class CommandContext(
    val androidContext: Context,
    val settings: TerminalSettings,
    val registry: CommandRegistry,
    val onClearScreen: () -> Unit,
    val onThemeChange: (ThemeMode) -> Unit,
    val onUserChange: (String) -> Unit,
    val openSystemFiles: () -> Unit,
    val openSettings: () -> Unit,
    val showMascotDialog: () -> Unit,
    val requestSystemOperation: (operationName: String, details: String, command: String) -> Unit,
    val playSound: (isBeep: Boolean) -> Unit,
    val executeNestedCommand: suspend (String) -> CommandResult
)
