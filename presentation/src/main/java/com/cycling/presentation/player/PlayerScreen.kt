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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cycling.core.ui.components.HiResBadgeOverlay
import com.cycling.core.ui.theme.SonBackdrop
import com.cycling.core.ui.theme.SonDynamicBackdrop
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.rememberPressShapeMorph
import com.cycling.domain.model.RepeatMode
import com.cycling.domain.model.Song
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import com.cycling.presentation.R
import com.cycling.presentation.components.formatDuration
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import com.cycling.domain.model.BeatState
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow

/** 队列列表的最大高度：再高就把底部抽屉撑满，失去"抽屉"的层次。 */
private val QueueListMaxHeight = 400.dp

/** 顶栏高度：一个 48dp 触摸目标 + 上下各 4dp 内边距。色面要按它算 hero 的下沿。 */
private val PlayerTopBarHeight = SonSizes.minTouchTarget + SonSpacing.extraSmall * 2

/**
 * 小金标距封面左下角的内缩，按封面边长取比例。
 *
 * 固定 dp 在分屏/小屏上会顶到圆角，比例内缩才能跟着封面一起收。
 */
private const val HiResBadgeInsetRatio = 0.06f

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit = {},
    beatFlow: Flow<BeatState> = emptyFlow(),
    modifier: Modifier = Modifier
) {
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = uiState.currentSong

    // 节拍快照只写进一个本地 State，并且只在 Canvas 的绘制阶段读 —— 见 BeatSpectrum。
    val beatHolder = remember { mutableStateOf(BeatState.Idle) }
    LaunchedEffect(beatFlow) {
        beatFlow.collect { beatHolder.value = it }
    }

    val playbackProgress = if (uiState.duration > 0) {
        (uiState.playbackPosition.toFloat() / uiState.duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // 页面底色即 surface（已由壁纸动态取色着色），动态色面画在它上面、内容之下。
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 色面的 hero 下沿按「状态栏 + 顶栏 + 上留白 + 封面」估算。封面本身是按剩余高度
        // 自适应的（见下面的 weight 区），这里取「按宽度铺满」的上界 —— 屏幕矮时真实封面更小，
        // 收束区间只是提前一点开始，不会露底。
        val coverSide = (maxWidth - SonLayout.screenMargin * 2).coerceAtLeast(0.dp)
        val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val heroBottom = statusBarInset + PlayerTopBarHeight + SonSpacing.medium + coverSide
        SonDynamicBackdrop(
            playing = uiState.isPlaying,
            fade = SonBackdrop.heroFade(heroBottom = heroBottom, screenHeight = maxHeight),
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = SonLayout.bottomBarClearance),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopBar(
                onNavigateBack = onNavigateBack,
                onNavigateToLyrics = onNavigateToLyrics,
                onToggleQueue = { showQueue = true }
            )

            // 这一屏不滚动（标题单行 + 跑马灯就是为了让版式高度固定），所以封面吃掉剩下的
            // 全部高度：`aspectRatio(1f)` 在 Box 的约束里取「可用宽 / 可用高中的较小值」，
            // 屏幕矮（分屏、小屏）时封面自己缩小，而不是把运输控件挤出屏幕。
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                // 小金标贴在封面左下角，所以封面自己再包一层 Box：标随封面尺寸走，
                // 不会因为外层留白（screenMargin）而偏到封面之外。
                //
                // 这层还负责裁剪：封面是圆角矩形，如果只在上面叠一个左上角对齐的 Box，
                // 标会落进被圆角切掉的那块透明区里 —— 看着像贴在页面背景上，而不是封面上。
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // 内缩按封面边长取比例：分屏/小屏时封面缩小，内缩跟着缩，标不会顶到圆角上。
                    val badgeInset = maxWidth * HiResBadgeInsetRatio
                    M3Artwork(
                        artwork = currentSong?.albumArt,
                        placeholderIcon = SymbolIconName.MusicNote,
                        contentDescription = currentSong?.title,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxSize()
                    )
                    HiResBadgeOverlay(
                        // 没有歌（空态占位）时不贴标：那是占位图，不是一张真正的封面。
                        badge = if (currentSong != null) uiState.hiResBadge else HiResBadge.OFF,
                        customUri = uiState.hiResBadgeCustomUri,
                        modifier = Modifier.padding(badgeInset)
                    )
                }
            }

            PlayerInfo(
                currentSong = currentSong,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) },
                onOpenPlaybackSettings = { onIntent(PlayerIntent.TogglePlaybackSettings) }
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

            Spacer(modifier = Modifier.height(SonSpacing.large))

            PlayerTransportToolbar(
                isPlaying = uiState.isPlaying,
                shuffleMode = uiState.shuffleMode,
                repeatMode = uiState.repeatMode,
                onPlayPause = { onIntent(PlayerIntent.PlayPause) },
                onSkipNext = { onIntent(PlayerIntent.SkipToNext) },
                onSkipPrevious = { onIntent(PlayerIntent.SkipToPrevious) },
                onToggleShuffle = { onIntent(PlayerIntent.ToggleShuffleMode) },
                onToggleRepeat = { onIntent(PlayerIntent.ToggleRepeatMode) }
            )

            Spacer(modifier = Modifier.height(SonSpacing.large))
        }

        // 频谱条贴屏幕底边，落在 bottomBarClearance 的留白里：不改动上面任何一块版式。
        // 只读 beat 快照，暂停时整条不画（见 BeatSpectrum 的约束说明）。
        if (uiState.playerSpectrumEnabled) {
            BeatSpectrum(
                beat = { if (uiState.isPlaying) beatHolder.value else BeatState.Idle },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(SonSpacing.extraLarge)
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.player_spectrum_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

// ── 顶部栏 ────────────────────────────────────────────────────────────────

/**
 * 顶栏只放"去哪"：关闭 / 歌词 / 队列。
 *
 * 收藏与播放设置属于"这首歌本身"，收在标题行右侧的动作簇里 —— 右上角不再是从左到右
 * 四个图标排成一排，视线在标题、动作、返回之间只需两跳。
 */
@Composable
private fun PlayerTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onToggleQueue: () -> Unit
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
            .height(PlayerTopBarHeight)
            .padding(horizontal = SonSpacing.small)
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
            IconButton(onClick = onNavigateToLyrics, colors = actionIconColors) {
                M3SymbolIcon(
                    name = SymbolIconName.Lyrics,
                    filled = true,
                    contentDescription = stringResource(R.string.player_toggle_lyrics)
                )
            }
            IconButton(onClick = onToggleQueue, colors = actionIconColors) {
                M3SymbolIcon(
                    name = SymbolIconName.QueueMusic,
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
    onToggleFavorite: () -> Unit,
    onOpenPlaybackSettings: () -> Unit
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
                // 播放页的标题时刻：headline 级强调变体，而不是把 body 放大。
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                // 标题永远只占一行：这一屏的版式高度是固定的（页面不滚动），多行标题会把
                // 运输控件顶出屏幕。放不下时由 `basicMarquee` 横向跑马灯，只在溢出时运动。
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = currentSong?.artist ?: "",
                // 歌手名降到 label 级，靠字距与标题拉开层级。
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
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
            IconButton(
                onClick = onOpenPlaybackSettings,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Tune,
                    filled = true,
                    contentDescription = stringResource(R.string.player_settings_desc)
                )
            }
        }
    }
}

// ── 进度 ──────────────────────────────────────────────────────────────────

/**
 * 播放进度：静态线性指示器承载视觉，指针手势承载拖拽/点按。
 * 拖拽时用本地值，松手才上报 seek，避免每一次移动都打到播放器。
 *
 * 用普通的 [LinearProgressIndicator]，不用波浪版：波浪是**常驻动画**，只要 `waveSpeed > 0`
 * 它就按屏幕刷新率无限重绘整页（实测暂停时 124 fps / 0.94 个 CPU 核，见
 * `docs/benchmarking.md`「第五轮」）。确定进度的直线版只在 `progress` 变化时重绘，
 * 播放页的动效全部交给背景色面。
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
        LinearProgressIndicator(
            progress = { shown },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── 运输控件 ──────────────────────────────────────────────────────────────

/**
 * 运输控件：一排图标，**没有容器**。
 *
 * 早先包在官方 `HorizontalFloatingToolbar` 的胶囊里。但这一页的背景本身就是封面派生的动态色面
 * （ADR-0005）：再叠一层不透明的浮动容器，等于在一块流动的色面上又盖一张面板，色面被切成
 * 「上面有颜色、下面一块板」两段。现在只留图标，色面可以一路淡到屏幕底部，中间不再有硬边。
 *
 * 播放键仍是 96dp 的 `LargeFloatingActionButton`（按住时圆角 28dp → 圆，
 * [rememberPressShapeMorph]，松手回弹），并且是这一屏唯一的实心形状 —— 运输区自己不再提供
 * 第二个容器。控件直接用 `IconButton` 的默认 48dp 触摸目标，图标之间靠 `SpaceEvenly` 拉开。
 *
 * 运输区落在屏幕下半部，单手拇指可达；底栏与迷你播放器都不在这一屏出现。
 */
@Composable
private fun PlayerTransportToolbar(
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    // 播放键的按压形变只读 FAB 自己的 InteractionSource；图标切换不会触发形状变化。
    val playInteractionSource = remember { MutableInteractionSource() }
    val playShape = rememberPressShapeMorph(playInteractionSource)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
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

        LargeFloatingActionButton(
            onClick = onPlayPause,
            shape = playShape,
            interactionSource = playInteractionSource
        ) {
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
