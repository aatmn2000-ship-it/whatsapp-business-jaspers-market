package com.aatmn2000.aibuilder.core.project

import com.aatmn2000.aibuilder.core.security.PathTraversalGuard
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages a project as the portable ZIP format:
 *
 * ```
 * MyClinicApp.zip
 * ├── app/ modules/ database/ assets/ tests/ docs/ config/
 * ├── project.json
 * └── README.md
 * ```
 *
 * The ZIP is the portable representation of a project: another user can
 * import it and the AI continues development on the same code base.
 */
object ZipPackager {

    fun packageProject(project: GeneratedProject): ByteArray {
        require(project.manifest.entryPoint.isNotBlank()) { "Project has no entry point" }
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer, StandardCharsets.UTF_8).use { zip ->
            project.files.forEach { file ->
                val entryName = PathTraversalGuard.normalize(file.path)
                    ?: throw IllegalArgumentException("Unsafe file path: ${file.path}")
                putEntry(zip, entryName, file.content)
            }
            putEntry(zip, "project.json", ManifestCodec.encode(project.manifest))
        }
        return buffer.toByteArray()
    }

    private fun putEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
