package com.example.shell

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DreamShellEnvironment(private val appContext: Context) {

    val homeDir: File = File(appContext.filesDir, "home").apply { mkdirs() }
    val prefixDir: File = File(appContext.filesDir, "usr").apply { mkdirs() }
    val binDir: File = File(prefixDir, "bin").apply { mkdirs() }
    val dbpkgDir: File = File(prefixDir, "var/lib/dbpkg").apply { mkdirs() }

    private val variables = ConcurrentHashMap<String, String>()

    var currentDirectory: File = homeDir
        private set

    var lastExitCode: Int = 0
        set(value) {
            field = value
            variables["?"] = value.toString()
        }

    init {
        variables["HOME"] = homeDir.absolutePath
        variables["PREFIX"] = prefixDir.absolutePath
        variables["PATH"] = "${binDir.absolutePath}:${prefixDir.absolutePath}/local/bin:/system/bin"
        variables["PWD"] = homeDir.absolutePath
        variables["SHELL"] = "dreamshell"
        variables["0"] = "dbsh"
        variables["?"] = "0"
        variables["USER"] = "jake"
        variables["HOST"] = "dreambyte"
        variables["OS"] = "DreamByte OS M"
        variables["VERSION"] = "0.1.0"
    }

    fun getVariable(key: String): String? {
        return variables[key]
    }

    fun setVariable(key: String, value: String) {
        variables[key] = value
        if (key == "PWD") {
            val f = File(value)
            if (f.exists() && f.isDirectory) {
                currentDirectory = f
            }
        }
    }

    fun getAllVariables(): Map<String, String> = variables.toMap()

    fun changeDirectory(path: String): Result<File> {
        val target = resolvePath(path)
        if (!target.exists()) {
            return Result.failure(IllegalArgumentException("cd: no such file or directory: $path"))
        }
        if (!target.isDirectory) {
            return Result.failure(IllegalArgumentException("cd: not a directory: $path"))
        }
        // Sandbox safety check: cannot escape outside appContext.filesDir or external storage
        val canonical = target.canonicalFile
        val allowedRoots = listOfNotNull(
            appContext.filesDir.canonicalFile,
            appContext.cacheDir.canonicalFile,
            appContext.getExternalFilesDir(null)?.canonicalFile
        )

        val isAllowed = allowedRoots.any { root ->
            canonical.startsWith(root)
        }

        if (!isAllowed) {
            return Result.failure(SecurityException("cd: access restricted by Android sandbox"))
        }

        currentDirectory = canonical
        variables["PWD"] = canonical.absolutePath
        return Result.success(canonical)
    }

    fun resolvePath(path: String): File {
        val trimmed = path.trim()
        val expanded = if (trimmed.startsWith("~")) {
            trimmed.replaceFirst("~", homeDir.absolutePath)
        } else {
            trimmed
        }

        val file = File(expanded)
        return if (file.isAbsolute) {
            file
        } else {
            File(currentDirectory, expanded)
        }
    }

    fun isPathWithinSandbox(file: File): Boolean {
        return try {
            val canonical = file.canonicalFile
            val allowedRoots = listOfNotNull(
                appContext.filesDir.canonicalFile,
                appContext.cacheDir.canonicalFile,
                appContext.getExternalFilesDir(null)?.canonicalFile
            )
            allowedRoots.any { root -> canonical.startsWith(root) }
        } catch (_: Exception) {
            false
        }
    }
}
