package com.cycling.domain.repository

import com.cycling.domain.model.Lyrics

interface LyricsRepository {
    suspend fun getLyrics(songPath: String): Lyrics
}
