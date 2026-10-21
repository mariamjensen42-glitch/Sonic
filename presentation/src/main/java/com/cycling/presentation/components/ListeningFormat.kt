package com.cycling.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cycling.presentation.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * 听歌时长的口语化写法：`2 小时 13 分钟` / `45 分钟` / `18 秒`。
 *
 * 统计页到处要显示时长，而"每秒都精确"在这里没有意义 —— 听歌时长动辄几十小时，
 * 精确到分钟已经足够，秒只在不足一分钟时出现。做成 Composable 是为了走 `stringResource`，
 * 页面和图表共用同一套单位文案。
 */
@Composable
fun formatListenDuration(ms: Long): String {
    if (ms <= 0L) return stringResource(R.string.listen_duration_zero)
    val totalMinutes = (ms / 60_000.0).roundToLong()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(
            R.string.listen_duration_hours_minutes,
            hours.toInt(),
            minutes.toInt()
        )
        hours > 0 -> stringResource(R.string.listen_duration_hours, hours.toInt())
        minutes > 0 -> stringResource(R.string.listen_duration_minutes, minutes.toInt())
        else -> stringResource(R.string.listen_duration_seconds, (ms / 1000.0).roundToLong().toInt())
    }
}

/** 计数的千分位写法：`12,345`。 */
@Composable
fun formatListenCount(count: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(count)

/** 计数的千分位写法（Int 重载，省掉调用处的转换）。 */
@Composable
fun formatListenCount(count: Int): String = formatListenCount(count.toLong())

/** 把 0..23 的小时写成 `09:00`。 */
fun formatListenHour(hour: Int): String = "%02d:00".format(Locale.getDefault(), hour)

/** 把 0..23 的小时写成下一个整点，用于「09:00–10:00」这种区间。 */
fun formatListenHourEnd(hour: Int): String = formatListenHour((hour + 1) % 24)
