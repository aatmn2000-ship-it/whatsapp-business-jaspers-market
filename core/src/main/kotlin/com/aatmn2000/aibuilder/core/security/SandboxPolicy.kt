package com.aatmn2000.aibuilder.core.security

/**
 * Rules for executing generated code.
 *
 * In V1 nothing generated is ever executed automatically — this policy
 * documents the constraints any future executor (V2 sandbox) must honor,
 * and is already enforced against build commands during the verify stage.
 */
object SandboxPolicy {

    data class Evaluation(
        val allowed: Boolean,
        val violations: List<String>
    )

    private val networkCommandRegex =
        Regex("(?i)(^|\\s)(curl|wget|nc|ncat|ssh|scp|telnet)(\\s|$)")

    private val destructiveCommandRegex =
        Regex("(?i)(rm\\s+-rf\\s+/|mkfs|dd\\s+if=|:\\(\\)\\{|\\bshutdown\\b)")

    fun evaluateCommand(command: String): Evaluation {
        val trimmed = command.trim()
        val violations = mutableListOf<String>()
        networkCommandRegex.findAll(trimmed).forEach { match ->
            violations += "network access ('${match.groupValues[2]}') is not allowed in the sandbox"
        }
        destructiveCommandRegex.findAll(trimmed).forEach { match ->
            violations += "destructive command ('${match.value.trim()}') is not allowed in the sandbox"
        }
        return Evaluation(allowed = violations.isEmpty(), violations = violations)
    }
}
