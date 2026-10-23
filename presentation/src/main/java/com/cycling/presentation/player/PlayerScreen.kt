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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.components.SonGlyphIcon
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.BeatState
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import com.cycling.presentation.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 播放页。
 *
 * 版式对齐 Apple Music（移植自 MeloX-Android 的 iOS 播放场景）：一屏三页就地滑动交叉淡入，
 * 没有独立顶栏、没有路由跳转。这里只做装配 —— 页面本身在 [NowPlayingSurface]，
 * 队列页复用 [DraggableQueueList]，播放设置面板留在本文件。
 */
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    beatFlow: Flow<BeatState> = emptyFlow(),
    modifier: Modifier = Modifier
) {
    // 节拍快照只写进一个本地 State，并且只在 Canvas 的绘制阶段读 —— 见 BeatSpectrum。
    val beatHolder = remember { mutableStateOf(BeatState.Idle) }
    LaunchedEffect(beatFlow) {
        beatFlow.collect { beatHolder.value = it }
    }

    NowPlayingSurface(
        uiState = uiState,
        onIntent = onIntent,
        onNavigateBack = onNavigateBack,
        beat = { beatHolder.value },
        modifier = modifier
    )

    if (uiState.showPlaybackSettings) {
        AppleSheet(onDismiss = { onIntent(PlayerIntent.TogglePlaybackSettings) }) {
            PlaybackSettingsSheet(
                playbackSpeed = uiState.playbackSpeed,
                sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                sleepTimerMinutes = uiState.sleepTimerMinutes,
                spectrumEnabled = uiState.playerSpectrumEnabled,
                onSetPlaybackSpeed = { onIntent(PlayerIntent.SetPlaybackSpeed(it)) },
                onSetSleepTimer = { onIntent(PlayerIntent.SetSleepTimer(it)) },
                onCancelSleepTimer = { onIntent(PlayerIntent.CancelSleepTimer) },
                onToggleSpectrum = { onIntent(PlayerIntent.SetPlayerSpectrumEnabled(it)) }
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
 * 播放页上的倍速、定时关闭与频谱条开关。
 *
 * 这几项以前只能退回设置页改，而它们都是"听着听着想调一下"的动作 —— 调完还要能立刻回到
 * 正在播的那首歌，所以放在播放页自己的面板里，而不是跳走。
 */
@Composable
private fun PlaybackSettingsSheet(
    playbackSpeed: Float,
    sleepTimerRemainingMs: Long?,
    sleepTimerMinutes: Int,
    spectrumEnabled: Boolean,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleSpectrum: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 面板内容已经比一屏高了（倍速滑杆 + 两排芯片 + 定时关闭 + 频谱条），
            // 不滚动的话最后一行会被顶到屏幕底边、点击区域落在可视区之外。
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = SonSpacing.large)
    ) {
        Text(
            text = stringResource(R.string.player_playback_settings_title),
            style = AppleType.title2Emphasized
        )

        // ── 倍速 ──
        Spacer(modifier = Modifier.height(SonSpacing.large))
        Text(
            text = stringResource(R.string.settings_playback_speed),
            style = AppleType.footnoteEmphasized,
            color = AppleTheme.colors.key
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = formatSpeed(playbackSpeed),
            style = AppleType.title2Emphasized,
            color = AppleTheme.colors.label
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
                AppleChip(
                    label = formatSpeed(preset),
                    selected = kotlin.math.abs(playbackSpeed - preset) < SPEED_PRESET_EPSILON,
                    onClick = { onSetPlaybackSpeed(preset) }
                )
            }
        }

        // ── 定时关闭 ──
        Spacer(modifier = Modifier.height(SonSpacing.large))
        Text(
            text = stringResource(R.string.settings_sleep_timer),
            style = AppleType.footnoteEmphasized,
            color = AppleTheme.colors.key
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = sleepTimerRemainingMs?.let {
                stringResource(R.string.settings_sleep_timer_remaining, formatTimerRemaining(it))
            } ?: stringResource(R.string.settings_sleep_timer_off),
            style = AppleType.body,
            color = AppleTheme.colors.label
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
        ) {
            SleepTimerPresetsMinutes.forEach { minutes ->
                AppleChip(
                    label = stringResource(R.string.sleep_timer_minutes, minutes),
                    // 定时器跑起来之后就不再知道当初选的是哪一档，只在没跑时高亮上次的选择。
                    selected = sleepTimerRemainingMs == null && sleepTimerMinutes == minutes,
                    onClick = { onSetSleepTimer(minutes) }
                )
            }
        }
        if (sleepTimerRemainingMs != null) {
            AppleTextAction(
                label = stringResource(R.string.sleep_timer_cancel),
                onClick = onCancelSleepTimer,
                tint = AppleTheme.colors.systemRed
            )
        }

        // ── 频谱条 ──
        // 它是这一屏唯一的自发重绘来源，所以开关放在这里：觉得费电就当场关掉，不用跳去设置页。
        Spacer(modifier = Modifier.height(SonSpacing.large))
        Row(
            // 整行可点：开关本身只有 48dp 的触点，行末贴边时很容易点空。
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSpectrum(!spectrumEnabled) }
                .padding(vertical = SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.player_spectrum_title),
                    style = AppleType.body,
                    color = AppleTheme.colors.label
                )
                Text(
                    text = stringResource(R.string.player_spectrum_subtitle),
                    style = AppleType.caption1,
                    color = AppleTheme.colors.secondaryLabel
                )
            }
            Spacer(modifier = Modifier.width(SonSpacing.medium))
            Switch(checked = spectrumEnabled, onCheckedChange = onToggleSpectrum)
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

// ── 队列列表 ──────────────────────────────────────────────────────────────

/**
 * 可拖动重排的队列。
 *
 * [modifier] 决定它在哪一页里：队列页要它铺满整屏。行高固定 72dp，拖拽位移按它换算落点。
 * 播放页把队列放在深色遮罩上，[onDarkSurface] 为真时行内文字改用封面之上的前景色。
 */
@Composable
internal fun DraggableQueueList(
    queue: List<Song>,
    currentIndex: Int,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    onDarkSurface: Boolean = false
) {
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val listState = rememberLazyListState()
    val rowHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
                                    color = AppleTheme.colors.fill,
                                    shape = RoundedCornerShape(AppleDims.cornerM)
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            SonGlyphIcon(
                                glyph = SonGlyph.Trash,
                                contentDescription = stringResource(R.string.player_swipe_delete_desc),
                                tint = AppleTheme.colors.label,
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
                        onDarkSurface = onDarkSurface,
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
    onDarkSurface: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Float) -> Unit = {}
) {
    val extended = LocalSonExtendedColors.current
    val headlineColor = if (onDarkSurface) {
        extended.onArtwork
    } else {
        AppleTheme.colors.label
    }
    val secondaryColor = if (onDarkSurface) {
        extended.onArtwork.copy(alpha = 0.6f)
    } else {
        AppleTheme.colors.secondaryLabel
    }

    // 正在播放的那一行尾标记，只在需要时构造，避免为其它行白建一个 lambda
    val currentSongMarker: (@Composable () -> Unit)? = if (isCurrentSong) {
        {
            SonGlyphIcon(
                glyph = SonGlyph.Play,
                contentDescription = null,
                tint = if (onDarkSurface) extended.onArtwork else AppleTheme.colors.key,
                modifier = Modifier.size(SonSizes.iconSmall)
            )
        }
    } else {
        null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentSong) {
                    if (onDarkSurface) {
                        extended.onArtwork.copy(alpha = 0.12f)
                    } else {
                        AppleTheme.colors.fill
                    }
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SonGlyphIcon(
            glyph = SonGlyph.Grip,
            contentDescription = stringResource(R.string.player_drag_handle_desc),
            tint = secondaryColor,
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
        Spacer(modifier = Modifier.width(8.dp))
        M3Artwork(
            artwork = song.albumArt,
            placeholderGlyph = SonGlyph.Music,
            shape = RoundedCornerShape(AppleDims.cornerS),
            modifier = Modifier.size(SonSizes.thumbnail)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = AppleType.body,
                color = headlineColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = AppleType.footnote,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val marker = currentSongMarker
        if (marker != null) marker()
    }
}
