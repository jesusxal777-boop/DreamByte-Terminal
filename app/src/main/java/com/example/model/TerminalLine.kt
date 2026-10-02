package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LineType {
    PROMPT,
    OUTPUT,
    SUCCESS,
    ERROR,
    WARNING,
    SYSTEM_NOTICE,
    HEADER_BANNER
}

data class TerminalLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis(),
    val promptPrefix: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
