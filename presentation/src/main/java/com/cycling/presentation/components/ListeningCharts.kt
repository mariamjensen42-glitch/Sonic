@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.DailyListening
import com.cycling.domain.model.HourlyListening
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 听歌足迹用到的三张图：本周节奏、近一年热力、听歌时刻。
 *
 * 全部用 Compose 原生 `Canvas` / 布局画，不引第三方图表库：
 *
 * - 图表要跟主题走（颜色只能来自 `MaterialTheme.colorScheme`），第三方库通常自带调色板；
 * - 三种图都是极简单的柱/格，引入一个库反而多一份要维护的依赖；
 * - **没有常驻动画**（ADR-0005 的要求）：界面静止时不该有像素在重绘。
 */

/** 柱状图区的固定高度：和 Expressive 的大按钮同档，视觉上不至于比标题还矮。 */
private val BarChartHeight = 96.dp

/** 空数据的那一天也留一条细线，否则 7 根柱子会看起来"缺了几天"。 */
private const val MinBarFraction = 0.04f

/**
 * 本周节奏：周一到周日 7 根柱子，高度按当天听歌时长归一。
 *
 * 点某一天回调 [onSelect]，由页面决定要不要弹出"这一天最常听"。
 */
@Composable
fun WeeklyRhythmChart(
    days: List<DailyListening>,
    selectedDate: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val peak = days.maxOf { it.playMs }.coerceAtLeast(1L)
    val today = LocalDate.now()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarChartHeight + SonSpacing.large),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
    ) {
        days.forEach { day ->
            val fraction = (day.playMs.toFloat() / peak).coerceIn(MinBarFraction, 1f)
            val isSelected = day.date == selectedDate
            val isToday = day.date == today
            val barColor = when {
                isSelected -> AppleTheme.colors.systemRed
                isToday -> MaterialTheme.colorScheme.tertiary
                day.playMs > 0L -> AppleTheme.colors.systemRed.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onSelectDay(day.date) },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(MaterialTheme.shapes.small)
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.height(SonSpacing.extraSmall))
                Text(
                    text = weekdayLabel(day.date),
                    style = AppleType.caption1,
                    color = if (isSelected || isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 近一年热力图：每列一周、每行一天，颜色越深当天听得越久。
 *
 * 只画"格子"，不画周标签：手机宽度下 53 列的格子本身只有 3~4dp，再塞文字会糊成一片；
 * 底部用一行「近一年」说明就够了。
 */
@Composable
fun YearHeatmap(
    days: List<DailyListening>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    // 对齐到周一，保证每列就是一周，和真实的日历格一致。
    val startDate = today.minusDays(YEAR_DAYS - 1L).with(DayOfWeek.MONDAY)
    val weekCount = ((java.time.temporal.ChronoUnit.DAYS.between(startDate, today) + 7) / 7)
        .toInt()
        .coerceAtLeast(1)
    val playMsByDate = days.associate { it.date to it.playMs }
    val peak = playMsByDate.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val levelColors = listOf(
        AppleTheme.colors.systemRed.copy(alpha = 0.28f),
        AppleTheme.colors.systemRed.copy(alpha = 0.5f),
        AppleTheme.colors.systemRed.copy(alpha = 0.74f),
        AppleTheme.colors.systemRed
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap: Dp = 2.dp
        val cell: Dp = ((maxWidth - gap * (weekCount - 1)) / weekCount).coerceAtLeast(2.dp)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(cell * 7 + gap * 6)
                .clearAndSetSemantics { }
        ) {
            val gapPx = gap.toPx()
            val cellPx = (size.width - gapPx * (weekCount - 1)) / weekCount
            val radius = androidx.compose.ui.geometry.CornerRadius(cellPx * 0.3f)
            for (week in 0 until weekCount) {
                for (dayOfWeek in 0 until 7) {
                    val date = startDate.plusDays(week * 7L + dayOfWeek)
                    if (date.isAfter(today)) continue
                    val playMs = playMsByDate[date] ?: 0L
                    val color = heatColor(playMs, peak, emptyColor, levelColors)
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = week * (cellPx + gapPx),
                            y = dayOfWeek * (cellPx + gapPx)
                        ),
                        size = androidx.compose.ui.geometry.Size(cellPx, cellPx),
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

/**
 * 听歌时刻：0..23 点各一根细柱。
 *
 * [busiestHour] 用 `tertiary` 强调，其余用 `primary` 的半透明版本 —— 24 根柱子全用实色太吵，
 * 用户真正想知道的是"我几点听得最多"。
 */
@Composable
fun HourPulseChart(
    hours: List<HourlyListening>,
    busiestHour: Int?,
    modifier: Modifier = Modifier
) {
    if (hours.isEmpty()) return
    val peak = hours.maxOf { it.playMs }.coerceAtLeast(1L)
    val idleColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeColor = AppleTheme.colors.systemRed.copy(alpha = 0.45f)
    val highlightColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HourBarHeight),
            horizontalArrangement = Arrangement.spacedBy(HourBarGap),
            verticalAlignment = Alignment.Bottom
        ) {
            hours.forEach { hour ->
                val fraction = (hour.playMs.toFloat() / peak).coerceIn(MinBarFraction, 1f)
                val color = when {
                    hour.hour == busiestHour && hour.playMs > 0L -> highlightColor
                    hour.playMs > 0L -> activeColor
                    else -> idleColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(idleColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(
                    text = "%02d:00".format(Locale.getDefault(), hour),
                    style = AppleType.caption1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 排行榜行里的占比条：整行宽度按"这一首占本时段总时长"的比例填充。 */
@Composable
fun ShareMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = AppleTheme.colors.systemRed
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShareMeterHeight)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
    }
}

private fun heatColor(
    playMs: Long,
    peak: Long,
    emptyColor: Color,
    levelColors: List<Color>
): Color {
    if (playMs <= 0L) return emptyColor
    val ratio = playMs.toFloat() / peak
    return when {
        ratio < 0.25f -> levelColors[0]
        ratio < 0.5f -> levelColors[1]
        ratio < 0.8f -> levelColors[2]
        else -> levelColors[3]
    }
}

private fun weekdayLabel(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

/** 热力图覆盖的天数：一年。 */
private const val YEAR_DAYS = 365L

private val HourBarHeight = 56.dp
private val HourBarGap = 2.dp
private val ShareMeterHeight = 4.dp
