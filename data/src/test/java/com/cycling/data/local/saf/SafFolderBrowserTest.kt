package com.cycling.data.local.saf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SAF 浏览里两处不能靠"跑一下看看"的逻辑：document id 的父子推导决定返回键行为，
 * 扩展名过滤决定列表里出现什么。这里把真实形态的 id 都钉住。
 */
class SafFolderBrowserTest {

    @Test
    fun `parent of a nested document id drops the last segment`() {
        assertEquals("primary:Music", parentDocumentId("primary:Music/Rock"))
        assertEquals("primary:Music/Rock", parentDocumentId("primary:Music/Rock/Bootlegs"))
    }

    @Test
    fun `document id without a slash is the tree root and has no parent`() {
        // 挂载卷（SD 卡）的树根形如 "1234-5678:Album"，同样没有父级
        assertNull(parentDocumentId("primary:Music"))
        assertNull(parentDocumentId("1234-5678:Album"))
    }

    @Test
    fun `audio extensions are matched case-insensitively`() {
        assertTrue(isAudioFileName("01 Track.FLAC"))
        assertTrue(isAudioFileName("song.mp3"))
        assertTrue(isAudioFileName("voice.m4a"))
    }

    @Test
    fun `non-audio files are filtered out`() {
        assertFalse(isAudioFileName("cover.jpg"))
        assertFalse(isAudioFileName("notes.txt"))
        assertFalse(isAudioFileName("README"))
        // 没有扩展名的目录项也不会被误判成歌曲
        assertFalse(isAudioFileName("AlbumArt"))
    }
}
