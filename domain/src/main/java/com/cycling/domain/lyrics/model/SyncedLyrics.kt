package com.cycling.domain.lyrics.model

import com.cycling.domain.lyrics.model.karaoke.KaraokeLine

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

        for (i in searchStartIndex downTo 0) {
            val line = lines[i]
            if (time in line.start..line.end) {
                results.add(i)
            }
            if (line.end < time) {
                break
            }
        }

        for (i in (searchStartIndex + 1) until lines.size) {
            val line = lines[i]
            if (time in line.start..line.end) {
                results.add(i)
            }
            if (line.start > time) {
                break
            }
        }

        return results.sorted()
    }
}
