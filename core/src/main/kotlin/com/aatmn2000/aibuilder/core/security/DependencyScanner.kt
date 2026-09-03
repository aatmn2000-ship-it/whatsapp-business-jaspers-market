package com.aatmn2000.aibuilder.core.security

import com.aatmn2000.aibuilder.core.project.ProjectManifest

/**
 * Flags dependencies that are suspicious for a local-first project.
 *
 * V1 uses a conservative blocklist and network-capability hints; real
 * vulnerability data plugs in here in V2.
 */
object DependencyScanner {

    private val blockedNames = setOf(
        "evalkit", "shellinject", "untrusted-eval", "fake-pip"
    )

    private val networkHints = setOf(
        "requests", "httpx", "aiohttp", "urllib3", "socket", "paramiko", "boto3"
    )

    fun scan(manifest: ProjectManifest): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        manifest.dependencies.forEach { dep ->
            val name = dep.name.lowercase()
            if (name in blockedNames) {
                issues += SecurityIssue(
                    severity = SecurityIssue.Severity.BLOCKING,
                    message = "Blocked dependency: ${dep.name}"
                )
            } else if (dep.scope == "runtime" && name in networkHints) {
                issues += SecurityIssue(
                    severity = SecurityIssue.Severity.WARNING,
                    message = "Network-capable dependency '${dep.name}' in a local-first project"
                )
            }
        }
        return issues
    }
}
