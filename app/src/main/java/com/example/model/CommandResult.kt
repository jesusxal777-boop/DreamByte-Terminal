package com.example.model

data class CommandResult(
    val output: String = "",
    val exitCode: Int = 0,
    val type: LineType = if (exitCode == 0) LineType.OUTPUT else LineType.ERROR,
    val lines: List<TerminalLine> = emptyList(),
    val action: CommandAction? = null
) {
    companion object {
        fun success(text: String) = CommandResult(output = text, exitCode = 0, type = LineType.OUTPUT)
        fun successHighlight(text: String) = CommandResult(output = text, exitCode = 0, type = LineType.SUCCESS)
        fun error(text: String, exitCode: Int = 1) = CommandResult(output = text, exitCode = exitCode, type = LineType.ERROR)
        fun warning(text: String) = CommandResult(output = text, exitCode = 0, type = LineType.WARNING)
        fun clear() = CommandResult(output = "", exitCode = 0, action = CommandAction.CLEAR_SCREEN)
        fun system(text: String) = CommandResult(output = text, exitCode = 0, type = LineType.SYSTEM_NOTICE)
    }
}

sealed interface CommandAction {
    data object CLEAR_SCREEN : CommandAction
    data class CHANGE_THEME(val mode: ThemeMode) : CommandAction
    data class CHANGE_USER(val username: String) : CommandAction
    data object OPEN_FILES : CommandAction
    data object OPEN_SETTINGS : CommandAction
    data object SHOW_MASCOT : CommandAction
    data class REQUEST_SYSTEM_OPERATION(val operationName: String, val details: String, val command: String) : CommandAction
}
