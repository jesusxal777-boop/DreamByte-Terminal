package com.example.command.impl

import com.example.command.Command
import com.example.command.CommandCategory
import com.example.command.CommandContext
import com.example.command.CommandHelp
import com.example.model.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class WgetCommand : Command {
    override val name: String = "wget"
    override val description: String = "Non-interactive network downloader (real HTTP/HTTPS)"
    override val usage: String = "wget [-O <output_file>] [-q] <URL>"
    override val category: CommandCategory = CommandCategory.UTILITY

    override val detailedHelp: CommandHelp = CommandHelp(
        summary = "Network Downloader for DreamByte Terminal",
        synopsis = "wget [options] <URL>",
        options = listOf(
            "-O <file>" to "Write documents to <file> instead of deriving name from URL",
            "-q, --quiet" to "Quiet mode: suppress progress output",
            "--help" to "Display this help screen"
        ),
        description = "Downloads files over HTTP and HTTPS protocols directly into the terminal sandbox. Performs real stream transfers, tracks downloaded byte count, and saves to the active working directory.",
        examples = listOf(
            "wget https://raw.githubusercontent.com/jesusxal777-boop/DreamByte-Package-Repository/main/README.md" to "Download repo README to current directory",
            "wget -O custom.txt https://example.com" to "Save example page as custom.txt"
        )
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun execute(context: CommandContext, args: List<String>): CommandResult = withContext(Dispatchers.IO) {
        if (args.isEmpty() || args.contains("--help")) {
            return@withContext CommandResult.error(
                "wget: missing URL\nUsage: wget [-O <output_file>] [-q] <URL>\nType 'help wget' for more options."
            )
        }

        var outputFile: String? = null
        var quiet = false
        var targetUrl: String? = null

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                arg == "-O" && i + 1 < args.size -> {
                    outputFile = args[i + 1]
                    i += 2
                    continue
                }
                arg.startsWith("-O") -> {
                    outputFile = arg.substring(2)
                    i++
                    continue
                }
                arg == "-q" || arg == "--quiet" -> {
                    quiet = true
                    i++
                    continue
                }
                !arg.startsWith("-") -> {
                    targetUrl = arg
                    i++
                    continue
                }
                else -> {
                    i++
                }
            }
        }

        if (targetUrl.isNullOrBlank()) {
            return@withContext CommandResult.error("wget: no target URL specified.")
        }

        var validUrl = targetUrl
        if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
            validUrl = "https://$validUrl"
        }

        val parsedUrl = try {
            URL(validUrl)
        } catch (e: Exception) {
            return@withContext CommandResult.error("wget: invalid URL format '$validUrl'")
        }

        // Determine output filename
        val targetName = if (!outputFile.isNullOrBlank()) {
            outputFile
        } else {
            val path = parsedUrl.path
            val rawName = path.substringAfterLast('/')
            if (rawName.isBlank()) "index.html" else rawName
        }

        val destination = context.shellEnv.resolvePath(targetName)
        if (!context.shellEnv.isPathWithinSandbox(destination)) {
            return@withContext CommandResult.error("wget: target destination outside allowed sandbox.")
        }

        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()
        if (!quiet) {
            sb.appendLine("--${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}--  $validUrl")
            sb.appendLine("Resolving ${parsedUrl.host}...")
            sb.appendLine("Connecting to ${parsedUrl.host}... connected.")
        }

        try {
            val request = Request.Builder()
                .url(validUrl)
                .header("User-Agent", "DreamByte-Wget/1.0 (Android Native; DreamByte OS M)")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val message = response.message

            if (!quiet) {
                sb.appendLine("HTTP request sent, awaiting response... $code $message")
            }

            if (!response.isSuccessful) {
                return@withContext CommandResult.error(
                    "$sb\n[wget ERROR]: Server responded with HTTP status $code ($message)"
                )
            }

            val body = response.body
                ?: return@withContext CommandResult.error("$sb\n[wget ERROR]: Response body is empty.")

            val contentLength = body.contentLength()
            val lengthStr = if (contentLength >= 0) "$contentLength bytes" else "unspecified"
            val contentType = body.contentType()?.toString() ?: "application/octet-stream"

            if (!quiet) {
                sb.appendLine("Length: $lengthStr ($contentType)")
                sb.appendLine("Saving to: '${destination.name}'")
            }

            destination.parentFile?.mkdirs()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                    }
                }
            }

            val durationSec = (System.currentTimeMillis() - startTime) / 1000.0
            val speedKb = if (durationSec > 0) (downloadedBytes / 1024.0 / durationSec) else 0.0

            if (!quiet) {
                sb.appendLine("───────────────────────────────────────────")
                sb.appendLine("100%[=====================>] $downloadedBytes bytes in ${String.format(java.util.Locale.US, "%.2fs", durationSec)} (${String.format(java.util.Locale.US, "%.1f", speedKb)} KB/s)")
                sb.appendLine("Saved: '${destination.name}' [${downloadedBytes} bytes]")
            }

            CommandResult.success(sb.toString().trimEnd())
        } catch (e: Exception) {
            CommandResult.error(
                "$sb\n[wget ERROR]: Network transfer failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
