package com.cycling.data.api.search.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 网易云 `api/cloudsearch/pc` 的搜索响应。
 *
 * 用的是 cloudsearch 而不是 `api/search/get/web`：后者只给 `album.picId`，
 * 拼封面还要过一层加密；cloudsearch 直接给 `al.picUrl`。
 * 字段名是缩写（`ar` 歌手 / `al` 专辑 / `dt` 时长毫秒）。
 */
@Serializable
internal data class NetEaseCloudSearchResponse(
    val code: Int = 0,
    val result: NetEaseCloudSearchResult? = null
)

@Serializable
internal data class NetEaseCloudSearchResult(
    val songs: List<NetEaseCloudSongDto> = emptyList()
)

@Serializable
internal data class NetEaseCloudSongDto(
    val id: Long = 0,
    val name: String = "",
    @SerialName("ar") val artists: List<NetEaseCloudArtistDto> = emptyList(),
    @SerialName("al") val album: NetEaseCloudAlbumDto? = null,
    /** 时长（毫秒）。 */
    @SerialName("dt") val durationMs: Long = 0
)

@Serializable
internal data class NetEaseCloudArtistDto(val name: String = "")

@Serializable
internal data class NetEaseCloudAlbumDto(
    val name: String = "",
    /** 封面地址，形如 `http://p1.music.126.net/…jpg`（调用方会升到 https）。 */
    val picUrl: String = ""
)

/**
 * 酷我（`search.kuwo.cn/r.s`）的搜索响应。
 *
 * 这个接口标着 `rformat=json`，回的却是**单引号**的伪 JSON（`{'abslist':[…]}`），
 * 调用方要先把引号归一化再解码，见 `OnlineCatalogSearchService.searchKuwo`。
 * 字段名全大写，与其它目录接口都不同。
 */
@Serializable
internal data class KuwoSearchResponse(
    @SerialName("abslist") val songs: List<KuwoSongDto> = emptyList()
)

@Serializable
internal data class KuwoSongDto(
    @SerialName("NAME") val name: String = "",
    @SerialName("ARTIST") val artist: String = "",
    @SerialName("ALBUM") val album: String = "",
    /** 时长（秒），以字符串下发。 */
    @SerialName("DURATION") val duration: String = "",
    /** 酷我取链要的 rid（`MUSIC_` 后面的数字）。 */
    @SerialName("DC_TARGETID") val rid: String = "",
    /** 封面路径（`120/54/93/…jpg`），拼到 `img1.kuwo.cn/star/albumcover/` 后面；部分条目为空。 */
    @SerialName("web_albumpic_short") val albumPicShort: String = ""
)

/**
 * 咪咕（`app.c.nf.migu.cn/MIGUM2.0/v1.0/content/search_all.do`）的搜索响应。
 *
 * 结果集是 `songResultData.result`，不带时长；封面在 `imgItems`（webp）。
 */
@Serializable
internal data class MiguSearchResponse(
    val code: String = "",
    @SerialName("songResultData") val songResult: MiguSongResult? = null
)

@Serializable
internal data class MiguSongResult(
    val result: List<MiguSongDto> = emptyList()
)

@Serializable
internal data class MiguSongDto(
    val id: String = "",
    /** 咪咕的内容 id，脚本拼取链请求时用它当 `songmid`。 */
    val contentId: String = "",
    /** 版权 id，脚本用它当 `hash`。 */
    val copyrightId: String = "",
    val name: String = "",
    val singers: List<MiguSingerDto> = emptyList(),
    val albums: List<MiguAlbumDto> = emptyList(),
    val imgItems: List<MiguImageDto> = emptyList()
)

@Serializable
internal data class MiguSingerDto(val name: String = "")

@Serializable
internal data class MiguAlbumDto(val name: String = "")

@Serializable
internal data class MiguImageDto(val img: String = "")
