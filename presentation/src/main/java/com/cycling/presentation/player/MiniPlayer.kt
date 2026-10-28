package com.cycling.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import com.cycling.core.ui.apple.AppleMiniPlayer
import com.cycling.domain.model.Song

/**
 * 迷你播放器：把 [Song] 映射到 Apple 样式的胶囊播放条。
 *
 * 视觉与几何全在 [AppleMiniPlayer]（core/ui/apple）里，这里只做数据适配。
 * 原版没有进度条与收藏按钮 —— 收藏在播放页，进度在播放页的 seekbar 上。
 *
 * 封面这一格是播放页共享元素的起点：位置往 [LocalPlayerArtworkTransition] 里报，播放页打开
 * 期间则由 [artworkAlpha] 把图让出来（同一张图正在根坐标里飞，两端都画会叠成重影）。
 */
@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkAlpha: Float = 1f
) {
    if (currentSong == null) return

    val transition = LocalPlayerArtworkTransition.current

    AppleMiniPlayer(
        artwork = currentSong.albumArt,
        title = currentSong.title,
        subtitle = currentSong.artist,
        isPlaying = isPlaying,
        onPlayPause = onPlayPause,
        onSkipNext = onSkipNext,
        onClick = onClick,
        modifier = modifier,
        artworkModifier = Modifier
            .onGloballyPositioned { transition?.miniBounds = it.rootRect() }
            .graphicsLayer { alpha = artworkAlpha },
        barModifier = Modifier
            .onGloballyPositioned { transition?.miniBarBounds = it.rootRect() }
    )
}
