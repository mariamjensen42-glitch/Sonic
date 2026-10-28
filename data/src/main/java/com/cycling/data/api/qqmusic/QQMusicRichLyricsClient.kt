package com.cycling.data.api.qqmusic

import com.cycling.data.online.lx.QrcCodec
import java.io.IOException
import java.util.Base64
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
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * QQ 音乐逐字歌词（QRC）的取词客户端。
 *
 * 普通歌词接口（`fcg_query_lyric_new.fcg`）只给行级 LRC，而 QQ 的逐字时间轴只存在于
 * `music.musichallSong.PlayLyricInfo / GetPlayLyricInfo` 的 QRC 字段里 —— 它是一段
 * 加密 zlib 流，必须用 [QrcCodec] 解。这里做三件事：
 *
 * 1. **换身份**：QRC 的「Mei」协议要数字 `songId`，而曲目在宿主里只有 `songmid`，
 *    所以先打一次 `fcg_play_single_song.fcg` 把它换成 `songId` + `songType`。
 * 2. **多档兜底**：同一支接口在不同客户端档位（Mei / Android / Web / Desktop）下给的
 *    字段不一样，会员曲目还只在登录态下才给完整 QRC。按档位依次试，任何一档给出逐字
 *    时间轴就收。
 * 3. **合并翻译**：QRC 的 `trans` 是**另一份**密文，解码后按行首时间插到原文行下面 ——
 *    [com.cycling.domain.lyrics.parser.YrcParser] 正是按「同 start、无逐字标签」把下一行
 *    认作译文，插错位置会让每句翻译变成独立歌词。
 *
 * 返回的文本是 **YRC**（`[start,dur](s,d,0)字…`），不是 QRC：歌词管线的格式猜测器认得
 * YRC，不认得 QRC 的 XML/密文。
 */
@Singleton
class QQMusicRichLyricsClient @Inject constructor(
    sharedClient: OkHttpClient,
    private val sessionProvider: QQMusicSessionProvider
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .withQqWebUserAgent()
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 取一首歌的逐字歌词。
     *
     * @param songMid 平台内的 `songmid`（也是宿主里的 `onlineSongId`）。
     * @param numericSongId 已知的数字歌曲 id；缺省时由 [resolveIdentity] 现查。
     * @return YRC 文本；所有档位都拿不到逐字时间轴时返回 `null`（调用方应回落到 LRC）。
     */
    suspend fun fetch(
        songMid: String,
        numericSongId: Long? = null,
        title: String = "",
        artist: String = "",
        album: String = "",
        durationMs: Long = 0L
    ): String? = withContext(Dispatchers.IO) {
        if (songMid.isBlank()) return@withContext null
        val session = sessionProvider.current()
        val identity = resolveIdentity(songMid, numericSongId, session)

        var best: String? = null
        for (profile in PROFILES) {
            // Mei 是唯一需要数字 id 的档位；没有 id 就直接跳过，别白打一次请求。
            if (profile == LyricProfile.MEI && identity.songId == null) continue
            val document = runCatching {
                requestPlayLyric(
                    profile = profile,
                    identity = identity,
                    session = session,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs
                )
            }.onFailure { Timber.d(it, "QQMusicRichLyrics: ${profile.name} 档取词失败 mid=$songMid") }
                .getOrNull()
            if (document == null) continue
            // 带译文的逐字文档是终点；只有逐字没译文先留着，后面的档位可能补上译文。
            if (document.hasTranslation) return@withContext document.text
            if (best == null) best = document.text
        }

        // 老的 PC 下载接口偶尔还能给出前面几档都拿不到的 QRC，作为最后一条路。
        val downloaded = runCatching {
            requestDownloadedLyrics(identity, session)
        }.onFailure { Timber.d(it, "QQMusicRichLyrics: 下载接口取词失败 mid=$songMid") }
            .getOrNull()
        if (downloaded != null && (best == null || downloaded.hasTranslation)) {
            return@withContext downloaded.text
        }

        best
    }

    /** 一次取词的产物：[text] 是 YRC，[hasTranslation] 决定它能不能顶掉已有结果。 */
    private data class LyricDocument(val text: String, val hasTranslation: Boolean)

    /**
     * `songmid` → 数字 id + `songType`。
     *
     * `songType` 不是常量：伴奏 / 翻唱 / 电视原声在接口里各有类型，填错会取到别人的歌词。
     * 查不到就退回 `1`（普通歌曲），这是绝大多数曲目的值。
     */
    private fun resolveIdentity(
        songMid: String,
        knownSongId: Long?,
        session: QQMusicSession?
    ): SongIdentity {
        if (knownSongId != null && knownSongId > 0L) {
            return SongIdentity(songMid = songMid, songId = knownSongId, songType = DEFAULT_SONG_TYPE)
        }
        val url = "$QQ_BASE/v8/fcg-bin/fcg_play_single_song.fcg".toHttpUrl().newBuilder()
            .addQueryParameter("songmid", songMid)
            .addQueryParameter("tpl", "yqq_song_detail")
            .addQueryParameter("format", "json")
            .addQueryParameter("g_tk", "5381")
            .addQueryParameter("loginUin", session?.uin.orEmpty().ifBlank { "0" })
            .addQueryParameter("hostUin", "0")
            .addQueryParameter("outCharset", "utf8")
            .addQueryParameter("notice", "0")
            .addQueryParameter("platform", "yqq")
            .addQueryParameter("needNewCode", "0")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Referer", "$QQ_WEB/")
            .apply { session?.cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .build()

        val song = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) return@use null
                // 响应是 `{ code, data: [ { songid, type, … } ] }`，歌曲卡片在 data 数组里。
                json.parseToJsonElement(text).jsonObject.array("data")?.firstOrNull()
            }
        }.onFailure { Timber.d(it, "QQMusicRichLyrics: 解析歌曲身份失败 mid=$songMid") }
            .getOrNull()

        return SongIdentity(
            songMid = songMid,
            songId = song?.long("songid")?.takeIf { it > 0L }
                ?: song?.long("id")?.takeIf { it > 0L },
            songType = song?.int("type")?.takeIf { it >= 0 }
                ?: song?.int("songtype")?.takeIf { it >= 0 }
                ?: DEFAULT_SONG_TYPE
        )
    }

    private fun requestPlayLyric(
        profile: LyricProfile,
        identity: SongIdentity,
        session: QQMusicSession?,
        title: String,
        artist: String,
        album: String,
        durationMs: Long
    ): LyricDocument {
        val lyricParam = buildJsonObject {
            put("crypt", 1)
            put("lrc_t", 0)
            put("qrc", 1)
            put("qrc_t", 0)
            put("trans", 1)
            put("trans_t", 0)
            put("roma", 1)
            put("roma_t", 0)
            put("needSingingAnnotations", false)
            put("type", if (profile == LyricProfile.MEI) 0 else identity.songType)
            if (profile == LyricProfile.MEI) {
                // Mei 协议按「歌名 + 歌手 + 专辑 + 时长」定位，参数全部 base64 过。
                put("albumName", base64(album))
                put("singerName", base64(artist))
                put("songName", base64(title))
                put("interval", (durationMs / 1_000L).coerceAtLeast(0L))
                put("ct", 19)
                put("cv", 2111)
                put("songID", identity.songId ?: 0L)
            } else {
                identity.songId?.let { put("songId", it) } ?: put("songMid", identity.songMid)
            }
        }

        val requestKey = if (profile == LyricProfile.MEI) MEI_REQUEST_KEY else REQ_KEY
        val payload = buildJsonObject {
            put("comm", commParams(profile, session))
            put(
                requestKey,
                buildJsonObject {
                    put("module", LYRIC_MODULE)
                    put("method", "GetPlayLyricInfo")
                    put("param", lyricParam)
                }
            )
        }

        val request = Request.Builder()
            .url(MUSICU_URL)
            .header("User-Agent", userAgent(profile))
            .header("Accept", "application/json, text/plain, */*")
            .apply {
                if (profile != LyricProfile.ANDROID) header("Referer", "$QQ_WEB/")
                session?.cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) }
            }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("QQ 音乐逐字歌词返回 HTTP ${response.code}")
            if (body.isBlank()) throw IOException("QQ 音乐逐字歌词返回空响应")
            val root = json.parseToJsonElement(body).jsonObject
            val module = root.obj(requestKey) ?: root.obj(REQ_KEY)
                ?: throw IOException("QQ 音乐逐字歌词响应缺少 $requestKey")
            val code = module.int("code") ?: 0
            if (code != 0) {
                throw IOException(
                    module.string("message").ifBlank { module.string("msg") }
                        .ifBlank { "QQ 音乐逐字歌词错误码 $code" }
                )
            }
            val data = module.obj("data") ?: JsonObject(emptyMap())
            val lyric = data.string("lyric")
            if (lyric.isBlank()) throw IOException("QQ 音乐没有返回 QRC")
            buildDocument(lyric, data.string("trans"))
        }
    }

    /**
     * 老的 PC 下载接口：回一段 XML，正文/译文/罗马音各在一个 CDATA 里。
     *
     * 它不保证存在，也不保证是 QRC（有些曲目只给 LRC），所以调用方只把它当兜底。
     */
    private fun requestDownloadedLyrics(
        identity: SongIdentity,
        session: QQMusicSession?
    ): LyricDocument {
        val songId = identity.songId ?: throw IOException("QQ 音乐无法解析歌曲数字 id")
        val form = FormBody.Builder()
            .add("version", "15")
            .add("miniversion", "82")
            .add("lrctype", "4")
            .add("musicid", songId.toString())
            .build()
        val request = Request.Builder()
            .url(LYRIC_DOWNLOAD_URL)
            .header("Referer", "$QQ_BASE/")
            .apply { session?.cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .post(form)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("QQ 音乐歌词下载返回 HTTP ${response.code}")
            // 注释块里包着正文，不去掉 XML 解析会把 `<!--` 当成文本。
            val xml = response.body?.string().orEmpty()
                .replace("<!--", "")
                .replace("-->", "")
            val primary = extractXmlText(xml, "content")
            if (primary.isBlank()) throw IOException("QQ 音乐歌词下载没有返回正文")
            buildDocument(primary, extractXmlText(xml, "contentts"))
        }
    }

    /**
     * 密文 → 可交给歌词管线的 YRC。
     *
     * @throws IOException 解不出逐字时间轴时抛，让调用方换下一档。
     */
    private fun buildDocument(primary: String, translation: String): LyricDocument {
        val yrc = decodeQrcToYrc(primary)
            ?: throw IOException("QQ 音乐 QRC 解码后没有逐字时间轴")
        val translations = qqSecondaryLines(translation)
        if (translations.isEmpty()) return LyricDocument(yrc, hasTranslation = false)
        return LyricDocument(qqMergeTranslation(yrc, translations), hasTranslation = true)
    }

    /**
     * QRC 密文/明文 → YRC。
     *
     * 译文偶尔是明文 LRC（2026 年起部分节点如此），主文档仍可能是密文，所以两种形态都收。
     */
    private fun decodeQrcToYrc(source: String): String? {
        val text = source.trim().trimEnd('\u0000')
        if (text.isEmpty()) return null
        val yrc = runCatching { QrcCodec.toYrc(text) }
            .onFailure { Timber.d(it, "QQMusicRichLyrics: QRC 解码失败") }
            .getOrNull()
        return yrc?.takeIf { QQ_YRC_LINE.containsMatchIn(it) }
    }

    private fun commParams(profile: LyricProfile, session: QQMusicSession?): JsonObject =
        when (profile) {
            LyricProfile.MEI -> buildJsonObject {
                put("_channelid", "")
                put("_os_version", "6.2.9200-2")
                put("authst", "")
                put("ct", 11)
                put("cv", "1003006")
                put("patch", "118")
                put("psrf_access_token_expiresAt", 0)
                put("psrf_qqaccess_token", "")
                put("psrf_qqopenid", "")
                put("psrf_qqunionid", "")
                put("tmeAppID", "qqmusiclight")
                put("tmeLoginType", 0)
                put("uin", "")
                put("wid", "")
            }

            LyricProfile.ANDROID -> buildJsonObject {
                put("ct", ANDROID_CLIENT_TYPE)
                put("cv", ANDROID_CLIENT_VERSION)
                put("v", ANDROID_CLIENT_VERSION)
                put("chid", "10003505")
                put("qq", session?.uin.orEmpty())
                put("authst", session?.musicKey.orEmpty())
                put("tmeAppID", "qqmusic")
                put("tmeLoginType", qqLoginType(session?.musicKey.orEmpty()))
                put("format", "json")
            }

            LyricProfile.WEB -> {
                val gtk = qqHash33(session?.musicKey.orEmpty(), 5381)
                buildJsonObject {
                    put("ct", 24)
                    put("cv", WEB_CLIENT_VERSION)
                    put("platform", "yqq.json")
                    put("chid", "0")
                    put("uin", session?.uin?.toLongOrNull() ?: 0L)
                    put("g_tk", gtk)
                    put("g_tk_new_20200303", gtk)
                    put("format", "json")
                    put("inCharset", "utf-8")
                    put("outCharset", "utf-8")
                    put("notice", 0)
                    put("need_new_code", 1)
                }
            }

            LyricProfile.DESKTOP -> buildJsonObject {
                put("ct", 19)
                put("cv", 2201)
                put("chid", "0")
                put("uin", session?.uin?.toLongOrNull() ?: 0L)
                put("g_tk", qqHash33(session?.musicKey.orEmpty(), 5381))
                put("format", "json")
            }
        }

    private fun userAgent(profile: LyricProfile): String = when (profile) {
        LyricProfile.MEI -> MEI_USER_AGENT
        LyricProfile.ANDROID -> "QQMusic $ANDROID_CLIENT_VERSION(android 15)"
        LyricProfile.WEB, LyricProfile.DESKTOP -> QQ_WEB_USER_AGENT
    }

    /** 微信登录的 musicKey 以 `W_X` 开头，服务端要求 `tmeLoginType` 与之对应。 */
    private fun qqLoginType(musicKey: String): Int =
        if (musicKey.startsWith("W_X")) 1 else 2

    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun extractXmlText(xml: String, tag: String): String {
        val pattern = Regex(
            "<$tag(?:\\s[^>]*)?>(.*?)</$tag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return pattern.find(xml)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .trim()
            .removePrefix("<![CDATA[")
            .removeSuffix("]]>")
            .trim()
    }

    private data class SongIdentity(
        val songMid: String,
        val songId: Long?,
        val songType: Int
    )

    /**
     * 取词的四个客户端档位。
     *
     * 顺序不是随手排的：Mei 给的 QRC 最全（带逐字 + 译文 + 罗马音），但它要数字 id；
     * Android 档要登录态；Web / Desktop 兜最后 —— 匿名也能拿到大部分逐字歌词。
     */
    private enum class LyricProfile {
        MEI,
        ANDROID,
        WEB,
        DESKTOP
    }

    private companion object {
        /** 取词档位顺序，见 [LyricProfile]。 */
        val PROFILES = listOf(
            LyricProfile.MEI,
            LyricProfile.ANDROID,
            LyricProfile.WEB,
            LyricProfile.DESKTOP
        )

        const val MUSICU_URL = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        const val QQ_BASE = "https://c.y.qq.com"
        const val QQ_WEB = "https://y.qq.com"
        const val LYRIC_DOWNLOAD_URL = "$QQ_BASE/qqmusic/fcgi-bin/lyric_download.fcg"

        const val LYRIC_MODULE = "music.musichallSong.PlayLyricInfo"
        const val REQ_KEY = "req_0"
        const val MEI_REQUEST_KEY = "music.musichallSong.PlayLyricInfo.GetPlayLyricInfo"

        const val DEFAULT_SONG_TYPE = 1
        const val ANDROID_CLIENT_TYPE = 11
        const val ANDROID_CLIENT_VERSION = 14_090_008
        const val WEB_CLIENT_VERSION = 4_747_474

        const val CALL_TIMEOUT_SECONDS = 15L
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        const val MEI_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36"
    }
}

/** YRC 行：`[start,dur]`。 */
private val QQ_YRC_LINE = Regex("^\\[(\\d+),(\\d+)]")

/** QRC/YRC 行：`[start,dur](s,d,0)字…`。 */
private val QQ_QRC_LINE = Regex("^\\[(\\d+),(\\d+)](.*)$")

/** QRC 逐字标签：`(start,dur)` 或 `(start,dur,0)`。 */
private val QQ_QRC_WORD_TAG = Regex("\\(\\d+,\\d+(?:,\\d+)?\\)")

/** LRC 行：`[mm:ss.xx]` / `[mm:ss.xxx]`。 */
private val QQ_LRC_LINE = Regex("^\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

/** QRC XML 的正文容器。 */
private val QQ_LYRIC_CONTENT = Regex("LyricContent\\s*=\\s*\"[\\s\\S]*?\"")

/**
 * 译文/罗马音 → 「行首毫秒 → 文本」。
 *
 * 三种形态都要认：QRC 密文、QRC XML、行级 LRC。解不出来就返回空表 —— 没有译文不算失败，
 * 主文档的逐字时间轴照样可用。
 *
 * 纯函数，不碰网络；见 `QQMusicRichLyricsTest`。
 */
internal fun qqSecondaryLines(source: String): Map<Long, String> {
    val raw = source.trim().trimEnd('\u0000')
    if (raw.isEmpty()) return emptyMap()
    val text = when {
        // 已经是明文 XML / LRC，别再当密文解一次。
        raw.startsWith('<') || raw.startsWith('[') -> raw
        else -> runCatching { QrcCodec.decrypt(raw) }.getOrDefault("")
    }
    if (text.isBlank()) return emptyMap()

    // QRC XML 里的 `LyricContent` 要抽出来转成行文本；普通 LRC 原样解析。
    val body = if (text.contains(QQ_LYRIC_CONTENT)) {
        runCatching { QrcCodec.toLrc(text) }.getOrDefault(text)
    } else {
        text
    }

    val lines = LinkedHashMap<Long, String>()
    body.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach
        val qrc = QQ_QRC_LINE.find(line)
        if (qrc != null) {
            val start = qrc.groupValues[1].toLongOrNull() ?: return@forEach
            // 译文行也可能带逐字标签（QRC 的译文是完整 QRC），文本要把标签剥掉。
            lines.putIfAbsent(start, QQ_QRC_WORD_TAG.replace(qrc.groupValues[3], "").trim())
            return@forEach
        }
        val lrc = QQ_LRC_LINE.find(line) ?: return@forEach
        val start = qqLrcStartMillis(lrc) ?: return@forEach
        lines.putIfAbsent(start, line.substring(lrc.range.last + 1).trim())
    }
    return lines.filterValues { it.isNotBlank() }
}

/**
 * 把译文插到对应原文行**下面**。
 *
 * 译文行的写法是 `[start,0]文本`：行标签必须与原文同 start，且不带任何逐字标签 ——
 * 这正是 `YrcParser` 认译文的两个条件（`combineRawWithTranslation`）。插到别处，
 * 每句译文都会变成一条独立歌词。
 *
 * 纯函数，不碰网络；见 `QQMusicRichLyricsTest`。
 */
internal fun qqMergeTranslation(yrc: String, translations: Map<Long, String>): String {
    if (translations.isEmpty()) return yrc
    val out = StringBuilder()
    yrc.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach
        out.append(line).append('\n')
        val start = QQ_YRC_LINE.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
        val translated = translations[start] ?: return@forEach
        out.append('[').append(start).append(",0]").append(translated).append('\n')
    }
    return out.toString().trimEnd()
}

/** `[mm:ss.xx]` 的行首毫秒；小数位长度不定，按位数补齐到毫秒。 */
private fun qqLrcStartMillis(match: MatchResult): Long? {
    val minutes = match.groupValues[1].toLongOrNull() ?: return null
    val seconds = match.groupValues[2].toLongOrNull() ?: return null
    val millis = match.groupValues[3]
        .takeIf { it.isNotEmpty() }
        ?.padEnd(3, '0')
        ?.take(3)
        ?.toLongOrNull()
        ?: 0L
    return minutes * 60_000 + seconds * 1_000 + millis
}

private fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it != "null" }.orEmpty()

private fun JsonObject.int(key: String): Int? = string(key).toIntOrNull()

private fun JsonObject.long(key: String): Long? = string(key).toLongOrNull()

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): List<JsonObject>? =
    (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }
