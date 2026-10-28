@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleChip
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
 * **按平台分组**：一屏里混着六家来源的十几个版本时，想找「网易云那份逐字的」只能一行行读前缀。
 * 顶部那排平台胶囊把列表筛成一家，数量写在胶囊上，一眼能看出哪家有货。
 *
 * 结果**逐家到达**（见 `LyricsRepository.findLyricsCandidates`）：面板先画出已经答完的平台，
 * 其余几家挂在下面的「正在查询」里。所以打开这个面板不需要等齐六家站点。
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
    @StringRes hintRes: Int = R.string.lyrics_candidates_hint,
    /** 还没回答的来源；空表示都问完了。 */
    pendingProviders: Set<LyricsProvider> = emptySet(),
    /** 查询失败的来源。与「查到了但是空的」分开说，前者值得再点一次。 */
    failedProviders: Set<LyricsProvider> = emptySet(),
    /**
     * 列表下方的次要动作（重新获取、界面设置）。
     *
     * 歌词页把「切换」提到了顶栏按钮上，其余两项就落在这里，免得再多一颗按钮 —— 顶栏那一行
     * 放不下第四颗。歌曲详情页传 null，它那边有自己的编辑入口。
     */
    footer: (@Composable () -> Unit)? = null
) {
    // 筛选只在本面板里有效：关掉再打开，用户要的是「这次各平台都有什么」重新开始看。
    var pickedProvider by remember { mutableStateOf<LyricsProvider?>(null) }

    val offered = LyricsProvider.entries.filter { provider ->
        candidates.any { it.provider == provider }
    }
    // 选中的平台这次没给出版本（换了首歌、或这一家本轮空手）时回到「全部」，
    // 不要停在一个筛不出东西的状态上。
    val active = pickedProvider?.takeIf { it in offered }
    val shown = if (active == null) candidates else candidates.filter { it.provider == active }

    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.lyrics_candidates_title),
            subtitle = stringResource(hintRes)
        )

        when {
            candidates.isEmpty() && isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }

            candidates.isEmpty() && pendingProviders.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.lyrics_candidates_empty),
                    style = AppleType.body,
                    color = AppleTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
                // 一个都没有的时候，「谁没查成」比「没有」更接近真相 —— 那是能重试的。
                SourceStatus(pendingProviders, failedProviders)
            }

            else -> {
                if (offered.size > 1) {
                    PlatformFilterRow(
                        candidates = candidates,
                        offered = offered,
                        active = active,
                        onPick = { pickedProvider = it }
                    )
                }
                SourceStatus(pendingProviders, failedProviders)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 平台胶囊与底部动作都要占高度，列表不能再按整屏留白，否则小屏上面板
                        // 会顶出可视区，最后几行永远点不到。
                        .heightIn(max = 360.dp)
                ) {
                    items(shown, key = { it.id }) { candidate ->
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
        footer?.invoke()
    }
}

/**
 * 平台筛选：一颗「全部」+ 每个有货的平台一颗，数量写在标签上。
 *
 * 只列有版本的平台 —— 一条「酷狗 0」占着一行却点不出东西，反而让人以为漏了什么。
 * 查询中与查询失败由下面的 [SourceStatus] 交代。
 */
@Composable
private fun PlatformFilterRow(
    candidates: List<LyricsCandidate>,
    offered: List<LyricsProvider>,
    active: LyricsProvider?,
    onPick: (LyricsProvider?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleChip(
            label = stringResource(
                R.string.lyrics_candidates_chip,
                stringResource(R.string.lyrics_candidates_all),
                candidates.size
            ),
            selected = active == null,
            onClick = { onPick(null) }
        )
        offered.forEach { provider ->
            AppleChip(
                label = stringResource(
                    R.string.lyrics_candidates_chip,
                    providerLabel(provider),
                    candidates.count { it.provider == provider }
                ),
                selected = active == provider,
                onClick = { onPick(provider) }
            )
        }
    }
}

/**
 * 「还在查谁 / 谁没查成」。
 *
 * 面板逐家长出来，没有这行字的话，先到的几家会让人以为这就是全部。两家都空时整行不画，
 * 面板保持安静。
 */
@Composable
private fun SourceStatus(
    pending: Set<LyricsProvider>,
    failed: Set<LyricsProvider>
) {
    val pendingLabels = pending.sortedBy { it.ordinal }.map { providerLabel(it) }
    val failedLabels = failed.sortedBy { it.ordinal }.map { providerLabel(it) }

    if (pendingLabels.isEmpty() && failedLabels.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        if (pendingLabels.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.lyrics_candidates_querying,
                    pendingLabels.joinToString("、")
                ),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )
        }
        if (failedLabels.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.lyrics_candidates_unavailable,
                    failedLabels.joinToString("、")
                ),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )
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
        LyricsProvider.LX -> R.string.settings_lyrics_provider_lx
    }
)

/** 逐字 / 行级，选择器与歌词信息面板共用。 */
@Composable
fun qualityLabel(wordByWord: Boolean): String = stringResource(
    if (wordByWord) R.string.lyrics_candidate_word_by_word else R.string.lyrics_candidate_line_level
)
