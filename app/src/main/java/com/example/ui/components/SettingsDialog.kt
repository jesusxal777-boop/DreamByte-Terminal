package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.TerminalFontSize
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun SettingsDialog(
    settings: TerminalSettings,
    onSaveSettings: (TerminalSettings) -> Unit,
    onResetSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDreamByteColors.current
    var currentTheme by remember(settings.theme) { mutableStateOf(settings.theme) }
    var currentFontSize by remember(settings.fontSize) { mutableStateOf(settings.fontSize) }
    var transparency by remember(settings.terminalTransparency) { mutableStateOf(settings.terminalTransparency) }
    var soundEnabled by remember(settings.soundEffectsEnabled) { mutableStateOf(settings.soundEffectsEnabled) }
    var hapticEnabled by remember(settings.hapticFeedbackEnabled) { mutableStateOf(settings.hapticFeedbackEnabled) }
    var showTimestamps by remember(settings.showTimestamps) { mutableStateOf(settings.showTimestamps) }
    var usernameText by remember(settings.username) { mutableStateOf(settings.username) }
    var systemTerminal by remember(settings.systemTerminalEnabled) { mutableStateOf(settings.systemTerminalEnabled) }
    var safeMode by remember(settings.safeMode) { mutableStateOf(settings.safeMode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp)),
            color = colors.backgroundEnd.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Terminal Settings",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.commandText
                        )
                        Text(
                            text = "DreamByte Studios • OS M Configuration",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.systemNoticeText
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.secondaryElectric
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Themes
                SectionHeader("Visual Theme")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = currentTheme == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.glassSurfaceHighlighted else colors.glassSurface)
                                .border(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) colors.primaryElectric else colors.glassBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    currentTheme = mode
                                    onSaveSettings(settings.copy(theme = mode))
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.primaryElectric else colors.terminalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Font Size
                SectionHeader("Font Size")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TerminalFontSize.entries.forEach { fs ->
                        val isSelected = currentFontSize == fs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.glassSurfaceHighlighted else colors.glassSurface)
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) colors.primaryElectric else colors.glassBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    currentFontSize = fs
                                    onSaveSettings(settings.copy(fontSize = fs))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fs.displayName.substringBefore(" "),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isSelected) colors.primaryElectric else colors.terminalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Glass Transparency
                SectionHeader("Terminal Glass Transparency (${(transparency * 100).toInt()}%)")
                Slider(
                    value = transparency,
                    onValueChange = {
                        transparency = it
                        onSaveSettings(settings.copy(terminalTransparency = it))
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primaryElectric,
                        activeTrackColor = colors.primaryElectric,
                        inactiveTrackColor = colors.glassBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Prompt Username
                SectionHeader("Active Username")
                OutlinedTextField(
                    value = usernameText,
                    onValueChange = {
                        val sanitized = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                        usernameText = sanitized
                        if (sanitized.isNotEmpty()) {
                            onSaveSettings(settings.copy(username = sanitized))
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = colors.commandText,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryElectric,
                        unfocusedBorderColor = colors.glassBorder,
                        focusedTextColor = colors.commandText,
                        unfocusedTextColor = colors.commandText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Feedback Toggles
                SectionHeader("Terminal Experience")
                SettingSwitchRow(
                    title = "Sound Effects",
                    subtitle = "Authentic terminal audio beeps and key clicks",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        onSaveSettings(settings.copy(soundEffectsEnabled = it))
                    }
                )
                SettingSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Subtle tactile vibrations on keypress and execution",
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        onSaveSettings(settings.copy(hapticFeedbackEnabled = it))
                    }
                )
                SettingSwitchRow(
                    title = "Show Timestamps",
                    subtitle = "Displays [HH:mm:ss] prefix on output lines",
                    checked = showTimestamps,
                    onCheckedChange = {
                        showTimestamps = it
                        onSaveSettings(settings.copy(showTimestamps = it))
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section: DreamByte OS M Advanced & Safe Mode
                SectionHeader("DreamByte OS M Advanced")
                SettingSwitchRow(
                    title = "System Terminal Mode",
                    subtitle = "Prepares mobile terminal for OS M integration",
                    checked = systemTerminal,
                    onCheckedChange = {
                        systemTerminal = it
                        onSaveSettings(settings.copy(systemTerminalEnabled = it))
                    }
                )
                SettingSwitchRow(
                    title = "Safe Mode",
                    subtitle = "Enforces Android sandbox security. Prevents elevated writes.",
                    checked = safeMode,
                    onCheckedChange = {
                        safeMode = it
                        onSaveSettings(settings.copy(safeMode = it))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset and About
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetSettings()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.warningText
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.warningText.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reset", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryElectric,
                            contentColor = androidx.compose.ui.graphics.Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Done", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalDreamByteColors.current
    Text(
        text = title,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = colors.primaryElectric,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalDreamByteColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.commandText
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.systemNoticeText
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primaryElectric,
                checkedTrackColor = colors.glassSurfaceHighlighted,
                uncheckedThumbColor = colors.glassBorder,
                uncheckedTrackColor = colors.glassSurface
            )
        )
    }
}
