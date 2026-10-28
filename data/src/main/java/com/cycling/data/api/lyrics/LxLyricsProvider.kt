package com.cycling.data.api.lyrics

import com.cycling.data.online.lx.QrcCodec
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineLyric
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.repository.OnlinePluginRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用用户导入的 LX 音源脚本取歌词。
 *
 * 与其它 provider 的区别：脚本按**平台歌曲 id** 取词，不能按「歌名 + 歌手」搜，
 * 所以只有在线曲目（[LyricsQuery.onlineSourceKey] / [LyricsQuery.onlineSongId] 都在）
 * 才问得到东西；本地歌曲直接返回空，让后面的 provider 接手。
 *
 * 脚本返回的格式不设限，这里做两件事：
 * 1. 合并翻译（脚本给的主歌词与 `tlyric` 是两份文档，插到对应行下面）；
 * 2. 如果是 QRC（QQ 音乐加密逐字），先解成 YRC —— 歌词管线的解析器认得 YRC，不认得 QRC。
 */
@Singleton
class LxLyricsProvider @Inject constructor(
    private val pluginRepository: OnlinePluginRepository
) : OnlineLyricsProvider {

    override val provider: LyricsProvider = LyricsProvider.LX

    override suspend fun fetchCandidates(query: LyricsQuery): List<OnlineLyrics> {
        val track = query.toMusicInfo() ?: return emptyList()
        val lyric = try {
            pluginRepository.fetchLyric(track)
        } catch (e: Exception) {
            Timber.w(e, "LxLyricsProvider: 取词失败 for ${query.cacheKey}")
            null
        } ?: return emptyList()

        val document = merge(lyric)
        if (document.isBlank()) return emptyList()
        return listOf(OnlineLyrics(rawContent = document))
    }

    /** 主文档 + 翻译合并成一份，翻译行紧跟在对应主行下面（解析器的约定）。 */
    private fun merge(lyric: OnlineLyric): String {
        val main = normalize(lyric.lyric)
        val translation = lyric.translation?.takeIf { it.isNotBlank() }?.let(::normalize)
            ?: return main

        val byStart = LinkedHashMap<Long, String>()
        translation.lineSequence().forEach { raw ->
            val line = raw.trim()
            val start = startMs(line) ?: return@forEach
            byStart.putIfAbsent(start, line)
        }
        if (byStart.isEmpty()) return main

        val used = HashSet<Long>()
        val out = StringBuilder()
        main.lineSequence().forEach { raw ->
            val line = raw.trim()
            out.append(line).append('\n')
            val start = startMs(line) ?: return@forEach
            val extra = byStart[start] ?: return@forEach
            used += start
            out.append(extra).append('\n')
        }
        byStart.forEach { (start, line) ->
            if (start !in used) out.append(line).append('\n')
        }
        return out.toString().trimEnd()
    }

    /**
     * 把脚本给的东西统一成歌词管线能读的文本。
     *
     * QRC 的判据：带 `LyricContent` 的 XML，或整段没有任何 `[`（密文 hex / base64 不会有）。
     * 其余情况原样返回 —— 猜错格式的代价是「一首歌没有歌词」，比乱解一场好。
     */
    private fun normalize(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return text
        val maybeQrc = text.contains("LyricContent") || !text.contains('[')
        if (!maybeQrc) return text
        return runCatching { QrcCodec.toYrc(text) }.getOrDefault(text)
    }

    private fun startMs(line: String): Long? {
        YRC_TAG.find(line)?.let { match ->
            return match.groupValues[1].toLongOrNull()
        }
        val match = LRC_TAG.find(line) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val fraction = match.groupValues[3]
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }

    /** 只有在线曲目能问脚本：它按平台 id 取词，本地歌曲没有 id。 */
    private fun LyricsQuery.toMusicInfo(): MusicInfo? {
        val source = OnlineSource.fromLxKey(onlineSourceKey ?: return null) ?: return null
        val songId = onlineSongId?.takeIf { it.isNotBlank() } ?: return null
        return MusicInfo(
            source = source,
            id = songId,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationMs / 1000
        )
    }

    private companion object {
        /** `[1234,567]` — YRC/QRC 的行标签。 */
        val YRC_TAG = Regex("^\\[(\\d+),(\\d+)]")

        /** `[mm:ss.xx]` / `[mm:ss.xxx]`。 */
        val LRC_TAG = Regex("^\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    }
}
