package com.cycling.presentation.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.FlamingoIcon
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.formatDuration

/**
 * 底部控制区：进度条 → 时间行 → 运输键 → 页面选择器。
 *
 * 与 Apple Music 一致，运输键只有上一首 / 播放 / 下一首三颗，页脚一排「歌词 / 随机 / 队列」；
 * 进度条是 4dp 细线，拖动时加粗到 6dp 并浮出一颗 6dp 的圆点 —— 平时不带圆点，落在歌词页
 * 时才不会有一条亮线横在文字下面。
 */
@Composable
internal fun PlayerControls(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = PlayerSceneSpec.controlSide,
                end = PlayerSceneSpec.controlSide,
                bottom = PlayerSceneSpec.controlBottom
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerSeekBar(
            position = uiState.playbackPosition,
            duration = uiState.duration,
            onSeek = { position ->
                onInteract()
                onIntent(PlayerIntent.SeekTo(position))
            }
        )

        Spacer(modifier = Modifier.height(PlayerSceneSpec.timeRowMargin))
        TimeRow(
            position = uiState.playbackPosition,
            duration = uiState.duration,
            quality = uiState.currentSong.losslessTier()
        )
        Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
        TransportRow(
            isPlaying = uiState.isPlaying,
            onIntent = onIntent,
            onInteract = onInteract
        )
        Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
        PageSelectorRow(
            viewMode = uiState.viewMode,
            shuffleEnabled = uiState.shuffleMode,
            onIntent = onIntent,
            onInteract = onInteract
        )
    }
}

/**
 * 进度条：4dp 圆头轨道 + 28dp 触摸高度 + 拖动时才出现的圆点。
 *
 * 拖拽用本地值、松手才上报 seek。轨道自己画：Material 的 `Slider` 带常驻滑块与内边距，
 * 复刻不出这条「平时是一条干净的细线」的观感。
 */
@Composable
private fun PlayerSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val extended = LocalSonExtendedColors.current
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val progressFraction = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shown = scrubFraction ?: progressFraction
    val scrubbing = scrubFraction != null

    val trackHeight by animateDpAsState(
        targetValue = if (scrubbing) {
            PlayerSceneSpec.seekTrackScrubHeight
        } else {
            PlayerSceneSpec.seekTrackHeight
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "seekTrackHeight"
    )

    fun fractionOf(x: Float, width: Int): Float =
        if (width <= 0) 0f else (x / width).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerSceneSpec.seekTouchHeight)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0L) {
                        onSeek((fractionOf(offset.x, size.width) * duration).toLong())
                    }
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubFraction = fractionOf(offset.x, size.width)
                    },
                    onDragEnd = {
                        if (duration > 0L) {
                            scrubFraction?.let { onSeek((it * duration).toLong()) }
                        }
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerSceneSpec.seekTouchHeight)
        ) {
            val y = center.y
            val stroke = trackHeight.toPx()
            drawLine(
                color = extended.onArtwork.copy(alpha = 0.22f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            if (shown > 0f) {
                drawLine(
                    color = extended.onArtwork,
                    start = Offset(0f, y),
                    end = Offset(size.width * shown, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            if (scrubbing) {
                drawCircle(
                    color = extended.onArtwork,
                    radius = PlayerSceneSpec.seekThumb.toPx(),
                    center = Offset(size.width * shown, y)
                )
            }
        }
    }
}

/** 时间行：左侧已播、右侧剩余（带负号）、中间是音质徽标 —— Apple Music 的读法。 */
@Composable
private fun TimeRow(
    position: Long,
    duration: Long,
    quality: LosslessTier?
) {
    val extended = LocalSonExtendedColors.current
    val tint = extended.onArtwork.copy(alpha = 0.6f)
    // 三格定位：左边已播、右边剩余、中间是品质徽标。用 Box 的三处 align 而不是
    // Row(SpaceBetween)：两边时间长度不一样时，Row 会把中间那颗挤偏，徽标就不在正中间了。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerSceneSpec.timeRowHeight)
    ) {
        Text(
            text = formatDuration(position),
            modifier = Modifier.align(Alignment.CenterStart),
            style = AppleType.caption1Emphasized,
            color = tint
        )
        if (quality != null) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FlamingoIcon(
                    res = R.drawable.ic_flamingo_lossless,
                    // 文字已经说明是「无损」，图标只是同一句话的图形版本，不进无障碍树。
                    contentDescription = null,
                    tint = tint,
                    size = 18.dp
                )
                Text(
                    text = stringResource(quality.labelRes),
                    style = AppleType.caption1Emphasized,
                    color = tint
                )
            }
        }
        Text(
            text = "-${formatDuration((duration - position).coerceAtLeast(0))}",
            modifier = Modifier.align(Alignment.CenterEnd),
            style = AppleType.caption1Emphasized,
            color = tint
        )
    }
}

/**
 * 进度条下方那枚品质徽标的档位。
 *
 * 只区分无损的两档 —— Apple Music 那个位置也只在这两种情况下出现徽标，
 * 有损曲目留白比硬塞一个「MP3 320k」干净。判定与设置页统计（HR / SQ）同一套阈值：
 * 先看容器是不是无损（mime / 后缀），再看码率是否高过 CD（1411 kbps）。
 */
private enum class LosslessTier(@param:StringRes val labelRes: Int) {
    HI_RES(R.string.player_quality_hi_res),
    LOSSLESS(R.string.player_quality_lossless)
}

/** 无损容器：flac / wav / aiff / ape / wavpack / tta / DSD。m4a 不放进来 —— AAC 也用它。 */
private val LosslessMimeTypes = setOf(
    "audio/flac", "audio/x-flac",
    "audio/wav", "audio/x-wav", "audio/wave",
    "audio/aiff", "audio/x-aiff",
    "audio/x-ape", "audio/x-wv", "audio/x-tta",
    "audio/x-dsf", "audio/x-dff", "audio/dsd"
)

private val LosslessExtensions = setOf(
    "flac", "wav", "wave", "aiff", "aif", "aifc", "ape", "wv", "tta", "dsf", "dff"
)

private fun Song?.losslessTier(): LosslessTier? {
    if (this == null) return null
    val lossless = mimeType.lowercase() in LosslessMimeTypes ||
        path.substringAfterLast('.', "").lowercase() in LosslessExtensions
    if (!lossless) return null
    return if (bitrate > 1411) LosslessTier.HI_RES else LosslessTier.LOSSLESS
}

/**
 * 运输键。
 *
 * 用 Material 的 [IconButton]：涟漪与 Expressive 的形状变形由组件内部完成，页面只给尺寸
 * 与语义，不再手写指示或缩放。上一首 / 下一首用 Flamingo 的**双三角**（`Rewind` /
 * `FastForward`），不是 Apple 的「竖条+三角」—— 换了图形语言就要连读法一起换，两种混着
 * 拼会得到一只四不像。
 */
@Composable
private fun TransportRow(
    isPlaying: Boolean,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit
) {
    val extended = LocalSonExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            icon = R.drawable.ic_flamingo_rewind,
            description = stringResource(R.string.player_previous_desc),
            iconSize = PlayerSceneSpec.transportIcon,
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(PlayerIntent.SkipToPrevious)
            }
        )
        Spacer(modifier = Modifier.width(PlayerSceneSpec.transportGap))
        TransportButton(
            icon = if (isPlaying) R.drawable.ic_flamingo_pause else R.drawable.ic_flamingo_play,
            description = if (isPlaying) {
                stringResource(R.string.player_pause_desc)
            } else {
                stringResource(R.string.player_play_desc)
            },
            iconSize = PlayerSceneSpec.playIcon,
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(PlayerIntent.PlayPause)
            }
        )
        Spacer(modifier = Modifier.width(PlayerSceneSpec.transportGap))
        TransportButton(
            icon = R.drawable.ic_flamingo_fast_forward,
            description = stringResource(R.string.player_next_desc),
            iconSize = PlayerSceneSpec.transportIcon,
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(PlayerIntent.SkipToNext)
            }
        )
    }
}

@Composable
private fun TransportButton(
    @DrawableRes icon: Int,
    description: String,
    iconSize: Dp,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(PlayerSceneSpec.transportSize)
    ) {
        FlamingoIcon(
            res = icon,
            contentDescription = description,
            tint = tint,
            size = iconSize
        )
    }
}

/**
 * 页脚三颗：歌词 / 随机 / 队列。选中项满亮，其余压到 45%。
 *
 * 歌词与队列有「开启态」的第二张图（Flamingo 的 `LyricsOn` / `QueueOn`），选中时换图而不是
 * 只提亮 —— 这两个图标本身就是靠「多一道笔画」表达打开，只改透明度看不出开关。
 */
@Composable
private fun PageSelectorRow(
    viewMode: PlayerViewMode,
    shuffleEnabled: Boolean,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit
) {
    val extended = LocalSonExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerSceneSpec.selectorHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectorButton(
            icon = R.drawable.ic_flamingo_lyrics,
            activeIcon = R.drawable.ic_flamingo_lyrics_on,
            active = viewMode == PlayerViewMode.LYRICS,
            description = stringResource(R.string.player_toggle_lyrics),
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(
                    if (viewMode == PlayerViewMode.LYRICS) {
                        PlayerIntent.SetViewMode(PlayerViewMode.COVER)
                    } else {
                        PlayerIntent.SetViewMode(PlayerViewMode.LYRICS)
                    }
                )
            }
        )
        SelectorButton(
            icon = R.drawable.ic_flamingo_shuffle,
            active = shuffleEnabled,
            description = stringResource(R.string.player_shuffle_desc),
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(PlayerIntent.ToggleShuffleMode)
            }
        )
        SelectorButton(
            icon = R.drawable.ic_flamingo_queue,
            activeIcon = R.drawable.ic_flamingo_queue_on,
            active = viewMode == PlayerViewMode.QUEUE,
            description = stringResource(R.string.player_queue_title),
            tint = extended.onArtwork,
            onClick = {
                onInteract()
                onIntent(
                    if (viewMode == PlayerViewMode.QUEUE) {
                        PlayerIntent.SetViewMode(PlayerViewMode.COVER)
                    } else {
                        PlayerIntent.SetViewMode(PlayerViewMode.QUEUE)
                    }
                )
            }
        )
    }
}

@Composable
private fun SelectorButton(
    @DrawableRes icon: Int,
    active: Boolean,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    @DrawableRes activeIcon: Int = icon
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(PlayerSceneSpec.selectorHeight)
    ) {
        FlamingoIcon(
            res = if (active) activeIcon else icon,
            contentDescription = description,
            tint = tint.copy(alpha = if (active) 1f else 0.45f),
            size = PlayerSceneSpec.selectorIcon
        )
    }
}
