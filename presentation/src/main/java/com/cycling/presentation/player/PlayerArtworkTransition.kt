package com.cycling.presentation.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * 迷你播放器封面与播放页封面共享的几何，外加浮层当前的变换。
 *
 * 两端各自在布局阶段上报自己的**根坐标**几何；浮层再把当前的缩放 / 下沉写进来：
 * 浮层里的封面测到的是**已经过变换**的位置，必须还原成展开态才是飞行的起点
 * （否则第一次打开时目标还是未知数，封面只能凭空出现）。
 */
@Stable
class PlayerArtworkTransition {

    /** 迷你播放器封面的根坐标矩形 —— 封面飞行的终点。 */
    var miniBounds by mutableStateOf(Rect.Zero)

    /** 迷你播放条（胶囊本体）的根坐标矩形。 */
    var miniBarBounds by mutableStateOf(Rect.Zero)

    /** 播放页封面在**展开态**下的根坐标矩形。 */
    var coverBounds by mutableStateOf(Rect.Zero)

    /** 浮层当前的缩放，由 [PlayerOverlay] 每帧写入。 */
    var shellScale by mutableFloatStateOf(1f)

    /** 浮层当前的下沉量，同上。 */
    var shellShiftY by mutableFloatStateOf(0f)

    /** 浮层尺寸（= 窗口尺寸）。 */
    var containerSize by mutableStateOf(IntSize.Zero)

    /**
     * 记录播放页封面的实测几何，并抵消浮层变换还原到展开态。
     *
     * [rect] 是已经过缩放 / 下沉的根坐标矩形，[shellScale] / [shellShiftY] 是同一帧的变换。
     */
    fun recordCover(rect: Rect) {
        val size = containerSize
        val s = shellScale
        if (size.width <= 0 || size.height <= 0 || s <= 0.01f) return
        if (rect.width <= 1f || rect.height <= 1f) return

        val cx = size.width / 2f
        val cy = size.height / 2f
        val restored = Rect(
            left = cx + (rect.left - cx) / s,
            top = cy + (rect.top - shellShiftY - cy) / s,
            right = cx + (rect.right - cx) / s,
            bottom = cy + (rect.bottom - shellShiftY - cy) / s
        )
        // 静止时每次上报的都是同一个值，浮点抖动不值得触发重组
        if (abs(restored.left - coverBounds.left) < 0.5f &&
            abs(restored.top - coverBounds.top) < 0.5f &&
            abs(restored.width - coverBounds.width) < 0.5f
        ) {
            return
        }
        coverBounds = restored
    }

    /** 展开态矩形映射到**当前变换**下的位置：页面缩小时封面跟着缩，而不是原地等着。 */
    fun transformed(bounds: Rect): Rect {
        val size = containerSize
        if (size.width <= 0 || size.height <= 0) return bounds
        val s = shellScale
        val cx = size.width / 2f
        val cy = size.height / 2f
        return Rect(
            left = cx + (bounds.left - cx) * s,
            top = cy + (bounds.top - cy) * s + shellShiftY,
            right = cx + (bounds.right - cx) * s,
            bottom = cy + (bounds.bottom - cy) * s + shellShiftY
        )
    }

    /**
     * 这一帧飞行中的封面应该占的矩形。
     *
     * 大部分行程里它**跟着页面走**（[transformed]）：页面缩它就缩，这才看得出来整页在收；
     * 进到 [PlayerMorph.dive] 的收尾窗口才从页面抽离、钻进迷你播放条的封面里。
     */
    fun flightRect(progress: Float): Rect {
        val target = transformed(coverBounds)
        val attached = PlayerMorph.dive(progress)
        if (attached >= 1f || target.width <= 1f) return target
        val from = miniBounds
        if (from.width <= 1f) return target
        return PlayerMorph.lerpRect(from, target, attached)
    }
}

/** 没有播放页浮层时（如独立歌词路由）为 null，两个端点都退化成普通布局。 */
val LocalPlayerArtworkTransition = compositionLocalOf<PlayerArtworkTransition?> { null }

/**
 * 根坐标矩形：位置走 [LayoutCoordinates.localToRoot]（含 graphicsLayer 变换），
 * 尺寸取布局尺寸。不用 `boundsInRoot()` 是因为它拿变换后的左上角配未变换的边长。
 */
fun LayoutCoordinates.rootRect(): Rect = Rect(
    offset = localToRoot(Offset.Zero),
    size = Size(size.width.toFloat(), size.height.toFloat())
)

/**
 * 飞行收尾的进度：到这之后封面交还两端自己的节点。
 *
 * 两端矩形已经重合，交接看不出来；提前交会让两份图短暂叠在一起（重影 + 双份投影）。
 */
const val ArtworkFlightEnd = 0.995f
