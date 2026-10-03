package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import java.io.File

class JavaCommand : Command {
    override val name: String = "java"
    override val description: String = "Java SE Runtime Environment launcher"
    override val aliases: List<String> = listOf("javac")
    override val usage: String = "java [--version | -jar <file.jar> | <class>]"
    override val category: CommandCategory = CommandCategory.SHELL

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Java SE Runtime & Compiler Environment",
        synopsis = "java [options] <mainclass> [args...] | javac [options] <source files>",
        options = listOf(
            "--version" to "Print product version to the output stream and exit",
            "-jar <file.jar>" to "Execute a program packaged in a JAR file",
            "-cp, -classpath" to "Specify search path of directories and zip/jar archives"
        ),
        description = "Provides the execution layer for Java SE applications. When a Java SDK package is published to the DreamByte Package Repository, it is installed into the prefix via 'pkg install java'.",
        examples = listOf(
            "java --version" to "Show Java runtime specifications",
            "javac --version" to "Show Java compiler status"
        )
    )

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
        // Priority 1: Check if installed via pkg into $PREFIX/bin/java
        val nativeBin = File(context.shellEnv.binDir, "java")
        if (nativeBin.exists() && nativeBin.canExecute()) {
            return try {
                val proc = ProcessBuilder(listOf(nativeBin.absolutePath) + args)
                    .directory(context.shellEnv.currentDirectory)
                    .redirectErrorStream(true)
                    .start()
                val text = proc.inputStream.bufferedReader().readText()
                val code = proc.waitFor()
                if (code == 0) CommandResult.success(text) else CommandResult.error(text, exitCode = code)
            } catch (e: Exception) {
                CommandResult.error("Execution of native java binary failed: ${e.message}")
            }
        }

        // Real JVM host environment information
        val jvmVersion = System.getProperty("java.version") ?: "17-internal"
        val vmName = System.getProperty("java.vm.name") ?: "Android Runtime (ART)"
        val vmVersion = System.getProperty("java.vm.version") ?: "2.1.0"
        val vendor = System.getProperty("java.vendor") ?: "The Android Project"

        if (args.contains("--version") || args.contains("-version")) {
            return CommandResult.success(
                """
                openjdk version "$jvmVersion"
                $vmName (build $vmVersion)
                Vendor: $vendor
                Package status: Architecture ready for OpenJDK / ECJ package.
                Use 'pkg search java' to monitor repository availability.
                """.trimIndent()
            )
        }

        return CommandResult.warning(
            """
            Java runtime launcher:
              Active Host VM : $vmName ($vmVersion)
              Java Spec      : $jvmVersion
              Binary Status  : Package 'java' is prepared in DreamByte Package System.
              To install full OpenJDK standalone toolchain once published:
                pkg install java
            """.trimIndent()
        )
    }
}
