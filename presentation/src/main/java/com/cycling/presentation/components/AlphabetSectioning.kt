package com.cycling.presentation.components

import android.icu.text.Transliterator
import com.cycling.domain.model.Song

/** 非字母分区的归集字母：数字、符号、以及转写不出拉丁字母的标题都落在这里。 */
const val AlphabetOther: Char = '#'

/** 一个字母分区。各区内的歌曲已按标题排好序，分区本身按 A–Z、`#` 收尾。 */
data class AlphabetSection(
    val letter: Char,
    val songs: List<Song>
)

/**
 * 中文归字母用的 ICU 变换。
 *
 * 与 Apple Music 7.0 的 `zh_CN.lproj/Sections.plist` 是同一条规则
 * （`Han-Latin; NFD; [:Mn:] Remove; NFC`），不是自己维护的拼音表。
 * API 29+ 才有 `android.icu.text.Transliterator`，本项目 minSdk 30；
 * 少数精简系统上拿不到实例时退回"非 ASCII 一律归 #"，不影响其它分区。
 */
private val hanToLatin: Transliterator? by lazy {
    runCatching {
        Transliterator.getInstance("Han-Latin; NFD; [:Mn:] Remove; NFC")
    }.getOrNull()
}

/** 取标题的分区字母：先看首字符，非拉丁字符再走 ICU 转写。 */
fun sectionLetterOf(title: String): Char {
    val first = title.trim().firstOrNull() ?: return AlphabetOther
    val upper = first.uppercaseChar()
    if (upper in 'A'..'Z') return upper
    if (first.code < 0x80) return AlphabetOther

    val latin = hanToLatin?.transliterate(first.toString()).orEmpty()
    val letter = latin.firstOrNull { it in 'A'..'Z' || it in 'a'..'z' } ?: return AlphabetOther
    return letter.uppercaseChar()
}

/**
 * 按首字母分组。
 *
 * 列表必须真的按这个键排过序，右侧索引条才跳得准 —— 所以这个函数同时负责排序，
 * 不接受"已经排好的列表 + 只是归个组"这种用法。
 */
fun sectionSongsByAlphabet(songs: List<Song>): List<AlphabetSection> {
    if (songs.isEmpty()) return emptyList()

    val keyed = songs.map { song -> sectionLetterOf(song.title) to song }
    val sorted = keyed.sortedWith(
        compareBy(
            // '#' 永远排在最后，否则它的 ASCII 值会让它插到字母中间。
            { (letter, _) -> if (letter == AlphabetOther) 1 else 0 },
            { (letter, _) -> letter },
            { (_, song) -> song.title.lowercase() }
        )
    )
    return sorted
        .groupBy({ (letter, _) -> letter }, { (_, song) -> song })
        .map { (letter, list) -> AlphabetSection(letter, list) }
}
