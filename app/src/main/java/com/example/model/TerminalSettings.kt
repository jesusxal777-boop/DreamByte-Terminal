package com.example.model

enum class TerminalFontSize(val displayName: String, val spValue: Float) {
    SMALL("Small (12sp)", 12f),
    MEDIUM("Medium (14sp)", 14f),
    LARGE("Large (16sp)", 16f),
    EXTRA_LARGE("Extra Large (18sp)", 18f);

    companion object {
        fun fromSp(sp: Float): TerminalFontSize {
            return entries.minByOrNull { kotlin.math.abs(it.spValue - sp) } ?: MEDIUM
        }
    }
}

data class TerminalSettings(
    val theme: ThemeMode = ThemeMode.LIQUID_GLASS,
    val fontSize: TerminalFontSize = TerminalFontSize.MEDIUM,
    val terminalTransparency: Float = 0.85f, // 0.2f (very translucent) to 1.0f (opaque)
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showTimestamps: Boolean = false,
    val username: String = "jake",
    val hostname: String = "dreambyte",
    val systemTerminalEnabled: Boolean = false,
    val safeMode: Boolean = true,
    val maxHistoryEntries: Int = 100
) {
    val promptPrefix: String
        get() = "$username@$hostname:~$ "
}
