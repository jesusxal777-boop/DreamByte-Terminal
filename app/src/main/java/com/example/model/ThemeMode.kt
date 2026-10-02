package com.example.model

enum class ThemeMode(val displayName: String, val id: String) {
    LIQUID_GLASS("Liquid Glass", "liquid"),
    HOLO("Holo", "holo"),
    RETRO_TERMINAL("Retro Terminal", "retro");

    companion object {
        fun fromId(id: String): ThemeMode {
            return entries.firstOrNull { 
                it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) 
            } ?: LIQUID_GLASS
        }
    }
}
