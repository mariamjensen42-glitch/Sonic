package com.cycling.domain.usecase

import com.cycling.domain.model.Lyrics
import com.cycling.domain.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(songPath: String): Lyrics {
        return lyricsRepository.getLyrics(songPath)
    }
}
