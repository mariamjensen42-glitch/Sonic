package com.cycling.data.api.qqmusic

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * 搜索结果里的一首歌。
 *
 * @property songMid 平台内的 `songmid`，歌词与封面接口都按它寻址。
 * @property songId 数字歌曲 id；逐字歌词的 Mei 协议要它。
 * @property title 曲名，**带版本信息**（`晴天 (Live)`、`青花瓷 (DJ版)`）。
 *   平台同时给一个不含版本的 `name`（`晴天`），但那一份分不出现场版与录音室版，
 *   匹配与选版都靠 `title`。
 */
data class QQSearchSong(
    val songMid: String,
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumMid: String,
    val durationMs: Long,
    /** 发行时间（秒）；平台偶尔不给。 */
    val pubTimeSeconds: Long = 0L
)

/**
 * QQ 音乐搜索（`musicu.fcg` 的 `music.search.SearchCgiService.DoSearchForQQMusicDesktop`）。
 *
 * **为什么不用老的 `client_search_cp`**：那条老接口已经下线，实测直接回 HTTP 500。
 * 它一度是本项目里三处（QQ 歌词、QQ 封面、在线目录搜索）的唯一入口，所以那三处一起失效，
 * 表现为「QQ 音乐全都搜不到」——歌词接口本身其实还是好的，缺的只是 `songmid`。
 *
 * **为什么要缓存**：QQ 对**重复搜索同一个关键词**有风控，同一个词短时间内再搜会拿到
 * `code = 2001` 与空列表（实测：新关键词立刻能搜到，重复搜同一个词就 2001）。而本项目的用法
 * 天然会重复搜同一首歌 —— 每次播放都查一次歌词、每次打开版本选择器都查一次。因此
 * 关键词 → 结果的映射要留在进程内：成功的结果放很久，失败的结果也放一小会儿（免得反复
 * 撞风控把窗口越撞越长）。
 *
 * **为什么要有多个关键词**：风控是按关键词算的，换一种写法就是另一个词。调用方给出主关键词
 * 与若干变体，这里按顺序试，第一个有结果的即为答案。
 */
@Singleton
class QQMusicSearchClient @Inject constructor(
    sharedClient: OkHttpClient,
    private val sessionProvider: QQMusicSessionProvider
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .withQqWebUserAgent()
        .build()

    /** 关键词 → 结果。成功的结果留到 [SUCCESS_TTL_MS]，失败的结果只留 [FAILURE_TTL_MS]。 */
    private val cache = ConcurrentHashMap<String, CachedSearch>()

    /**
     * 所有 QQ 搜索共用一把锁。
     *
     * 两个作用：同一时刻只发一个搜索（并发搜同一关键词必被风控），以及给两次请求之间留出
     * 最小间隔。锁的粒度是进程内的搜索入口，不影响其它网络请求。
     */
    private val requestLock = Mutex()
    private var lastRequestAt = 0L

    /**
     * 按 [keywords] 依次搜索，返回第一份有结果的。
     *
     * @param keywords 主关键词在前，变体在后。空的关键词会被跳过。
     * @param limit 最多要几条。
     * @param page 第几页，从 1 起。
     * @return 搜索结果；全部关键词都没搜到时是空列表。
     */
    suspend fun search(keywords: List<String>, limit: Int, page: Int = 1): List<QQSearchSong> {
        val wanted = keywords.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (wanted.isEmpty() || limit <= 0) return emptyList()

        val safePage = page.coerceAtLeast(1)
        val session = sessionProvider.current()
        for (keyword in wanted) {
            val cacheKey = "$keyword#$safePage"
            val cached = cache[cacheKey]
            if (cached != null && !cached.isExpired()) {
                if (cached.songs.isNotEmpty()) return cached.songs.take(limit)
                // 缓存里是一次失败：换下一个变体词，别在同一个词上再撞一次。
                continue
            }

            val found = requestLock.withLock { request(keyword, limit, safePage, session) }
            if (found.isNotEmpty()) {
                cache[cacheKey] = CachedSearch(found, success = true)
                return found.take(limit)
            }

            // 失败也要缓存：短时间内反复搜同一个词只会把风控窗口越撞越长。
            cache[cacheKey] = CachedSearch(emptyList(), success = false)
        }
        return emptyList()
    }

    /** 一次真正的搜索请求；调用方已持有 [requestLock]。 */
    private suspend fun request(
        keyword: String,
        limit: Int,
        page: Int,
        session: QQMusicSession?
    ): List<QQSearchSong> = withContext(Dispatchers.IO) {
        // 与上一次搜索之间留出间隔：连着打同一类请求最容易被判成爬虫。
        val since = System.currentTimeMillis() - lastRequestAt
        if (since < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - since)
        lastRequestAt = System.currentTimeMillis()

        val payload = buildJsonObject {
            put("comm", buildJsonObject {
                put("ct", "19")
                put("cv", "1859")
                put("uin", session?.uin?.takeIf { it.isNotBlank() } ?: "0")
                put("format", "json")
            })
            put("req_0", buildJsonObject {
                put("method", "DoSearchForQQMusicDesktop")
                put("module", "music.search.SearchCgiService")
                put("param", buildJsonObject {
                    put("query", keyword)
                    put("num_per_page", limit.coerceIn(1, MAX_RESULTS))
                    put("page_num", page)
                    put("search_type", 0)
                })
            })
        }

        val request = Request.Builder()
            .url(MUSICU_URL)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "$QQ_WEB/")
            .apply { session?.cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val body = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("QQ 音乐搜索返回 HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            Timber.w(e, "QQMusicSearch: 搜索失败 keyword=$keyword")
            return@withContext emptyList()
        }

        val songs = parseQqMusicSearch(body)
        if (songs.isEmpty()) {
            // 2001 是风控：不是「没有这首歌」，而是「这个词现在不给搜」。
            Timber.d("QQMusicSearch: 搜索无结果 keyword=$keyword code=${qqSearchCode(body)}")
        }
        songs
    }

    private data class CachedSearch(
        val songs: List<QQSearchSong>,
        val success: Boolean,
        val savedAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            val ttl = if (success) SUCCESS_TTL_MS else FAILURE_TTL_MS
            return System.currentTimeMillis() - savedAt > ttl
        }
    }

    private companion object {
        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val QQ_WEB = "https://y.qq.com"
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        const val CALL_TIMEOUT_SECONDS = 15L

        /** 两次搜索之间的最小间隔（毫秒）。 */
        const val MIN_INTERVAL_MS = 900L

        /** 平台一次最多给多少条。 */
        const val MAX_RESULTS = 20

        /** 搜到的结果多久内可以直接复用（`songmid` 不会变）。 */
        const val SUCCESS_TTL_MS = 6L * 60 * 60 * 1000

        /** 搜不到的结果多久后再重试：短到能自我恢复，长到不会连着撞风控。 */
        const val FAILURE_TTL_MS = 2L * 60 * 1000
    }
}

/**
 * 解析 `musicu.fcg` 的搜索响应。
 *
 * 响应结构：`req_0.data.body.song.list[]`。每首歌给出 `mid`（寻址用）、`id`（Mei 协议用）、
 * `title`（带版本信息）、`singer[]`、`album`、`interval`（秒）。
 *
 * 纯函数，不碰网络；见 `QQMusicSearchTest`。
 */
internal fun parseQqMusicSearch(body: String): List<QQSearchSong> {
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
    val request = root["req_0"] as? JsonObject ?: return emptyList()
    val songs = request.obj("data")?.obj("body")?.obj("song")?.array("list") ?: return emptyList()

    return songs.mapNotNull { element ->
        val song = element as? JsonObject ?: return@mapNotNull null
        val mid = song.string("mid")
        if (mid.isBlank()) return@mapNotNull null
        QQSearchSong(
            songMid = mid,
            songId = song.string("id").toLongOrNull() ?: 0L,
            // `title` 带版本信息（`晴天 (Live)`），`name` 不带 —— 选版要用带版本的那一份。
            title = song.string("title").ifBlank { song.string("name") },
            artist = song.array("singer")
                ?.mapNotNull { (it as? JsonObject)?.string("name") }
                ?.filter { it.isNotBlank() }
                ?.joinToString(", ")
                .orEmpty(),
            album = song.obj("album")?.string("name").orEmpty(),
            albumMid = song.obj("album")?.string("mid").orEmpty(),
            durationMs = song.string("interval").toLongOrNull()?.times(1000L) ?: 0L,
            pubTimeSeconds = song.string("time_public").toLongOrNull() ?: 0L
        )
    }
}

/** 搜索响应的 `req_0.code`；`2001` 是风控，不是「没有结果」。 */
internal fun qqSearchCode(body: String): Int {
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return -1
    return (root["req_0"] as? JsonObject)?.int("code") ?: -1
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

private fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }.orEmpty()

private fun JsonObject.int(key: String): Int? = string(key).toIntOrNull()
