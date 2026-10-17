package com.cycling.sonic.log

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import timber.log.Timber

class FileLoggingTreeTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private fun plant(tree: Timber.Tree) {
        Timber.plant(tree)
    }

    private fun uproot(tree: Timber.Tree) {
        Timber.uproot(tree)
    }

    @Test
    fun `writes formatted line with timestamp level tag and message`() {
        val tree = FileLoggingTree(tempDir.root)
        plant(tree)
        try {
            Timber.tag("TEST").i("hello world")
        } finally {
            uproot(tree)
        }

        val files = tempDir.root.listFiles()
        assertNotNull(files)
        assertEquals(1, files!!.size)
        assertTrue(files.single().name.startsWith("sonic-log-"))

        val content = files.single().readText()
        assertTrue(content.startsWith("["))
        assertTrue(content.contains("] I/TEST: hello world\n"))
    }

    @Test
    fun `filters levels not enabled for file output`() {
        val tree = FileLoggingTree(tempDir.root, setOf(Log.INFO, Log.WARN, Log.ERROR))
        plant(tree)
        try {
            Timber.tag("TEST").d("debug only")
            Timber.tag("TEST").v("verbose only")
        } finally {
            uproot(tree)
        }

        val files = tempDir.root.listFiles()
        assertNotNull(files)
        assertTrue(files!!.isEmpty())
    }

    @Test
    fun `appends throwable stack trace below message`() {
        val tree = FileLoggingTree(tempDir.root)
        plant(tree)
        try {
            Timber.tag("TEST").e(RuntimeException("boom"), "operation failed")
        } finally {
            uproot(tree)
        }

        val content = tempDir.root.listFiles()!!.single().readText()
        assertTrue(content.contains("] E/TEST: operation failed\n"))
        assertTrue(content.contains("RuntimeException"))
        assertTrue(content.contains("boom"))
    }
}
