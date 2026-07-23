package com.cycling.sonic.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.sonic.core.ui.components.M3ArtworkImage
import com.cycling.sonic.core.ui.theme.SpotifyColors
import org.jetbrains.compose.resources.painterResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.added
import sonic.shared.generated.resources.ic_add
import sonic.shared.generated.resources.ic_playing
import sonic.shared.generated.resources.play_svgrepo_com

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color = SpotifyColors.Surface
) {
    val currentSong = uiState.currentSong ?: return

    val songProgress = if (uiState.duration > 0) {
        uiState.playbackPosition.toFloat() / uiState.duration.toFloat()
    } else 0f

    val isLiked = currentSong.isFavorite

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clipToBounds()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                M3ArtworkImage(
                    artworkUrl = currentSong.albumArt,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    fallback = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SpotifyColors.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = SpotifyColors.TextTertiary
                            )
                        }
                    }
                )

                Column( modifier = Modifier.widthIn(max = 200.dp) ) {
                    Text(
                        text = currentSong.title,
                        color = SpotifyColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        color = SpotifyColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(70.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleFavorite
                        ),
                    painter = if (isLiked) painterResource(Res.drawable.added)
                    else painterResource(Res.drawable.ic_add),
                    tint = SpotifyColors.TextPrimary,
                    contentDescription = if (isLiked) "取消收藏" else "收藏"
                )

                Icon(
                    painter = if (uiState.isPlaying) painterResource(Res.drawable.ic_playing)
                    else painterResource(Res.drawable.play_svgrepo_com),
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = SpotifyColors.TextPrimary,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(horizontal = 5.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayPause
                        )
                )
            }
        }

        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.Gray
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
        ) {
            Slider(
                value = songProgress,
                onValueChange = {},
                valueRange = 0f..1f,
                colors = sliderColors,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(1.dp),
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = (-4).dp
                    )
                },
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = remember { MutableInteractionSource() },
                        modifier = Modifier,
                        colors = sliderColors,
                        thumbSize = DpSize(0.dp, 0.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
