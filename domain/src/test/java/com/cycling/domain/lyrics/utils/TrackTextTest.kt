package com.cycling.domain.lyrics.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 曲名 / 歌手标签的归一化。
 *
 * 这些规则决定「搜得到」与「认得出」两件事：去噪后的曲名用于检索，原始曲名用于判断是不是
 * 同一首。规则改错的后果是双向的 —— 搜不到，或者把别人的歌词认成自己的。
 */
class TrackTextTest {

    @Test
    fun `brackets and release notes are dropped from the base title`() {
        assertEquals("Song", TrackText.baseTitle("Song (Live)"))
        assertEquals("Song", TrackText.baseTitle("Song（现场版）"))
        assertEquals("Song", TrackText.baseTitle("Song [Remastered 2011]"))
        assertEquals("晴天", TrackText.baseTitle("晴天 (Live)"))
    }

    @Test
    fun `the dash suffix is dropped but a hyphen inside a name is kept`() {
        assertEquals("Song", TrackText.baseTitle("Song - Live"))
        assertEquals("Song", TrackText.baseTitle("Song – Remastered 2011"))
        // 名字里的连字符不是版本说明。
        assertEquals("Jay-Z", TrackText.baseTitle("Jay-Z"))
    }

    @Test
    fun `the track number prefix is dropped`() {
        assertEquals("Song", TrackText.baseTitle("01 - Song"))
        assertEquals("Song", TrackText.baseTitle("03. Song"))
        // 没有分隔符的数字不是序号，别把曲名吃掉。
        assertEquals("1989", TrackText.baseTitle("1989"))
    }

    @Test
    fun `the collaboration suffix is dropped`() {
        assertEquals("Song", TrackText.baseTitle("Song feat. Someone"))
        assertEquals("Song", TrackText.baseTitle("Song ft. Someone"))
    }

    @Test
    fun `a title that is nothing but noise still yields something`() {
        // 全去掉就空了：空串会让下游把每一条候选都判成「不是同一首」。
        assertEquals("(Intro)", TrackText.baseTitle("(Intro)"))
    }

    @Test
    fun `comparable drops punctuation, spacing and case`() {
        assertEquals("song", TrackText.comparable("Song"))
        assertEquals("hellomoonlight", TrackText.comparable("Hello Moonlight!"))
        assertEquals("晴天", TrackText.comparable("晴 天"))
    }

    @Test
    fun `version tags tell a live take from the studio one`() {
        assertTrue(TrackText.versionTags("Song (Live)").contains("live"))
        assertTrue(TrackText.versionTags("Song (Live)").contains("live"))
        assertTrue(TrackText.versionTags("歌曲（伴奏）").contains("instrumental"))
        assertTrue(TrackText.versionTags("Song (Acoustic)").contains("acoustic"))
        assertTrue(TrackText.versionTags("Song (DJ版)").contains("remix"))
    }

    @Test
    fun `remastering is not a version tag`() {
        // 重制只动音质，不动时长与演奏；当成另一个版本会让正常的重制曲目匹配失败。
        assertTrue(TrackText.versionTags("Song (Remastered 2011)").isEmpty())
        assertTrue(TrackText.versionTags("Song (Deluxe Edition)").isEmpty())
    }

    @Test
    fun `a song that merely mentions live is not a live take`() {
        assertTrue(TrackText.versionTags("Deliver").isEmpty())
    }

    @Test
    fun `artist names are split on the usual separators`() {
        assertEquals(
            listOf("A", "B"),
            TrackText.artistNames("A feat. B")
        )
        assertEquals(
            listOf("A", "B"),
            TrackText.artistNames("A & B")
        )
        assertEquals(
            listOf("A", "B"),
            TrackText.artistNames("A、B")
        )
        assertEquals(
            listOf("A"),
            TrackText.artistNames("A")
        )
    }
}
