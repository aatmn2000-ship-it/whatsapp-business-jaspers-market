package com.aatmn2000.aibuilder.core.project

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Encodes and decodes `project.json` (schema v1). */
object ManifestCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(manifest: ProjectManifest): String =
        json.encodeToString(ProjectManifest.serializer(), manifest)

    /**
     * @throws IllegalArgumentException when the manifest is missing or invalid.
     */
    fun decode(raw: String): ProjectManifest {
        val manifest = try {
            json.decodeFromString(ProjectManifest.serializer(), raw)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Invalid project.json: ${e.message}", e)
        }
        if (manifest.id.isBlank()) {
            throw IllegalArgumentException("project.json is missing 'id'")
        }
        if (manifest.name.isBlank()) {
            throw IllegalArgumentException("project.json is missing 'name'")
        }
        if (manifest.entryPoint.isBlank()) {
            throw IllegalArgumentException("project.json is missing 'entryPoint'")
        }
        return manifest
    }
}
