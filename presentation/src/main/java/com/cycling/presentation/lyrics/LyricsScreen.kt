@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.lyrics

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.sonDynamicBackdrop
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_LYRICS_TEXT_SIZE_SP
import com.cycling.presentation.R
import com.cycling.presentation.components.LyricsCandidatesSheet
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.lyrics.components.KaraokeLyricsView
import kotlin.math.roundToInt
import com.cycling.presentation.player.LYRICS_OFFSET_STEP_MS
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerUiState
import java.util.Locale

/** 帧间隔超过这个值就认为掉帧严重（例如刚从后台回来），插值不再外推 */
private const val StallThresholdNanos = 250_000_000L

/** 辉光的底色透明度：只做"发光"，不该长出一圈可见的描边。 */
private const val LyricsGlowAlpha = 0.45f

/** 辉光半径按字号取倍数，大字号那档才不会发光过窄。 */
private const val LyricsGlowRadiusRatio = 1.1f

/**
 * 歌词页的动态色面强度。
 *
 * 这一屏是阅读面：色面整屏铺开会让文字落色（对比度表按安静 surface 算的），所以压掉两成多，
 * 让它只作为「在动」的背景存在。数值与播放页的 hero 强度一起在 ADR-0005 里定。
 */
private const val LyricsBackdropDim = 0.22f

@Composable
fun LyricsScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val positionRef = remember { intArrayOf(0) }
    val lineTrigger = remember { mutableIntStateOf(0) }
    val latestPosition by rememberUpdatedState(uiState.playbackPosition.toInt())
    val latestIsPlaying by rememberUpdatedState(uiState.isPlaying)
    val latestLines by rememberUpdatedState(uiState.lyrics?.lines ?: emptyList())
    val latestOffset by rememberUpdatedState(uiState.lyricsOffsetMs)
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // 字号与辉光都只改样式，不改歌词数据：切档位时不需要重新解析、重新测量整份歌词。
    val extended = LocalSonExtendedColors.current
    val density = LocalDensity.current
    val normalBase = MaterialTheme.typography.headlineMedium
    val accompanimentBase = MaterialTheme.typography.titleMedium
    val textSizeSp = uiState.lyricsTextSizeSp
    // 伴奏行没有自己的字号设置：按主题里两者原始字号的比值跟着缩放，比例关系保持不变。
    val accompanimentRatio =
        if (normalBase.fontSize.isSpecified && accompanimentBase.fontSize.isSpecified &&
            normalBase.fontSize.value > 0f
        ) {
            accompanimentBase.fontSize.value / normalBase.fontSize.value
        } else {
            1f
        }
    val accompanimentSizeSp = textSizeSp * accompanimentRatio
    val glowColor = extended.lyricsInactive.copy(alpha = LyricsGlowAlpha)
    val glowRadiusPx = with(density) { (textSizeSp * LyricsGlowRadiusRatio).sp.toPx() }
    val normalStyle = remember(normalBase, textSizeSp, uiState.lyricsGlowEnabled, glowColor, glowRadiusPx) {
        normalBase
            .atSize(textSizeSp.toFloat())
            .glowIf(uiState.lyricsGlowEnabled, glowColor, glowRadiusPx)
    }
    val accompanimentStyle = remember(
        accompanimentBase, accompanimentSizeSp, uiState.lyricsGlowEnabled, glowColor, glowRadiusPx
    ) {
        accompanimentBase
            .atSize(accompanimentSizeSp)
            .glowIf(uiState.lyricsGlowEnabled, glowColor, glowRadiusPx)
    }

    // 绘制阶段读时间：lineTrigger 只用于换行时触发重绘，位置取插值后的平滑值。
    // 偏移在这里减去而不是去改歌词本身 —— 它只决定"此刻该高亮哪一句"，改数据就得重新解析、
    // 重新缓存，而偏移是随时可调的。
    val currentPositionProvider = remember { { lineTrigger.intValue; positionRef[0] - latestOffset.toInt() } }

    // 播放位置每秒才上报一次，这里用帧时钟在两次上报之间插值，
    // 使歌词进度逐帧连续推进，而不是每秒跳一次
    LaunchedEffect(Unit) {
        var basePosition = Int.MIN_VALUE
        var baseNanos = 0L
        var lastFrameNanos = 0L
        var smoothPosition = 0
        var lastLineIndex = Int.MIN_VALUE
        while (true) {
            withFrameNanos { frameNanos ->
                // 掉帧太严重（比如刚从后台回来）时不外推，直接以上报值为准，
                // 否则插值会冲到播放位置前面去
                val stalled = frameNanos - lastFrameNanos > StallThresholdNanos
                lastFrameNanos = frameNanos

                val reported = latestPosition
                if (reported != basePosition || stalled) {
                    // 新的一次上报（或 seek 跳变）：重置插值基准
                    basePosition = reported
                    baseNanos = frameNanos
                    smoothPosition = reported
                } else if (latestIsPlaying) {
                    smoothPosition =
                        basePosition + ((frameNanos - baseNanos) / 1_000_000L).toInt()
                }
                // 暂停时不回退，保持暂停前推进到的位置
                positionRef[0] = smoothPosition

                val currentLineIndex =
                    latestLines.indexOfLast { it.start <= smoothPosition - latestOffset.toInt() }
                if (currentLineIndex != lastLineIndex) {
                    lastLineIndex = currentLineIndex
                    lineTrigger.intValue = currentLineIndex
                }
            }
        }
    }

    // 页面底色即 surface（已由壁纸动态取色着色）；动态色面铺在 padding 之外，铺满整屏。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .sonDynamicBackdrop(playing = uiState.isPlaying, dim = LyricsBackdropDim)
            .statusBarsPadding()
    ) {
        val lyrics = uiState.lyrics
        if (lyrics != null) {
            // 列表直接从「当前播放到的那一行」开始铺：不要先渲染第 0 行再滚/跳到当前行。
            // 跳那一帧虽然不再做动画，但仍会白组合一屏不在播放位置的行（每行都要做音节测量、
            // 渐隐遮罩），进歌词页的卡顿主要就来自这里。
            val initialLineIndex = remember(lyrics) {
                val time = uiState.playbackPosition.toInt() - uiState.lyricsOffsetMs.toInt()
                val lines = lyrics.lines
                if (lines.isEmpty()) {
                    0
                } else {
                    lines.indexOfLast { it.start <= time }.coerceIn(0, lines.lastIndex)
                }
            }
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = initialLineIndex
            )
            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = currentPositionProvider,
                isPlaying = uiState.isPlaying,
                onLineClicked = { line ->
                    onIntent(PlayerIntent.SeekTo(line.start.toLong()))
                },
                onLinePressed = { showOffsetDialog = true },
                normalLineTextStyle = normalStyle,
                accompanimentLineTextStyle = accompanimentStyle,
                useBlurEffect = uiState.lyricsBlurEnabled,
                alignCenter = uiState.lyricsAlignCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = SonSpacing.huge, bottom = SonLayout.bottomBarClearance)
            )
        } else if (uiState.isLoadingLyrics) {
            // sidecar / 缓存里的歌词通常瞬间就到，只有真去网络查时才需要转圈。
            SonLoadingState()
        } else if (uiState.currentSong != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.player_lyrics_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CenterAlignedTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = {
                Text(
                    text = uiState.currentSong?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    M3SymbolIcon(
                        name = SymbolIconName.ArrowBack,
                        filled = true,
                        contentDescription = stringResource(R.string.lyrics_back_desc)
                    )
                }
            },
            actions = {
                // 下载期间把按钮换成加载指示器：网络查询要几秒，点了没反应会让人以为按钮坏了
                if (uiState.isLoadingLyrics) {
                    LoadingIndicator(
                        modifier = Modifier
                            .padding(end = SonSpacing.medium)
                            .size(SonSizes.iconMedium)
                    )
                } else {
                    // 本地 .lrc 或内嵌标签是用户自己的，换一份下载的也会被它们盖掉，所以这时不提供
                    // 「选择歌词」—— 点了不生效比没有这个按钮更糟。
                    // 用户在歌曲详情页手写的歌词同理，而且连「重新获取」也一并收起：它同样是
                    // 一份会被覆盖的下载，要改就该在写它的地方改。
                    if (uiState.lyricsSource != LyricsSource.USER) {
                        if (uiState.lyricsSource == LyricsSource.ONLINE || uiState.lyricsSource == LyricsSource.NONE) {
                            IconButton(onClick = { onIntent(PlayerIntent.OpenLyricsCandidates) }) {
                                M3SymbolIcon(
                                    name = SymbolIconName.QueueMusic,
                                    filled = true,
                                    contentDescription = stringResource(R.string.lyrics_candidates_desc)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onIntent(PlayerIntent.RefreshLyrics) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            M3SymbolIcon(
                                name = SymbolIconName.Refresh,
                                filled = true,
                                contentDescription = stringResource(R.string.lyrics_fetch_desc)
                            )
                        }
                    }
                }
                // 字号 / 对齐 / 辉光 / 模糊就在这一页调：听歌时觉得字小了，跳去设置页再回来
                // 会打断正在对的那一句。
                IconButton(onClick = { showSettingsSheet = true }) {
                    M3SymbolIcon(
                        name = SymbolIconName.Tune,
                        filled = true,
                        contentDescription = stringResource(R.string.lyrics_settings_desc)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }

    if (showOffsetDialog) {
        LyricsOffsetDialog(
            offsetMs = uiState.lyricsOffsetMs,
            onAdjust = { onIntent(PlayerIntent.AdjustLyricsOffset(it)) },
            onReset = { onIntent(PlayerIntent.ResetLyricsOffset) },
            onDismiss = { showOffsetDialog = false }
        )
    }

    if (uiState.showLyricsCandidates) {
        LyricsCandidatesSheet(
            candidates = uiState.lyricsCandidates,
            isLoading = uiState.isLoadingLyricsCandidates,
            onSelect = { onIntent(PlayerIntent.SelectLyricsCandidate(it)) },
            onDismiss = { onIntent(PlayerIntent.DismissLyricsCandidates) }
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LyricsSettingsSheet(
                textSizeSp = uiState.lyricsTextSizeSp,
                alignCenter = uiState.lyricsAlignCenter,
                glowEnabled = uiState.lyricsGlowEnabled,
                blurEnabled = uiState.lyricsBlurEnabled,
                onTextSizeChange = { onIntent(PlayerIntent.SetLyricsTextSizeSp(it)) },
                onAlignCenterChange = { onIntent(PlayerIntent.SetLyricsAlignCenter(it)) },
                onGlowChange = { onIntent(PlayerIntent.SetLyricsGlowEnabled(it)) },
                onBlurChange = { onIntent(PlayerIntent.SetLyricsBlurEnabled(it)) }
            )
        }
    }
}

/**
 * 歌词界面设置面板。
 *
 * 放在歌词页而不是设置页：这几项都是"看着这一句觉得不对"时才想起来调的，
 * 跳出去再回来会打断正在看的位置。面板不遮住正在唱的那几行（底部弹出），改完立刻能看到。
 */
@Composable
private fun LyricsSettingsSheet(
    textSizeSp: Int,
    alignCenter: Boolean,
    glowEnabled: Boolean,
    blurEnabled: Boolean,
    onTextSizeChange: (Int) -> Unit,
    onAlignCenterChange: (Boolean) -> Unit,
    onGlowChange: (Boolean) -> Unit,
    onBlurChange: (Boolean) -> Unit
) {
    // 拖动只改本地值，松手才写 DataStore：跟着手指一格格落盘会明显掉帧。
    var sliderValue by remember(textSizeSp) { mutableFloatStateOf(textSizeSp.toFloat()) }
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // 拖到一半直接把面板划走时 onValueChangeFinished 不会来，走的时候补一次提交。
    val latestSliderValue by rememberUpdatedState(sliderValue)
    DisposableEffect(Unit) {
        onDispose {
            val committed = latestSliderValue.roundToInt()
            if (committed != textSizeSp) onTextSizeChange(committed)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonSpacing.medium)
            .padding(bottom = SonSpacing.extraLarge)
    ) {
        Text(
            text = stringResource(R.string.lyrics_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = SonSpacing.small)
        )

        // 字号：12–48sp，与 Salt Player 同范围
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.lyrics_settings_text_size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.lyrics_settings_text_size_value, sliderValue.roundToInt()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = MIN_LYRICS_TEXT_SIZE_SP.toFloat()..MAX_LYRICS_TEXT_SIZE_SP.toFloat(),
            steps = MAX_LYRICS_TEXT_SIZE_SP - MIN_LYRICS_TEXT_SIZE_SP - 1,
            onValueChangeFinished = { onTextSizeChange(sliderValue.roundToInt()) }
        )

        SettingsSheetSwitch(
            title = stringResource(R.string.lyrics_settings_align_center),
            supporting = stringResource(R.string.lyrics_settings_align_center_subtitle),
            checked = alignCenter,
            onCheckedChange = onAlignCenterChange
        )
        SettingsSheetSwitch(
            title = stringResource(R.string.lyrics_settings_glow),
            supporting = stringResource(R.string.lyrics_settings_glow_subtitle),
            checked = glowEnabled,
            onCheckedChange = onGlowChange
        )
        SettingsSheetSwitch(
            title = stringResource(R.string.lyrics_settings_blur),
            // 虚化走 RenderEffect，Android 12 以下没有这条路径 —— 说明原因并置灰，而不是悄悄失效。
            supporting = if (blurSupported) {
                stringResource(R.string.lyrics_settings_blur_subtitle)
            } else {
                stringResource(R.string.lyrics_settings_blur_unsupported)
            },
            checked = blurEnabled && blurSupported,
            enabled = blurSupported,
            onCheckedChange = onBlurChange
        )
    }
}

@Composable
private fun SettingsSheetSwitch(
    title: String,
    supporting: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked && enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * 歌词对轴。
 *
 * 入口是长按歌词行 —— 对轴这件事只有在"听着觉得不对"的时候才有人找，所以不需要一直在屏幕上占
 * 一个按钮，但也不需要藏进设置里：那正是听歌时不想离开的地方。
 */
@Composable
private fun LyricsOffsetDialog(
    offsetMs: Long,
    onAdjust: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.lyrics_offset_title))
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatOffset(offsetMs),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(SonSpacing.small))
                Text(
                    text = stringResource(R.string.lyrics_offset_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(SonSpacing.mediumSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                    TextButton(onClick = { onAdjust(-LYRICS_OFFSET_STEP_MS) }) {
                        Text(stringResource(R.string.lyrics_offset_earlier))
                    }
                    TextButton(onClick = { onAdjust(LYRICS_OFFSET_STEP_MS) }) {
                        Text(stringResource(R.string.lyrics_offset_later))
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.lyrics_offset_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_dialog_done))
                }
            }
        }
    )
}

private fun formatOffset(offsetMs: Long): String =
    if (offsetMs == 0L) "0.0s"
    else String.format(Locale.getDefault(), "%+.1fs", offsetMs / 1000f)

/** 按档位缩放主题排版 token。字号/行高未指定时保持原样，避免乘出 NaN。 */
private fun TextStyle.atSize(sizeSp: Float): TextStyle {
    if (!fontSize.isSpecified || fontSize.value <= 0f) return this
    val ratio = sizeSp / fontSize.value
    return copy(
        fontSize = sizeSp.sp,
        lineHeight = if (lineHeight.isSpecified) (lineHeight.value * ratio).sp else lineHeight
    )
}

/** 辉光：用一层同色阴影贴在字形后面，颜色取"非当前行"色，点亮后自然退到高亮色之下。 */
private fun TextStyle.glowIf(enabled: Boolean, color: Color, radiusPx: Float): TextStyle =
    if (enabled) {
        copy(shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = radiusPx))
    } else {
        this
    }


