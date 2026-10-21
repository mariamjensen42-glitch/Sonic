package com.cycling.domain.repository

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.Song

/**
 * Cover art for a track: the user's own copy, not whatever MediaStore happens to publish.
 *
 * A chosen cover is copied into app-private storage and recorded on the song row, so it survives
 * rescans (MediaStore would reset its own art URI) and keeps working when the provider's URL
 * expires. MediaStore's artwork remains the fallback — clearing the custom one simply reveals it
 * again.
 */
interface CoverRepository {

    /**
     * Every candidate the enabled sources can offer, best first.
     *
     * A candidate carries the cover URL *and* whatever metadata the source could supply, because
     * both come from the same search response: the cover picker reads only the image URL, while the
     * "fill in from online" action reads the text fields. One search therefore feeds both.
     *
     * Costs at least one network round trip, so this is only for the pickers. Sources are asked in
     * order and the first that answers wins: four sources listing the same album would only turn
     * a five-item choice into twenty duplicates.
     */
    suspend fun findCandidates(song: Song): List<CoverCandidate>

    /**
     * Downloads [candidate] and makes it the song's cover.
     *
     * Returns the artwork URI now stored on the song, which is what the UI should render.
     */
    suspend fun applyCandidate(song: Song, candidate: CoverCandidate): Result<String>

    /**
     * Copies the image behind [imageUri] — a photo-picker / content-resolver URI — into the app's
     * own storage and makes it the song's cover.
     */
    suspend fun applyLocalImage(song: Song, imageUri: String): Result<String>

    /** Drops the user's cover, handing the song back to MediaStore's artwork. */
    suspend fun clearCustomCover(song: Song): Result<Unit>
}
