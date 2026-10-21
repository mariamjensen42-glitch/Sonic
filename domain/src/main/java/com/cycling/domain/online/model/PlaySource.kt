package com.cycling.domain.online.model

/** 脚本返回的播放地址，可能带取链要求的请求头（Referer / User-Agent / Cookie）。 */
data class PlaySource(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)
