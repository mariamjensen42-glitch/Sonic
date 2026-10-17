package com.cycling.domain.usecase

import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Picks what should play after the current queue runs out.
 *
 * Local-first and deliberately simple: same album beats same artist beats everything else, and a
 * random tie-break keeps two consecutive runs from producing the identical 20 songs. There is no
 * genre or BPM data in the library to rank with, and asking a provider for "similar tracks" would
 * make offline listening depend on the network — which is the one thing a local player must not do.
 *
 * Songs already in the queue are excluded, so repeated calls keep extending the session instead of
 * replaying what the user just heard. An exhausted library returns an empty list rather than
 * looping.
 */
@Singleton
class GetContinuationSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {

    suspend operator fun invoke(
        seed: Song,
        excludedSongIds: Set<Long>,
        limit: Int = DEFAULT_LIMIT
    ): List<Song> {
        if (limit <= 0) return emptyList()

        val library = withContext(Dispatchers.Default) {
            songRepository.getAllSongs().first()
        }
        if (library.isEmpty()) return emptyList()

        return library.asSequence()
            .filter { it.id != seed.id && it.id !in excludedSongIds }
            .map { candidate -> candidate to (score(seed, candidate) + Random.nextFloat()) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(seed: Song, candidate: Song): Int {
        var score = when {
            candidate.albumId == seed.albumId && candidate.albumId > 0 -> 10
            candidate.album.equals(seed.album, ignoreCase = true) && seed.album.isNotBlank() -> 9
            candidate.artistId == seed.artistId && candidate.artistId > 0 -> 6
            candidate.artist.equals(seed.artist, ignoreCase = true) && seed.artist.isNotBlank() -> 5
            else -> 0
        }
        // A favourite is a weak signal, not a strong one: it breaks ties between two equally
        // unrelated tracks but never beats a same-album pick.
        if (candidate.isFavorite) score += 1
        return score
    }

    companion object {
        const val DEFAULT_LIMIT = 20
    }
}
