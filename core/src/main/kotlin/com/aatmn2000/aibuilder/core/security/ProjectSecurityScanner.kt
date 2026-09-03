package com.aatmn2000.aibuilder.core.security

import com.aatmn2000.aibuilder.core.project.GeneratedProject

/**
 * The "verify" stage of the build pipeline:
 * secrets in code + suspicious dependencies + sandbox policy for build
 * commands. Runs before every package.
 */
object ProjectSecurityScanner {

    fun scan(project: GeneratedProject): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()

        issues += project.files.flatMap { file ->
            SecretScanner.scanFile(file.path, file.content)
        }

        issues += DependencyScanner.scan(project.manifest)

        val commands = listOf(
            project.manifest.build.commands.run,
            project.manifest.build.commands.test
        )
        commands.forEach { command ->
            if (command.isBlank()) return@forEach
            SandboxPolicy.evaluateCommand(command).violations.forEach { violation ->
                issues += SecurityIssue(
                    severity = SecurityIssue.Severity.BLOCKING,
                    message = "Build command rejected: $violation"
                )
            }
        }

        return issues
    }
}
