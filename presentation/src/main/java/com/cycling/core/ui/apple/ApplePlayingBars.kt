package com.cycling.core.ui.apple

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 播放中的音柱，对应 `collection_page_list_item` 里的 `IndexDancingBarView`（最小宽度 24dp）。
 *
 * 只在**当前曲目**那一行渲染，因此常驻动画最多只有一条；暂停时调用方换回静态序号，
 * 不会出现"整页在跑动画"的情况。
 */
@Composable
fun ApplePlayingBars(
    modifier: Modifier = Modifier,
    color: Color = AppleTheme.colors.systemRed,
    barCount: Int = 3
) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    Row(
        modifier = modifier
            .width(24.dp)
            .height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { i ->
            // 每根柱子错开周期，避免整齐划一的"呼吸感"
            val scale = transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520 + i * 130, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp * scale.value)
                    .background(color, RoundedCornerShape(1.5.dp))
            )
        }
    }
}
