package com.cycling.data.api.qqmusic

import com.cycling.domain.online.model.MusicQuality
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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
 * QQ 音乐官方取链（`music.vkey.GetVkey / UrlGetVkey`）。
 *
 * 宿主默认靠 LX 音源脚本取链，这条路只在**脚本取不到**时兜底 —— 它解决的是「用户登了
 * QQ 音乐账号、却没导入 `tx` 脚本（或脚本被风控挡住）」这一种情况，不该抢在前面。
 *
 * 三个协议细节决定了它的形状：
 *
 * 1. **按音质降级**：请求里只能给一个 `filename`，拿不到就换下一个前缀重试。所以档位是
 *    一条候选链（[vkeyCandidates]），而不是一次请求带多个。
 * 2. **`purl` 为空不是错误**：它表示「这个账号没有该音质的权限」，真正的失败原因在
 *    `midurlinfo[0].result` 的业务码里（104003 无权限 / 104013 设备受限）。
 * 3. **域名在 `sip` 里**：`purl` 是相对路径，要拼上 `data.sip` 的第一个可用域名；
 *    `ws` 开头的流媒体节点要先跳过。
 */
@Singleton
class QQMusicPlaybackVkeyClient @Inject constructor(
    sharedClient: OkHttpClient,
    private val sessionProvider: QQMusicSessionProvider
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .withQqWebUserAgent()
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 取一条可播放地址。
     *
     * @param songMid 平台内的 `songmid`。
     * @param mediaMid 媒体 id（`file.media_mid`）；没有时按 QQ 的规则用 `songmid` 顶替。
     * @return 可播放的 https 地址；未登录、无权限、全部候选都失败时返回 `null`。
     */
    suspend fun resolve(
        songMid: String,
        mediaMid: String? = null,
        quality: MusicQuality
    ): String? = withContext(Dispatchers.IO) {
        if (songMid.isBlank()) return@withContext null
        val session = sessionProvider.current() ?: return@withContext null

        val businessCodes = linkedSetOf<Int>()
        var lastReason: String? = null
        for (candidate in quality.vkeyCandidates()) {
            val outcome = runCatching {
                requestUrl(songMid, mediaMid, candidate, session)
            }
            val url = outcome.getOrNull()
            if (!url.isNullOrBlank()) {
                Timber.d("QQMusicVkey: %s 命中 %s", songMid, candidate.prefix)
                return@withContext url
            }
            when (val failure = outcome.exceptionOrNull()) {
                is QQVkeyBusinessException -> {
                    businessCodes += failure.businessCode
                    lastReason = failure.message
                }

                null -> Unit
                else -> lastReason = failure.message ?: lastReason
            }
        }

        // 全都没拿到：把最有信息量的那个业务码报出来，日志里能直接看出是「没权限」还是「设备受限」。
        Timber.w(
            "QQMusicVkey: %s 取链失败（%s）%s",
            songMid,
            businessCodes.joinToString(",").ifBlank { "no-code" },
            lastReason.orEmpty()
        )
        null
    }

    /**
     * 一次候选尝试。
     *
     * @return `purl` 拼出的绝对地址；`purl` 为空返回 `null`（业务码非 0 时抛 [QQVkeyBusinessException]）。
     */
    private fun requestUrl(
        songMid: String,
        mediaMid: String?,
        candidate: VkeyCandidate,
        session: QQMusicSession
    ): String? {
        val fileName = candidate.fileName(songMid, mediaMid)
        val guid = Random.nextLong(GUID_MIN, GUID_MAX).toString()
        val payload = buildJsonObject {
            put(
                "comm",
                buildJsonObject {
                    put("ct", ANDROID_CLIENT_TYPE)
                    put("cv", ANDROID_CLIENT_VERSION)
                    put("v", ANDROID_CLIENT_VERSION)
                    put("qq", session.uin.ifBlank { "0" })
                    put("authst", session.musicKey)
                    put("tmeAppID", "qqmusic")
                    put("format", "json")
                }
            )
            put(
                "req_0",
                buildJsonObject {
                    put("module", "music.vkey.GetVkey")
                    put("method", "UrlGetVkey")
                    put(
                        "param",
                        buildJsonObject {
                            put("uin", session.uin.ifBlank { "0" })
                            put("filename", buildJsonArray { add(JsonPrimitive(fileName)) })
                            put("guid", guid)
                            put("songmid", buildJsonArray { add(JsonPrimitive(songMid)) })
                            put("songtype", buildJsonArray { add(JsonPrimitive(0)) })
                            put("ctx", 0)
                        }
                    )
                }
            )
        }

        val request = Request.Builder()
            .url(MUSICU_URL)
            .header("User-Agent", "QQMusic $ANDROID_CLIENT_VERSION(android 15)")
            .header("Accept", "application/json, text/plain, */*")
            .apply { session.cookie.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("QQ 音乐取链返回 HTTP ${response.code}")
            if (body.isBlank()) throw IOException("QQ 音乐取链返回空响应")
            val root = json.parseToJsonElement(body).jsonObject
            val module = root.obj("req_0") ?: throw IOException("QQ 音乐取链响应缺少 req_0")
            val code = module.int("code") ?: 0
            if (code != 0) {
                throw IOException(
                    module.string("message").ifBlank { module.string("msg") }
                        .ifBlank { "QQ 音乐取链错误码 $code" }
                )
            }
            val data = module.obj("data") ?: return@use null
            val midUrl = data.array("midurlinfo")?.firstOrNull() ?: return@use null
            val businessCode = midUrl.int("result") ?: 0
            if (businessCode != 0) throw QQVkeyBusinessException(businessCode)
            val purl = midUrl.string("purl").trim()
            if (purl.isBlank()) return@use null
            if (purl.startsWith("https://") || purl.startsWith("http://")) {
                return@use purl.toHttps()
            }
            val domain = playableDomain(data.stringArray("sip")) ?: DEFAULT_STREAM_HOST
            (domain + purl).toHttps()
        }
    }

    /** `sip` 里第一个非 `ws` 的域名；`ws` 是流媒体协议，直接拼 http 地址会放不出来。 */
    private fun playableDomain(sips: List<String>): String? {
        val usable = sips.map { it.trim() }.filter { it.isNotBlank() }
        return usable.firstOrNull { !it.startsWith("http://ws", ignoreCase = true) }
            ?: usable.firstOrNull()
    }

    private fun String.toHttps(): String =
        if (startsWith("http://", ignoreCase = true)) "https://${substringAfter("://")}" else this

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }.orEmpty()

    private fun JsonObject.int(key: String): Int? = string(key).toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): List<JsonObject>? =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }

    /** `sip` 是字符串数组，和 `midurlinfo` 这种对象数组不是一回事。 */
    private fun JsonObject.stringArray(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    /** 一个文件前缀候选：[prefix] 决定音质，[extension] 决定容器。 */
    private data class VkeyCandidate(val prefix: String, val extension: String) {

        /**
         * QQ 的文件名规则：有 `media_mid` 时 `前缀 + media_mid + 后缀`，
         * 没有时用 `前缀 + songmid + songmid + 后缀`（两段 mid 不是笔误）。
         */
        fun fileName(songMid: String, mediaMid: String?): String {
            val media = mediaMid?.takeIf(String::isNotBlank)
            return if (media != null) {
                "$prefix$media$extension"
            } else {
                "$prefix$songMid$songMid$extension"
            }
        }
    }

    /**
     * 音质档位 → 候选链。
     *
     * QQ 没有与宿主 11 档一一对应的文件前缀：`flac24bit` / `鲸云` 系列在官方接口里没有独立
     * 前缀，只能落到 `F000`（无损）或 `AI00`（母带）。这里如实降级，不把 FLAC 冒充成 Hi-Res。
     */
    private fun MusicQuality.vkeyCandidates(): List<VkeyCandidate> = when (this) {
        MusicQuality.STANDARD -> listOf(MP3_128)
        MusicQuality.EXHIGH -> listOf(MP3_320, MP3_128)
        MusicQuality.LOSSLESS -> listOf(FLAC, MP3_320, MP3_128)
        MusicQuality.HIRES -> listOf(FLAC, MP3_320, MP3_128)
        MusicQuality.FLAC24BIT -> listOf(FLAC, MP3_320, MP3_128)
        MusicQuality.JY_EFFECT -> listOf(FLAC, MP3_320, MP3_128)
        MusicQuality.JY_SKY -> listOf(FLAC, MP3_320, MP3_128)
        MusicQuality.JY_MASTER -> listOf(MASTER, FLAC, MP3_320, MP3_128)
        MusicQuality.MASTER -> listOf(MASTER, FLAC, MP3_320, MP3_128)
        MusicQuality.ATMOS,
        MusicQuality.ATMOS_PLUS -> listOf(ATMOS_Q001, ATMOS_Q000, FLAC, MP3_320, MP3_128)
    }

    private companion object {
        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val DEFAULT_STREAM_HOST = "https://isure.stream.qqmusic.qq.com/"
        const val ANDROID_CLIENT_TYPE = 11
        const val ANDROID_CLIENT_VERSION = 14_090_008
        const val CALL_TIMEOUT_SECONDS = 20L
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        /** `guid` 只是设备标识，随便给一个 10 位数即可；固定值会被服务端当成同一台设备。 */
        const val GUID_MIN = 1_000_000_000L
        const val GUID_MAX = 9_999_999_999L

        val MP3_128 = VkeyCandidate("M500", ".mp3")
        val MP3_320 = VkeyCandidate("M800", ".mp3")
        val FLAC = VkeyCandidate("F000", ".flac")
        val MASTER = VkeyCandidate("AI00", ".flac")
        val ATMOS_Q001 = VkeyCandidate("Q001", ".flac")
        val ATMOS_Q000 = VkeyCandidate("Q000", ".flac")
    }
}

/**
 * `purl` 为空且业务码非 0 时的信号。
 *
 * 单独一个类型是因为调用方要区分「这个音质没权限」（换低档可能成功）与「网络/协议失败」
 * （换档也白搭，但值得记一条日志）。
 */
internal class QQVkeyBusinessException(
    val businessCode: Int
) : IOException(vkeyBusinessReason(businessCode))

/** 业务码 → 人话。码值来自 QQ 音乐 Android 客户端的实测。 */
internal fun vkeyBusinessReason(code: Int): String = when (code) {
    104003 -> "当前 QQ 音乐账号没有该音源的播放权限（104003）"
    104004 -> "QQ 音乐 VKey 获取失败（104004）"
    104013 -> "QQ 音乐当前播放设备受限（104013）"
    else -> "QQ 音乐音源授权失败（$code）"
}
