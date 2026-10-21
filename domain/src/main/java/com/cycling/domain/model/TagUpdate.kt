package com.cycling.domain.model

/**
 * A set of tag changes to write into an audio file.
 *
 * `null` means "leave this tag alone" — that is what lets the detail page send only the fields
 * the user actually touched, instead of rewriting every tag (and every id3 frame) on each save.
 * An empty string is still a value: for the text fields that clears the tag.
 */
data class TagUpdate(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: String? = null,
    val genre: String? = null,
    /** Written to `TRCK`; a leading number plus an optional `/total`, e.g. `3/12`. */
    val trackNumber: String? = null,
    /** Written to `TPOS`, likewise `1/2`. */
    val discNumber: String? = null,
    val composer: String? = null,
    val publisher: String? = null,
    val copyright: String? = null,
    val comment: String? = null
)
