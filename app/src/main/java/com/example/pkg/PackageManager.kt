package com.example.pkg

import android.os.Build
import com.example.shell.DreamShellEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RepoPackageInfo(
    val name: String,
    val version: String,
    val description: String,
    val type: String,
    val status: String,
    val architecture: List<String>,
    val dependencies: List<String>,
    val manifest: String,
    val manifestSha256: String
)

data class InstalledPackage(
    val name: String,
    val version: String,
    val installedAt: String,
    val files: List<String>
)

class PackageManager(
    private val shellEnv: DreamShellEnvironment
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val repoBaseUrl = "https://raw.githubusercontent.com/jesusxal777-boop/DreamByte-Package-Repository/main/"
    private val indexUrl = "${repoBaseUrl}repository.json"

    private val dbpkgDir: File get() = shellEnv.dbpkgDir
    private val localIndexFile: File get() = File(dbpkgDir, "repository.json")
    private val installedFile: File get() = File(dbpkgDir, "installed.json")

    /**
     * pkg update: Downloads and caches repository.json from GitHub
     */
    suspend fun update(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(indexUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP error ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response from repository"))

                val json = JSONObject(body)
                val formatVer = json.optInt("format_version", 1)
                if (formatVer != 1) {
                    return@withContext Result.failure(
                        Exception("Unsupported repository format_version: $formatVer (expected 1)")
                    )
                }

                val repoVer = json.optInt("repository_version", 0)
                val pkgCount = json.optInt("package_count", 0)

                localIndexFile.parentFile?.mkdirs()
                localIndexFile.writeText(body)

                Result.success(
                    "Repository updated successfully.\n" +
                    "  Source: $indexUrl\n" +
                    "  Repository Version: $repoVer\n" +
                    "  Available Packages: $pkgCount\n" +
                    "Local cache updated at ${localIndexFile.name}"
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update repository: ${e.localizedMessage}"))
        }
    }

    /**
     * pkg search <query>
     */
    fun search(query: String): Result<List<RepoPackageInfo>> {
        val packages = loadLocalIndex()
            ?: return Result.failure(Exception("Repository index not found. Please run 'pkg update' first."))

        val q = query.lowercase().trim()
        val matched = packages.filter {
            q.isEmpty() || it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
        return Result.success(matched)
    }

    /**
     * pkg list [--installed]
     */
    fun list(onlyInstalled: Boolean = false): Result<String> {
        val installed = loadInstalledPackages()

        if (onlyInstalled) {
            if (installed.isEmpty()) {
                return Result.success("No packages installed currently.")
            }
            val sb = StringBuilder()
            sb.appendLine("INSTALLED PACKAGES (${installed.size})")
            sb.appendLine("───────────────────────────────────────────")
            installed.values.sortedBy { it.name }.forEach { p ->
                sb.appendLine("  ${p.name.padEnd(16)} v${p.version} (installed ${p.installedAt})")
            }
            return Result.success(sb.toString().trimEnd())
        }

        val all = loadLocalIndex()
            ?: return Result.failure(Exception("Repository index not found. Please run 'pkg update' first."))

        val sb = StringBuilder()
        sb.appendLine("AVAILABLE PACKAGES (${all.size})")
        sb.appendLine("───────────────────────────────────────────")
        all.sortedBy { it.name }.forEach { p ->
            val isInst = if (installed.containsKey(p.name)) " [installed]" else ""
            val statusTag = "[${p.status}]"
            sb.appendLine("  ${p.name.padEnd(14)} v${p.version.padEnd(8)} ${statusTag.padEnd(11)} ${p.description}$isInst")
        }
        return Result.success(sb.toString().trimEnd())
    }

    /**
     * pkg info <name>
     */
    suspend fun info(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val all = loadLocalIndex()
            ?: return@withContext Result.failure(Exception("Repository index not found. Please run 'pkg update' first."))

        val entry = all.find { it.name.equals(packageName, ignoreCase = true) }
            ?: return@withContext Result.failure(Exception("Package '$packageName' not found in repository."))

        val installed = loadInstalledPackages()[entry.name]
        val installedStr = if (installed != null) "Installed (v${installed.version})" else "Not installed"

        // Fetch detailed manifest from repository
        val manifestUrl = "${repoBaseUrl}${entry.manifest}"
        var manifestObj: JSONObject? = null
        try {
            val req = Request.Builder().url(manifestUrl).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val gotSha = sha256Hex(body.toByteArray())
                    if (gotSha.equals(entry.manifestSha256, ignoreCase = true)) {
                        manifestObj = JSONObject(body)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val sb = StringBuilder()
        sb.appendLine("PACKAGE: ${entry.name}")
        sb.appendLine("───────────────────────────────────────────")
        sb.appendLine("Version      : ${entry.version}")
        sb.appendLine("Status       : ${entry.status.uppercase()}")
        sb.appendLine("Type         : ${entry.type}")
        sb.appendLine("State        : $installedStr")
        sb.appendLine("Description  : ${entry.description}")
        sb.appendLine("Architecture : ${entry.architecture.joinToString(", ")}")
        sb.appendLine("Dependencies : ${if (entry.dependencies.isEmpty()) "None" else entry.dependencies.joinToString(", ")}")

        manifestObj?.let { m ->
            sb.appendLine("Maintainer   : ${m.optString("maintainer", "DreamByte Studios")}")
            sb.appendLine("License      : ${m.optString("license", "Unknown")}")
            sb.appendLine("Scope        : ${m.optString("scope", "user")}")
            if (m.has("homepage")) {
                sb.appendLine("Homepage     : ${m.optString("homepage")}")
            }
        }

        sb.appendLine("Manifest     : ${entry.manifest}")
        return@withContext Result.success(sb.toString().trimEnd())
    }

    /**
     * pkg install <name>
     */
    suspend fun install(
        packageName: String,
        onProgress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val all = loadLocalIndex()
            ?: return@withContext Result.failure(Exception("Repository index not found. Please run 'pkg update' first."))

        val entry = all.find { it.name.equals(packageName, ignoreCase = true) }
            ?: return@withContext Result.failure(Exception("Package '$packageName' not found in repository."))

        onProgress("Checking package '${entry.name}' manifest...")
        val manifestUrl = "${repoBaseUrl}${entry.manifest}"
        val manifestJsonString: String
        try {
            val req = Request.Builder().url(manifestUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download manifest: HTTP ${resp.code}"))
            }
            manifestJsonString = resp.body?.string() ?: ""
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Cannot fetch manifest for ${entry.name}: ${e.message}"))
        }

        // Verify manifest SHA-256 against repository.json announced hash
        val calculatedManifestSha = sha256Hex(manifestJsonString.toByteArray())
        if (!calculatedManifestSha.equals(entry.manifestSha256, ignoreCase = true)) {
            return@withContext Result.failure(
                SecurityException(
                    "Manifest SHA-256 verification failed for ${entry.name}!\n" +
                    "  Expected: ${entry.manifestSha256}\n" +
                    "  Got     : $calculatedManifestSha"
                )
            )
        }

        val manifest = JSONObject(manifestJsonString)
        val status = manifest.optString("status", entry.status)

        // Rule: Reject planned or metadata-only packages
        if (status in listOf("planned", "metadata-only")) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Package '${entry.name}' is registered as '$status' in the official repository.\n" +
                    "Status: Planned for future release. No downloadable artifact (.dbpkg) is currently published.\n" +
                    "DreamByte Package Policy prohibits inventing unreleased downloads."
                )
            )
        }

        // Rule: Check scope
        val scope = manifest.optString("scope", "user")
        if (scope == "system") {
            return@withContext Result.failure(
                SecurityException(
                    "Package '${entry.name}' requires scope 'system' (DreamByte OS M privileged backend).\n" +
                    "Rejected by Android sandbox security policy."
                )
            )
        }

        // Rule: Architecture check
        val supportedArchs = manifest.optJSONArray("architecture")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: entry.architecture

        val deviceAbis = Build.SUPPORTED_ABIS.toList()
        val matchingAbi = deviceAbis.firstOrNull { supportedArchs.contains(it) }
            ?: if (supportedArchs.contains("all")) "all" else null

        if (matchingAbi == null) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "Unsupported architecture. Package '${entry.name}' supports: [${supportedArchs.joinToString(", ")}], but device ABI order is: [${deviceAbis.joinToString(", ")}]."
                )
            )
        }

        // Rule: Locate download info
        if (!manifest.has("download")) {
            return@withContext Result.failure(Exception("Manifest has no 'download' configuration."))
        }

        val downloadObj = manifest.getJSONObject("download")
        val downloadSpec: JSONObject = if (downloadObj.has("url")) {
            downloadObj
        } else if (downloadObj.has(matchingAbi)) {
            downloadObj.getJSONObject(matchingAbi)
        } else {
            return@withContext Result.failure(
                Exception("No download artifact for ABI '$matchingAbi' in manifest.")
            )
        }

        val downloadUrl = downloadSpec.getString("url")
        val expectedSha = downloadSpec.getString("sha256")
        val expectedSize = downloadSpec.optLong("size", -1L)

        if (!downloadUrl.startsWith("https://")) {
            return@withContext Result.failure(SecurityException("Insecure download URL: Only HTTPS is allowed."))
        }

        onProgress("Downloading artifact: $downloadUrl")
        val tempFile = File(shellEnv.prefixDir, "tmp_${entry.name}_${System.currentTimeMillis()}.dbpkg")

        try {
            val downloadReq = Request.Builder().url(downloadUrl).build()
            val downloadResp = client.newCall(downloadReq).execute()
            if (!downloadResp.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to download package archive: HTTP ${downloadResp.code} ${downloadResp.message}")
                )
            }

            val body = downloadResp.body
                ?: return@withContext Result.failure(Exception("Empty package archive body"))

            val md = MessageDigest.getInstance("SHA-256")
            tempFile.parentFile?.mkdirs()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        md.update(buffer, 0, read)
                    }
                }
            }

            val actualSha = md.digest().joinToString("") { "%02x".format(it) }

            onProgress("Verifying SHA-256 checksum...")
            if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                tempFile.delete()
                return@withContext Result.failure(
                    SecurityException(
                        "SHA-256 MISMATCH for ${entry.name}!\n" +
                        "  Expected: $expectedSha\n" +
                        "  Actual  : $actualSha\n" +
                        "Refusing installation: corrupted or altered archive."
                    )
                )
            }

            onProgress("Extracting package to ${shellEnv.prefixDir.name}/...")
            val extractedFiles = TarExtractor.extract(tempFile, shellEnv.prefixDir)
            tempFile.delete()

            // Register installation
            val relativePaths = extractedFiles.map { it.relativeTo(shellEnv.prefixDir).path }
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val installedMap = loadInstalledPackages().toMutableMap()
            installedMap[entry.name] = InstalledPackage(
                name = entry.name,
                version = entry.version,
                installedAt = nowStr,
                files = relativePaths
            )
            saveInstalledPackages(installedMap)

            Result.success(
                "Successfully installed ${entry.name} v${entry.version} [${matchingAbi}].\n" +
                "  Extracted ${relativePaths.size} file(s) into ${shellEnv.prefixDir.absolutePath}\n" +
                "  Binaries available in: ${shellEnv.binDir.absolutePath}"
            )
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }

    /**
     * pkg uninstall <name>
     */
    fun uninstall(packageName: String): Result<String> {
        val installedMap = loadInstalledPackages().toMutableMap()
        val target = installedMap[packageName]
            ?: return Result.failure(Exception("Package '$packageName' is not currently installed."))

        var deletedCount = 0
        for (relPath in target.files) {
            val file = File(shellEnv.prefixDir, relPath)
            if (file.exists() && file.isFile) {
                if (file.delete()) deletedCount++
            }
        }

        installedMap.remove(packageName)
        saveInstalledPackages(installedMap)

        return Result.success("Uninstalled $packageName v${target.version}. Removed $deletedCount file(s).")
    }

    private fun loadLocalIndex(): List<RepoPackageInfo>? {
        if (!localIndexFile.exists()) return null
        return try {
            val json = JSONObject(localIndexFile.readText())
            val arr = json.getJSONArray("packages")
            val list = mutableListOf<RepoPackageInfo>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val archArr = obj.optJSONArray("architecture")
                val archList = if (archArr != null) {
                    (0 until archArr.length()).map { archArr.getString(it) }
                } else emptyList()

                val depArr = obj.optJSONArray("dependencies")
                val depList = if (depArr != null) {
                    (0 until depArr.length()).map { depArr.getString(it) }
                } else emptyList()

                list.add(
                    RepoPackageInfo(
                        name = obj.getString("name"),
                        version = obj.getString("version"),
                        description = obj.optString("description", ""),
                        type = obj.optString("type", "cli"),
                        status = obj.optString("status", "planned"),
                        architecture = archList,
                        dependencies = depList,
                        manifest = obj.getString("manifest"),
                        manifestSha256 = obj.getString("manifest_sha256")
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    private fun loadInstalledPackages(): Map<String, InstalledPackage> {
        if (!installedFile.exists()) return emptyMap()
        return try {
            val json = JSONObject(installedFile.readText())
            val instObj = json.optJSONObject("installed") ?: return emptyMap()
            val map = mutableMapOf<String, InstalledPackage>()
            val keys = instObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val pkgObj = instObj.getJSONObject(key)
                val filesArr = pkgObj.optJSONArray("files")
                val filesList = if (filesArr != null) {
                    (0 until filesArr.length()).map { filesArr.getString(it) }
                } else emptyList()

                map[key] = InstalledPackage(
                    name = key,
                    version = pkgObj.optString("version", "1.0.0"),
                    installedAt = pkgObj.optString("installed_at", ""),
                    files = filesList
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveInstalledPackages(map: Map<String, InstalledPackage>) {
        installedFile.parentFile?.mkdirs()
        val root = JSONObject()
        val instObj = JSONObject()
        for ((name, pkg) in map) {
            val p = JSONObject()
            p.put("name", pkg.name)
            p.put("version", pkg.version)
            p.put("installed_at", pkg.installedAt)
            val fArr = JSONArray()
            pkg.files.forEach { fArr.put(it) }
            p.put("files", fArr)
            instObj.put(name, p)
        }
        root.put("installed", instObj)
        installedFile.writeText(root.toString(2))
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
