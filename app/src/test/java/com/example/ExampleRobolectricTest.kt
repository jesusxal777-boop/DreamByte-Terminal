package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.command.CommandParser
import com.example.command.CommandRegistry
import com.example.command.impl.AboutCommand
import com.example.command.impl.EchoCommand
import com.example.command.impl.HelpCommand
import com.example.command.impl.ThemeCommand
import com.example.command.impl.VersionCommand
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DreamByte Terminal", appName)
    }

    @Test
    fun `command parser handles quotes and chaining`() {
        val settings = TerminalSettings()
        val pipeline = CommandParser.parseLine("echo \"Hello DreamByte\" && theme", settings)
        assertEquals(2, pipeline.commands.size)
        assertEquals("echo", pipeline.commands[0].parsed.commandName)
        assertEquals(listOf("Hello DreamByte"), pipeline.commands[0].parsed.arguments)
        assertEquals("theme", pipeline.commands[1].parsed.commandName)
    }

    @Test
    fun `command registry registers and retrieves commands`() {
        val registry = CommandRegistry()
        registry.register(HelpCommand())
        registry.register(AboutCommand())
        registry.register(VersionCommand())
        registry.register(EchoCommand())
        registry.register(ThemeCommand())

        assertNotNull(registry.findCommand("help"))
        assertNotNull(registry.findCommand("?")) // alias
        assertNotNull(registry.findCommand("about"))
        assertNotNull(registry.findCommand("version"))
        assertNotNull(registry.findCommand("theme"))
    }

    @Test
    fun `theme mode parser returns expected theme`() {
        assertEquals(ThemeMode.LIQUID_GLASS, ThemeMode.fromId("liquid"))
        assertEquals(ThemeMode.HOLO, ThemeMode.fromId("holo"))
        assertEquals(ThemeMode.RETRO_TERMINAL, ThemeMode.fromId("retro"))
    }
}
