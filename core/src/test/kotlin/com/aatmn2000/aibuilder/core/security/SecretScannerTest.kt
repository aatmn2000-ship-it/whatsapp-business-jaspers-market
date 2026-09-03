package com.aatmn2000.aibuilder.core.security

import org.junit.Assert.assertTrue
import org.junit.Test

class SecretScannerTest {

    @Test
    fun `flags an AWS access key id`() {
        val issues = SecretScanner.scanFile("config.py", "key = 'AKIAIOSFODNN7EXAMPLE'")
        assertTrue(issues.isNotEmpty())
    }

    @Test
    fun `flags a private key block`() {
        val content = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"
        val issues = SecretScanner.scanFile("key.pem", content)
        assertTrue(issues.any { it.severity == SecurityIssue.Severity.WARNING })
    }

    @Test
    fun `flags a long hardcoded token assignment`() {
        val issues = SecretScanner.scanFile("app.py", "api_key = \"abcdef1234567890abcdef1234567890\"")
        assertTrue(issues.isNotEmpty())
    }

    @Test
    fun `clean generated code passes`() {
        val clean = """
            def main() -> int:
                print("hello")
                return 0
        """.trimIndent()
        assertTrue(SecretScanner.scanFile("app/main.py", clean).isEmpty())
    }
}
