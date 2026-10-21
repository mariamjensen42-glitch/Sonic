package com.cycling.data.api.lyrics.dto

import kotlinx.serialization.Serializable

/**
 * QQ Music's lyric endpoint (`fcg_query_lyric_new.fcg`).
 *
 * `retcode` / `code` are checked instead of the HTTP status: the endpoint reports "no lyric" and
 * some failures as HTTP 200 with a non-zero code. Both text fields are base64 unless the request
 * asked for `nobase64=1`, and some edges ignore that flag, so the provider sniffs the payload.
 */
@Serializable
internal data class QqLyricResponse(
    val retcode: Int = 0,
    val code: Int = 0,
    /** Line-level LRC. */
    val lyric: String? = null,
    /** Translation LRC with the same timestamps as [lyric]; often absent. */
    val trans: String? = null
)
