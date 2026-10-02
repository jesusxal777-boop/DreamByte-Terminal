package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeMode

data class DreamByteColors(
    val themeMode: ThemeMode,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val backgroundBrush: Brush,
    val glassSurface: Color,
    val glassSurfaceHighlighted: Color,
    val glassBorder: Color,
    val glassBorderGlow: Color,
    val primaryElectric: Color,
    val secondaryElectric: Color,
    val terminalText: Color,
    val promptUser: Color,
    val promptHost: Color,
    val promptSymbol: Color,
    val commandText: Color,
    val successText: Color,
    val errorText: Color,
    val warningText: Color,
    val systemNoticeText: Color,
    val cursorColor: Color,
    val accessoryKeyBackground: Color,
    val accessoryKeyContent: Color,
    val scanlineColor: Color
)

val LiquidGlassColors = DreamByteColors(
    themeMode = ThemeMode.LIQUID_GLASS,
    backgroundStart = Color(0xFF040A14),
    backgroundEnd = Color(0xFF0C1E34),
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF040A14), Color(0xFF0A192C), Color(0xFF0C1E34))
    ),
    glassSurface = Color(0x330F2642),
    glassSurfaceHighlighted = Color(0x551E3A5F),
    glassBorder = Color(0x4038BDF8),
    glassBorderGlow = Color(0x6600E5FF),
    primaryElectric = Color(0xFF00E5FF),
    secondaryElectric = Color(0xFF38BDF8),
    terminalText = Color(0xFFE2F1FD),
    promptUser = Color(0xFF38BDF8),
    promptHost = Color(0xFF00E5FF),
    promptSymbol = Color(0xFF67E8F9),
    commandText = Color(0xFFFFFFFF),
    successText = Color(0xFF34D399),
    errorText = Color(0xFFF87171),
    warningText = Color(0xFFFBBF24),
    systemNoticeText = Color(0xFF7DD3FC),
    cursorColor = Color(0xFF00E5FF),
    accessoryKeyBackground = Color(0x40132B45),
    accessoryKeyContent = Color(0xFFBAE6FD),
    scanlineColor = Color.Transparent
)

val HoloColors = DreamByteColors(
    themeMode = ThemeMode.HOLO,
    backgroundStart = Color(0xFF020617),
    backgroundEnd = Color(0xFF0B112C),
    backgroundBrush = Brush.radialGradient(
        listOf(Color(0xFF0F1B40), Color(0xFF050A1A), Color(0xFF020617))
    ),
    glassSurface = Color(0x28020617),
    glassSurfaceHighlighted = Color(0x440F1E3D),
    glassBorder = Color(0x6600D2FF),
    glassBorderGlow = Color(0x9900FFFF),
    primaryElectric = Color(0xFF00D2FF),
    secondaryElectric = Color(0xFF818CF8),
    terminalText = Color(0xFFCCFBF1),
    promptUser = Color(0xFF818CF8),
    promptHost = Color(0xFF00D2FF),
    promptSymbol = Color(0xFF22D3EE),
    commandText = Color(0xFFE0F2FE),
    successText = Color(0xFF22C55E),
    errorText = Color(0xFFFF2A6D),
    warningText = Color(0xFFF59E0B),
    systemNoticeText = Color(0xFF67E8F9),
    cursorColor = Color(0xFF00FFFF),
    accessoryKeyBackground = Color(0x33091428),
    accessoryKeyContent = Color(0xFF67E8F9),
    scanlineColor = Color(0x0800FFFF)
)

val RetroTerminalColors = DreamByteColors(
    themeMode = ThemeMode.RETRO_TERMINAL,
    backgroundStart = Color(0xFF070B0E),
    backgroundEnd = Color(0xFF10171D),
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF06090C), Color(0xFF0B1217), Color(0xFF10171D))
    ),
    glassSurface = Color(0x2E181205),
    glassSurfaceHighlighted = Color(0x4A291C05),
    glassBorder = Color(0x55F59E0B),
    glassBorderGlow = Color(0x88FBBF24),
    primaryElectric = Color(0xFFF59E0B),
    secondaryElectric = Color(0xFFFBBF24),
    terminalText = Color(0xFFFDE68A),
    promptUser = Color(0xFFF59E0B),
    promptHost = Color(0xFFFBBF24),
    promptSymbol = Color(0xFFFEF3C7),
    commandText = Color(0xFFFFFBEB),
    successText = Color(0xFF34D399),
    errorText = Color(0xFFEF4444),
    warningText = Color(0xFFF59E0B),
    systemNoticeText = Color(0xFFFCD34D),
    cursorColor = Color(0xFFF59E0B),
    accessoryKeyBackground = Color(0x35281804),
    accessoryKeyContent = Color(0xFFFDE68A),
    scanlineColor = Color(0x10000000)
)

val LocalDreamByteColors = staticCompositionLocalOf { LiquidGlassColors }

@Composable
fun DreamByteTerminalTheme(
    themeMode: ThemeMode = ThemeMode.LIQUID_GLASS,
    content: @Composable () -> Unit
) {
    val dreamColors = when (themeMode) {
        ThemeMode.LIQUID_GLASS -> LiquidGlassColors
        ThemeMode.HOLO -> HoloColors
        ThemeMode.RETRO_TERMINAL -> RetroTerminalColors
    }

    val materialColors = darkColorScheme(
        primary = dreamColors.primaryElectric,
        onPrimary = Color.Black,
        secondary = dreamColors.secondaryElectric,
        onSecondary = Color.Black,
        background = dreamColors.backgroundStart,
        onBackground = dreamColors.terminalText,
        surface = dreamColors.glassSurface,
        onSurface = dreamColors.terminalText,
        error = dreamColors.errorText,
        onError = Color.White
    )

    CompositionLocalProvider(
        LocalDreamByteColors provides dreamColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
