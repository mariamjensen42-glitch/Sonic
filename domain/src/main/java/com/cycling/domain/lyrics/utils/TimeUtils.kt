package com.cycling.domain.lyrics.utils

internal fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}

internal fun String.parseAsTime(): Int {
    fun parseSecondsAndMillis(part: String): Int {
        val timeParts = part.split('.', limit = 2)
        val seconds = timeParts[0].toIntOrNull()?.times(1000) ?: 0

        if (timeParts.size == 1) return seconds

        val millisStr = timeParts[1]
        val millis = when (millisStr.length) {
            1 -> millisStr.toIntOrNull()?.times(10) ?: 0
            2 -> millisStr.toIntOrNull()?.times(10) ?: 0
            else -> {
                val normalizedMillisStr = millisStr.padEnd(3, '0').substring(0, 3)
                normalizedMillisStr.toIntOrNull() ?: 0
            }
        }

        return seconds + millis
    }

    return try {
        val parts = this.split(":")
        when (parts.size) {
            3 -> {
                val hours = parts[0].toIntOrNull()?.times(3600 * 1000) ?: 0
                val minutes = parts[1].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[2])
                hours + minutes + secondsAndMillis
            }

            2 -> {
                val minutes = parts[0].toIntOrNull()?.times(60 * 1000) ?: 0
                val secondsAndMillis = parseSecondsAndMillis(parts[1])
                minutes + secondsAndMillis
            }

            1 -> {
                parseSecondsAndMillis(parts[0])
            }

            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}

internal fun Int.toTimeFormattedString(): String {
    val totalMillis = this
    if (totalMillis < 0) return "00:00:00.000"

    val hours = totalMillis / 3600_000
    val minutes = (totalMillis % 3600_000) / 60_000
    val seconds = (totalMillis % 60_000) / 1000
    val millis = totalMillis % 1000

    val h = hours.toString().padStart(2, '0')
    val m = minutes.toString().padStart(2, '0')
    val s = seconds.toString().padStart(2, '0')
    val ms = millis.toString().padStart(3, '0')

    return "$h:$m:$s.$ms"
}
