@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
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
                text = stringResource(R.string.song_detail_cover_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = SonSpacing.large, vertical = SonSpacing.small)
            )
            Text(
                text = stringResource(R.string.song_detail_cover_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SonSpacing.large)
            )

            Row(
                modifier = Modifier.padding(horizontal = SonSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPickLocal) {
                    M3SymbolIcon(
                        name = SymbolIconName.Add,
                        filled = true,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon)
                    )
                    Spacer(Modifier.width(SonSpacing.extraSmall))
                    Text(text = stringResource(R.string.song_detail_cover_pick_local))
                }
                TextButton(onClick = onRetry) {
                    M3SymbolIcon(
                        name = SymbolIconName.Refresh,
                        filled = true,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon)
                    )
                    Spacer(Modifier.width(SonSpacing.extraSmall))
                    Text(text = stringResource(R.string.song_detail_cover_search))
                }
            }

            if (hasCustomCover) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.padding(horizontal = SonSpacing.small)
                ) {
                    M3SymbolIcon(
                        name = SymbolIconName.Restore,
                        filled = true,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon)
                    )
                    Spacer(Modifier.width(SonSpacing.extraSmall))
                    Text(text = stringResource(R.string.song_detail_cover_clear))
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
                        .padding(SonSpacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.song_detail_cover_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(SonSpacing.extraSmall))
                    Text(
                        text = stringResource(R.string.song_detail_cover_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun SheetStatus(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator()
        Spacer(Modifier.height(SonSpacing.small))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoverCandidateItem(
    candidate: CoverCandidate,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            M3Artwork(
                artwork = candidate.imageUrl,
                placeholderIcon = SymbolIconName.Album,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnailLarge)
            )
        },
        // 可点击的 ListItem 里，主内容槽叫 content。
        content = {
            Text(
                text = candidate.album ?: candidate.title.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(text = candidateSupportingText(candidate))
        },
        trailingContent = {
            if (candidate.isBestMatch) {
                Text(
                    text = stringResource(R.string.song_detail_cover_best_match),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = { if (enabled) onClick() }
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
