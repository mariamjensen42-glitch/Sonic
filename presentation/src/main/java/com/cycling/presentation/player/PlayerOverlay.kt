package com.cycling.presentation.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.cycling.domain.model.BeatState
import kotlinx.coroutines.flow.Flow

/**
 * 播放页浮层。
 *
 * 与 MeloX 的 `MeloXIOSNowPlayingSharedHost` 一致：播放页不是导航目的地，而是**盖在整个 app
 * 之上的一层**。底下的页面、底部标签栏、迷你播放器都还在组合中，所以下拉时露出来的是它们
 * 本身 —— 不需要拿一块色面去冒充「背景」。
 *
 * 收起只有**一个变换**（[PlayerMorph.shell]）：整块等比缩小 + 下沉 + 尾段淡出，全部线性
 * 跟着 `progress` 走。这里不再按卡片矩形重排内容，也不再做矩形 morph —— 那两样都试过，
 * 结果都是「进度走了、画面几乎没动」或者「只有尾段才动」，读不出跟手。
 *
 * 三层的顺序：挡板（全屏）→ 变换后的整页（色面 + 内容）。挡板放在变换之外，所以页面缩小时
 * 露出来的那一圈仍然不会把点击漏给底下的主页。
 */
@Composable
fun PlayerOverlay(
    progress: Float,
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onProgressChange: (Float) -> Unit,
    onSettle: (Boolean) -> Unit,
    beatFlow: Flow<BeatState>,
    modifier: Modifier = Modifier,
    /** 封面已交给外层那份飞行中的图：页面自己的封面节点要让开，否则会叠成重影。 */
    artworkOwned: Boolean = false
) {
    val p = progress.coerceIn(0f, 1f)
    val transition = LocalPlayerArtworkTransition.current

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val height = layoutSize.height.toFloat()

    val shell = PlayerMorph.shell(p, height)
    // clip 挂在 layer 内层，圆角会跟着缩放一起变小 —— 按缩放反补，视觉半径才稳定。
    val corner = PlayerSceneSpec.cardCorner *
        (PlayerMorph.cornerFactor(p) / shell.scale.coerceAtLeast(0.01f))

    // 浮层变换写出去：封面要按同一帧的缩放 / 下沉还原展开态，飞行中的图也走同一套变换。
    SideEffect {
        transition?.let {
            it.shellScale = shell.scale
            it.shellShiftY = shell.translationY
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                layoutSize = it
                transition?.containerSize = it
            }
    ) {
        // 吃掉落在空白处的点击：底栏与迷你播放器仍在下面组合中，不挡的话点空白会点到 tab。
        // 放在变换之外 = 整个窗口都被挡住，页面缩小时露出来的那一圈也不漏。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { } }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = shell.scale
                    scaleY = shell.scale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    translationY = shell.translationY
                    alpha = shell.alpha
                }
                .clip(RoundedCornerShape(corner))
        ) {
            PlayerCardBackdrop(playing = uiState.isPlaying)

            PlayerScreen(
                uiState = uiState,
                onIntent = onIntent,
                progress = p,
                onProgressChange = onProgressChange,
                onSettle = onSettle,
                beatFlow = beatFlow,
                artworkOwned = artworkOwned
            )
        }
    }
}
