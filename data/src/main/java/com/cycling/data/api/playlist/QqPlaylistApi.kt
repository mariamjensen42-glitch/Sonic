package com.cycling.data.api.playlist

import com.cycling.data.api.cover.jsonObjectOrNull
import com.cycling.data.api.cover.textOf
import com.cycling.data.api.playlist.dto.QqPlaylistResponse
import com.cycling.data.api.playlist.dto.QqPlaylistSongDto
import com.cycling.domain.model.ImportedPlaylist
import com.cycling.domain.online.PlaylistFetchResult
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * 抓 QQ 音乐歌单的公开详情。
 *
 * 用的是老的那支 `fcg_ucc_getcdinfo_byids_cp.fcg`：不带任何登录态也能读公开歌单，一次就把
 * `songnum` 首歌全给回来（实测 671 首不分页），比走 `musicu.fcg` 的那套 `sirius` 模块省事得多。
 * 它只认 `Referer: https://y.qq.com/`，缺了会回一段错误体。
 *
 * 拿到的 `songmid` 与搜索接口（`OnlineCatalogSearchService.searchQq`）用的是**同一个值**，
 * 也就是洛雪 `tx` 源脚本要的 `musicId` —— 所以这里产出的 [MusicInfo] 可以直接喂给取链链路，
 * 不需要再搜一遍。
 */
@Singleton
class QqPlaylistApi @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // 短链靠重定向展开，默认就是跟随的；这里显式写出来，免得以后有人给共享 client 加上开关
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchPlaylist(shareText: String): PlaylistFetchResult =
        withContext(Dispatchers.IO) {
            val disstid = resolveDisstid(shareText) ?: return@withContext PlaylistFetchResult.InvalidLink
            Timber.d("QqPlaylistApi: disstid=$disstid")

            val request = Request.Builder()
                .url(detailUrl(disstid))
                .header("Referer", "$QQ_WEB/")
                .build()
            val payload = client.textOf(request)?.jsonObjectOrNull()
                ?: return@withContext PlaylistFetchResult.Failed(null)
            val response = runCatching { json.decodeFromString<QqPlaylistResponse>(payload) }
                .onFailure { Timber.w(it, "QqPlaylistApi: unreadable playlist $disstid") }
                .getOrNull() ?: return@withContext PlaylistFetchResult.Failed(null)

            if (response.code != 0) {
                Timber.w("QqPlaylistApi: code=${response.code} subcode=${response.subcode}")
                return@withContext PlaylistFetchResult.Failed(null)
            }
            // 私密 / 已删除的歌单 code 仍是 0，只是 cdlist 空着 —— 这个接口不告诉我们到底是哪种
            val detail = response.cdlist.firstOrNull()
                ?: return@withContext PlaylistFetchResult.NotAccessible
            if (detail.songlist.isEmpty()) {
                return@withContext PlaylistFetchResult.NotAccessible
            }

            PlaylistFetchResult.Success(
                ImportedPlaylist(
                    source = OnlineSource.QQ,
                    name = detail.dissname.ifBlank { "QQ 音乐歌单" },
                    description = detail.desc.takeIf { it.isNotBlank() },
                    coverUrl = detail.logo.takeIf { it.isNotBlank() },
                    tracks = detail.songlist.mapNotNull { it.toMusicInfo() }
                )
            )
        }

    /**
     * 从「分享文本 / 链接 / 裸 id」里挖出歌单 id。
     *
     * 短链（`c6.y.qq.com/base/fcgi-bin/u?__=xxx`）里没有任何 id，只能请求一次让 OkHttp 跟着
     * 重定向走到底，再从**最终** URL 上取。这一步只读响应头就够，不下载页面正文。
     */
    private fun resolveDisstid(shareText: String): String? {
        extractDisstid(shareText)?.let { return it }
        val url = URL_IN_TEXT.find(shareText)?.value ?: return null
        if (!url.contains(QQ_HOST)) return null

        val finalUrl = runCatching {
            client.newCall(Request.Builder().url(url).header("Referer", "$QQ_WEB/").build())
                .execute()
                .use { it.request.url.toString() }
        }.onFailure { Timber.w(it, "QqPlaylistApi: short link expand failed") }
            .getOrNull() ?: return null
        return extractDisstid(finalUrl)
    }

    private fun extractDisstid(text: String): String? {
        BARE_ID.matchEntire(text.trim())?.let { return it.value }
        for (pattern in ID_PATTERNS) {
            pattern.find(text)?.let { return it.groupValues[1] }
        }
        return null
    }

    private fun detailUrl(disstid: String) =
        "$QQ_BASE/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg".toHttpUrl().newBuilder()
            .addQueryParameter("type", "1")
            .addQueryParameter("json", "1")
            .addQueryParameter("utf8", "1")
            .addQueryParameter("onlysong", "0")
            .addQueryParameter("disstid", disstid)
            .addQueryParameter("format", "json")
            .addQueryParameter("g_tk", "5381")
            .addQueryParameter("loginUin", "0")
            .addQueryParameter("hostUin", "0")
            .addQueryParameter("inCharset", "utf8")
            .addQueryParameter("outCharset", "utf-8")
            .addQueryParameter("notice", "0")
            .addQueryParameter("platform", "yqq.json")
            .addQueryParameter("needNewCode", "0")
            .build()

    private fun QqPlaylistSongDto.toMusicInfo(): MusicInfo? {
        val mid = songmid.takeIf { it.isNotBlank() } ?: return null
        return MusicInfo(
            source = OnlineSource.QQ,
            id = mid,
            title = songname,
            artist = singer.joinToString(", ") { it.name }.ifBlank { "未知艺术家" },
            album = albumname,
            durationSeconds = interval.toLong(),
            coverUrl = albummid.takeIf { it.isNotBlank() }?.let { "$QQ_COVER$it.jpg" },
            extras = mapOf("mid" to mid)
        )
    }

    private companion object {
        const val CALL_TIMEOUT_SECONDS = 20L
        const val QQ_BASE = "https://c.y.qq.com"
        const val QQ_WEB = "https://y.qq.com"
        const val QQ_HOST = "y.qq.com"
        const val QQ_COVER = "https://y.gtimg.cn/music/photo_new/T002R300x300M000"

        val BARE_ID = Regex("""^\d{4,}$""")
        val URL_IN_TEXT = Regex("""https?://[^\s，、"'）】]+""")
        val ID_PATTERNS = listOf(
            Regex("""[?&](?:id|disstid)=(\d+)"""),
            Regex("""/playlist/(\d+)"""),
            Regex("""taoge\.html\?[^\s]*?id=(\d+)""")
        )
    }
}
