package com.cycling.domain.lyrics.model

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<Artist>? = emptyList(),
) {
    fun getCurrentFirstHighlightLineIndexByTime(time: Int): Int {
        if (lines.isEmpty()) return 0

        var low = 0
        var high = lines.size - 1
        var resultIndex = lines.size

        while (low <= high) {
            val mid = low + (high - low) / 2
            val line = lines[mid]

            if (line.start > time) {
                resultIndex = mid
                high = mid - 1
            } else if (line.end < time) {
                low = mid + 1
            } else {
                resultIndex = mid
                high = mid - 1
            }
        }

        return if (resultIndex < lines.size && time in lines[resultIndex].start..lines[resultIndex].end) {
            resultIndex
        } else {
            low.coerceAtMost(lines.size)
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
