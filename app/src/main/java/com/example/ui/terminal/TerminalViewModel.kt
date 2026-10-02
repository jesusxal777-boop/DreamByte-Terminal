package com.example.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.command.CommandContext
import com.example.command.CommandExecutor
import com.example.command.CommandRegistry
import com.example.command.impl.AboutCommand
import com.example.command.impl.AppsCommand
import com.example.command.impl.ClearCommand
import com.example.command.impl.DateCommand
import com.example.command.impl.DpmCommand
import com.example.command.impl.DreamCommand
import com.example.command.impl.EchoCommand
import com.example.command.impl.FilesCommand
import com.example.command.impl.HelpCommand
import com.example.command.impl.HistoryCommand
import com.example.command.impl.ScriptCommand
import com.example.command.impl.SettingsCommand
import com.example.command.impl.SudoCommand
import com.example.command.impl.SysInfoCommand
import com.example.command.impl.ThemeCommand
import com.example.command.impl.TimeCommand
import com.example.command.impl.VersionCommand
import com.example.command.impl.WhoamiCommand
import com.example.data.SettingsRepository
import com.example.history.CommandHistoryManager
import com.example.model.CommandResult
import com.example.model.LineType
import com.example.model.TerminalLine
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import com.example.util.TerminalAudioFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingSystemOp(
    val title: String,
    val details: String,
    val command: String
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    val settings: StateFlow<TerminalSettings> = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        settingsRepo.settings.value
    )

    private val historyManager = CommandHistoryManager(application)
    private val audioFeedback = TerminalAudioFeedback(application)

    val registry = CommandRegistry()
    private val executor = CommandExecutor(registry)

    private val _lines = MutableStateFlow<List<TerminalLine>>(createInitialLines())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showMascotDialog = MutableStateFlow(false)
    val showMascotDialog: StateFlow<Boolean> = _showMascotDialog.asStateFlow()

    private val _pendingSystemOp = MutableStateFlow<PendingSystemOp?>(null)
    val pendingSystemOp: StateFlow<PendingSystemOp?> = _pendingSystemOp.asStateFlow()

    // Trigger for file manager intent
    private val _openFilePickerEvent = MutableStateFlow(false)
    val openFilePickerEvent: StateFlow<Boolean> = _openFilePickerEvent.asStateFlow()

    init {
        registerBuiltInCommands()
    }

    private fun registerBuiltInCommands() {
        registry.register(HelpCommand())
        registry.register(ClearCommand())
        registry.register(AboutCommand())
        registry.register(VersionCommand())
        registry.register(EchoCommand())
        registry.register(DateCommand())
        registry.register(TimeCommand())
        registry.register(ThemeCommand())
        registry.register(AppsCommand())
        registry.register(FilesCommand())
        registry.register(DreamCommand())
        registry.register(HistoryCommand(historyManager))
        registry.register(WhoamiCommand())
        registry.register(SysInfoCommand())
        registry.register(ScriptCommand())
        registry.register(SettingsCommand())
        // Future OS M commands
        registry.register(SudoCommand())
        registry.register(DpmCommand())
    }

    private fun createInitialLines(): List<TerminalLine> {
        return listOf(
            TerminalLine(
                text = "DREAMBYTE TERMINAL",
                type = LineType.HEADER_BANNER
            ),
            TerminalLine(
                text = "─────────────────────────────",
                type = LineType.SYSTEM_NOTICE
            ),
            TerminalLine(
                text = "DreamByte OS M Mobile Shell • Prototype Edition",
                type = LineType.SYSTEM_NOTICE
            ),
            TerminalLine(
                text = "Type 'help' for available commands or 'dream help'.",
                type = LineType.SYSTEM_NOTICE
            )
        )
    }

    fun onInputChange(newInput: String) {
        _currentInput.value = newInput
    }

    fun submitCommand() {
        val input = _currentInput.value
        val trimmed = input.trim()
        val currentSettings = settings.value

        // Haptic feedback
        if (currentSettings.hapticFeedbackEnabled) {
            audioFeedback.performHapticClick()
        }
        if (currentSettings.soundEffectsEnabled) {
            audioFeedback.playExecuteClick()
        }

        // Add prompt line
        val promptPrefix = currentSettings.promptPrefix
        val promptLine = TerminalLine(
            text = input,
            type = LineType.PROMPT,
            promptPrefix = promptPrefix
        )

        val updatedLines = _lines.value.toMutableList().apply { add(promptLine) }
        _lines.value = updatedLines
        _currentInput.value = ""
        historyManager.resetNavigation()

        if (trimmed.isEmpty()) {
            return
        }

        historyManager.addCommand(trimmed)

        viewModelScope.launch {
            _isExecuting.value = true
            val context = buildContext(currentSettings)
            val outputLines = executor.executeInput(trimmed, context)

            if (outputLines.any { it.type == LineType.ERROR } && currentSettings.soundEffectsEnabled) {
                audioFeedback.playErrorBeep()
            }

            _lines.value = _lines.value + outputLines
            _isExecuting.value = false
        }
    }

    private fun buildContext(currentSettings: TerminalSettings): CommandContext {
        return CommandContext(
            androidContext = getApplication(),
            settings = currentSettings,
            registry = registry,
            onClearScreen = { clearScreen() },
            onThemeChange = { mode -> updateTheme(mode) },
            onUserChange = { user -> updateUser(user) },
            openSystemFiles = { _openFilePickerEvent.value = true },
            openSettings = { _showSettingsDialog.value = true },
            showMascotDialog = { _showMascotDialog.value = true },
            requestSystemOperation = { opName, details, cmd ->
                _pendingSystemOp.value = PendingSystemOp(opName, details, cmd)
            },
            playSound = { isBeep ->
                if (currentSettings.soundEffectsEnabled) {
                    if (isBeep) audioFeedback.playErrorBeep() else audioFeedback.playKeyClick()
                }
            },
            executeNestedCommand = { cmdText ->
                val innerContext = buildContext(settings.value)
                val lines = executor.executeInput(cmdText, innerContext)
                val combinedText = lines.joinToString("\n") { it.text }
                val hasError = lines.any { it.type == LineType.ERROR }
                CommandResult(
                    output = combinedText,
                    exitCode = if (hasError) 1 else 0,
                    type = if (hasError) LineType.ERROR else LineType.OUTPUT
                )
            }
        )
    }

    fun navigateHistoryUp() {
        val prev = historyManager.getPrevious(_currentInput.value)
        if (prev != null) {
            _currentInput.value = prev
            if (settings.value.hapticFeedbackEnabled) {
                audioFeedback.performHapticClick()
            }
        }
    }

    fun navigateHistoryDown() {
        val next = historyManager.getNext()
        if (next != null) {
            _currentInput.value = next
            if (settings.value.hapticFeedbackEnabled) {
                audioFeedback.performHapticClick()
            }
        }
    }

    fun insertAccessoryText(token: String) {
        if (settings.value.hapticFeedbackEnabled) {
            audioFeedback.performHapticClick()
        }
        if (token == "TAB") {
            handleTabAutocomplete()
            return
        }
        if (token == "ESC") {
            _currentInput.value = ""
            return
        }
        if (token == "CLEAR") {
            clearScreen()
            return
        }
        _currentInput.value = _currentInput.value + token
    }

    private fun handleTabAutocomplete() {
        val current = _currentInput.value.trim()
        if (current.isEmpty()) return

        val tokens = current.split(" ")
        val lastToken = tokens.last().lowercase()
        val allCmds = registry.getAllCommandNames()
        val matches = allCmds.filter { it.startsWith(lastToken) }

        if (matches.size == 1) {
            val completed = (tokens.dropLast(1) + matches[0]).joinToString(" ") + " "
            _currentInput.value = completed
        } else if (matches.size > 1) {
            val suggestionsLine = TerminalLine(
                text = "Matches: " + matches.joinToString("  "),
                type = LineType.SYSTEM_NOTICE
            )
            _lines.value = _lines.value + suggestionsLine
        }
    }

    fun clearScreen() {
        _lines.value = emptyList()
    }

    fun updateTheme(themeMode: ThemeMode) {
        settingsRepo.updateSettings(settings.value.copy(theme = themeMode))
    }

    fun updateUser(newUsername: String) {
        settingsRepo.updateSettings(settings.value.copy(username = newUsername))
    }

    fun updateSettings(newSettings: TerminalSettings) {
        settingsRepo.updateSettings(newSettings)
    }

    fun resetSettings() {
        settingsRepo.resetSettings()
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun openMascotDialog() {
        _showMascotDialog.value = true
    }

    fun closeMascotDialog() {
        _showMascotDialog.value = false
    }

    fun onFilePickerOpened() {
        _openFilePickerEvent.value = false
    }

    fun confirmPendingSystemOperation() {
        val op = _pendingSystemOp.value ?: return
        _pendingSystemOp.value = null
        val confirmationLine = TerminalLine(
            text = "[DREAMBYTE OS AUTHORIZED]: Confirmed '${op.command}'. Simulated execution granted.",
            type = LineType.SUCCESS
        )
        _lines.value = _lines.value + confirmationLine
    }

    fun cancelPendingSystemOperation() {
        val op = _pendingSystemOp.value ?: return
        _pendingSystemOp.value = null
        val cancelLine = TerminalLine(
            text = "[OPERATION ABORTED]: System operation was cancelled by user.",
            type = LineType.WARNING
        )
        _lines.value = _lines.value + cancelLine
    }

    override fun onCleared() {
        super.onCleared()
        audioFeedback.release()
    }
}
