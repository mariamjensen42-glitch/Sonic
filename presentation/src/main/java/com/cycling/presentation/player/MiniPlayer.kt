package com.cycling.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cycling.core.ui.apple.AppleMiniPlayer
import com.cycling.domain.model.Song

/**
 * 迷你播放器：把 [Song] 映射到 Apple 样式的胶囊播放条。
 *
 * 视觉与几何全在 [AppleMiniPlayer]（core/ui/apple）里，这里只做数据适配。
 * 原版没有进度条与收藏按钮 —— 收藏在播放页，进度在播放页的 seekbar 上。
 */
@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    AppleMiniPlayer(
        artwork = currentSong.albumArt,
        title = currentSong.title,
        subtitle = currentSong.artist,
        isPlaying = isPlaying,
        onPlayPause = onPlayPause,
        onSkipNext = onSkipNext,
        onClick = onClick,
        modifier = modifier
    )
}
