@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.lyrics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsSource
import com.cycling.presentation.R
import com.cycling.presentation.lyrics.components.KaraokeLyricsView
import com.cycling.presentation.player.LYRICS_OFFSET_STEP_MS
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerUiState
import java.util.Locale

/** 帧间隔超过这个值就认为掉帧严重（例如刚从后台回来），插值不再外推 */
private const val StallThresholdNanos = 250_000_000L

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

    // 页面底色即 surface（已由封面种子色着色），不再叠静态灰带。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        val lyrics = uiState.lyrics
        if (lyrics != null) {
            val listState = rememberLazyListState()
            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = currentPositionProvider,
                onLineClicked = { line ->
                    onIntent(PlayerIntent.SeekTo(line.start.toLong()))
                },
                onLinePressed = { showOffsetDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = SonSpacing.huge, bottom = SonLayout.bottomBarClearance)
            )
        } else if (uiState.isLoadingLyrics) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
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

/**
 * 歌词版本选择。
 *
 * 在线来源会把同一首歌上架很多次，不同版本带的文档还不一样 —— 有的有逐字时间轴，有的只有整行，
 * 有的又是另一版母带、对轴差半秒 —— 所以自动挑出来的那一份只是默认值，不是答案。这里把各版本摊开
 * 让用户自己定，选中的那份写进缓存，下次播放直接用它。
 *
 * 每次打开都重新查一遍而不是复用上次的结果：用户点这个按钮，多半就是因为自动挑的那份不对，那么
 * 刚才那次自动挑选的结果正是他想绕开的东西。
 */
@Composable
private fun LyricsCandidatesSheet(
    candidates: List<LyricsCandidate>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SonSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.lyrics_candidates_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = SonSpacing.large, vertical = SonSpacing.small)
            )
            Text(
                text = stringResource(R.string.lyrics_candidates_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SonSpacing.large)
            )
            Spacer(Modifier.height(SonSpacing.small))

            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SonSpacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }

                candidates.isEmpty() -> Text(
                    text = stringResource(R.string.lyrics_candidates_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SonSpacing.large)
                )

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(candidates, key = { it.id }) { candidate ->
                        LyricsCandidateItem(
                            candidate = candidate,
                            onClick = { onSelect(candidate.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsCandidateItem(
    candidate: LyricsCandidate,
    onClick: () -> Unit
) {
    ListItem(
        // 可点击的 ListItem 里，主标题这一格叫 content 而不是 headlineContent。
        content = {
            Text(
                text = "${providerLabel(candidate.provider)} · ${qualityLabel(candidate.wordByWord)}"
            )
        },
        // 版本之间真正的区别在专辑名（同一首歌的不同上架），拿不到才退回歌词首句。
        supportingContent = {
            val detail = candidate.label ?: candidate.preview
            if (detail != null) {
                Text(text = detail, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = {
            if (candidate.isCurrent) {
                Text(
                    text = stringResource(R.string.lyrics_candidate_current),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun providerLabel(provider: LyricsProvider): String = stringResource(
    when (provider) {
        LyricsProvider.NETEASE -> R.string.settings_lyrics_provider_netease
        LyricsProvider.LRCLIB -> R.string.settings_lyrics_provider_lrclib
    }
)

@Composable
private fun qualityLabel(wordByWord: Boolean): String = stringResource(
    if (wordByWord) R.string.lyrics_candidate_word_by_word else R.string.lyrics_candidate_line_level
)
