package com.cycling.data.api.search

import com.cycling.data.api.cover.dto.KugouSearchResponse
import com.cycling.data.api.cover.dto.QqSearchResponse
import com.cycling.data.api.cover.jsonObjectOrNull
import com.cycling.data.api.cover.textOf
import com.cycling.data.api.search.dto.KuwoSearchResponse
import com.cycling.data.api.search.dto.MiguSearchResponse
import com.cycling.data.api.search.dto.NetEaseCloudSearchResponse
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * 各平台公开搜索接口的薄封装，产出可以直接交给音源脚本取链的 [MusicInfo]。
 *
 * 只做「关键词 → 平台歌曲 id」这一件事，取链仍然是 `LxPluginManager` 的职责。
 * 之所以要自己搜：LX 音源脚本协议里只有 `musicUrl`，没有 `search`，
 * 宿主不搜就没有 `songmid` / `hash` 可交给脚本。
 *
 * 三个接口的 DTO 与既有歌词 / 封面 provider 共用（同一份响应，不重复定义），
 * 只有酷狗缺 `FileHash` 一个字段，已在 [com.cycling.data.api.cover.dto.KugouSongDto] 里补上。
 */
@Singleton
class OnlineCatalogSearchService @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 搜一个平台；暂不支持搜索的平台（汽水）返回空列表。
     *
     * 网络异常直接抛给调用方，由仓库层决定是「丢这一个平台」还是整体失败。
     */
    suspend fun search(
        source: OnlineSource,
        keyword: String,
        page: Int,
        limit: Int
    ): List<MusicInfo> =
        withContext(Dispatchers.IO) {
            if (keyword.isBlank() || limit <= 0) return@withContext emptyList()
            val safePage = page.coerceAtLeast(1)
            when (source) {
                OnlineSource.NETEASE -> searchNetEase(keyword, safePage, limit)
                OnlineSource.QQ -> searchQq(keyword, safePage, limit)
                OnlineSource.KUGOU -> searchKugou(keyword, safePage, limit)
                OnlineSource.KUWO -> searchKuwo(keyword, safePage, limit)
                OnlineSource.MIGU -> searchMigu(keyword, safePage, limit)
                OnlineSource.SODA -> emptyList()
            }
        }

    private fun searchNetEase(keyword: String, page: Int, limit: Int): List<MusicInfo> {
        val form = FormBody.Builder()
            .add("s", keyword)
            .add("type", "1")
            .add("offset", ((page - 1) * limit).toString())
            .add("limit", limit.toString())
            .add("total", "true")
            .build()
        // cloudsearch 而不是 search/get/web：后者只给 picId，拼封面还要过一层加密。
        val request = Request.Builder()
            .url("$NETEASE_BASE/api/cloudsearch/pc")
            .header("Referer", "$NETEASE_BASE/")
            // 不带 web 播放器的 Cookie，接口会回一段错误体而不是结果。
            .header("Cookie", "appver=2.0.2")
            .post(form)
            .build()

        val payload = client.textOf(request)?.jsonObjectOrNull() ?: return emptyList()
        val response = runCatching { json.decodeFromString<NetEaseCloudSearchResponse>(payload) }
            .onFailure { Timber.w(it, "OnlineCatalogSearch: unreadable NetEase search") }
            .getOrNull() ?: return emptyList()

        return response.result?.songs.orEmpty().mapNotNull { dto ->
            val id = dto.id.takeIf { it > 0L }?.toString() ?: return@mapNotNull null
            MusicInfo(
                source = OnlineSource.NETEASE,
                id = id,
                title = dto.name,
                artist = dto.artists.joinToString(", ") { it.name },
                album = dto.album?.name.orEmpty(),
                durationSeconds = dto.durationMs / 1000L,
                coverUrl = dto.album?.picUrl?.takeIf { it.isNotBlank() }?.toHttps(),
                extras = mapOf("song_id" to id)
            )
        }
    }

    private fun searchQq(keyword: String, page: Int, limit: Int): List<MusicInfo> {
        val url = "$QQ_BASE/soso/fcgi-bin/client_search_cp".toHttpUrl().newBuilder()
            .addQueryParameter("w", keyword)
            .addQueryParameter("format", "json")
            .addQueryParameter("n", limit.toString())
            .addQueryParameter("p", page.toString())
            .addQueryParameter("cr", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Referer", "$QQ_BASE/")
            .build()

        val payload = client.textOf(request)?.jsonObjectOrNull() ?: return emptyList()
        val response = runCatching { json.decodeFromString<QqSearchResponse>(payload) }
            .onFailure { Timber.w(it, "OnlineCatalogSearch: unreadable QQ search") }
            .getOrNull() ?: return emptyList()

        return response.data?.song?.list.orEmpty().mapNotNull { dto ->
            val songmid = dto.songmid.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MusicInfo(
                source = OnlineSource.QQ,
                id = songmid,
                title = dto.songname,
                artist = dto.singer.joinToString(", ") { it.name },
                album = dto.albumname,
                durationSeconds = dto.interval.toLong(),
                coverUrl = dto.albummid.takeIf { it.isNotBlank() }?.let { "$QQ_COVER$it.jpg" },
                extras = mapOf("mid" to songmid)
            )
        }
    }

    private fun searchKugou(keyword: String, page: Int, limit: Int): List<MusicInfo> {
        val url = "$KUGOU_BASE/song_search_v2".toHttpUrl().newBuilder()
            .addQueryParameter("keyword", keyword)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("pagesize", limit.toString())
            .addQueryParameter("platform", "WebFilter")
            .addQueryParameter("userid", "-1")
            .addQueryParameter("clientver", "2000")
            .addQueryParameter("iscorrection", "1")
            .addQueryParameter("privilege_filter", "0")
            .addQueryParameter("filter", "10")
            .addQueryParameter("area_code", "1")
            // 不要加 tagtype：带 `tagtype=0` 时接口回 status=1 但 lists 为空（实测 2026-10-21）。
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Referer", "$KUGOU_BASE/")
            .build()

        val payload = client.textOf(request)?.jsonObjectOrNull() ?: return emptyList()
        val response = runCatching { json.decodeFromString<KugouSearchResponse>(payload) }
            .onFailure { Timber.w(it, "OnlineCatalogSearch: unreadable Kugou search") }
            .getOrNull() ?: return emptyList()

        return response.data?.lists.orEmpty().mapNotNull { dto ->
            // 酷狗取链认的是文件 hash，不是 songid。
            val hash = dto.fileHash.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MusicInfo(
                source = OnlineSource.KUGOU,
                id = hash,
                title = dto.songName,
                artist = dto.singerName,
                album = dto.albumName,
                durationSeconds = dto.durationSeconds.toLong(),
                coverUrl = dto.image
                    .takeIf { it.isNotBlank() }
                    ?.replace(KUGOU_IMAGE_SIZE_TOKEN, KUGOU_IMAGE_SIZE)
                    ?.replaceFirst("http://", "https://"),
                extras = mapOf("hash" to hash)
            )
        }
    }

    private fun searchKuwo(keyword: String, page: Int, limit: Int): List<MusicInfo> {
        val url = "$KUWO_BASE/r.s".toHttpUrl().newBuilder()
            .addQueryParameter("all", keyword)
            .addQueryParameter("ft", "music")
            .addQueryParameter("itemset", "web_2013")
            .addQueryParameter("client", "kt")
            // 酷我的页码从 0 开始，其它平台从 1 开始。
            .addQueryParameter("pn", (page - 1).toString())
            .addQueryParameter("rn", limit.toString())
            .addQueryParameter("rformat", "json")
            .addQueryParameter("encoding", "utf8")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEB_USER_AGENT)
            .build()

        // `rformat=json` 回的是单引号伪 JSON，不换成双引号连解码都进不去。
        val raw = client.textOf(request) ?: return emptyList()
        val payload = raw.replace('\'', '"').jsonObjectOrNull() ?: return emptyList()
        val response = runCatching { json.decodeFromString<KuwoSearchResponse>(payload) }
            .onFailure { Timber.w(it, "OnlineCatalogSearch: unreadable Kuwo search") }
            .getOrNull() ?: return emptyList()

        return response.songs.mapNotNull { dto ->
            // 酷我取链认 rid（`MUSIC_` 后面那串数字）。
            val rid = dto.rid.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MusicInfo(
                source = OnlineSource.KUWO,
                id = rid,
                title = dto.name.htmlUnescape(),
                artist = dto.artist.htmlUnescape(),
                album = dto.album.htmlUnescape(),
                durationSeconds = dto.duration.toLongOrNull() ?: 0L,
                coverUrl = dto.albumPicShort
                    .takeIf { it.isNotBlank() }
                    ?.let { "$KUWO_COVER$it" },
                extras = mapOf("rid" to rid)
            )
        }
    }

    private fun searchMigu(keyword: String, page: Int, limit: Int): List<MusicInfo> {
        val url = "$MIGU_BASE/MIGUM2.0/v1.0/content/search_all.do".toHttpUrl().newBuilder()
            .addQueryParameter("text", keyword)
            .addQueryParameter("pageNo", page.toString())
            .addQueryParameter("pageSize", limit.toString())
            .addQueryParameter("isCorrect", "1")
            .addQueryParameter("sort", "1")
            // 只要歌曲，别的类型（专辑 / 歌手 / MV）会挤占页大小。
            .addQueryParameter("searchSwitch", MIGU_SONG_ONLY)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MIGU_USER_AGENT)
            .build()

        val payload = client.textOf(request)?.jsonObjectOrNull() ?: return emptyList()
        val response = runCatching { json.decodeFromString<MiguSearchResponse>(payload) }
            .onFailure { Timber.w(it, "OnlineCatalogSearch: unreadable Migu search") }
            .getOrNull() ?: return emptyList()

        return response.songResult?.result.orEmpty().mapNotNull { dto ->
            val contentId = dto.contentId.takeIf { it.isNotBlank() }
                ?: dto.id.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            MusicInfo(
                source = OnlineSource.MIGU,
                id = contentId,
                title = dto.name,
                artist = dto.singers.joinToString(", ") { it.name },
                album = dto.albums.firstOrNull()?.name.orEmpty(),
                // 咪咕的搜索响应不带时长；`toLxPayload` 在没有时长时就不发 interval/duration。
                durationSeconds = 0L,
                coverUrl = dto.imgItems.firstOrNull()?.img?.takeIf { it.isNotBlank() },
                extras = mapOf(
                    "content_id" to contentId,
                    "copyright_id" to dto.copyrightId
                )
            )
        }
    }

    /** 酷我的字段里混着 `&nbsp;` 这类 HTML 实体，标题直接显示会露出原样。 */
    private fun String.htmlUnescape(): String = replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()

    /** 网易云的封面地址仍是 http，升到 https 免得走明文（App 虽然放行了明文，但没必要）。 */
    private fun String.toHttps(): String = replaceFirst("http://", "https://")

    private companion object {
        const val NETEASE_BASE = "https://music.163.com"
        const val QQ_BASE = "https://c.y.qq.com"
        const val QQ_COVER = "https://y.gtimg.cn/music/photo_new/T002R300x300M000"

        const val KUGOU_BASE = "https://songsearch.kugou.com"
        const val KUGOU_IMAGE_SIZE_TOKEN = "{size}"
        const val KUGOU_IMAGE_SIZE = "500"

        const val KUWO_BASE = "https://search.kuwo.cn"
        const val KUWO_COVER = "https://img1.kuwo.cn/star/albumcover/"

        const val MIGU_BASE = "https://app.c.nf.migu.cn"
        const val MIGU_SONG_ONLY = """{"song":1}"""
        const val MIGU_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15"

        const val WEB_USER_AGENT = "Mozilla/5.0 (Linux; Android 13) Sonic"

        const val CALL_TIMEOUT_SECONDS = 12L
    }
}
