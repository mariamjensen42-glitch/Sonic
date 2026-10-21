package com.cycling.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Song
import com.cycling.presentation.R

/**
 * 迷你播放器。
 *
 * 悬浮于底部导航栏之上的一块高圆角表面：左封面、中信息、右控制，
 * 进度用一条贯穿底部的静态线性指示器 —— 不用零高度 Slider 硬凑，也**不用
 * `LinearWavyProgressIndicator`**：那个波浪是常驻动画，只要迷你播放器在场（也就是只要库里有
 * 当前曲目），整块界面就会以 120 fps 持续重绘，实测暂停时也一样（10 秒 1231 帧、CPU 100%+）。
 * 波浪保留给全屏的播放页，那里它才是值得的画面。
 */
@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    duration: Long,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val progress = if (duration > 0) {
        (playbackPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SonSpacing.medium,
                end = SonSpacing.medium,
                top = SonSpacing.extraSmall,
                bottom = SonSpacing.extraSmall
            )
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SonSpacing.small,
                        end = SonSpacing.small,
                        top = SonSpacing.small,
                        bottom = SonSpacing.extraSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3Artwork(
                    artwork = currentSong.albumArt,
                    placeholderIcon = SymbolIconName.MusicNote,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(SonSizes.thumbnail)
                )

                Spacer(modifier = Modifier.width(SonSpacing.mediumSmall))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    M3SymbolIcon(
                        name = SymbolIconName.Favorite,
                        filled = isFavorite,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.player_unfavorite_desc
                            else R.string.player_favorite_desc
                        ),
                        modifier = Modifier.size(SonSizes.iconSmall)
                    )
                }

                FilledIconButton(onClick = onPlayPause) {
                    M3SymbolIcon(
                        name = if (isPlaying) SymbolIconName.Pause else SymbolIconName.PlayArrow,
                        filled = true,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.player_pause_desc else R.string.player_play_desc
                        ),
                        modifier = Modifier.size(SonSizes.iconMedium)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonSpacing.medium)
            )
        }
    }
}
