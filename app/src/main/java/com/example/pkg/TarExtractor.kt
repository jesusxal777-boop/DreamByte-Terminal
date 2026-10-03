package com.example.pkg

import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

object TarExtractor {

    /**
     * Extracts a .dbpkg (tar.gz archive) safely into the target directory.
     * Enforces the DreamByte security rules:
     * - Rejects absolute paths and paths containing '..'
     * - Rejects backslashes
     * - Rejects symlinks/hardlinks
     * Returns the list of relative file paths extracted.
     */
    fun extract(archiveFile: File, destinationDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        destinationDir.mkdirs()

        archiveFile.inputStream().use { fileIn ->
            GZIPInputStream(fileIn).use { gzipIn ->
                extractTarStream(gzipIn, destinationDir, extractedFiles)
            }
        }

        return extractedFiles
    }

    private fun extractTarStream(input: InputStream, destDir: File, extractedList: MutableList<File>) {
        val header = ByteArray(512)

        while (true) {
            val bytesRead = readFully(input, header)
            if (bytesRead < 512) break

            // Check for empty block (end of tar archive)
            var allZero = true
            for (b in header) {
                if (b.toInt() != 0) {
                    allZero = false
                    break
                }
            }
            if (allZero) break

            // Parse entry name (offset 0, length 100)
            val nameBytes = header.copyOfRange(0, 100)
            val rawName = String(nameBytes).trim { it <= ' ' || it == '\u0000' }
            if (rawName.isEmpty()) continue

            // Parse file size (offset 124, length 12 in octal ASCII)
            val sizeStr = String(header.copyOfRange(124, 136)).trim { it <= ' ' || it == '\u0000' }
            val fileSize = try {
                if (sizeStr.isEmpty()) 0L else sizeStr.toLong(8)
            } catch (_: Exception) {
                0L
            }

            // Parse type flag (offset 156, length 1)
            val typeFlag = header[156].toInt().toChar()

            // DreamByte security check
            validatePath(rawName, typeFlag)

            val targetFile = File(destDir, rawName)

            // '5' is directory, '0' or '\0' is regular file
            if (typeFlag == '5' || rawName.endsWith("/")) {
                targetFile.mkdirs()
            } else if (typeFlag == '0' || typeFlag == '\u0000') {
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { out ->
                    var remaining = fileSize
                    val buffer = ByteArray(4096)
                    while (remaining > 0) {
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val r = input.read(buffer, 0, toRead)
                        if (r < 0) break
                        out.write(buffer, 0, r)
                        remaining -= r
                    }
                }

                // If in bin/ or executable, set executable permission
                if (rawName.startsWith("bin/") || rawName.contains("/bin/")) {
                    targetFile.setExecutable(true, false)
                    targetFile.setReadable(true, false)
                }

                extractedList.add(targetFile)
            } else {
                throw SecurityException("dbpkg: unsupported or unsafe archive entry type '$typeFlag' for $rawName")
            }

            // Tar entries are padded to 512-byte blocks
            val padding = (512 - (fileSize % 512)) % 512
            if (padding > 0) {
                skipFully(input, padding)
            }
        }
    }

    private fun validatePath(path: String, typeFlag: Char) {
        if (path.startsWith("/") || path.contains("\\")) {
            throw SecurityException("dbpkg: invalid path with absolute or backward slash: $path")
        }
        val segments = path.split("/")
        if (segments.any { it == ".." }) {
            throw SecurityException("dbpkg: path traversal attack detected: $path")
        }
        // Reject symlinks ('2') and hard links ('1') per dbpkg-format.md
        if (typeFlag == '1' || typeFlag == '2') {
            throw SecurityException("dbpkg: symlinks and hardlinks are rejected by security policy: $path")
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val r = input.read(buffer, total, buffer.size - total)
            if (r < 0) break
            total += r
        }
        return total
    }

    private fun skipFully(input: InputStream, bytesToSkip: Long) {
        var remaining = bytesToSkip
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }
}
