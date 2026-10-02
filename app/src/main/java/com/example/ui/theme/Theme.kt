package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.example.model.ThemeMode

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.LIQUID_GLASS,
    content: @Composable () -> Unit
) {
    DreamByteTerminalTheme(themeMode = themeMode, content = content)
}
