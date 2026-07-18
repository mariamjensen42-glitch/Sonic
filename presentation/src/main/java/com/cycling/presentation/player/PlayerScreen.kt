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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.presentation.components.formatDuration
import com.cycling.presentation.lyrics.components.KaraokeLyricsView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = uiState.currentSong
    var sliderValue by remember { mutableStateOf<Float?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        BlurredBackground(
            albumArt = currentSong?.albumArt,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = M3Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "关闭",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在播放",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentSong != null) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "播放队列"
                    )
                }
                IconButton(onClick = { onIntent(PlayerIntent.ToggleViewMode) }) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "歌词",
                        tint = if (uiState.viewMode == PlayerViewMode.LYRICS) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(M3Spacing.extraLarge))

            androidx.compose.animation.Crossfade(
                targetState = uiState.viewMode,
                animationSpec = tween(durationMillis = 300),
                label = "view_mode_crossfade",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = M3Spacing.extraLarge)
            ) { viewMode ->
                when (viewMode) {
                    PlayerViewMode.COVER -> {
                        CoverView(
                            currentSong = currentSong,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    PlayerViewMode.LYRICS -> {
                        LyricsView(
                            lyrics = uiState.lyrics,
                            hasLyrics = uiState.hasLyrics,
                            isLoading = uiState.isLoadingLyrics,
                            playbackPosition = uiState.playbackPosition,
                            onSeekTo = { position -> onIntent(PlayerIntent.SeekTo(position)) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(M3Spacing.large))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = M3Spacing.extraLarge)
            ) {
                if (currentSong != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        val favInteractionSource = remember { MutableInteractionSource() }
                        val isFavPressed by favInteractionSource.collectIsPressedAsState()
                        val favScale by animateFloatAsState(
                            targetValue = if (isFavPressed) M3Motion.buttonPressScale else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 400f
                            ),
                            label = "fav_scale"
                        )
                        IconButton(
                            onClick = { onIntent(PlayerIntent.ToggleFavorite) },
                            modifier = Modifier
                                .size(48.dp)
                                .scale(favScale),
                            interactionSource = favInteractionSource
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (uiState.isFavorite) "取消收藏" else "收藏",
                                tint = if (uiState.isFavorite) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(M3Spacing.extraSmall))
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(M3Spacing.large))

            Column(
                modifier = Modifier.padding(horizontal = M3Spacing.medium)
            ) {
                Slider(
                    value = sliderValue ?: uiState.playbackPosition.toFloat(),
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        sliderValue?.let { value ->
                            onIntent(PlayerIntent.SeekTo(value.toLong()))
                            sliderValue = null
                        }
                    },
                    valueRange = 0f..(if (uiState.duration > 0) uiState.duration.toFloat() else 1f),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(uiState.playbackPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(uiState.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(M3Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onIntent(PlayerIntent.ToggleShuffleMode) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "随机播放",
                        modifier = Modifier.size(24.dp),
                        tint = if (uiState.shuffleMode) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onIntent(PlayerIntent.SkipToPrevious) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(40.dp)
                    )
                }

                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) M3Motion.buttonPressScale else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 200f
                    ),
                    label = "play_scale"
                )

                IconButton(
                    onClick = { onIntent(PlayerIntent.PlayPause) },
                    modifier = Modifier
                        .size(M3ComponentSize.fabStandardSize)
                        .scale(playScale)
                        .background(accentColor, CircleShape),
                    interactionSource = playInteractionSource
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { onIntent(PlayerIntent.SkipToNext) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = { onIntent(PlayerIntent.ToggleRepeatMode) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ALL -> Icons.Default.RepeatOn
                            RepeatMode.ONE -> Icons.Default.RepeatOneOn
                        },
                        contentDescription = "循环模式",
                        modifier = Modifier.size(24.dp),
                        tint = if (uiState.repeatMode != RepeatMode.OFF) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(M3Spacing.medium))
        }
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
            
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 0.dp,
                animationSpec = tween(200),
                label = "elevation"
            )
            
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        shadowElevation = elevation.toPx()
                        clip = false
                    }
            ) {
                QueueSongItem(
                    song = song,
                    isCurrentSong = isCurrentSong,
                    accentColor = accentColor,
                    onClick = { onPlayFromQueue(index) },
                    onRemove = { onRemoveFromQueue(index) },
                    onDragStart = {
                        draggingItemIndex = index
                        dragOffset = 0f
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        val itemHeight = 72f
                        val targetIndex = index + (dragOffset / itemHeight).roundToInt()
                        if (targetIndex != index && targetIndex in queue.indices) {
                            onMoveItem(index, targetIndex)
                            draggingItemIndex = targetIndex
                            dragOffset = 0f
                        }
                    },
                    onDragEnd = {
                        draggingItemIndex = -1
                        dragOffset = 0f
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSongItem(
    song: Song,
    isCurrentSong: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, M3Shapes.cornerMedium)
                    .padding(horizontal = M3Spacing.medium),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(M3Shapes.cornerMedium)
                .background(
                    if (isCurrentSong) accentColor.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                        modifier = Modifier.size(24.dp),
                        tint = if (isCurrentSong) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
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
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "拖拽排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { _ -> onDragStart() },
                            onDrag = { _, dragAmount ->
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
            )
        }
    }
}

@Composable
private fun BlurredBackground(
    albumArt: String?,
    modifier: Modifier = Modifier
) {
    val dominantColors = rememberDominantColors(albumArt)
    
    androidx.compose.animation.Crossfade(
        targetState = albumArt,
        animationSpec = tween(durationMillis = 500),
        label = "background_crossfade",
        modifier = modifier
    ) { art ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = BlurEffect(
                        radiusX = 25f,
                        radiusY = 25f,
                        edgeTreatment = TileMode.Clamp
                    )
                }
        ) {
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColors.primary.copy(alpha = 0.7f),
                        dominantColors.secondary.copy(alpha = 0.8f),
                        dominantColors.tertiary.copy(alpha = 0.9f)
                    )
                )
            )
    )
}

@Composable
private fun CoverView(
    currentSong: Song?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(M3Shapes.cornerLarge),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong.albumArt != null) {
                    AsyncImage(
                        model = currentSong.albumArt,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsView(
    lyrics: com.cycling.domain.lyrics.model.SyncedLyrics?,
    hasLyrics: Boolean,
    isLoading: Boolean,
    playbackPosition: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Text(
                    text = "加载中...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            hasLyrics && lyrics != null -> {
                val listState = rememberLazyListState()
                
                KaraokeLyricsView(
                    listState = listState,
                    lyrics = lyrics,
                    currentPosition = { playbackPosition.toInt() },
                    onLineClicked = { line -> onSeekTo(line.start.toLong()) },
                    onLinePressed = { },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(M3Spacing.medium))
                    Text(
                        text = "暂无歌词",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
