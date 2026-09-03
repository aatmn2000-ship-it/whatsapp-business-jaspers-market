package com.aatmn2000.aibuilder.core.pipeline

import com.aatmn2000.aibuilder.core.project.GeneratedProject
import com.aatmn2000.aibuilder.core.project.ProjectFile

/**
 * V1 "compiler + test runner" for Python projects: a deterministic static
 * analysis that catches the errors AI code generation most often produces,
 * without ever executing the code:
 *
 *  - empty files and leftover placeholder tokens (TODO(, FIXME, ???)
 *  - unbalanced double quotes
 *  - tab indentation and indents that are not multiples of 4 spaces
 *  - malformed JSON files
 *  - unresolved imports: `from modules.x import y` must resolve to a real
 *    file modules/x.py and a real `def y` / `class y`
 *  - the declared entry point must exist
 *  - test files must exist and contain `test_` functions
 */
object PythonStaticValidator : CodeValidator {

    override val language: String = "python"

    private val placeholderTokens = listOf(
        "TODO(", "TODO:", "FIXME", "???", "<placeholder>", "<YourCodeHere>"
    )

    /** Project-internal packages whose imports must resolve to real files. */
    private val localPackages = setOf("modules", "database", "app", "assets", "tests")

    private val fromImportRegex = Regex("^from\\s+([a-zA-Z_][\\w.]*)\\s+import\\s+(.+)$")

    override fun compile(project: GeneratedProject): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        project.files
            .filter { it.path.endsWith(".py") }
            .forEach { file -> issues += checkPythonFile(file) }
        project.files
            .filter { it.path.endsWith(".json") }
            .forEach { file -> issues += checkJsonFile(file) }
        issues += checkCrossFileImports(project, CodeIssue.Kind.COMPILE)
        if (project.fileAt(project.manifest.entryPoint) == null) {
            issues += CodeIssue(
                project.manifest.entryPoint,
                0,
                "Entry point file is missing"
            )
        }
        return issues
    }

    override fun runTests(project: GeneratedProject): List<CodeIssue> {
        val testFiles = project.files.filter {
            it.path.startsWith("tests/") && it.path.endsWith(".py")
        }
        if (testFiles.isEmpty()) {
            val domain = project.manifest.domain.ifBlank { "simple_tool" }
            return listOf(
                CodeIssue(
                    "tests/test_$domain.py",
                    0,
                    "No test files found",
                    CodeIssue.Kind.TEST
                )
            )
        }
        val issues = mutableListOf<CodeIssue>()
        testFiles.forEach { file ->
            val hasTestFunction = file.content.lineSequence().any {
                it.trimStart().startsWith("def test_")
            }
            if (!hasTestFunction) {
                issues += CodeIssue(
                    file.path,
                    0,
                    "Test file contains no test functions",
                    CodeIssue.Kind.TEST
                )
            }
        }
        issues += checkCrossFileImports(project, CodeIssue.Kind.TEST)
        return issues
    }

    private fun checkPythonFile(file: ProjectFile): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val lines = file.content.split("\n")
        if (lines.joinToString("\n").isBlank()) {
            issues += CodeIssue(file.path, 0, "File is empty")
            return issues
        }
        lines.forEachIndexed { index, line ->
            placeholderTokens.firstOrNull { line.contains(it) }?.let { token ->
                issues += CodeIssue(file.path, index + 1, "Leftover placeholder token '$token'")
            }
        }
        if (file.content.count { it == '"' } % 2 != 0) {
            issues += CodeIssue(file.path, 0, "Unbalanced quotes in generated code")
        }
        lines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            if (line.startsWith("\t")) {
                issues += CodeIssue(file.path, index + 1, "Tab indentation is not allowed")
            } else {
                val indent = line.length - line.trimStart().length
                if (indent % 4 != 0) {
                    issues += CodeIssue(
                        file.path,
                        index + 1,
                        "Indentation must be a multiple of 4 spaces"
                    )
                }
            }
        }
        return issues
    }

    /**
     * Structural JSON check (no external parser): the file must contain a
     * single balanced object with balanced inner arrays and strings.
     */
    private fun checkJsonFile(file: ProjectFile): List<CodeIssue> {
        val trimmed = file.content.trim()
        if (trimmed.isEmpty()) {
            return listOf(CodeIssue(file.path, 0, "JSON file is empty"))
        }
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return listOf(CodeIssue(file.path, 0, "JSON file must contain an object"))
        }
        var braceDepth = 0
        var bracketDepth = 0
        var inString = false
        var escaped = false
        for (ch in trimmed) {
            when {
                escaped -> escaped = false
                ch == '\\' -> escaped = true
                ch == '"' -> inString = !inString
                !inString && ch == '{' -> braceDepth++
                !inString && ch == '}' -> braceDepth--
                !inString && ch == '[' -> bracketDepth++
                !inString && ch == ']' -> bracketDepth--
            }
        }
        if (braceDepth != 0 || bracketDepth != 0 || inString) {
            return listOf(CodeIssue(file.path, 0, "JSON file has unbalanced braces or strings"))
        }
        return emptyList()
    }

    private fun checkCrossFileImports(project: GeneratedProject, kind: CodeIssue.Kind): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        val filesByPath = project.files.associateBy { it.path }
        project.files
            .filter { it.path.endsWith(".py") }
            .forEach { file ->
                file.content.split("\n").forEachIndexed { index, line ->
                    val match = fromImportRegex.find(line.trim()) ?: return@forEachIndexed
                    val moduleName = match.groupValues[1]
                    if (moduleName.split('.').first() !in localPackages) {
                        return@forEachIndexed // stdlib / third-party
                    }
                    val names = match.groupValues[2]
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val targetPath = moduleName.replace('.', '/') + ".py"
                    val target = filesByPath[targetPath]
                    if (target == null) {
                        issues += CodeIssue(
                            file.path,
                            index + 1,
                            "Imported module is missing: $targetPath",
                            kind
                        )
                    } else {
                        names.forEach { name ->
                            if (!hasDefinition(target.content, name)) {
                                issues += CodeIssue(
                                    file.path,
                                    index + 1,
                                    "Unresolved name '$name' in $targetPath",
                                    kind
                                )
                            }
                        }
                    }
                }
            }
        return issues
    }

    private fun hasDefinition(content: String, name: String): Boolean {
        val defRegex = Regex("(?m)^\\s*(def|class)\\s+$name\\s*[(:]")
        return defRegex.containsMatchIn(content)
    }
}
