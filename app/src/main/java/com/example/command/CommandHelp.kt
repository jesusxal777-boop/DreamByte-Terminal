package com.example.command

data class CommandHelp(
    val summary: String,
    val synopsis: String,
    val options: List<Pair<String, String>> = emptyList(),
    val description: String,
    val examples: List<Pair<String, String>> = emptyList()
)
