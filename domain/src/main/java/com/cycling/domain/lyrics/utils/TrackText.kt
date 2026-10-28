package com.cycling.domain.lyrics.utils

/**
 * 曲名 / 歌手标签的归一化，在线歌词的**搜索**与**匹配**共用一套规则。
 *
 * 本地文件的标签是用户自己抓的，写法五花八门：`晴天 (Live)`、`晴天 - Live`、
 * `01 - 晴天`、`Song (feat. X) [Remastered 2011]`、`A; B`、`A feat. B`。把这些原样拼成搜索词，
 * 平台侧的检索命中率会明显下降；拿原样去和平台的规范曲名比，又会把同一首歌判成两首。
 *
 * 两类比较刻意分开：
 * - [baseTitle] 去掉「版本说明」与「合作歌手」，用于**搜索**，也用于判断两条记录是不是同一首歌；
 * - [versionTags] 提取「现场 / 伴奏 / 翻唱 / 混音」这类**会改变演奏本身**的标记。同一首歌的
 *   现场版与录音室版时长差几十秒，只有这些标记能把它们分开。
 *
 * 注意 `Remastered` / `Deluxe Edition` **不算**版本标记：重制只动音质，不动时长与演奏，
 * 把它们当成另一个版本会让正常的重制曲目匹配失败。
 */
object TrackText {

    /** 括号及其内容：`(Live)`、`（现场版）`、`[Remastered 2011]`、`【附伴奏】`。 */
    private val BRACKETED = Regex("[(（\\[【〈《][^)）\\]】〉》]*[)）\\]】〉》]")

    /** 曲目序号前缀：`01 - `、`03. `、`07、`。只认带分隔符的，避免把 `1989` 这种曲名吃掉。 */
    private val TRACK_NUMBER_PREFIX = Regex("^\\s*\\d{1,3}\\s*[-.、)）]\\s*")

    /** 破折号后缀：`Song - Live`、`Song – Remastered 2011`。要求两侧有空格，`Jay-Z` 不受影响。 */
    private val DASH_SUFFIX = Regex("\\s+[-–—]\\s+.*$")

    /** 合作歌手后缀：`Song feat. X`、`Song ft. X`。 */
    private val FEAT_SUFFIX = Regex("\\b(?:feat|ft|featuring)\\b\\.?.*$", RegexOption.IGNORE_CASE)

    /** 多歌手标签的分隔符，以及 feat / ft / with 这类连接词（连接词后允许一个点）。 */
    private val ARTIST_SEPARATORS = Regex(
        "[、,，&/;；+·]|\\b(?:feat|ft|with)\\b\\.?",
        RegexOption.IGNORE_CASE
    )

    /** 只保留字母与数字：大小写、空格、括号和标点都不该让同一个名字比较失败。 */
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]")

    /**
     * 会改变演奏本身的版本标记。
     *
     * 每个标记给一组写法；`\b` 只对拉丁词有效，中文直接按子串匹配。
     */
    private val VERSION_PATTERNS: List<Pair<String, Regex>> = listOf(
        "live" to Regex("\\blive\\b|现场|演唱会", RegexOption.IGNORE_CASE),
        "acoustic" to Regex("\\bacoustic\\b|\\bunplugged\\b|不插电", RegexOption.IGNORE_CASE),
        "instrumental" to Regex(
            "\\binstrumental\\b|\\bkaraoke\\b|\\boff ?vocal\\b|伴奏|纯音乐",
            RegexOption.IGNORE_CASE
        ),
        "remix" to Regex("\\bremix\\b|\\bmix\\b|混音|dj版", RegexOption.IGNORE_CASE),
        "demo" to Regex("\\bdemo\\b|小样", RegexOption.IGNORE_CASE),
        "cover" to Regex("\\bcover\\b|翻唱", RegexOption.IGNORE_CASE),
        "edit" to Regex(
            "\\bradio edit\\b|\\btv size\\b|\\btv ver\\b|\\bshort ver\\b|剪辑版",
            RegexOption.IGNORE_CASE
        )
    )

    /**
     * 这首歌「叫什么」：去掉曲目序号、括号内容、破折号后缀与合作歌手。
     *
     * 全部去掉之后为空（例如曲名本身就是 `(Intro)`）时退回原文，宁可保留噪音也不返回空串 ——
     * 空串会让下游的匹配把所有候选都判成「不是同一首」。
     */
    fun baseTitle(raw: String): String {
        var text = raw
        // 括号可能嵌套，反复替换到稳定为止；每次替换都会缩短字符串，不会停不下来。
        while (true) {
            val next = BRACKETED.replace(text, " ")
            if (next == text) break
            text = next
        }
        text = TRACK_NUMBER_PREFIX.replace(text, "")
        text = DASH_SUFFIX.replace(text, "")
        text = FEAT_SUFFIX.replace(text, "")
        return text.trim().ifBlank { raw.trim() }
    }

    /** 只留字母与数字的小写形式，用于「是不是同一个名字」的相等判断。 */
    fun comparable(raw: String): String = raw.lowercase().replace(NON_WORD, "")

    /** [raw] 带哪些版本标记；空集表示它没说自己是什么版本。 */
    fun versionTags(raw: String): Set<String> =
        VERSION_PATTERNS.filter { (_, pattern) -> pattern.containsMatchIn(raw) }
            .map { it.first }
            .toSet()

    /** 多歌手标签拆成单个名字：`A feat. B` → `[A, B]`。 */
    fun artistNames(raw: String): List<String> = raw
        .split(ARTIST_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
