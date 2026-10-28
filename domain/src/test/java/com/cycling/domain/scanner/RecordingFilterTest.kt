package com.cycling.domain.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFilterTest {

    @Test
    fun `files under a recordings folder are recognized`() {
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/Recordings/memo.m4a"))
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/MIUI/sound_recorder/2026.m4a"))
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/Recorder/new.mp3"))
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/recordings/Call/1830.amr"))
    }

    @Test
    fun `chinese recording folders are recognized`() {
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/录音/通话录音_20261023.mp3"))
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/语音备忘/note.m4a"))
    }

    @Test
    fun `call recordings are recognized by file name`() {
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/Download/Call_20261023_1830.m4a"))
        assertTrue(RecordingFilter.isRecording("/storage/emulated/0/Download/通话录音20261023.amr"))
    }

    @Test
    fun `recording apps writing tags are recognized by metadata`() {
        assertTrue(
            RecordingFilter.isRecording(
                path = "/storage/emulated/0/Music/unknown.m4a",
                album = "通话录音"
            )
        )
        assertTrue(
            RecordingFilter.isRecording(
                path = "/storage/emulated/0/Music/unknown.m4a",
                title = "Voice Memo 12"
            )
        )
    }

    @Test
    fun `ordinary music is not touched`() {
        assertFalse(RecordingFilter.isRecording("/storage/emulated/0/Music/周杰伦/晴天.flac"))
        // 专辑名里带 Records 的不能误杀
        assertFalse(
            RecordingFilter.isRecording(
                path = "/storage/emulated/0/Music/Vicetone/Anywhere I Go.mp3",
                album = "SPINNIN' RECORDS",
                title = "Anywhere I Go"
            )
        )
        // 歌名里带 call 的也不能误杀
        assertFalse(
            RecordingFilter.isRecording(
                path = "/storage/emulated/0/Music/Call Me Maybe.mp3",
                title = "Call Me Maybe"
            )
        )
        assertFalse(RecordingFilter.isRecording(""))
    }
}
