package com.cycling.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 缓存面板报的数字就是这里算出来的。算错只会表现为"清完了还剩几百 MB"，
 * 所以直接把递归求和钉住：多层嵌套都算、目录本身不计、没有目录算 0。
 */
class CacheSizeTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `nested files are summed and directories add nothing`() {
        val root = temp.newFolder("image_cache")
        File(root, "a.bin").writeBytes(ByteArray(100))
        val nested = File(root, "journal").apply { mkdirs() }
        File(nested, "b.bin").writeBytes(ByteArray(24))

        assertEquals(124L, root.sizeOnDisk())
    }

    @Test
    fun `empty and missing directories are zero`() {
        val empty = temp.newFolder("http_cache")

        assertEquals(0L, empty.sizeOnDisk())
        assertEquals(0L, File(temp.root, "absent").sizeOnDisk())
    }
}
