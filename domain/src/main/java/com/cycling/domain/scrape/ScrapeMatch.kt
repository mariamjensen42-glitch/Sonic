package com.cycling.domain.scrape

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeField
import com.cycling.domain.model.Song
import kotlin.math.abs

/**
 * 本地文件与在线搜索结果的匹配判定。
 *
 * 纯函数，便于单测：批量刮削要「自动应用」就必须先能判断「这条结果可信」，否则一次
 * 全自动刮削会把整库的标题改错，而那种错误比没有元数据更难发现。
 *
 * 打分只看三样：标题、艺术家、时长。年份 / 流派 / 曲号由音源自己给，不参与判定 ——
 * 它们本来就是我们要补的东西，拿它们反推匹配度是循环论证。
 */
object ScrapeMatch {

    /** 自动应用的门槛：标题对得上，或者标题近似 + 艺术家对得上。 */
    const val AUTO_APPLY_THRESHOLD = 0.6f

    /**
     * 归一化：去掉大小写、空白、标点和括号里的附加说明。
     *
     * 本地文件名常常是 `周杰伦 - 稻香 (Live).mp3`，在线条目叫 `稻香`；
     * `(Live)`、`【官方】`、`feat. xxx` 这类后缀不该影响匹配。
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        var value = text.lowercase()
        // 括号里的装饰性后缀整段丢掉（半角 / 全角 / 方括号都算）
        value = value.replace(SUFFIX_BRACKETS, "")
        // feat / ft. 之后的内容通常是合作歌手，标题本体在它前面
        value = value.replace(FEAT_SUFFIX, "")
        return value.replace(NON_WORD, "")
    }

    /** 0..1 的置信度。 */
    fun confidence(song: Song, candidate: CoverCandidate): Float {
        val localTitle = normalize(song.title)
        val remoteTitle = normalize(candidate.title.orEmpty())
        if (localTitle.isEmpty() || remoteTitle.isEmpty()) return 0f

        var score = 0f
        if (localTitle == remoteTitle) {
            score += 0.7f
        } else if (localTitle.contains(remoteTitle) || remoteTitle.contains(localTitle)) {
            // 一方带副标题的情况很常见（`稻香` vs `稻香 (Live)` 归一化后仍可能不等）
            score += 0.45f
        } else if (similarity(localTitle, remoteTitle) >= 0.8f) {
            score += 0.3f
        }

        val localArtist = normalize(song.artist)
        val remoteArtist = normalize(candidate.artist.orEmpty())
        if (localArtist.isNotEmpty() && remoteArtist.isNotEmpty()) {
            if (localArtist == remoteArtist || remoteArtist.contains(localArtist) ||
                localArtist.contains(remoteArtist)
            ) {
                score += 0.25f
            }
        }

        val localDuration = song.duration
        val remoteDuration = candidate.durationMs
        if (localDuration > 0 && remoteDuration != null && remoteDuration > 0) {
            // 同名不同版本靠时长兜底：差 3 秒内几乎一定是同一条录音。
            if (abs(localDuration - remoteDuration) <= 3_000) {
                score += 0.15f
            } else if (abs(localDuration - remoteDuration) >= 30_000) {
                // 同名但时长相差半分钟以上，多半是另一个版本（现场 / 加长 / 翻唱）：
                // 这时候连「标题 + 艺术家都对」也要压到自动应用线以下。
                score -= 0.45f
            }
        }

        return score.coerceIn(0f, 1f)
    }

    /** 足够可信到可以自动应用（用户不用逐条确认）。 */
    fun isConfident(song: Song, candidate: CoverCandidate): Boolean =
        confidence(song, candidate) >= AUTO_APPLY_THRESHOLD

    /**
     * 按置信度从高到低排；同分时保持音源给的顺序（那本身就是相关度排序）。
     *
     * `isBestMatch` 只是音源自己的猜测，在这里只作为同分时的加成，不直接置顶：
     * 音源经常把「同名翻唱」放在第一条。
     */
    fun rank(song: Song, candidates: List<CoverCandidate>): List<CoverCandidate> =
        candidates
            .mapIndexed { index, candidate ->
                val boost = if (candidate.isBestMatch) 0.05f else 0f
                Triple(candidate, confidence(song, candidate) + boost, index)
            }
            .sortedWith(compareByDescending<Triple<CoverCandidate, Float, Int>> { it.second }
                .thenBy { it.third })
            .map { it.first }

    /** 最长公共子序列比例，够用即可：标题最多几十个字符。 */
    private fun similarity(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val previous = IntArray(b.length + 1)
        val current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                current[j] = if (a[i - 1] == b[j - 1]) {
                    previous[j - 1] + 1
                } else {
                    maxOf(previous[j], current[j - 1])
                }
            }
            previous.indices.forEach { previous[it] = current[it] }
        }
        return previous[b.length].toFloat() / maxOf(a.length, b.length)
    }

    private val SUFFIX_BRACKETS = Regex("""[\(\[\{【（〔][^)\]】）〕]*[)\]】）〕]""")
    private val FEAT_SUFFIX = Regex("""\b(feat|ft|featuring)\b.*$""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]""")

    /** 这些值等同于「没有元数据」：MediaStore 与常见中文播放器各有一套写法。 */
    private val UNKNOWN_VALUES = setOf(
        "", "<unknown>", "unknown", "unknown artist", "unknown album",
        "未知", "未知艺术家", "未知歌手", "未知专辑", "未知作者", "佚名", "无"
    )

    /** 空 / 占位值 / 与文件名同名的标题，都算「信息不全」。 */
    fun isMissingTitle(song: Song): Boolean = isPlaceholder(song.title) || titleLooksLikeFileName(song)

    fun isMissingArtist(song: Song): Boolean = isPlaceholder(song.artist)

    fun isMissingAlbum(song: Song): Boolean = isPlaceholder(song.album)

    fun missingFields(song: Song): Set<ScrapeField> = buildSet {
        if (isMissingTitle(song)) add(ScrapeField.TITLE)
        if (isMissingArtist(song)) add(ScrapeField.ARTIST)
        if (isMissingAlbum(song)) add(ScrapeField.ALBUM)
    }

    private fun isPlaceholder(value: String): Boolean =
        UNKNOWN_VALUES.contains(value.trim().lowercase())

    /**
     * 标题等于文件名（去掉扩展名与开头的曲号）—— 扫描器在文件没有标题标签时就是这么填的，
     * 界面上看着"有标题"，其实是从文件名抄来的。
     */
    private fun titleLooksLikeFileName(song: Song): Boolean {
        val name = song.path.substringAfterLast('/').substringBeforeLast('.')
        if (name.isBlank()) return false
        val stripped = name.replace(LEADING_TRACK_NO, "").trim()
        val title = song.title.trim()
        return title.equals(stripped, ignoreCase = true) && title.contains(Regex("""[\s_\-]"""))
    }

    /** 开头的 `01 ` / `01.` / `01-` 曲号。 */
    private val LEADING_TRACK_NO = Regex("""^\d{1,3}[\s._\-]+""")
}
