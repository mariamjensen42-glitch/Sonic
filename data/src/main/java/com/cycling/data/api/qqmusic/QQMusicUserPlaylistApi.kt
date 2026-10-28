package com.cycling.data.api.qqmusic

import com.cycling.domain.model.ImportedPlaylist
import com.cycling.domain.model.QQMusicPlaylistSummary
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * 登录账号的「我的歌单」读取。
 *
 * 与 [com.cycling.data.api.playlist.QqPlaylistApi] 的分工：
 * - 那支走公开的 `fcg_ucc_getcdinfo_byids_cp.fcg`，只要知道链接就能读，**不需要登录**；
 * - 这支走 `musicu.fcg` 的 `PlaylistBaseRead` / `srfDissInfo`，**必须要登录态**，
 *   换来的是「账号名下有哪些歌单」—— 这是链接导入给不了的信息。
 *
 * 两个接口的字段名都是 QQ 的老拼法（`dirName` / `dissname` / `songmid`），且同一语义在
 * 不同模块里叫法不同（`tid` / `disstid` / `dirId`），所以解析一律按「多候选键依次回落」，
 * 不做严格 DTO —— 一个字段改名就让整个歌单读不出来，代价太大。
 */
@Singleton
class QQMusicUserPlaylistApi @Inject constructor(
    sharedClient: OkHttpClient,
    private val sessionProvider: QQMusicSessionProvider
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .withQqWebUserAgent()
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 账号名下的全部歌单（含「我喜欢」）。
     *
     * 未登录返回空列表而不是抛异常：界面在未登录时本来就不该展示这个入口，
     * 真走到了也只是「没有歌单」，不是错误。
     */
    suspend fun myPlaylists(): List<QQMusicPlaylistSummary> = withContext(Dispatchers.IO) {
        val session = sessionProvider.current() ?: return@withContext emptyList()
        val data = postMusicu(
            session = session,
            module = "music.musicasset.PlaylistBaseRead",
            method = "GetPlaylistByUin",
            param = buildJsonObject { put("uin", session.uin) }
        )
        val raw = data.array("v_playlist") ?: data.array("playlist") ?: return@withContext emptyList()
        raw.mapNotNull(::parsePlaylistSummary)
    }

    /**
     * 拉一张歌单的全部曲目。
     *
     * 分页拉到底：`CgiGetDiss` 单页最多 200 首，几百首的歌单只取第一页会静默少歌。
     * 上限 [MAX_TRACKS] 是防死循环的保险 —— 真有超过两千首的歌单，导入也不该在这里卡住。
     *
     * @return 歌单信息与曲目；歌单不可读（私密、已删除）时返回 `null`。
     */
    suspend fun playlistDetail(playlistId: String): ImportedPlaylist? = withContext(Dispatchers.IO) {
        val session = sessionProvider.current() ?: return@withContext null
        val numericId = playlistId.toLongOrNull() ?: return@withContext null

        var summary: QQMusicPlaylistSummary? = null
        val tracks = LinkedHashMap<String, MusicInfo>()
        var offset = 0
        while (tracks.size < MAX_TRACKS) {
            val data = postMusicu(
                session = session,
                module = "music.srfDissInfo.DissInfo",
                method = "CgiGetDiss",
                param = buildJsonObject {
                    put("disstid", numericId)
                    put("dirid", 0)
                    put("tag", true)
                    put("song_begin", offset)
                    put("song_num", PAGE_SIZE)
                    put("userinfo", true)
                    put("orderlist", true)
                    put("onlysonglist", false)
                }
            )
            summary = summary ?: data.obj("dirinfo")?.let(::parsePlaylistSummary)
            val page = data.array("songlist").orEmpty()
            if (page.isEmpty()) break
            page.forEach { item ->
                val track = parseTrack(item) ?: return@forEach
                tracks.putIfAbsent(track.id, track)
            }
            offset += page.size
            val total = data.int("total_song_num") ?: 0
            if (total > 0 && offset >= total) break
            // 服务端可能忽略分页参数、每页都回同一批：没有新增就停，别原地打转。
            if (page.size < PAGE_SIZE) break
        }

        if (tracks.isEmpty()) {
            Timber.d("QQMusicUserPlaylist: %s 没有可读曲目", playlistId)
            return@withContext null
        }
        ImportedPlaylist(
            source = OnlineSource.QQ,
            name = summary?.title?.takeIf(String::isNotBlank) ?: "QQ 音乐歌单",
            description = null,
            coverUrl = summary?.coverUrl,
            tracks = tracks.values.toList()
        )
    }

    private fun postMusicu(
        session: QQMusicSession,
        module: String,
        method: String,
        param: JsonObject
    ): JsonObject {
        val gtk = qqHash33(session.musicKey, 5381)
        val payload = buildJsonObject {
            put(
                "comm",
                buildJsonObject {
                    put("ct", 24)
                    put("cv", WEB_CLIENT_VERSION)
                    put("platform", "yqq.json")
                    put("chid", "0")
                    put("uin", session.uin.toLongOrNull() ?: 0L)
                    put("g_tk", gtk)
                    put("g_tk_new_20200303", gtk)
                    put("format", "json")
                    put("inCharset", "utf-8")
                    put("outCharset", "utf-8")
                    put("notice", 0)
                    put("need_new_code", 1)
                }
            )
            put(
                "req_0",
                buildJsonObject {
                    put("module", module)
                    put("method", method)
                    put("param", param)
                }
            )
        }
        val request = Request.Builder()
            .url(MUSICU_URL)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "$QQ_WEB/")
            .apply { session.cookie.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("QQ 音乐歌单返回 HTTP ${response.code}")
            if (body.isBlank()) throw IOException("QQ 音乐歌单返回空响应")
            val root = json.parseToJsonElement(body).jsonObject
            val moduleData = root.obj("req_0") ?: throw IOException("QQ 音乐歌单响应缺少 req_0")
            val code = moduleData.int("code") ?: 0
            if (code != 0) {
                throw IOException(
                    moduleData.string("message").ifBlank { moduleData.string("msg") }
                        .ifBlank { "QQ 音乐歌单错误码 $code" }
                )
            }
            moduleData.obj("data") ?: JsonObject(emptyMap())
        }
    }

    private fun parsePlaylistSummary(item: JsonObject): QQMusicPlaylistSummary? {
        val id = item.firstString("tid", "dirId", "disstid", "dissid", "id")
            .ifBlank { item.firstLong("tid", "dirId", "disstid", "dissid", "id")?.toString().orEmpty() }
        if (id.isBlank()) return null
        val cover = item.firstString("picUrl", "picurl", "logo", "cover", "coverUrl")
            .takeIf(String::isNotBlank)
            ?.toHttps()
        return QQMusicPlaylistSummary(
            id = id,
            title = item.firstString("dirName", "dissname", "title", "name").ifBlank { "QQ 音乐歌单" },
            coverUrl = cover,
            trackCount = (item.firstLong("songNum", "songnum", "song_cnt") ?: 0L).toInt(),
            creatorName = item.obj("creator")?.firstString("nick", "nickname", "name")
                ?.takeIf(String::isNotBlank)
        )
    }

    private fun parseTrack(item: JsonObject): MusicInfo? {
        val songMid = item.firstString("mid", "songmid", "songMid")
            .ifBlank { item.obj("mid")?.string("song").orEmpty() }
        if (songMid.isBlank()) return null
        val singers = item.array("singer") ?: item.array("singers").orEmpty()
        val artist = singers
            .mapNotNull { it.firstString("name", "title", "singerName").takeIf(String::isNotBlank) }
            .joinToString(", ")
            .ifBlank { "未知艺术家" }
        val albumObject = item.obj("album")
        val albumMid = albumObject?.firstString("mid", "albumMid").orEmpty()
            .ifBlank { item.firstString("albummid", "albumMid") }
        val albumName = albumObject?.firstString("name", "title").orEmpty()
            .ifBlank { item.firstString("albumname", "albumName") }
        val durationSeconds = item.firstLong("interval", "duration") ?: 0L
        return MusicInfo(
            source = OnlineSource.QQ,
            id = songMid,
            title = item.firstString("name", "title", "songname").ifBlank { "未知歌曲" },
            artist = artist,
            album = albumName,
            durationSeconds = durationSeconds,
            coverUrl = albumMid.takeIf(String::isNotBlank)?.let { "$QQ_COVER$it.jpg" },
            extras = buildMap {
                put("mid", songMid)
                item.obj("file")?.string("media_mid")
                    ?.takeIf(String::isNotBlank)
                    ?.let { put("media_mid", it) }
            }
        )
    }

    private fun String.toHttps(): String =
        if (startsWith("http://", ignoreCase = true)) "https://${substringAfter("://")}" else this

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }.orEmpty()

    private fun JsonObject.int(key: String): Int? = string(key).toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): List<JsonObject>? =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }

    /** 同一语义在 QQ 的各模块里叫法不同，按候选键依次取第一个非空值。 */
    private fun JsonObject.firstString(vararg keys: String): String =
        keys.asSequence().map { string(it) }.firstOrNull(String::isNotBlank).orEmpty()

    private fun JsonObject.firstLong(vararg keys: String): Long? =
        keys.asSequence().mapNotNull { string(it).toLongOrNull() }.firstOrNull()

    private companion object {
        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val QQ_WEB = "https://y.qq.com"
        const val QQ_COVER = "https://y.qq.com/music/photo_new/T002R300x300M000"
        const val WEB_CLIENT_VERSION = 4_747_474
        const val CALL_TIMEOUT_SECONDS = 20L
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        /** `CgiGetDiss` 单页上限；再大服务端会截断。 */
        const val PAGE_SIZE = 200

        /** 单张歌单最多拉多少首，防分页死循环。 */
        const val MAX_TRACKS = 2_000
    }
}
