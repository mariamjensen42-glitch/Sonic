package com.cycling.sonic.presentation.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.cycling.sonic.core.ui.components.M3ArtworkImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.sonic.core.ui.theme.M3Shapes
import com.cycling.sonic.core.ui.theme.M3Spacing
import com.cycling.sonic.core.ui.theme.SpotifyColors
import com.cycling.sonic.domain.model.RepeatMode
import com.cycling.sonic.domain.model.Song
import org.jetbrains.compose.resources.painterResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.added
import sonic.shared.generated.resources.ic_add
import sonic.shared.generated.resources.ic_dots
import sonic.shared.generated.resources.ic_down
import sonic.shared.generated.resources.ic_lyrics
import sonic.shared.generated.resources.ic_player_back
import sonic.shared.generated.resources.ic_player_shuffle
import sonic.shared.generated.resources.ic_player_skip
import sonic.shared.generated.resources.ic_playing
import sonic.shared.generated.resources.ic_repeat
import sonic.shared.generated.resources.ic_repeat_active
import sonic.shared.generated.resources.play_svgrepo_com
import kotlin.math.roundToInt

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit = {},
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    gradientStartColor: Color = accentColor
) {
    val effectiveAccent = accentColor
    val showQueue = uiState.showQueue
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = uiState.currentSong
    val queueSheetBg = MaterialTheme.colorScheme.surfaceContainer

    val playbackProgress = if (uiState.duration > 0) {
        uiState.playbackPosition.toFloat() / uiState.duration.toFloat()
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientStartColor, Color.Black),
                    startY = 100f
                )
            )
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        PlayerTopBar(
            onNavigateBack = onNavigateBack,
            onNavigateToLyrics = onNavigateToLyrics,
            onToggleQueue = { onIntent(PlayerIntent.ToggleQueue) }
        )

        // Large Cover Art
        Box(
            modifier = Modifier
                .size(385.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            M3ArtworkImage(
                artworkUrl = currentSong?.albumArt,
                contentDescription = currentSong?.title,
                modifier = Modifier.fillMaxSize(),
                fallback = { Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.6f)) }
            )
        }

        // Info + Controls
        Column(
            modifier = Modifier
                .height(300.dp)
                .padding(0.dp, 0.dp, 0.dp, 50.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            PlayerInfo(
                currentSong = currentSong,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) }
            )

            // Progress Slider
            var sliderValue by remember { mutableFloatStateOf(-1f) }
            val progress = if (sliderValue >= 0f) sliderValue else playbackProgress
            CustomSlider(
                value = progress,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    if (sliderValue >= 0f) {
                        onIntent(PlayerIntent.SeekTo((sliderValue * uiState.duration).toLong()))
                        sliderValue = -1f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 0.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray
                )
            )

            // Time Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(uiState.playbackPosition),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDuration(uiState.duration),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Playback Controls
            PlayerFullControls(
                isPlaying = uiState.isPlaying,
                shuffleMode = uiState.shuffleMode,
                repeatMode = uiState.repeatMode,
                onPlayPause = { onIntent(PlayerIntent.PlayPause) },
                onSkipNext = { onIntent(PlayerIntent.SkipToNext) },
                onSkipPrevious = { onIntent(PlayerIntent.SkipToPrevious) },
                onToggleShuffle = { onIntent(PlayerIntent.ToggleShuffleMode) },
                onToggleRepeat = { onIntent(PlayerIntent.ToggleRepeatMode) }
            )
        }
    }

    // Queue Bottom Sheet
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PlayerIntent.ToggleQueue) },
            sheetState = sheetState,
            containerColor = queueSheetBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = M3Spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放队列",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.queueIndex + 1} / ${uiState.playbackQueue.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.playbackQueue.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(M3Spacing.small))
                            TextButton(
                                onClick = { onIntent(PlayerIntent.ClearQueue) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(M3Spacing.extraSmall))
                                Text("清空", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(M3Spacing.small))

                if (uiState.playbackQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "播放队列为空",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    uiState.playbackQueue.forEachIndexed { index, song ->
                        val isCurrentSong = index == uiState.queueIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrentSong) effectiveAccent.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clip(M3Shapes.cornerSmall)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onIntent(PlayerIntent.PlayFromQueue(index)) }
                                )
                                .padding(vertical = M3Spacing.small, horizontal = M3Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "拖动排序",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(M3Spacing.small))
                            M3ArtworkImage(
                                artworkUrl = song.albumArt,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(M3Shapes.cornerSmall),
                                fallback = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(M3Spacing.small))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrentSong) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = effectiveAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(M3Spacing.medium))
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onToggleQueue: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNavigateBack
                ),
            painter = painterResource(Res.drawable.ic_down),
            tint = Color.White,
            contentDescription = "关闭"
        )
        Text(
            text = "正在播放",
            color = SpotifyColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToLyrics
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_lyrics),
                    contentDescription = "歌词",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                modifier = Modifier
                    .size(23.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleQueue
                    ),
                painter = painterResource(Res.drawable.ic_dots),
                tint = Color.White,
                contentDescription = "播放队列"
            )
        }
    }
}

@Composable
private fun PlayerInfo(
    currentSong: Song?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.artist ?: "",
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val favInteractionSource = remember { MutableInteractionSource() }
        val isFavPressed by favInteractionSource.collectIsPressedAsState()
        val favScale by animateFloatAsState(
            targetValue = if (isFavPressed) 0.9f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "fav_scale"
        )
        Icon(
            modifier = Modifier
                .size(26.dp)
                .scale(favScale)
                .clickable(
                    interactionSource = favInteractionSource,
                    indication = null,
                    onClick = onToggleFavorite
                ),
            painter = if (isFavorite) painterResource(Res.drawable.added)
            else painterResource(Res.drawable.ic_add),
            tint = if (isFavorite) SpotifyColors.Accent else Color.White,
            contentDescription = if (isFavorite) "取消收藏" else "收藏"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors()
) {
    Box(modifier = modifier.height(10.dp)) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = (-4).dp
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier,
                    colors = colors,
                    thumbSize = DpSize(0.dp, 0.dp)
                )
            }
        )
    }
}

@Composable
private fun PlayerFullControls(
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        ControlIconButton(
            painter = painterResource(Res.drawable.ic_player_shuffle),
            contentDescription = "随机播放",
            onClick = onToggleShuffle,
            tint = if (shuffleMode) SpotifyColors.Accent else Color.White,
            size = 25.dp
        )

        ControlIconButton(
            painter = painterResource(Res.drawable.ic_player_back),
            contentDescription = "上一首",
            onClick = onSkipPrevious,
            tint = Color.White,
            size = 35.dp
        )

        val playInteractionSource = remember { MutableInteractionSource() }
        val isPlayPressed by playInteractionSource.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.9f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
            label = "play_scale"
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(65.dp)
                .scale(playScale)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(
                    interactionSource = playInteractionSource,
                    indication = null,
                    onClick = onPlayPause
                )
        ) {
            Icon(
                painter = if (isPlaying) painterResource(Res.drawable.ic_playing)
                else painterResource(Res.drawable.play_svgrepo_com),
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(30.dp),
                tint = Color.Black
            )
        }

        ControlIconButton(
            painter = painterResource(Res.drawable.ic_player_skip),
            contentDescription = "下一首",
            onClick = onSkipNext,
            tint = Color.White,
            size = 35.dp
        )

        val repeatIcon = when (repeatMode) {
            RepeatMode.ONE -> Res.drawable.ic_repeat_active
            else -> Res.drawable.ic_repeat
        }
        ControlIconButton(
            painter = painterResource(repeatIcon),
            contentDescription = "循环模式",
            onClick = onToggleRepeat,
            tint = if (repeatMode != RepeatMode.OFF) SpotifyColors.Accent else Color.White,
            size = 20.dp
        )
    }
}

@Composable
private fun ControlIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    size: Dp
) {
    Icon(
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        painter = painter,
        tint = tint,
        contentDescription = contentDescription
    )
}

@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            )
    )
}
