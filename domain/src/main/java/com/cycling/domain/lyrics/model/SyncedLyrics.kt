package com.cycling.domain.lyrics.model

import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
) {
    /**
     * 是否含逐字（卡拉 OK）行。
     *
     * 播放页据此决定逐字高亮还是整行高亮，缓存据此决定保留多久，歌词选择器据此标注「逐字 / 行级」。
     */
    val isWordByWord: Boolean
        get() = lines.any { it is KaraokeLine }

    /**
     * 是否含译文。
     *
     * 译文是**逐行可有可无**的：一份纯外语 LRC 往往一行翻译都没有，这时候歌词页就不该摆一个
     * 按了没反应的「译文开关」。逐行类型各自带 `translation`，[ISyncedLine] 本身没有，所以只能
     * 按类型问；将来加新的行类型时这里的 `else` 会安静地当作「没有译文」。
     */
    val hasTranslation: Boolean
        get() = lines.any { line ->
            when (line) {
                is KaraokeLine -> line.translation != null
                is SyncedLine -> line.translation != null
                else -> false
            }
        }

    fun getCurrentAllHighlightLineIndicesByTime(time: Int): List<Int> {
        if (lines.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<Int>()

        var low = 0
        var high = lines.size - 1
        var probeIndex = -1

        while (low <= high) {
            val mid = low + (high - low) / 2
            val line = lines[mid]

            if (line.start > time) {
                high = mid - 1
            } else if (line.end < time) {
                low = mid + 1
            } else {
                probeIndex = mid
                break
            }
        }

        val searchStartIndex = if (probeIndex >= 0) {
            probeIndex
        } else {
            (low - 1).coerceAtLeast(0)
        }

        // 区间必须是**左闭右开**：LRC 的惯例是每一行的 end 就取下一行的 start，
        // 右端闭合时「跳到某句开头」会让上一句的 end 也等于这个时刻，于是两句一起亮。
        for (i in searchStartIndex downTo 0) {
            val line = lines[i]
            if (time >= line.start && time < line.end) {
                results.add(i)
            }
            if (line.end <= time) {
                break
            }
        }

        for (i in (searchStartIndex + 1) until lines.size) {
            val line = lines[i]
            if (time >= line.start && time < line.end) {
                results.add(i)
            }
            if (line.start > time) {
                break
            }
        }

        // 行与行之间有空隙、或最后一句已经唱完时，`results` 会空 —— 那就保持
        // 「最后开始的那一句」亮着，否则整屏歌词会突然一句都不高亮。
        if (results.isEmpty()) {
            val lastStarted = lines.indexOfLast { it.start <= time }
            if (lastStarted >= 0) results.add(lastStarted)
        }

        return results.sorted()
    }
}
