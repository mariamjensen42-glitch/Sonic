package com.cycling.presentation.player

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SonGlyph

/**
 * 迷你播放器封面与播放页封面之间那一张共享的图。
 *
 * 两端各自把封面藏起来，由这一份在根坐标里连续变形，所以中途不会出现两张图叠在一起，
 * 也不会在两端交接时跳一下。矩形由 [PlayerArtworkTransition.flightRect] 按同一个
 * [progress] 算出，因此和整页的缩小 / 下沉一起跟手。
 *
 * 节点按「一个屏宽见方的框」排版一次，之后每帧只改 [graphicsLayer] 的缩放与平移 ——
 * 逐帧改尺寸会让 Coil 的请求反复重建。
 */
@Composable
fun PlayerArtworkFlight(
    artwork: String?,
    /** 这一帧应该占的矩形（根坐标）。 */
    rect: Rect,
    /** 收起进度：1 = 完全展开，0 = 已收进迷你条。只用来推圆角与投影。 */
    progress: Float,
    modifier: Modifier = Modifier
) {
    val transition = LocalPlayerArtworkTransition.current ?: return
    val side = transition.containerSize.width.toFloat()
    if (side <= 1f || rect.width <= 1f) return

    val density = LocalDensity.current
    val sideDp = with(density) { side.toDp() }

    // 圆角与投影都按「离页面多远」算，不按尺寸比：迷你条的缩略图只有大图宽度的 3%，
    // 用尺寸比插值的话圆角还留着 27dp，落位时会明显偏方。
    val docked = 1f - PlayerMorph.dive(progress)
    val shape = RoundedCornerShape(lerp(PlayerSceneSpec.artworkCorner, AppleDims.cornerS, docked))
    val elevationPx = with(density) { (PlayerSceneSpec.artworkShadow * (1f - docked)).toPx() }

    M3Artwork(
        artwork = artwork,
        shape = shape,
        placeholderGlyph = SonGlyph.Music,
        modifier = modifier
            .size(sideDp)
            // 缩放与阴影写进**同一个** layer：阴影会跟着同一套变换落笔。
            // 分成两层（`.shadow(...)` 在外、缩放在内）时，阴影按未缩放的整屏方框画，
            // 而封面已经被缩小 —— 屏幕上就是封面外面多出一圈「框」。
            .graphicsLayer {
                // 左上角为原点：缩放只改尺寸，位置交给平移。
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = rect.width / side
                scaleY = rect.height / side
                translationX = rect.left
                translationY = rect.top
                shadowElevation = elevationPx
                this.shape = shape
                clip = false
            }
    )
}
