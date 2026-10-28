package com.cycling.presentation.player

import androidx.compose.animation.core.Easing
import kotlin.math.exp

/**
 * 播放页浮层展开 / 收起的时长与曲线（点击打开、松手后跑完收尾用）。
 *
 * 曲线是归一化的临界阻尼弹簧 `p(f) = [1 − (1+kf)e^(−kf)] / [1 − (1+k)e^(−k)]`，k = 6：
 * 单调、无过冲，起步快、尾段连续减速。这是 MeloX 逐帧量 Apple Music 参考视频后定的形状
 * （几何剖面 0.27 / 0.60 / 0.82 / 0.92，对应 f = 0.167 / 0.333 / 0.5 / 0.667）。
 * 分母归一化保证 p(1) 严格 = 1，末帧不会跳一下。
 *
 * 手势路径不经过这里：拖动时进度直接由手指写，跟手就是跟手。
 */
object PlayerMotion {

    const val OPEN_MS = 320
    const val CLOSE_MS = 280

    private const val SpringK = 6f
    private val SpringEnd = 1f - (1f + SpringK) * exp(-SpringK)

    val easing: Easing = Easing { fraction ->
        val f = fraction.coerceIn(0f, 1f)
        val z = SpringK * f
        (1f - (1f + z) * exp(-z)) / SpringEnd
    }
}
