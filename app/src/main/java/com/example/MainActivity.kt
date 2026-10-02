package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.terminal.TerminalScreen
import com.example.ui.terminal.TerminalViewModel
import com.example.ui.theme.DreamByteTerminalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val terminalViewModel: TerminalViewModel = viewModel()
            val settings by terminalViewModel.settings.collectAsState()
            val openFilePicker by terminalViewModel.openFilePickerEvent.collectAsState()

            // Safe Android System File Explorer Integration
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { _ ->
                terminalViewModel.onFilePickerOpened()
            }

            LaunchedEffect(openFilePicker) {
                if (openFilePicker) {
                    try {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            startActivity(intent)
                        } catch (_: Exception) {
                        }
                    } finally {
                        terminalViewModel.onFilePickerOpened()
                    }
                }
            }

            DreamByteTerminalTheme(themeMode = settings.theme) {
                TerminalScreen(viewModel = terminalViewModel)
            }
        }
    }
}
