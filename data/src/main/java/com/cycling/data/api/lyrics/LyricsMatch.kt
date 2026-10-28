package com.cycling.data.api.lyrics

import com.cycling.data.api.lyrics.dto.AmllSongItemDto
import com.cycling.data.api.lyrics.dto.LrclibTrackDto
import com.cycling.domain.lyrics.provider.OnlineLyrics
import com.cycling.domain.lyrics.utils.TrackText
import com.cycling.domain.model.LyricsQuery
import com.cycling.domain.online.model.OnlineSource
import kotlin.math.abs

/**
 * 在线歌词的匹配规则：**先确认是不是同一首歌，再看时长对不对得上**。
 *
 * 以前这里只按时长筛（±5 秒）。那是错的：平台的检索是模糊的，`"歌手 曲名"` 能返回翻唱、伴奏、
 * 同名歌、DJ 版，其中任何一条只要时长落在 5 秒内就会被当成答案 —— 于是"匹配到的歌词是别人的"。
 * 反过来，现场版、不同母带、被裁剪过的文件与在线时长相差十几秒时，真正的原曲又被这条 5 秒
 * 的硬线全部丢掉 —— 于是"找不到歌词"。
 *
 * 现在的顺序是：
 * 1. 曲名必须对得上（[matchTitle]），对不上直接丢 —— 这是防串歌的硬门槛；
 * 2. 歌手必须对得上（[artistMatches]），本地或候选缺歌手时算"无信号"，不否决；
 * 3. 时长按**曲名匹配的强度分级**放宽容差（[toleranceMs]）：曲名完全一致可以差 15 秒
 *    （重制、编码、片头片尾差异）；曲名只是去掉 `(Live)` 之后才相同、而两边的版本标记不同
 *    （一边是录音室版、一边是现场版）时只给 6 秒；
 * 4. 排序先按曲名强度，再按时长距离。
 */

/** 曲名一致时允许的时长差：重制、重新编码、片头片尾多几秒都落在这里。 */
private const val EXACT_TOLERANCE_MS = 15_000L

/** 去掉 `(Live)` 之类的写法差异后一致，且两边版本标记相同（例如都是重制版）。 */
private const val SAME_VERSION_TOLERANCE_MS = 12_000L

/** 曲名底子相同，但一边标了现场 / 伴奏 / 翻唱 —— 那是另一次演奏，时长对不上就不认。 */
private const val DIFFERENT_VERSION_TOLERANCE_MS = 6_000L

/** 歌手名过短时不做前缀判断，避免 "Jo" 命中 "Jolin"。 */
private const val MIN_PARTIAL_ARTIST_LENGTH = 3

/** 曲名对到什么程度。顺序即强度，[NONE] 表示不是同一首歌。 */
internal enum class TitleMatch {
    /** 归一化后完全一致。 */
    EXACT,

    /** 去掉括号 / 破折号 / feat. 之后一致 —— 同一首歌的两种上架写法。 */
    SAME_BASE,

    /** 对不上。 */
    NONE
}

/**
 * 两条曲名是不是同一首歌，以及有多确定。
 *
 * 比较用的是**原始**曲名归一化后的形式（见 [TrackText.comparable]），而不是搜索时用的去噪版本：
 * 去噪只负责让检索命中，判定同一性要保留版本信息。
 *
 * 刻意**不做**「一方包含另一方」的判断。关键词检索对泛用曲名（`Song`、`Hello`、`晴天`）会返回
 * 一堆 `Song of the Wind`、`Hello Again`，包含关系会把它们全放进来；而真正需要模糊匹配的写法差异
 * （`Song (Live)`、`Song - Remastered 2011`）已经被 [TrackText.baseTitle] 处理掉了。
 */
internal fun matchTitle(queryTitle: String, candidateTitle: String): TitleMatch {
    if (queryTitle.isBlank() || candidateTitle.isBlank()) return TitleMatch.NONE

    val wanted = TrackText.comparable(queryTitle)
    val offered = TrackText.comparable(candidateTitle)
    if (wanted.isNotEmpty() && wanted == offered) return TitleMatch.EXACT

    val wantedBase = TrackText.comparable(TrackText.baseTitle(queryTitle))
    val offeredBase = TrackText.comparable(TrackText.baseTitle(candidateTitle))
    if (wantedBase.isNotEmpty() && wantedBase == offeredBase) return TitleMatch.SAME_BASE

    return TitleMatch.NONE
}

/**
 * 歌手是否对得上。
 *
 * 任一方没有歌手信息时返回 `true`（不否决）：本地文件常常没有艺术家标签，平台侧也偶尔只回
 * 曲名。这种时候靠曲名与时长把关，比直接放弃更接近用户想要的。
 */
internal fun artistMatches(queryArtist: String, candidateArtist: String): Boolean {
    val wanted = artistKeys(queryArtist)
    if (wanted.isEmpty()) return true
    val offered = artistKeys(candidateArtist)
    if (offered.isEmpty()) return true
    return wanted.any { want -> offered.any { have -> sameName(want, have) } }
}

private fun artistKeys(raw: String): List<String> =
    TrackText.artistNames(raw).map { TrackText.comparable(it) }.filter { it.isNotEmpty() }

/**
 * 两个歌手名是不是同一个人。
 *
 * 只认三种情况：完全相等；去掉括号等噪音后相等（`Artist (Band)` 与 `Artist`）；后者是前者的
 * 前缀（`周杰伦` 与 `周杰伦 (Jay Chou)`）。**不做任意位置的子串包含** —— 那会把
 * `Different Artist` 判成 `Artist`，正是「匹配到别人的歌」的一类来源。
 */
private fun sameName(a: String, b: String): Boolean {
    if (a == b) return true
    val baseA = TrackText.comparable(TrackText.baseTitle(a))
    val baseB = TrackText.comparable(TrackText.baseTitle(b))
    if (baseA.isEmpty() || baseB.isEmpty()) return false
    if (baseA == baseB) return true

    val shorter = if (baseA.length <= baseB.length) baseA else baseB
    val longer = if (baseA.length <= baseB.length) baseB else baseA
    return shorter.length >= MIN_PARTIAL_ARTIST_LENGTH && longer.startsWith(shorter)
}

/**
 * 通过匹配的候选，最强的在前。
 *
 * 空列表表示**没有一条可信**——宁可让下一个来源接手，也不把别人的歌词显示出来。
 */
internal fun <T> rankCandidates(
    candidates: List<T>,
    query: LyricsQuery,
    titleOf: (T) -> String,
    artistOf: (T) -> String,
    durationMsOf: (T) -> Long?
): List<T> {
    if (query.title.isBlank()) return emptyList()

    val scored = candidates.mapNotNull { candidate ->
        val title = titleOf(candidate)
        val level = matchTitle(query.title, title)
        if (level == TitleMatch.NONE) return@mapNotNull null
        if (!artistMatches(query.artist, artistOf(candidate))) return@mapNotNull null

        // 本地没有时长就没有可比的基准，此时只按曲名强度排，不因为"距离未知"被丢。
        val distance = if (query.durationMs > 0) {
            durationMsOf(candidate)?.let { abs(it - query.durationMs) }
        } else {
            null
        }
        if (distance != null && distance > toleranceMs(level, query.title, title)) {
            return@mapNotNull null
        }

        Scored(candidate, level, distance)
    }

    return scored
        .sortedWith(
            compareBy<Scored<T>> { it.level.ordinal }
                .thenBy { it.distanceMs ?: Long.MAX_VALUE }
        )
        .map { it.value }
}

/** The candidate worth using, or `null` when none of them is really this song. */
internal fun <T> bestMatch(
    candidates: List<T>,
    query: LyricsQuery,
    titleOf: (T) -> String,
    artistOf: (T) -> String,
    durationMsOf: (T) -> Long?
): T? = rankCandidates(candidates, query, titleOf, artistOf, durationMsOf).firstOrNull()

/**
 * 检索用的关键词：`主歌手 去噪曲名`，缺歌手时只用曲名。
 *
 * 用去噪后的曲名而不是标签原文：本地标签里的 `(Live)`、`- Remastered 2011`、`feat. X`
 * 在平台的索引里通常不存在，带上它们只会把本来能命中的结果筛掉。
 */
internal fun LyricsQuery.searchKeywords(): String =
    listOf(searchArtist, searchTitle).filter { it.isNotBlank() }.joinToString(" ")

/**
 * 时长容差随「曲名对得有多准」放宽。
 *
 * 曲名完全一致时，差十几秒仍然是同一首歌（重制、编码、多一段前奏）；曲名只是去掉括号之后
 * 才相同时，两边是不是同一个版本就要看标记 —— 现场版与录音室版是两次演奏，容差收得最紧。
 */
private fun toleranceMs(level: TitleMatch, queryTitle: String, candidateTitle: String): Long =
    when (level) {
        TitleMatch.EXACT -> EXACT_TOLERANCE_MS
        TitleMatch.SAME_BASE -> if (TrackText.versionTags(queryTitle) == TrackText.versionTags(candidateTitle)) {
            SAME_VERSION_TOLERANCE_MS
        } else {
            DIFFERENT_VERSION_TOLERANCE_MS
        }
        TitleMatch.NONE -> 0L
    }

private data class Scored<T>(val value: T, val level: TitleMatch, val distanceMs: Long?)

/**
 * Picks the one candidate worth downloading, or `null` if the provider has nothing usable.
 *
 * Candidates without *synced* lyrics are dropped: `plainLyrics` has no timestamps, and the player
 * can only render a `SyncedLyrics`. An instrumental is skipped for the same reason a blank one is —
 * there is nothing to display.
 */
internal fun selectLrclibLyrics(candidates: List<LrclibTrackDto>, query: LyricsQuery): OnlineLyrics? {
    val usable = candidates.filter { !it.instrumental && !it.syncedLyrics.isNullOrBlank() }
    val match = bestMatch(
        usable,
        query,
        LrclibTrackDto::trackName,
        LrclibTrackDto::artistName,
        LrclibTrackDto::durationMs
    ) ?: return null
    return OnlineLyrics(rawContent = match.syncedLyrics!!)
}

/**
 * 搜索用的曲名变体，最可能命中的在前。
 *
 * 词库的 `/v1/lyrics/search` 是**子串**匹配（实测 `emon` 能搜到 `Lemon`），索引的是
 * `musicNames` 里作者登记的全部写法 —— 既有规范曲名，也有 `匿名M (feat. 初音ミク & ARuFa)`、
 * `日剧《非自然死亡》主题曲` 这种别名。所以先去噪曲名（命中率最高），本地标签恰好就是词库里那个
 * 写法时再去原文。两条变体只在第一条没结果时才会依次请求，命中时仍然只有一次搜索。
 */
internal fun amllSearchTerms(query: LyricsQuery): List<String> =
    listOf(query.searchTitle, query.title)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

/**
 * Orders AMLL's search results so the submission belonging to the local file comes first.
 *
 * The artist has already been checked (see [selectAmllCandidates]); what is left is *which
 * submission* of that recording. Three signals decide, strongest first:
 *
 * 1. 平台 id（[platformRank]）—— 词库登记的网易云 / QQ id 与本曲相同，就是同一条母带；
 * 2. 曲名强度 —— `Song` 排在 `Song (Live)` 前面，后者只是去噪之后才同名；
 * 3. 专辑 —— 本地标签的专辑与文档登记的一致，说明是同一个发行版本。
 *
 * This ranks rather than filters: a single whose album tag differs from the album it also appears
 * on is still the same recording, and rejecting it would lose the lyrics entirely. The sort is
 * stable, so whatever the signals cannot separate keeps the order the server ranked it in — which
 * is also why this provider does no duration comparison before downloading: AMLL's `dur` is the
 * last lyric timestamp, not the track's length. It is only used afterwards, and only one way — see
 * `amllExceedsTrack`.
 */
internal fun rankAmllCandidates(
    candidates: List<AmllSongItemDto>,
    query: LyricsQuery
): List<AmllSongItemDto> = candidates.sortedWith(
    compareByDescending<AmllSongItemDto> { it.platformRank(query) }
        .thenBy { it.titleLevel(query.title).ordinal }
        .thenByDescending { it.albumRank(query.album) }
)

/**
 * 1 = 文档登记的平台 id 与本曲相同，0 = 没有可用的 id 信号。
 *
 * 只有**在线曲目**带平台 id（本地文件没有），所以这条信号只对它们生效；也正因为如此，
 * 它一旦命中就是决定性的：同一个网易云 / QQ id 不可能指向两首不同的歌。
 */
private fun AmllSongItemDto.platformRank(query: LyricsQuery): Int {
    val id = query.onlineSongId?.trim().orEmpty()
    if (id.isEmpty()) return 0
    val ids = when (query.onlineSourceKey) {
        OnlineSource.NETEASE.lxKey -> ncmMusicIds
        OnlineSource.QQ.lxKey -> qqMusicIds
        else -> return 0
    }
    return if (ids.any { it.trim() == id }) 1 else 0
}

/** 文档在曲名上与本地曲名对得多准 —— 取它登记的写法里最强的那条。 */
private fun AmllSongItemDto.titleLevel(title: String): TitleMatch =
    musicNames.map { matchTitle(title, it) }.minByOrNull { it.ordinal } ?: TitleMatch.NONE

/** 2 = same album, 1 = one title contains the other, 0 = no usable signal. */
private fun AmllSongItemDto.albumRank(album: String): Int {
    if (album.isBlank()) return 0
    val names = albumNames.filter { it.isNotBlank() }
    return when {
        names.any { it.equals(album, ignoreCase = true) } -> 2
        names.any { it.contains(album, ignoreCase = true) || album.contains(it, ignoreCase = true) } -> 1
        else -> 0
    }
}

/**
 * The AMLL submissions worth keeping for [query]: same title, same artist, release-album first.
 *
 * The provider searches by **title only** and filters here, because the API's own `artistName`
 * parameter proved unreliable: a search for `musicName=Lemon&artistName=米津玄師` answers empty
 * while `artistName=米津` finds the document whose `artistNames` is exactly that name — so the
 * server-side AND would silently drop lyrics it has. Title-only search is safe here because both
 * fields are verified before anything is downloaded.
 *
 * Filtering is not optional: a common title ("Hello", "晴天") is shared by many recordings, and
 * serving another artist's lyrics is worse than serving none. The title is re-checked too, because
 * the search endpoint answers substring matches — a query for `晴天` also returns `晴天 (Live)`.
 *
 * 本地**没有歌手标签**时不再一刀切放弃（那会让一整类文件的 AMLL 歌词永远取不到），改为只采信有
 * 旁证的结果：平台 id 相同、专辑同名、或整个词库里这个曲名只有一份文档。三者都没有时仍然放弃
 * —— 同名曲目随手挑一份，等于把别人的歌词显示出来。
 */
internal fun selectAmllCandidates(
    candidates: List<AmllSongItemDto>,
    query: LyricsQuery
): List<AmllSongItemDto> {
    val titled = candidates.filter { it.id > 0 && it.namesTitle(query.title) }
    if (titled.isEmpty()) return emptyList()

    if (query.artist.isNotBlank()) {
        return rankAmllCandidates(titled.filter { it.namesArtist(query.artist) }, query)
    }

    val byPlatform = titled.filter { it.platformRank(query) > 0 }
    if (byPlatform.isNotEmpty()) return rankAmllCandidates(byPlatform, query)

    val onAlbum = titled.filter { it.albumRank(query.album) == 2 }
    if (onAlbum.isNotEmpty()) return rankAmllCandidates(onAlbum, query)

    return if (titled.size == 1) rankAmllCandidates(titled, query) else emptyList()
}

private fun AmllSongItemDto.namesTitle(title: String): Boolean =
    musicNames.any { matchTitle(title, it) != TitleMatch.NONE }

/**
 * Whether any of [AmllSongItemDto.artistNames] names the same artist as [artist].
 *
 * Delegates to [artistMatches] so the AMLL catalogue and the Chinese catalogues cannot drift apart:
 * both sides are split on the separators tags use for collaborations (`feat.`, `&`, `、` …) and
 * stripped to letters/digits, so "Taylor Swift feat. Brendon Urie" matches an entry of either name.
 *
 * 与其它来源不同，这里**要求词库侧有歌手名**：没有歌手名的文档无从核对，同名曲目里混进一个陌生
 * 文档就是串歌。本地缺歌手标签的情形由 [selectAmllCandidates] 用旁证处理。
 */
private fun AmllSongItemDto.namesArtist(artist: String): Boolean {
    if (artist.isBlank()) return false
    val offered = artistNames.filter { it.isNotBlank() }
    if (offered.isEmpty()) return false
    return artistMatches(artist, offered.joinToString(ARTIST_JOIN))
}

private const val ARTIST_JOIN = ", "
