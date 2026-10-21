package com.cycling.domain.repository

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song

/**
 * Searches one online catalogue for cover art.
 *
 * Providers are best-effort like the lyrics ones: these are unofficial endpoints that can answer
 * with an error body under HTTP 200, rate-limit, or disappear entirely, so anything unexpected is
 * reported as "nothing found" and the repository moves on to the next source.
 */
interface CoverArtProvider {

    val source: CoverSource

    /**
     * Cover art candidates for [song], best guess first.
     *
     * The caller passes the whole song rather than a title/artist pair because every source ranks
     * by duration as well, and because a user-corrected title must be what gets searched.
     */
    suspend fun search(song: Song): List<CoverCandidate>
}
