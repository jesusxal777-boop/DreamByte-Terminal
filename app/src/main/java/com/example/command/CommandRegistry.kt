package com.example.command

import java.util.concurrent.ConcurrentHashMap

class CommandRegistry {
    private val commandsByName = ConcurrentHashMap<String, Command>()
    private val commandsByAlias = ConcurrentHashMap<String, Command>()

    fun register(command: Command) {
        commandsByName[command.name.lowercase()] = command
        for (alias in command.aliases) {
            commandsByAlias[alias.lowercase()] = command
        }
    }

    fun unregister(commandName: String) {
        val cmd = commandsByName.remove(commandName.lowercase()) ?: return
        for (alias in cmd.aliases) {
            commandsByAlias.remove(alias.lowercase())
        }
    }

    fun findCommand(nameOrAlias: String): Command? {
        val lower = nameOrAlias.lowercase()
        return commandsByName[lower] ?: commandsByAlias[lower]
    }

    fun getAllCommands(): List<Command> {
        return commandsByName.values.distinct()
    }

    fun getAllCommandNames(): List<String> {
        return (commandsByName.keys + commandsByAlias.keys).distinct().sorted()
    }
}
