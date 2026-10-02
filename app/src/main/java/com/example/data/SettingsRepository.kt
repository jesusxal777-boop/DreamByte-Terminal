package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.TerminalFontSize
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dreambyte_terminal_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<TerminalSettings> = _settings.asStateFlow()

    private fun loadSettings(): TerminalSettings {
        val themeStr = prefs.getString(KEY_THEME, ThemeMode.LIQUID_GLASS.id) ?: ThemeMode.LIQUID_GLASS.id
        val fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, TerminalFontSize.MEDIUM.spValue)
        val transparency = prefs.getFloat(KEY_TRANSPARENCY, 0.85f)
        val sound = prefs.getBoolean(KEY_SOUND, true)
        val haptic = prefs.getBoolean(KEY_HAPTIC, true)
        val timestamps = prefs.getBoolean(KEY_TIMESTAMPS, false)
        val username = prefs.getString(KEY_USERNAME, "jake") ?: "jake"
        val systemTerminal = prefs.getBoolean(KEY_SYSTEM_TERMINAL, false)
        val safeMode = prefs.getBoolean(KEY_SAFE_MODE, true)

        return TerminalSettings(
            theme = ThemeMode.fromId(themeStr),
            fontSize = TerminalFontSize.fromSp(fontSizeSp),
            terminalTransparency = transparency,
            soundEffectsEnabled = sound,
            hapticFeedbackEnabled = haptic,
            showTimestamps = timestamps,
            username = username,
            systemTerminalEnabled = systemTerminal,
            safeMode = safeMode
        )
    }

    fun updateSettings(newSettings: TerminalSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putString(KEY_THEME, newSettings.theme.id)
            putFloat(KEY_FONT_SIZE, newSettings.fontSize.spValue)
            putFloat(KEY_TRANSPARENCY, newSettings.terminalTransparency)
            putBoolean(KEY_SOUND, newSettings.soundEffectsEnabled)
            putBoolean(KEY_HAPTIC, newSettings.hapticFeedbackEnabled)
            putBoolean(KEY_TIMESTAMPS, newSettings.showTimestamps)
            putString(KEY_USERNAME, newSettings.username)
            putBoolean(KEY_SYSTEM_TERMINAL, newSettings.systemTerminalEnabled)
            putBoolean(KEY_SAFE_MODE, newSettings.safeMode)
            apply()
        }
    }

    fun resetSettings() {
        val defaultSettings = TerminalSettings()
        updateSettings(defaultSettings)
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_TRANSPARENCY = "transparency"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_TIMESTAMPS = "timestamps_enabled"
        private const val KEY_USERNAME = "username"
        private const val KEY_SYSTEM_TERMINAL = "system_terminal"
        private const val KEY_SAFE_MODE = "safe_mode"
    }
}
