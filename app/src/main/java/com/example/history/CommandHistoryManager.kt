package com.example.history

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommandHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("dreambyte_cmd_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<String>>(loadSavedHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    // Navigation index: -1 means currently typing new command
    private var currentIndex = -1

    private fun loadSavedHistory(): List<String> {
        val raw = prefs.getString("history_items", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    private fun saveHistory(list: List<String>) {
        val raw = list.takeLast(100).joinToString("|||")
        prefs.edit().putString("history_items", raw).apply()
    }

    fun addCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        val current = _history.value.toMutableList()
        // If the command is duplicate of the last entry, don't repeat consecutively
        if (current.isNotEmpty() && current.last() == trimmed) {
            currentIndex = -1
            return
        }
        current.add(trimmed)
        _history.value = current
        saveHistory(current)
        currentIndex = -1
    }

    fun getPrevious(currentInput: String): String? {
        val list = _history.value
        if (list.isEmpty()) return null
        if (currentIndex == -1) {
            currentIndex = list.size - 1
        } else if (currentIndex > 0) {
            currentIndex--
        }
        return list.getOrNull(currentIndex)
    }

    fun getNext(): String? {
        val list = _history.value
        if (list.isEmpty() || currentIndex == -1) return null
        if (currentIndex < list.size - 1) {
            currentIndex++
            return list[currentIndex]
        } else {
            currentIndex = -1
            return ""
        }
    }

    fun resetNavigation() {
        currentIndex = -1
    }

    fun clear() {
        _history.value = emptyList()
        prefs.edit().remove("history_items").apply()
        currentIndex = -1
    }
}
