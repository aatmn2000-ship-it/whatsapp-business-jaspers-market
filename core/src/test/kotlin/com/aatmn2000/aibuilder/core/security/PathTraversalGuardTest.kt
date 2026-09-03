package com.aatmn2000.aibuilder.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathTraversalGuardTest {

    @Test
    fun `accepts ordinary relative paths`() {
        assertTrue(PathTraversalGuard.isSafe("app/main.py"))
        assertTrue(PathTraversalGuard.isSafe("project.json"))
        assertTrue(PathTraversalGuard.isSafe("modules/a/b.py"))
    }

    @Test
    fun `rejects parent traversal`() {
        assertFalse(PathTraversalGuard.isSafe("../etc/passwd"))
        assertFalse(PathTraversalGuard.isSafe("a/../../b"))
        assertFalse(PathTraversalGuard.isSafe(".."))
    }

    @Test
    fun `rejects absolute paths and drive letters`() {
        assertFalse(PathTraversalGuard.isSafe("/etc/passwd"))
        assertFalse(PathTraversalGuard.isSafe("C:\\Windows\\system32"))
        assertFalse(PathTraversalGuard.isSafe("c:/temp/x"))
    }

    @Test
    fun `rejects empty names`() {
        assertFalse(PathTraversalGuard.isSafe(""))
    }

    @Test
    fun `normalize collapses redundant segments`() {
        assertEquals("a/b.py", PathTraversalGuard.normalize("./a/./b.py"))
        assertEquals("b.py", PathTraversalGuard.normalize("./b.py"))
    }

    @Test
    fun `normalize returns null for unsafe names`() {
        assertNull(PathTraversalGuard.normalize("../x"))
        assertNull(PathTraversalGuard.normalize("/x"))
    }
}
