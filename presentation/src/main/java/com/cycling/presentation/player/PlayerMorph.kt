package com.cycling.presentation.player

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp

/**
 * 播放页浮层收起几何的**唯一计算出口**。
 *
 * 只有一个变换：整块页面**等比缩小 + 下沉 + 尾段淡出**，全部由 `progress` 线性驱动，
 * 所以拖多少走多少。不做矩形 morph、不重排内容 —— 那两样都会让「跟手」这件事看起来
 * 只发生在某一小段进度里，或者干脆看不出来。
 *
 * 三个量共用一条时间线，视觉上是「这张纸被推下去、变小、最后消失」：
 * - 缩放 1 → [END_SCALE]
 * - 下沉 0 → [END_SINK] 个屏高（与缩放叠加后，**顶边**位移 ≈ 0.86 屏高 = 手势行程，1:1 跟手）
 * - 透明度只在最后 [FADE_START] 之后才降，之前一直是实心的
 */
internal object PlayerMorph {

    /** 收起到底时整页的缩放。0.62 已经明确是「一张卡片」，再小就不像页面了。 */
    const val END_SCALE = 0.62f

    /** 手势行程（屏高比例）。手指走完这么多就收到底。 */
    private val DRAG_RANGE = PlayerSceneSpec.DISMISS_DRAG_RANGE

    /**
     * 收起到底时整页下沉的屏高比例 —— 由「顶边 1:1 跟手」**反推**：
     * 缩放会把顶边抬起 `(1 - END_SCALE) / 2` 个屏高，剩下的才由下沉补，
     * 两者之和必须正好等于手势行程。
     */
    private val END_SINK = DRAG_RANGE - (1f - END_SCALE) / 2f

    /** 淡出窗口：最后 16% 才开始降，之前必须实心，否则整段拖动只剩一个"变淡"。 */
    private const val FADE_START = 0.16f
    private const val FADE_END = 0.02f

    /** 圆角到位的进度：满屏时必须是 0（否则四角把底下那页漏出一条弧）。 */
    private const val CORNER_READY = 0.28f

    /** 封面从页面抽离、钻进迷你条缩略图的进度窗口。 */
    private const val DIVE_START = 0.02f
    private const val DIVE_END = 0.72f

    /** 收起进度下整块页面的变换。 */
    data class Shell(
        val scale: Float,
        val translationY: Float,
        val alpha: Float
    )

    /** `smoothstep`：两端导数为 0，跟手时没有起步 / 收尾的硬拐角。 */
    fun smooth(edge0: Float, edge1: Float, value: Float): Float {
        if (edge1 <= edge0) return if (value >= edge1) 1f else 0f
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 反向渐隐：`value <= gone` 时为 0，`value >= full` 时为 1。 */
    fun fade(value: Float, gone: Float, full: Float): Float =
        smooth(0f, 1f, (value - gone) / (full - gone))

    fun shell(progress: Float, screenHeight: Float): Shell {
        val p = progress.coerceIn(0f, 1f)
        val t = 1f - p
        return Shell(
            scale = scaleAt(p),
            translationY = t * END_SINK * screenHeight,
            alpha = fade(p, FADE_END, FADE_START)
        )
    }

    /**
     * 收起进度对应的整页缩放。
     *
     * 手势也要用它：指针事件是在**缩放后的坐标系**里收到的，手指走 100px、本地只报
     * `100 * scale`，不补回去就会越拖越快。
     */
    fun scaleAt(progress: Float): Float =
        lerp(1f, END_SCALE, 1f - progress.coerceIn(0f, 1f))

    /**
     * 页面被缩放后占的矩形（根坐标）。
     *
     * 缩放原点是屏幕中心，再叠一个下沉。目前只给文档与潜在的裁切用；飞行中的封面直接用
     * [PlayerArtworkTransition.transformed] 走同一套变换，不需要经过这里。
     */
    fun shellBounds(progress: Float, width: Float, height: Float): Rect {
        val shell = shell(progress, height)
        val halfW = width / 2f
        val halfH = height / 2f
        val scaledW = halfW * shell.scale
        val scaledH = halfH * shell.scale
        return Rect(
            left = halfW - scaledW,
            top = halfH - scaledH + shell.translationY,
            right = halfW + scaledW,
            bottom = halfH + scaledH + shell.translationY
        )
    }

    /**
     * 圆角系数（0..1）：满屏 0，脱离屏幕边缘 [CORNER_READY] 之后到 1。
     *
     * 调用方乘上自己的圆角 token；返回系数而不是像素，是为了让调用方不必在 dp / px 之间换算。
     */
    fun cornerFactor(progress: Float): Float =
        smooth(0f, 1f, (1f - progress.coerceIn(0f, 1f)) / CORNER_READY)

    /** 封面从页面交给迷你条的进度：1 = 跟着页面走，0 = 已经贴到迷你条缩略图上。 */
    fun dive(progress: Float): Float =
        smooth(DIVE_START, DIVE_END, progress.coerceIn(0f, 1f))

    /** 两端矩形逐边插值。 */
    fun lerpRect(from: Rect, to: Rect, t: Float): Rect = Rect(
        left = lerp(from.left, to.left, t),
        top = lerp(from.top, to.top, t),
        right = lerp(from.right, to.right, t),
        bottom = lerp(from.bottom, to.bottom, t)
    )
}
