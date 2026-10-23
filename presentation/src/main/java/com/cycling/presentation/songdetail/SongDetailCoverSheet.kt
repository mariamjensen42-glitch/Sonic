@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.presentation.R

/**
 * 更换封面：相册选图、在线搜索候选、恢复默认封面，三件事放在同一张面板里。
 *
 * 打开面板即开始搜索，搜索完之前显示加载中 —— 用户点「更换封面」要的是候选图，不是一个还要
 * 再点一次的空面板。搜索为空时保留「重新搜索」与「从相册选择」两条退路，而不是只给一句失败。
 *
 * 封面是立即生效的（下载完就写进歌曲行），不像元数据那样要等页面保存：它是独立于标签的一次
 * 选择，没有「草稿」状态。
 */
@Composable
internal fun CoverPickerSheet(
    candidates: List<CoverCandidate>,
    isLoading: Boolean,
    isApplying: Boolean,
    hasCustomCover: Boolean,
    onPickLocal: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onSelectCandidate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.song_detail_cover_title),
            subtitle = stringResource(R.string.song_detail_cover_hint)
        )

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SheetTextAction(
                label = stringResource(R.string.song_detail_cover_pick_local),
                icon = SymbolIconName.Add,
                onClick = onPickLocal
            )
            SheetTextAction(
                label = stringResource(R.string.song_detail_cover_search),
                icon = SymbolIconName.Refresh,
                onClick = onRetry
            )
            if (hasCustomCover) {
                SheetTextAction(
                    label = stringResource(R.string.song_detail_cover_clear),
                    icon = SymbolIconName.Restore,
                    onClick = onClear
                )
            }
        }

        when {
            // Applying wins over searching: the image is already chosen and the spinner is
            // telling the user why the panel has not closed yet.
            isApplying -> SheetStatus(text = stringResource(R.string.song_detail_cover_applying))

            isLoading -> SheetStatus(text = stringResource(R.string.song_detail_cover_searching))

            candidates.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.song_detail_cover_empty),
                    style = AppleType.body,
                    color = AppleTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.song_detail_cover_empty_hint),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                items(candidates, key = { it.id }) { candidate ->
                    CoverCandidateItem(
                        candidate = candidate,
                        enabled = !isApplying,
                        onClick = { onSelectCandidate(candidate.id) }
                    )
                }
            }
        }
    }
}

/** 面板里的文字动作，key 色 17sp + 前置图标。 */
@Composable
private fun SheetTextAction(
    label: String,
    icon: SymbolIconName,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = null,
            filled = true,
            tint = AppleTheme.colors.key,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = AppleType.body, color = AppleTheme.colors.key)
    }
}

@Composable
private fun SheetStatus(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator()
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
    }
}

@Composable
private fun CoverCandidateItem(
    candidate: CoverCandidate,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AppleSheetRow(
        title = candidate.album ?: candidate.title.orEmpty(),
        subtitle = candidateSupportingText(candidate),
        onClick = { if (enabled) onClick() },
        leading = {
            M3Artwork(
                artwork = candidate.imageUrl,
                placeholderIcon = SymbolIconName.Album,
                shape = RoundedCornerShape(AppleDims.cornerS),
                modifier = Modifier.size(48.dp)
            )
        },
        trailing = {
            if (candidate.isBestMatch) {
                Text(
                    text = stringResource(R.string.song_detail_cover_best_match),
                    style = AppleType.caption1Emphasized,
                    color = AppleTheme.colors.key
                )
            }
        }
    )
}

@Composable
private fun candidateSupportingText(candidate: CoverCandidate): String {
    val provider = coverSourceLabel(candidate.source)
    val artist = candidate.artist.orEmpty()
    return if (artist.isBlank()) provider else "$provider · $artist"
}

/** 目录来源的名字；封面选择器与在线元数据面板共用。 */
@Composable
internal fun coverSourceLabel(source: CoverSource): String = stringResource(
    when (source) {
        CoverSource.NETEASE -> R.string.song_detail_cover_source_netease
        CoverSource.QQ_MUSIC -> R.string.song_detail_cover_source_qq
        CoverSource.KUGOU -> R.string.song_detail_cover_source_kugou
        CoverSource.ITUNES -> R.string.song_detail_cover_source_itunes
    }
)
