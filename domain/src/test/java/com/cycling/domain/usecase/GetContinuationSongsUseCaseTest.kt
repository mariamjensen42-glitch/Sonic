package com.cycling.domain.usecase

import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetContinuationSongsUseCaseTest {

    private val repository: SongRepository = mockk()
    private val useCase = GetContinuationSongsUseCase(repository)

    @Test
    fun `same album and same artist outrank unrelated tracks`() = runTest {
        val seed = song(id = 1, albumId = 10, artistId = 100)
        val sameAlbum = song(id = 2, albumId = 10, artistId = 100)
        val sameArtist = song(id = 3, albumId = 20, artistId = 100)
        val unrelated = song(id = 4, albumId = 30, artistId = 300, favorite = true)
        every { repository.getAllSongs() } returns flowOf(listOf(seed, sameAlbum, sameArtist, unrelated))

        val result = useCase(seed, excludedSongIds = emptySet(), limit = 2)

        assertEquals(2, result.size)
        assertEquals(sameAlbum.id, result.first().id)
        assertTrue(result.any { it.id == sameArtist.id })
    }

    @Test
    fun `songs already in the queue are excluded`() = runTest {
        val seed = song(id = 1, albumId = 10, artistId = 100)
        val queued = song(id = 2, albumId = 10, artistId = 100)
        val fresh = song(id = 3, albumId = 10, artistId = 100)
        every { repository.getAllSongs() } returns flowOf(listOf(seed, queued, fresh))

        val result = useCase(seed, excludedSongIds = setOf(seed.id, queued.id), limit = 20)

        assertEquals(listOf(fresh.id), result.map { it.id })
    }

    @Test
    fun `the seed itself is never suggested`() = runTest {
        val seed = song(id = 1, albumId = 10, artistId = 100)
        every { repository.getAllSongs() } returns flowOf(listOf(seed))

        assertTrue(useCase(seed, emptySet()).isEmpty())
    }

    @Test
    fun `an empty library yields nothing`() = runTest {
        every { repository.getAllSongs() } returns flowOf(emptyList())

        assertFalse(useCase(song(id = 1), emptySet()).isNotEmpty())
    }

    private fun song(
        id: Long,
        albumId: Long = 1,
        artistId: Long = 1,
        favorite: Boolean = false
    ) = Song(
        id = id,
        title = "Song $id",
        artist = "Artist $artistId",
        album = "Album $albumId",
        duration = 180_000L,
        path = "/music/$id.mp3",
        albumId = albumId,
        artistId = artistId,
        dateAdded = 0L,
        dateModified = 0L,
        size = 1L,
        mimeType = "audio/mpeg",
        isFavorite = favorite
    )
}
