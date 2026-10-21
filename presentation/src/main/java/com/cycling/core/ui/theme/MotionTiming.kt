package com.cycling.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * 时序例外：`MotionScheme` 只提供弹簧，没有时长与缓动。
 *
 * 这里只放三类「必须按固定节奏走」的 token —— 歌词屏的自动滚动与伴奏行进出，
 * 以及动态色面的流动周期 / 帧间隔。物理参数一律来自 `MaterialTheme.motionScheme`，
 * 因此本文件**不得**出现 `dampingRatio` / `stiffness` / `spring`。新增一项须先在
 * `docs/design/motion.md` 的「时序例外」一节登记。
 */
object SonMotionTiming {

    /** 自动滚动到当前高亮行。 */
    const val SCROLL_MS = 300

    /** 伴奏行淡入 / 淡出（同时用于可见性时间窗）。 */
    const val ACCOMPANIMENT_MS = 600

    /**
     * 动态色面一轮完整流动的时长。
     *
     * 16 秒：色场要在这段时间里把 4 格网格的颜色整个走一遍，看起来才像在流。
     * 之前定 32 秒配合 0.05 的扭曲振幅，一秒内采样点只挪不到半个纹素，真机上就是「一张
     * 不动的背景」，所以周期与振幅是一起定的（振幅见 `SonMeshGradient.WARP`）。
     */
    const val BACKDROP_CYCLE_MS = 16_000

    /**
     * 动态色面的帧间隔上限：30fps。
     *
     * 背景是慢速流动，30fps 与 60fps 肉眼无差；省下的一半绘制与电量才是差别。
     */
    const val BACKDROP_FRAME_MS = 33

    /** 强调减速：进入视野。 */
    private val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** 强调加速：离开视野，比进场更快。 */
    private val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** 歌词自动滚动：不能换弹簧，过冲会让长列表来回弹。 */
    fun <T> scrollSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = SCROLL_MS, easing = emphasizedDecelerate)

    /** 伴奏行进出：`entering` 决定用减速还是加速曲线。 */
    fun <T> accompanimentSpec(entering: Boolean): FiniteAnimationSpec<T> =
        tween(
            durationMillis = ACCOMPANIMENT_MS,
            easing = if (entering) emphasizedDecelerate else emphasizedAccelerate
        )
}
