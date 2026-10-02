package com.example.ui.terminal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun TerminalHeader(
    settings: TerminalSettings,
    onOpenSettings: () -> Unit,
    onOpenMascot: () -> Unit,
    onClearScreen: () -> Unit,
    onCopyLogs: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDreamByteColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(colors.glassSurface)
            .border(
                0.75.dp,
                colors.glassBorder,
                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left branding: Mascot / Logo + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onOpenMascot)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.glassSurfaceHighlighted)
                        .border(1.dp, colors.primaryElectric, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dreambyte_mascot),
                        contentDescription = "DreamByte Mascot",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DreamByte",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.commandText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primaryElectric.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OS M",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryElectric
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (settings.safeMode) colors.successText else colors.warningText)
                        )
                        Text(
                            text = if (settings.safeMode) "Safe Mode" else "Elevated Ready",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.systemNoticeText
                        )
                    }
                }
            }

            // Right actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Theme pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.glassSurfaceHighlighted)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleTheme)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("theme_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme",
                            tint = colors.primaryElectric,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = settings.theme.displayName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.primaryElectric
                        )
                    }
                }

                IconButton(
                    onClick = onCopyLogs,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("copy_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Log",
                        tint = colors.secondaryElectric,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onClearScreen,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_screen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Screen",
                        tint = colors.secondaryElectric,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.primaryElectric,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
