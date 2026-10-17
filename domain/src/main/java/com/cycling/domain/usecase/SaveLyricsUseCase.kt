package com.cycling.domain.usecase

import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.Song
import com.cycling.domain.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the lyrics the user typed for a song, replacing whatever it had.
 *
 * A blank [invoke] argument means "forget my edit", not "this song has no lyrics": an empty
 * override would suppress the online lookup for a track the user was only trying to reset, so the
 * row is dropped instead and the answer is looked up again from the automatic sources.
 */
@Singleton
class SaveLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(song: Song, rawContent: String): Lyrics {
        if (rawContent.isBlank()) {
            lyricsRepository.clearLyrics(song)
            return lyricsRepository.getLyrics(song)
        }
        return lyricsRepository.saveLyrics(song, rawContent)
    }
}
