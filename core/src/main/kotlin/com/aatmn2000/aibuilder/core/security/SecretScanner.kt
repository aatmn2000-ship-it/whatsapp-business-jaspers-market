package com.aatmn2000.aibuilder.core.security

/**
 * Scans project files for accidentally embedded credentials.
 *
 * Generated projects must never contain API keys — this scanner runs before
 * every package and on every import.
 */
object SecretScanner {

    private val patterns: List<Pattern> = listOf(
        Pattern("AWS access key id", Regex("AKIA[0-9A-Z]{16}")),
        Pattern("OpenAI-style API key", Regex("(?i)sk-[A-Za-z0-9]{20,}")),
        Pattern("GitHub token", Regex("ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{22,}")),
        Pattern(
            "hard-coded credential",
            Regex("""(?i)\b(api[_-]?key|secret|password|token)\b\s*[=:]\s*['"]?[A-Za-z0-9/+_-]{16,}""")
        ),
        Pattern("private key material", Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----"))
    )

    fun scanFile(path: String, content: String): List<SecurityIssue> {
        return patterns.flatMap { pattern ->
            pattern.regex.findAll(content).map {
                SecurityIssue(
                    severity = SecurityIssue.Severity.WARNING,
                    message = "Possible ${pattern.label} embedded in code",
                    file = path
                )
            }
        }
    }

    private data class Pattern(val label: String, val regex: Regex)
}
