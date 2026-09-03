package com.aatmn2000.aibuilder.core.project

import com.aatmn2000.aibuilder.core.security.PathTraversalGuard
import com.aatmn2000.aibuilder.core.security.SecretScanner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipInputStream

/**
 * Validates and reconstructs an imported project ZIP.
 *
 * Safety rules (see docs/architecture.md):
 *  - entry budgets: max entry count, per-entry size, total uncompressed
 *    size (zip-bomb protection, enforced while streaming);
 *  - every entry name must pass [PathTraversalGuard];
 *  - `project.json` must be present and valid;
 *  - the declared entry point file must exist in the ZIP;
 *  - non-UTF-8 (binary) entries are reported and skipped (V1 projects are
 *    text-only);
 *  - imported code is NEVER executed here — it is only parsed.
 */
object ZipImporter {

    data class Result(
        val project: GeneratedProject?,
        val issues: List<ImportIssue>
    ) {
        val isSuccessful: Boolean
            get() = project != null
    }

    fun importZip(bytes: ByteArray): Result {
        val warnings = mutableListOf<ImportIssue>()
        var manifestRaw: String? = null
        val files = mutableListOf<ProjectFile>()
        var entryCount = 0
        var totalBytes = 0L

        val input = ZipInputStream(ByteArrayInputStream(bytes), StandardCharsets.UTF_8)
        try {
            while (true) {
                val entry = input.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ENTRIES) {
                    return Result(
                        null,
                        warnings + ImportIssue(
                            ImportIssue.Severity.ERROR,
                            "ZIP has too many entries (zip-bomb protection)"
                        )
                    )
                }
                val entryName = entry.name
                if (entry.isDirectory) {
                    input.closeEntry()
                    continue
                }

                val normalized = PathTraversalGuard.normalize(entryName)
                if (normalized == null) {
                    return Result(
                        null,
                        warnings + ImportIssue(
                            ImportIssue.Severity.ERROR,
                            "Unsafe entry name rejected: $entryName",
                            entryName
                        )
                    )
                }
                if (entry.compressedSize > MAX_ENTRY_BYTES) {
                    return Result(
                        null,
                        warnings + ImportIssue(
                            ImportIssue.Severity.ERROR,
                            "Entry too large: $normalized",
                            entryName
                        )
                    )
                }

                val contentBytes = ByteArrayOutputStream()
                var read = 0L
                val chunk = ByteArray(8192)
                while (true) {
                    val n = input.read(chunk)
                    if (n == -1) break
                    read += n
                    totalBytes += n
                    if (read > MAX_ENTRY_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                        return Result(
                            null,
                            warnings + ImportIssue(
                                ImportIssue.Severity.ERROR,
                                "ZIP size budget exceeded (zip-bomb protection)"
                            )
                        )
                    }
                    contentBytes.write(chunk, 0, n)
                }
                input.closeEntry()

                val text = decodeStrictUtf8(contentBytes.toByteArray())
                when {
                    text == null ->
                        warnings += ImportIssue(
                            ImportIssue.Severity.WARNING,
                            "Binary entry skipped: $normalized",
                            entryName
                        )
                    normalized == MANIFEST_FILE -> manifestRaw = text
                    else -> files += ProjectFile(normalized, text, ProjectFile.inferLanguage(normalized))
                }
            }
        } catch (e: java.io.IOException) {
            return Result(
                null,
                warnings + ImportIssue(ImportIssue.Severity.ERROR, "Corrupt ZIP: ${e.message}")
            )
        }

        val rawManifest = manifestRaw
            ?: return Result(
                null,
                warnings + ImportIssue(ImportIssue.Severity.ERROR, "Missing $MANIFEST_FILE")
            )

        val manifest = try {
            ManifestCodec.decode(rawManifest)
        } catch (e: IllegalArgumentException) {
            return Result(
                null,
                warnings + ImportIssue(ImportIssue.Severity.ERROR, e.message ?: "Invalid $MANIFEST_FILE")
            )
        }

        if (files.none { it.path == manifest.entryPoint }) {
            warnings += ImportIssue(
                ImportIssue.Severity.ERROR,
                "Entry point file not found in ZIP: ${manifest.entryPoint}"
            )
        }

        files.forEach { file ->
            SecretScanner.scanFile(file.path, file.content).forEach { issue ->
                warnings += ImportIssue(
                    ImportIssue.Severity.WARNING,
                    issue.message,
                    file.path
                )
            }
        }

        if (warnings.any { it.isFatal }) {
            return Result(null, warnings)
        }

        val project = GeneratedProject(
            manifest = manifest,
            files = files,
            status = ProjectStatus.IMPORTED,
            history = listOf(
                EditRecord(
                    timestamp = Instant.now().toString(),
                    reason = "Imported from ZIP",
                    changedFiles = files.map { it.path }
                )
            )
        )
        return Result(project, warnings)
    }

    /** Returns null when [bytes] are not valid strict UTF-8. */
    private fun decodeStrictUtf8(bytes: ByteArray): String? {
        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            null
        }
    }

    private const val MANIFEST_FILE = "project.json"
    private const val MAX_ENTRIES = 10_000
    private const val MAX_ENTRY_BYTES = 50L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 200L * 1024 * 1024
}
