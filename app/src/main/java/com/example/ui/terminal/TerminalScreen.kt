package com.example.ui.terminal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.model.ThemeMode
import com.example.ui.components.DreamByteConfirmDialog
import com.example.ui.components.MascotDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = LocalDreamByteColors.current

    val settings by viewModel.settings.collectAsState()
    val lines by viewModel.lines.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showMascotDialog by viewModel.showMascotDialog.collectAsState()
    val pendingSystemOp by viewModel.pendingSystemOp.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.backgroundStart,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundBrush)
                .padding(innerPadding)
                .padding(top = statusBarPadding.calculateTopPadding(), bottom = navBarPadding.calculateBottomPadding())
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isTabletOrWide = maxWidth > 600.dp
                val contentModifier = if (isTabletOrWide) {
                    Modifier
                        .widthIn(max = 900.dp)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                }

                Column(
                    modifier = contentModifier.fillMaxSize()
                ) {
                    // Header Bar
                    TerminalHeader(
                        settings = settings,
                        onOpenSettings = { viewModel.openSettingsDialog() },
                        onOpenMascot = { viewModel.openMascotDialog() },
                        onClearScreen = { viewModel.clearScreen() },
                        onCopyLogs = {
                            val allText = lines.joinToString("\n") { it.text }
                            clipboardManager.setText(AnnotatedString(allText))
                            Toast.makeText(context, "Terminal log copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onToggleTheme = {
                            val nextTheme = when (settings.theme) {
                                ThemeMode.LIQUID_GLASS -> ThemeMode.HOLO
                                ThemeMode.HOLO -> ThemeMode.RETRO_TERMINAL
                                ThemeMode.RETRO_TERMINAL -> ThemeMode.LIQUID_GLASS
                            }
                            viewModel.updateTheme(nextTheme)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Liquid Glass Terminal Log Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurface.copy(alpha = settings.terminalTransparency))
                            .border(0.75.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                            .testTag("terminal_log_container")
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = lines,
                                key = { it.id }
                            ) { line ->
                                TerminalLineItem(
                                    line = line,
                                    fontSize = settings.fontSize,
                                    showTimestamps = settings.showTimestamps
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Virtual Keyboard Accessory Bar
                    VirtualKeyboardBar(
                        onKeyPress = { token -> viewModel.insertAccessoryText(token) },
                        onHistoryUp = { viewModel.navigateHistoryUp() },
                        onHistoryDown = { viewModel.navigateHistoryDown() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val relCwd = if (viewModel.shellEnv.currentDirectory == viewModel.shellEnv.homeDir) "~" else viewModel.shellEnv.currentDirectory.name
                    val activePrompt = "${settings.username}@${settings.hostname}:$relCwd$ "

                    // Terminal Input Row
                    TerminalInputRow(
                        input = currentInput,
                        onInputChange = { viewModel.onInputChange(it) },
                        onSubmit = { viewModel.submitCommand() },
                        settings = settings,
                        isExecuting = isExecuting,
                        promptPrefix = activePrompt
                    )
                }
            }
        }

        // Modals & Dialogs
        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                onSaveSettings = { viewModel.updateSettings(it) },
                onResetSettings = { viewModel.resetSettings() },
                onDismiss = { viewModel.closeSettingsDialog() }
            )
        }

        if (showMascotDialog) {
            MascotDialog(onDismiss = { viewModel.closeMascotDialog() })
        }

        pendingSystemOp?.let { op ->
            DreamByteConfirmDialog(
                operation = op,
                onConfirm = { viewModel.confirmPendingSystemOperation() },
                onCancel = { viewModel.cancelPendingSystemOperation() }
            )
        }
    }
}
