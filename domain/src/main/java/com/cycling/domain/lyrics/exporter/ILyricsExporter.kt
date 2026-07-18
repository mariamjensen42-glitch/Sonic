package com.cycling.domain.lyrics.exporter

import com.cycling.domain.lyrics.model.SyncedLyrics

interface ILyricsExporter {
    fun export(lyrics: SyncedLyrics): String
}
