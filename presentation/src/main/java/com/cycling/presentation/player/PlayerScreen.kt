package com.cycling.presentation.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.SpotifyColors
import androidx.compose.ui.res.painterResource
import com.cycling.presentation.R
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.presentation.components.formatDuration
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    gradientStartColor: Color = accentColor
) {
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = uiState.currentSong
    var sliderValue by remember { mutableStateOf<Float?>(null) }

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
        // ── Top Bar (Neptune style: back + "PLAYING SONG" + more) ──
        PlayerTopBar(
            onNavigateBack = onNavigateBack,
            onToggleQueue = { showQueue = true }
        )

        // ── Large Cover Art (385dp, Neptune style) ──
        Box(
            modifier = Modifier
                .size(385.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (currentSong?.albumArt != null) {
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
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── Info + Controls Section ──
        Column(
            modifier = Modifier
                .height(300.dp)
                .padding(0.dp, 0.dp, 0.dp, 50.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Song Title + Artist + Like
            PlayerInfo(
                currentSong = currentSong,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) }
            )

            // Progress Slider
            val progress = sliderValue ?: playbackProgress
            CustomSlider(
                value = progress,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    sliderValue?.let { value ->
                        onIntent(PlayerIntent.SeekTo((value * uiState.duration).toLong()))
                        sliderValue = null
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

            Spacer(modifier = Modifier.padding(5.dp))

            // Playback Controls (Neptune style: shuffle / prev / play-white-circle / next / repeat)
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

    // ── Queue Bottom Sheet ──
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            Spacer(modifier = Modifier.width(M3Spacing.medium))
                            IconButton(onClick = { onIntent(PlayerIntent.ClearQueue) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "清空队列",
                                    tint = MaterialTheme.colorScheme.error
                                )
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
                    DraggableQueueList(
                        queue = uiState.playbackQueue,
                        currentIndex = uiState.queueIndex,
                        accentColor = accentColor,
                        onPlayFromQueue = { index -> onIntent(PlayerIntent.PlayFromQueue(index)) },
                        onRemoveFromQueue = { index -> onIntent(PlayerIntent.RemoveFromQueue(index)) },
                        onMoveItem = { from, to -> onIntent(PlayerIntent.MoveQueueItem(from, to)) }
                    )
                }

                Spacer(modifier = Modifier.height(M3Spacing.medium))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// PlayerTopBar — Neptune style
// ════════════════════════════════════════════════════════════════

@Composable
private fun PlayerTopBar(
    onNavigateBack: () -> Unit,
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
            painter = painterResource(id = R.drawable.ic_down),
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
        Icon(
            modifier = Modifier
                .size(23.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleQueue
                ),
            imageVector = Icons.AutoMirrored.Filled.List,
            tint = Color.White,
            contentDescription = "播放队列"
        )
    }
}

// ════════════════════════════════════════════════════════════════
// PlayerInfo — Neptune style
// ════════════════════════════════════════════════════════════════

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
        Column(
            modifier = Modifier.weight(1f)
        ) {
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
            painter = if (isFavorite) painterResource(id = R.drawable.added) else painterResource(id = R.drawable.ic_add),
            tint = if (isFavorite) SpotifyColors.Accent else Color.White,
            contentDescription = if (isFavorite) "取消收藏" else "收藏"
        )
    }
}

// ════════════════════════════════════════════════════════════════
// CustomSlider — matches reference project's Neptune-style slider
// ════════════════════════════════════════════════════════════════

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

// ════════════════════════════════════════════════════════════════
// PlayerFullControls — Neptune style: shuffle / prev / play / next / repeat
// ════════════════════════════════════════════════════════════════

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
        // Shuffle
        ControlIconButton(
            icon = R.drawable.ic_player_shuffle,
            contentDescription = "随机播放",
            onClick = onToggleShuffle,
            tint = if (shuffleMode) SpotifyColors.Accent else Color.White,
            size = 25.dp
        )

        // Previous
        ControlIconButton(
            icon = R.drawable.ic_player_back,
            contentDescription = "上一首",
            onClick = onSkipPrevious,
            tint = Color.White,
            size = 35.dp
        )

        // Play/Pause (white circle)
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
                painter = if (isPlaying) painterResource(id = R.drawable.ic_playing) else painterResource(id = R.drawable.play_svgrepo_com),
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(30.dp),
                tint = Color.Black
            )
        }

        // Next
        ControlIconButton(
            icon = R.drawable.ic_player_skip,
            contentDescription = "下一首",
            onClick = onSkipNext,
            tint = Color.White,
            size = 35.dp
        )

        // Repeat
        ControlIconButton(
            icon = R.drawable.ic_repeat,
            contentDescription = "循环模式",
            onClick = onToggleRepeat,
            tint = if (repeatMode != RepeatMode.OFF) SpotifyColors.Accent else Color.White,
            size = 20.dp
        )
    }
}

@Composable
private fun ControlIconButton(
    @DrawableRes icon: Int,
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
        painter = painterResource(id = icon),
        tint = tint,
        contentDescription = contentDescription
    )
}

// ════════════════════════════════════════════════════════════════
// DraggableQueueList (unchanged from original Sonic)
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableQueueList(
    queue: List<Song>,
    currentIndex: Int,
    accentColor: Color,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        state = listState
    ) {
        itemsIndexed(
            items = queue,
            key = { _, song -> song.id }
        ) { index, song ->
            val isCurrentSong = index == currentIndex
            val isDragging = index == draggingItemIndex

            val elevationDp by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 0.dp,
                animationSpec = tween(200),
                label = "elevation"
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        shadowElevation = elevationDp.toPx()
                        clip = false
                    }
            ) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveFromQueue(index)
                                true
                            } else false
                        }
                    ),
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clip(M3Shapes.cornerMedium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(end = M3Spacing.medium)
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    modifier = Modifier
                        .padding(vertical = M3Spacing.extraSmall)
                ) {
                    QueueSongItem(
                        song = song,
                        isCurrentSong = isCurrentSong,
                        accentColor = accentColor,
                        onClick = { onPlayFromQueue(index) },
                        onDragStart = { draggingItemIndex = index },
                        onDragEnd = { draggingItemIndex = -1; dragOffset = 0f },
                        onDrag = { delta ->
                            dragOffset += delta
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueSongItem(
    song: Song,
    isCurrentSong: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Float) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrentSong) accentColor.copy(alpha = 0.1f) else Color.Transparent)
            .clip(M3Shapes.cornerSmall)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = M3Spacing.small, horizontal = M3Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "拖动排序",
            modifier = Modifier
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        }
                    )
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(M3Spacing.small))

        // Album art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(M3Shapes.cornerSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(M3Spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentSong) accentColor else MaterialTheme.colorScheme.onSurface,
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
                tint = accentColor
            )
        }
    }
}
