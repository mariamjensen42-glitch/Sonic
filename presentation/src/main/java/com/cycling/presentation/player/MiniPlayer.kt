package com.cycling.presentation.player

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SpotifyColors.Surface
) {
    if (currentSong == null) return

    val songProgress = if (duration > 0) {
        playbackPosition.toFloat() / duration.toFloat()
    } else 0f

    var isLiked by remember { mutableStateOf(false) }

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
                // Cover art (42dp, 6dp rounded)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentSong.albumArt != null) {
                        AsyncImage(
                            model = currentSong.albumArt,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
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
                }

                // Title + Artist
                Column(
                    modifier = Modifier.widthIn(max = 200.dp)
                ) {
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

            // Like + Play/Pause
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(70.dp)
            ) {
                // Like button
                Icon(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isLiked = !isLiked
                        },
                    painter = if (isLiked) painterResource(id = R.drawable.added)
                    else painterResource(id = R.drawable.ic_add),
                    tint = SpotifyColors.TextPrimary,
                    contentDescription = if (isLiked) "取消收藏" else "收藏"
                )

                // Play/Pause button
                Icon(
                    painter = if (isPlaying) painterResource(id = R.drawable.ic_playing) else painterResource(id = R.drawable.play_svgrepo_com),
                    contentDescription = if (isPlaying) "暂停" else "播放",
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

        // Progress bar — Box with 0dp height so slider overlays without taking space
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
