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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleMenuRow
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.sonDynamicBackdrop
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_LYRICS_TEXT_SIZE_SP
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_LYRICS_TEXT_SIZE_SP
import com.cycling.presentation.R
import com.cycling.presentation.components.LyricsCandidatesSheet
import com.cycling.presentation.player.LYRICS_OFFSET_STEP_MS
import com.cycling.presentation.player.NowPlayingBar
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerSceneSpec
import com.cycling.presentation.player.PlayerUiState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 歌词页的动态色面强度。
 *
 * 这一屏是阅读面：色面整屏铺开会让文字落色（对比度表按安静 surface 算的），所以压掉两成多，
 * 让它只作为「在动」的背景存在。数值与播放页的 hero 强度一起在 ADR-0005 里定。
 */
private const val LyricsBackdropDim = 0.22f

/**
 * 独立的歌词路由。
 *
 * 播放页内部已经有一页歌词（Apple Music 的三页结构），这个路由是给「从别处直接进歌词」用的
 * —— 迷你播放器、歌曲详情页的歌词入口。版式与播放页那一页共用 [LyricsPage]，
 * 区别只是这里顶部多一个返回键、歌词设置收在「更多」里。
 */
@Composable
fun LyricsScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // 页面底色即 surface（已由壁纸动态取色着色）；动态色面铺满整屏。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleTheme.colors.background)
            .sonDynamicBackdrop(playing = uiState.isPlaying, dim = LyricsBackdropDim)
            .statusBarsPadding()
    ) {
        LyricsPage(
            uiState = uiState,
            onIntent = onIntent,
            onLineLongPressed = { showOffsetDialog = true },
            headerInset = PlayerSceneSpec.headerHeight,
            header = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        // 这里保留 Material Symbols 的返回箭头：Flamingo 那支 `Back` 是给
                        // 32×55dp 整页翻页按钮画的（缩到 20dp 仍有 4dp 臂厚，48dp 的顶栏压不住），
                        // 拿它当 24dp 的导航图标会又粗又高。顶栏的收藏 / 更多已经是 Flamingo 了。
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.lyrics_back_desc)
                        )
                    }
                    NowPlayingBar(
                        uiState = uiState,
                        onIntent = onIntent,
                        onArtworkClick = onNavigateBack,
                        // 一键直达在线歌词选择：这颗按钮的全部职责就是它，不再套一层面板。
                        // 原来那层面板里的「重新获取」与「界面设置」挪到了选择列表的底部。
                        onOverflow = { onIntent(PlayerIntent.OpenLyricsCandidates) },
                        showTranslationToggle = true,
                        overflowIcon = R.drawable.ic_flamingo_lyrics,
                        overflowDescription = stringResource(R.string.lyrics_pick_online),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
            pendingProviders = uiState.lyricsCandidatesPending,
            failedProviders = uiState.lyricsCandidatesFailed,
            onSelect = { onIntent(PlayerIntent.SelectLyricsCandidate(it)) },
            onDismiss = { onIntent(PlayerIntent.DismissLyricsCandidates) },
            footer = {
                // 当前用的是哪一份 —— 分不清「自己写的」和「下载的」，就无从判断该不该换。
                AppleSheetRow(
                    title = stringResource(
                        R.string.lyrics_panel_source,
                        lyricsSourceLabel(uiState.lyricsSource)
                    )
                )
                // 手写歌词排在 resolveLyrics 最前面，换不掉，这一档不给「重新获取」。
                if (uiState.lyricsSource != LyricsSource.USER) {
                    SheetAction(
                        icon = SymbolIconName.Refresh,
                        title = stringResource(R.string.lyrics_fetch_desc)
                    ) {
                        onIntent(PlayerIntent.DismissLyricsCandidates)
                        onIntent(PlayerIntent.RefreshLyrics)
                    }
                }
                SheetAction(
                    icon = SymbolIconName.Tune,
                    title = stringResource(R.string.lyrics_settings_desc)
                ) {
                    onIntent(PlayerIntent.DismissLyricsCandidates)
                    showSettingsSheet = true
                }
            }
        )
    }

    if (showSettingsSheet) {
        AppleSheet(onDismiss = { showSettingsSheet = false }) {
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
 * 「更多」面板里的一行。
 *
 * 这里的图标仍是 Material Symbols（[SymbolIconName]）：Flamingo 那套资源里没有「刷新」与
 * 「调节」两个图形，凑不出对应形状，硬换只会让同一个菜单里出现两种笔触。顶栏的收藏 / 更多
 * 与播放页共用 `NowPlayingBar`，已经跟着一起换成了 Flamingo 的图形语言。
 */

/** 歌词来源的中文名，面板副标题用。文案与歌曲详情页共用一套。 */
@Composable
private fun lyricsSourceLabel(source: LyricsSource): String = stringResource(
    when (source) {
        LyricsSource.USER -> R.string.song_detail_lyrics_source_user
        LyricsSource.EMBEDDED -> R.string.song_detail_lyrics_source_embedded
        LyricsSource.FILE -> R.string.song_detail_lyrics_source_file
        LyricsSource.ONLINE -> R.string.song_detail_lyrics_source_online
        LyricsSource.NONE -> R.string.song_detail_lyrics_source_none
    }
)

@Composable
private fun SheetAction(
    icon: SymbolIconName,
    title: String,
    onClick: () -> Unit
) {
    AppleMenuRow(
        icon = icon,
        label = title,
        onClick = onClick,
        showChevron = false
    )
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
            style = AppleType.title2Emphasized,
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
                style = AppleType.body,
                color = AppleTheme.colors.label,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.lyrics_settings_text_size_value, sliderValue.roundToInt()),
                style = AppleType.caption1Emphasized,
                color = AppleTheme.colors.key
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
    AppleSheetRow(
        title = title,
        subtitle = supporting,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailing = {
            Switch(
                checked = checked && enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        }
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
    AppleAlert(
        title = stringResource(R.string.lyrics_offset_title),
        confirmLabel = stringResource(R.string.settings_dialog_done),
        onConfirm = onDismiss,
        onDismiss = onDismiss
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatOffset(offsetMs),
                style = AppleType.title2Emphasized,
                color = AppleTheme.colors.label
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lyrics_offset_hint),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppleChip(
                    label = stringResource(R.string.lyrics_offset_earlier),
                    selected = false,
                    onClick = { onAdjust(-LYRICS_OFFSET_STEP_MS) }
                )
                AppleChip(
                    label = stringResource(R.string.lyrics_offset_later),
                    selected = false,
                    onClick = { onAdjust(LYRICS_OFFSET_STEP_MS) }
                )
            }
            Spacer(Modifier.height(4.dp))
            AppleTextAction(
                label = stringResource(R.string.lyrics_offset_reset),
                onClick = onReset
            )
        }
    }
}

private fun formatOffset(offsetMs: Long): String =
    if (offsetMs == 0L) "0.0s"
    else String.format(Locale.getDefault(), "%+.1fs", offsetMs / 1000f)
