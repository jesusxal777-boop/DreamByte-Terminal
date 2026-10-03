package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.command.CommandContext
import com.example.command.CommandExecutor
import com.example.command.CommandRegistry
import com.example.command.impl.AboutCommand
import com.example.command.impl.CatCommand
import com.example.command.impl.EchoCommand
import com.example.command.impl.FilesCommand
import com.example.command.impl.GrepCommand
import com.example.command.impl.HelpCommand
import com.example.command.impl.JavaCommand
import com.example.command.impl.PkgCommand
import com.example.command.impl.PythonCommand
import com.example.command.impl.ScriptCommand
import com.example.command.impl.SysInfoCommand
import com.example.command.impl.ThemeCommand
import com.example.command.impl.VersionCommand
import com.example.command.impl.WgetCommand
import com.example.command.impl.WhoamiCommand
import com.example.model.CommandResult
import com.example.model.TerminalSettings
import com.example.model.ThemeMode
import com.example.python.PythonEngine
import com.example.shell.DreamShellEnvironment
import com.example.shell.DreamShellParser
import com.example.shell.RedirectionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

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
    fun `command registry registers all enhanced commands`() {
        val registry = CommandRegistry()
        registry.register(HelpCommand())
        registry.register(PkgCommand())
        registry.register(FilesCommand())
        registry.register(WgetCommand())
        registry.register(PythonCommand())
        registry.register(JavaCommand())
        registry.register(ScriptCommand())
        registry.register(SysInfoCommand())
        registry.register(WhoamiCommand())

        assertNotNull(registry.findCommand("pkg"))
        assertNotNull(registry.findCommand("files"))
        assertNotNull(registry.findCommand("wget"))
        assertNotNull(registry.findCommand("python"))
        assertNotNull(registry.findCommand("java"))
        assertNotNull(registry.findCommand("sh"))
        assertNotNull(registry.findCommand("sysinfo"))
    }

    @Test
    fun `dreamshell parser parses pipelines and redirections`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val env = DreamShellEnvironment(context)

        val nodes = DreamShellParser.parse("echo \"Hello DreamByte\" > test.txt && cat < test.txt | grep Hello", env)
        assertEquals(2, nodes.size)

        val firstPipeline = nodes[0].pipeline
        assertEquals(1, firstPipeline.commands.size)
        assertEquals("echo", firstPipeline.commands[0].commandName)
        assertEquals(listOf("Hello DreamByte"), firstPipeline.commands[0].arguments)
        assertEquals(1, firstPipeline.commands[0].redirections.size)
        assertEquals(RedirectionType.WRITE, firstPipeline.commands[0].redirections[0].type)
        assertEquals("test.txt", firstPipeline.commands[0].redirections[0].target)

        val secondPipeline = nodes[1].pipeline
        assertEquals(2, secondPipeline.commands.size)
        assertEquals("cat", secondPipeline.commands[0].commandName)
        assertEquals(RedirectionType.READ_INPUT, secondPipeline.commands[0].redirections[0].type)
        assertEquals("grep", secondPipeline.commands[1].commandName)
        assertEquals(listOf("Hello"), secondPipeline.commands[1].arguments)
    }

    @Test
    fun `dreamshell variable expansion works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val env = DreamShellEnvironment(context)
        env.setVariable("MY_VAR", "DreamByteOS")

        val expanded = DreamShellParser.expandVariables("Value is: \$MY_VAR", env)
        assertEquals("Value is: DreamByteOS", expanded)

        val expandedCode = DreamShellParser.expandVariables("Exit: \$?", env)
        assertEquals("Exit: 0", expandedCode)
    }

    @Test
    fun `python engine executes real statements and loops`() {
        val engine = PythonEngine()
        val script = """
            x = 10
            y = 25
            total = x + y
            print("Total is:", total)
            
            nums = []
            for i in range(3):
                nums += [i * 2]
            print("Nums:", nums)
        """.trimIndent()

        val res = engine.execute(script)
        assertEquals(0, res.exitCode)
        assertTrue(res.stdout.contains("Total is: 35"))
        assertTrue(res.stdout.contains("Nums: [0, 2, 4]"))
    }

    @Test
    fun `files command writes reads and deletes inside sandbox`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val env = DreamShellEnvironment(context)
        val registry = CommandRegistry()
        val filesCmd = FilesCommand()
        registry.register(filesCmd)

        val dummyCtx = CommandContext(
            androidContext = context,
            settings = TerminalSettings(),
            registry = registry,
            shellEnv = env,
            onClearScreen = {},
            onThemeChange = {},
            onUserChange = {},
            openSystemFiles = {},
            openSettings = {},
            showMascotDialog = {},
            requestSystemOperation = { _, _, _ -> },
            playSound = {},
            executeNestedCommand = { CommandResult.success("") }
        )

        // Write
        val writeRes = filesCmd.execute(dummyCtx, listOf("write", "test_file.txt", "Hello", "DreamByte", "FS"))
        assertEquals(0, writeRes.exitCode)

        // Read
        val readRes = filesCmd.execute(dummyCtx, listOf("read", "test_file.txt"))
        assertEquals(0, readRes.exitCode)
        assertEquals("Hello DreamByte FS", readRes.output)

        // Delete
        val delRes = filesCmd.execute(dummyCtx, listOf("delete", "test_file.txt"))
        assertEquals(0, delRes.exitCode)

        val target = env.resolvePath("test_file.txt")
        assertFalse(target.exists())
    }

    @Test
    fun `help command generates detailed manuals for specific tools`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val env = DreamShellEnvironment(context)
        val registry = CommandRegistry().apply {
            register(HelpCommand())
            register(PkgCommand())
            register(WgetCommand())
            register(PythonCommand())
            register(FilesCommand())
        }

        val dummyCtx = CommandContext(
            androidContext = context,
            settings = TerminalSettings(),
            registry = registry,
            shellEnv = env,
            onClearScreen = {},
            onThemeChange = {},
            onUserChange = {},
            openSystemFiles = {},
            openSettings = {},
            showMascotDialog = {},
            requestSystemOperation = { _, _, _ -> },
            playSound = {},
            executeNestedCommand = { CommandResult.success("") }
        )

        val helpCmd = HelpCommand()

        val pkgHelp = helpCmd.execute(dummyCtx, listOf("pkg"))
        assertTrue(pkgHelp.output.contains("DREAMBYTE MANUAL: PKG"))
        assertTrue(pkgHelp.output.contains("SYNOPSIS"))
        assertTrue(pkgHelp.output.contains("EXAMPLES"))

        val wgetHost = helpCmd.execute(dummyCtx, listOf("wget"))
        assertTrue(wgetHost.output.contains("DREAMBYTE MANUAL: WGET"))

        val pyHelp = helpCmd.execute(dummyCtx, listOf("python"))
        assertTrue(pyHelp.output.contains("DREAMBYTE MANUAL: PYTHON"))
    }
}
