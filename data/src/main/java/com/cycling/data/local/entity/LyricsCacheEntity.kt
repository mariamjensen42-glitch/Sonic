package com.cycling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Downloaded lyrics, kept raw so they can be re-parsed on load.
 *
 * Keyed by a normalised `title|artist|duration` fingerprint rather than a song id, so a cache
 * entry stays valid when the same track is re-imported, moved, or matched on another device.
 */
@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    /** Which provider produced this, e.g. `netease` / `lrclib`. */
    val source: String,
    /** Format hint recorded at download time, e.g. `LRC` / `YRC`. Null means "guess on load". */
    val format: String? = null,
    val rawContent: String,
    val createdAt: Long
)
