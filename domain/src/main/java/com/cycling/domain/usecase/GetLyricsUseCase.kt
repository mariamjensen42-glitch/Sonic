package com.cycling.domain.usecase

import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.Song
import com.cycling.domain.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(song: Song): Lyrics {
        return lyricsRepository.getLyrics(song)
    }

    /**
     * Looks [song] up again, discarding anything downloaded for it before — the manual fetch behind
     * the lyrics screen's button, for when the copy on screen is missing or the wrong one.
     */
    suspend fun refresh(song: Song): Lyrics {
        return lyricsRepository.refreshLyrics(song)
    }

    /** Every version the online sources can offer for [song], for the lyrics screen's picker. */
    suspend fun candidates(song: Song): List<LyricsCandidate> {
        return lyricsRepository.findLyricsCandidates(song)
    }

    /** Uses [candidate] for [song] from now on. */
    suspend fun selectCandidate(song: Song, candidate: LyricsCandidate): Lyrics {
        return lyricsRepository.selectLyricsCandidate(song, candidate)
    }
}
