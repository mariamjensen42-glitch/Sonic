@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsProvider
import com.cycling.presentation.R

/**
 * 歌词版本选择。
 *
 * 在线来源会把同一首歌上架很多次，不同版本带的文档还不一样 —— 有的有逐字时间轴，有的只有整行，
 * 有的又是另一版母带、对轴差半秒 —— 所以自动挑出来的那一份只是默认值，不是答案。播放页与歌曲
 * 详情页都提供这个面板：播放页选中即替换当前歌词，详情页选中则是填入编辑器、保存后才生效。
 *
 * 每次打开都重新查一遍而不是复用上次的结果：用户点这个按钮，多半就是因为自动挑的那份不对，那么
 * 刚才那次自动挑选的结果正是他想绕开的东西。
 */
@Composable
fun LyricsCandidatesSheet(
    candidates: List<LyricsCandidate>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    @StringRes hintRes: Int = R.string.lyrics_candidates_hint
) {
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.lyrics_candidates_title),
            subtitle = stringResource(hintRes)
        )

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }

            candidates.isEmpty() -> Text(
                text = stringResource(R.string.lyrics_candidates_empty),
                style = AppleType.body,
                color = AppleTheme.colors.secondaryLabel,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                items(candidates, key = { it.id }) { candidate ->
                    AppleSheetRow(
                        title = "${providerLabel(candidate.provider)} · " +
                            qualityLabel(candidate.wordByWord),
                        // 版本之间真正的区别在专辑名（同一首歌的不同上架），拿不到才退回歌词首句。
                        subtitle = candidate.label ?: candidate.preview,
                        onClick = { onSelect(candidate.id) },
                        trailing = {
                            if (candidate.isCurrent) {
                                Text(
                                    text = stringResource(R.string.lyrics_candidate_current),
                                    style = AppleType.caption1Emphasized,
                                    color = AppleTheme.colors.key
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 在线歌词来源的名字，选择器与歌词信息面板共用。 */
@Composable
fun providerLabel(provider: LyricsProvider): String = stringResource(
    when (provider) {
        LyricsProvider.AMLL -> R.string.settings_lyrics_provider_amll
        LyricsProvider.NETEASE -> R.string.settings_lyrics_provider_netease
        LyricsProvider.QQ_MUSIC -> R.string.settings_lyrics_provider_qq_music
        LyricsProvider.KUGOU -> R.string.settings_lyrics_provider_kugou
        LyricsProvider.LRCLIB -> R.string.settings_lyrics_provider_lrclib
    }
)

/** 逐字 / 行级，选择器与歌词信息面板共用。 */
@Composable
fun qualityLabel(wordByWord: Boolean): String = stringResource(
    if (wordByWord) R.string.lyrics_candidate_word_by_word else R.string.lyrics_candidate_line_level
)
