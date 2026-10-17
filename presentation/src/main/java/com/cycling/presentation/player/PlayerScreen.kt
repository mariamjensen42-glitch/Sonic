@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)

package com.cycling.presentation.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import com.cycling.presentation.R
import com.cycling.presentation.components.formatDuration
import java.util.Locale
import kotlin.math.roundToInt

/** 队列列表的最大高度：再高就把底部抽屉撑满，失去"抽屉"的层次。 */
private val QueueListMaxHeight = 400.dp

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = uiState.currentSong

    val playbackProgress = if (uiState.duration > 0) {
        (uiState.playbackPosition.toFloat() / uiState.duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // 页面底色即 surface（已由封面种子色着色），不再叠静态灰带。
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = SonLayout.bottomBarClearance),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopBar(
                onNavigateBack = onNavigateBack,
                onNavigateToLyrics = onNavigateToLyrics,
                onToggleQueue = { showQueue = true },
                onOpenPlaybackSettings = { onIntent(PlayerIntent.TogglePlaybackSettings) }
            )

            M3Artwork(
                artwork = currentSong?.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium)
                    .aspectRatio(1f),
                contentDescription = currentSong?.title
            )

            PlayerInfo(
                currentSong = currentSong,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) }
            )

            PlayerProgress(
                progress = playbackProgress,
                onSeek = { fraction ->
                    onIntent(PlayerIntent.SeekTo((fraction * uiState.duration).toLong()))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonLayout.screenMargin)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonLayout.screenMargin),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(uiState.playbackPosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(uiState.duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(SonSpacing.medium))

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

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonSpacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.player_queue_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.queueIndex + 1} / ${uiState.playbackQueue.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.playbackQueue.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(SonSpacing.small))
                            TextButton(onClick = { onIntent(PlayerIntent.ClearQueue) }) {
                                M3SymbolIcon(
                                    name = SymbolIconName.Delete,
                                    filled = true,
                                    contentDescription = null,
                                    modifier = Modifier.size(SonSizes.buttonIcon)
                                )
                                Spacer(modifier = Modifier.width(SonSpacing.extraSmall))
                                Text(stringResource(R.string.player_clear_queue))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SonSpacing.small))

                if (uiState.playbackQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SonSpacing.extraExtraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.player_empty_queue),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    DraggableQueueList(
                        queue = uiState.playbackQueue,
                        currentIndex = uiState.queueIndex,
                        onPlayFromQueue = { index -> onIntent(PlayerIntent.PlayFromQueue(index)) },
                        onRemoveFromQueue = { index -> onIntent(PlayerIntent.RemoveFromQueue(index)) },
                        onMoveItem = { from, to -> onIntent(PlayerIntent.MoveQueueItem(from, to)) }
                    )
                }

                Spacer(modifier = Modifier.height(SonSpacing.medium))
            }
        }
    }

    if (uiState.showPlaybackSettings) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PlayerIntent.TogglePlaybackSettings) },
            sheetState = settingsSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            PlaybackSettingsSheet(
                playbackSpeed = uiState.playbackSpeed,
                sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                sleepTimerMinutes = uiState.sleepTimerMinutes,
                onSetPlaybackSpeed = { onIntent(PlayerIntent.SetPlaybackSpeed(it)) },
                onSetSleepTimer = { onIntent(PlayerIntent.SetSleepTimer(it)) },
                onCancelSleepTimer = { onIntent(PlayerIntent.CancelSleepTimer) }
            )
        }
    }
}

// ── 播放设置面板 ──────────────────────────────────────────────────────────

/** 倍速的可选档位。定档而不是只给滑杆：常用倍速就那么几个，点一下比拖准得多。 */
private val PlaybackSpeedPresets = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** 定时关闭的可选时长，分钟。与设置页用同一组数字。 */
private val SleepTimerPresetsMinutes = listOf(15, 30, 45, 60, 90)

/**
 * 播放页上的倍速与定时关闭。
 *
 * 这两项以前只能退回设置页改，而它们都是"听着听着想调一下"的动作 —— 调完还要能立刻回到
 * 正在播的那首歌，所以放在播放页自己的面板里，而不是跳走。
 */
@Composable
private fun PlaybackSettingsSheet(
    playbackSpeed: Float,
    sleepTimerRemainingMs: Long?,
    sleepTimerMinutes: Int,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonSpacing.large)
    ) {
        Text(
            text = stringResource(R.string.player_playback_settings_title),
            style = MaterialTheme.typography.titleLarge
        )

        // ── 倍速 ──
        Spacer(modifier = Modifier.height(SonSpacing.large))
        Text(
            text = stringResource(R.string.settings_playback_speed),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = formatSpeed(playbackSpeed),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = playbackSpeed,
            onValueChange = onSetPlaybackSpeed,
            valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
            // 0.5..2.0 之间每 0.1 一档，端点之外还有 14 个中间值
            steps = 13
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
        ) {
            PlaybackSpeedPresets.forEach { preset ->
                FilterChip(
                    selected = kotlin.math.abs(playbackSpeed - preset) < SPEED_PRESET_EPSILON,
                    onClick = { onSetPlaybackSpeed(preset) },
                    label = { Text(formatSpeed(preset)) }
                )
            }
        }

        // ── 定时关闭 ──
        Spacer(modifier = Modifier.height(SonSpacing.large))
        Text(
            text = stringResource(R.string.settings_sleep_timer),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = sleepTimerRemainingMs?.let {
                stringResource(R.string.settings_sleep_timer_remaining, formatTimerRemaining(it))
            } ?: stringResource(R.string.settings_sleep_timer_off),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
        ) {
            SleepTimerPresetsMinutes.forEach { minutes ->
                FilterChip(
                    // 定时器跑起来之后就不再知道当初选的是哪一档，只在没跑时高亮上次的选择。
                    selected = sleepTimerRemainingMs == null && sleepTimerMinutes == minutes,
                    onClick = { onSetSleepTimer(minutes) },
                    label = { Text(stringResource(R.string.sleep_timer_minutes, minutes)) }
                )
            }
        }
        if (sleepTimerRemainingMs != null) {
            TextButton(onClick = onCancelSleepTimer) {
                Text(stringResource(R.string.sleep_timer_cancel))
            }
        }

        Spacer(modifier = Modifier.height(SonSpacing.large))
    }
}

private const val SPEED_PRESET_EPSILON = 0.001f

private fun formatSpeed(speed: Float): String =
    String.format(Locale.getDefault(), "%.2fx", speed)

/** 向上取整到秒：显示 0:00 却还在响会显得定时器坏了。 */
private fun formatTimerRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

// ── 顶部栏 ────────────────────────────────────────────────────────────────

@Composable
private fun PlayerTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenPlaybackSettings: () -> Unit
) {
    val navIconColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val actionIconColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonSpacing.small, vertical = SonSpacing.extraSmall)
    ) {
        IconButton(onClick = onNavigateBack, colors = navIconColors) {
            M3SymbolIcon(
                name = SymbolIconName.KeyboardArrowDown,
                filled = true,
                contentDescription = stringResource(R.string.player_close)
            )
        }
        Text(
            text = stringResource(R.string.player_now_playing),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenPlaybackSettings, colors = actionIconColors) {
                M3SymbolIcon(
                    name = SymbolIconName.Tune,
                    filled = true,
                    contentDescription = stringResource(R.string.player_settings_desc)
                )
            }
            IconButton(onClick = onNavigateToLyrics, colors = actionIconColors) {
                M3SymbolIcon(
                    name = SymbolIconName.Lyrics,
                    filled = true,
                    contentDescription = stringResource(R.string.player_toggle_lyrics)
                )
            }
            IconButton(onClick = onToggleQueue, colors = actionIconColors) {
                M3SymbolIcon(
                    name = SymbolIconName.MoreVert,
                    filled = true,
                    contentDescription = stringResource(R.string.player_queue_desc)
                )
            }
        }
    }
}

// ── 歌曲信息 ──────────────────────────────────────────────────────────────

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
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.artist ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Favorite,
                filled = isFavorite,
                contentDescription = if (isFavorite) {
                    stringResource(R.string.player_unfavorite_desc)
                } else {
                    stringResource(R.string.player_favorite_desc)
                }
            )
        }
    }
}

// ── 进度 ──────────────────────────────────────────────────────────────────

/**
 * 播放进度：波浪线性指示器承载视觉，指针手势承载拖拽/点按。
 * 拖拽时用本地值，松手才上报 seek，避免每一次移动都打到播放器。
 */
@Composable
private fun PlayerProgress(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val shown = scrubFraction ?: progress

    fun fractionOf(x: Float, width: Int): Float =
        if (width <= 0) 0f else (x / width).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(SonSizes.minTouchTarget)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek(fractionOf(offset.x, size.width))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubFraction = fractionOf(offset.x, size.width)
                    },
                    onDragEnd = {
                        scrubFraction?.let(onSeek)
                        scrubFraction = null
                    },
                    onDragCancel = { scrubFraction = null },
                    onHorizontalDrag = { change, _ ->
                        scrubFraction = fractionOf(change.position.x, size.width)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        LinearWavyProgressIndicator(
            progress = { shown },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── 播放控制 ──────────────────────────────────────────────────────────────

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
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        IconButton(
            onClick = onToggleShuffle,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (shuffleMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Shuffle,
                filled = true,
                contentDescription = stringResource(R.string.player_shuffle_desc)
            )
        }

        IconButton(
            onClick = onSkipPrevious,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            M3SymbolIcon(
                name = SymbolIconName.SkipPrevious,
                filled = true,
                contentDescription = stringResource(R.string.player_previous_desc),
                modifier = Modifier.size(SonSizes.iconLarge)
            )
        }

        LargeFloatingActionButton(onClick = onPlayPause) {
            M3SymbolIcon(
                name = if (isPlaying) SymbolIconName.Pause else SymbolIconName.PlayArrow,
                filled = true,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.player_pause_desc)
                } else {
                    stringResource(R.string.player_play_desc)
                }
            )
        }

        IconButton(
            onClick = onSkipNext,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            M3SymbolIcon(
                name = SymbolIconName.SkipNext,
                filled = true,
                contentDescription = stringResource(R.string.player_next_desc),
                modifier = Modifier.size(SonSizes.iconLarge)
            )
        }

        IconButton(
            onClick = onToggleRepeat,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Repeat,
                filled = true,
                contentDescription = stringResource(R.string.player_repeat_desc)
            )
        }
    }
}

// ── 可拖拽队列 ────────────────────────────────────────────────────────────

@Composable
private fun DraggableQueueList(
    queue: List<Song>,
    currentIndex: Int,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit
) {
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()
    // 队列行是两行 ListItem，固定 72dp 高；拖拽位移按它换算成落点索引。
    val rowHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(QueueListMaxHeight),
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
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
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
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            M3SymbolIcon(
                                name = SymbolIconName.Delete,
                                filled = true,
                                contentDescription = stringResource(R.string.player_swipe_delete_desc),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier
                                    .padding(end = SonSpacing.medium)
                                    .size(SonSizes.iconMedium)
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    modifier = Modifier.padding(vertical = SonSpacing.extraSmall)
                ) {
                    QueueSongItem(
                        song = song,
                        isCurrentSong = isCurrentSong,
                        onClick = { onPlayFromQueue(index) },
                        onDragStart = { draggingItemIndex = index },
                        onDragEnd = {
                            val steps = (dragOffset / rowHeightPx).roundToInt()
                            val target = (index + steps).coerceIn(0, queue.lastIndex)
                            if (target != index) onMoveItem(index, target)
                            draggingItemIndex = -1
                            dragOffset = 0f
                        },
                        onDrag = { delta -> dragOffset += delta }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSongItem(
    song: Song,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Float) -> Unit = {}
) {
    // 正在播放的那一行尾标记，只在需要时构造，避免为其它行白建一个 lambda
    val currentSongMarker: (@Composable () -> Unit)? = if (isCurrentSong) {
        {
            M3SymbolIcon(
                name = SymbolIconName.PlayArrow,
                filled = true,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SonSizes.iconSmall)
            )
        }
    } else {
        null
    }

    ListItem(
        onClick = onClick,
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrentSong) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            }
        ),
        content = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                M3SymbolIcon(
                    name = SymbolIconName.DragHandle,
                    filled = true,
                    contentDescription = stringResource(R.string.player_drag_handle_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(SonSizes.iconMedium)
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
                        }
                )
                Spacer(modifier = Modifier.width(SonSpacing.small))
                M3Artwork(
                    artwork = song.albumArt,
                    placeholderIcon = SymbolIconName.MusicNote,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(SonSizes.thumbnail)
                )
            }
        },
        trailingContent = if (isCurrentSong) {
            currentSongMarker
        } else {
            null
        }
    )
}
