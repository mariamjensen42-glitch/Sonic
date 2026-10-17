package com.cycling.domain.model

/**
 * One hit from a lyrics search.
 *
 * [snippet] is the few characters around the match, not the whole document: the search row needs
 * to show *why* this song matched, and a full lyrics body would be far too long for a list row.
 */
data class LyricsMatch(
    val song: Song,
    val snippet: String
)
