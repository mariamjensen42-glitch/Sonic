package com.cycling.data.repository

import com.cycling.data.local.lyrics.EmbeddedLyricsHelper
import com.cycling.data.local.lyrics.LyricsFileHelper
import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.parser.AutoParser
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.repository.LyricsRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val embeddedLyricsHelper: EmbeddedLyricsHelper,
    private val lyricsFileHelper: LyricsFileHelper
) : LyricsRepository {

    private val parser = AutoParser(LyricsFormatGuesser())

    override suspend fun getLyrics(songPath: String): Lyrics {
        Timber.d("getLyrics: songPath=$songPath")
        val fileLyrics = lyricsFileHelper.getLyricsContent(songPath)
        if (!fileLyrics.isNullOrBlank()) {
            Timber.d("getLyrics: found file lyrics for songPath=$songPath")
            val syncedLyrics = parseLyrics(fileLyrics)
            return Lyrics(
                syncedLyrics = syncedLyrics,
                rawContent = fileLyrics,
                source = LyricsSource.FILE
            )
        }

        val embeddedLyrics = embeddedLyricsHelper.extractLyrics(songPath)
        if (!embeddedLyrics.isNullOrBlank()) {
            Timber.d("getLyrics: found embedded lyrics for songPath=$songPath")
            val syncedLyrics = parseLyrics(embeddedLyrics)
            return Lyrics(
                syncedLyrics = syncedLyrics,
                rawContent = embeddedLyrics,
                source = LyricsSource.EMBEDDED
            )
        }

        Timber.d("getLyrics: no lyrics found for songPath=$songPath")
        return Lyrics(
            syncedLyrics = null,
            rawContent = null,
            source = LyricsSource.NONE
        )
    }

    private fun parseLyrics(content: String): SyncedLyrics? {
        return try {
            parser.parse(content)
        } catch (e: Exception) {
            Timber.e(e, "parseLyrics: failed to parse lyrics")
            null
        }
    }
}
