package com.cycling.domain.model

/**
 * What a music player can read from — and, for the containers Sonic can edit, write back to — a
 * track's file tags.
 *
 * Read from ID3v2 (MP3), VorbisComment (FLAC / OGG), or MP4 `ilst` atoms; the same fields can be
 * written back for MP3 / FLAC / OGG / M4A / MP4 / WAV / AIFF / WMA / DSF. The extra fields beyond
 * the descriptive ones are deliberately absent from [Song]: the `songs` table mirrors what
 * MediaStore publishes, while these come from the file's own tags and are read on demand for the
 * detail page.
 */
data class AudioMetadata(
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val encodingType: String = "",
    val year: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    /** `[TALB]`-style "album artist" (`TPE2`) — the artist the album is filed under. */
    val albumArtist: String? = null,
    /** `TRCK`, kept verbatim so `3/12` survives a round trip. */
    val trackNumber: String? = null,
    /** `TPOS`, likewise kept verbatim so `1/2` survives. */
    val discNumber: String? = null,
    val publisher: String? = null,
    val comment: String? = null,
    val copyright: String? = null
)
