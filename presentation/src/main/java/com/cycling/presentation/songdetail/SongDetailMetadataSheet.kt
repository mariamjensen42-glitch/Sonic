@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

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
import com.cycling.presentation.R

/**
 * 在线元数据补全。
 *
 * 与封面选择器共用同一批在线搜索结果（`CoverCandidate` 现在同时带封面与文本字段），区别只在
 * 选中之后做什么：封面面板立刻下载图片，这里只把标题/艺术家/专辑/年份/流派/音轨号等填进编辑器，
 * 等页面保存才落库 —— 和歌词版本选择、LRC 元数据写回是同一条「编辑页的东西保存才生效」规则。
 *
 * 来源只报了什么就填什么：没报的字段保持用户已经填好的值，不会被空值覆盖。
 */
@Composable
internal fun MetadataCandidatesSheet(
    candidates: List<CoverCandidate>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
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
                text = stringResource(R.string.song_detail_metadata_online_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = SonSpacing.large, vertical = SonSpacing.small)
            )
            Text(
                text = stringResource(R.string.song_detail_metadata_online_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SonSpacing.large)
            )

            Row(
                modifier = Modifier.padding(horizontal = SonSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            when {
                isLoading -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SonSpacing.extraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoadingIndicator()
                    Spacer(Modifier.height(SonSpacing.small))
                    Text(
                        text = stringResource(R.string.song_detail_metadata_online_searching),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                candidates.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SonSpacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.song_detail_metadata_online_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(SonSpacing.extraSmall))
                    Text(
                        text = stringResource(R.string.song_detail_metadata_online_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(candidates, key = { it.id }) { candidate ->
                        MetadataCandidateItem(
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
private fun MetadataCandidateItem(
    candidate: CoverCandidate,
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
                text = candidate.title ?: candidate.album.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                val artistAlbum = listOfNotNull(candidate.artist, candidate.album)
                    .joinToString(" · ")
                if (artistAlbum.isNotEmpty()) {
                    Text(text = artistAlbum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(text = candidateFacts(candidate), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
        onClick = onClick
    )
}

/** 年份 · 音轨 · 流派 · 来源：一行说清这条命中带了哪些可填的字段。 */
@Composable
private fun candidateFacts(candidate: CoverCandidate): String {
    val track = candidate.trackNumber
    val trackLabel = if (track != null) {
        stringResource(R.string.song_detail_metadata_online_track, track)
    } else {
        null
    }
    return listOfNotNull(
        candidate.year,
        trackLabel,
        candidate.genre,
        coverSourceLabel(candidate.source)
    ).joinToString(" · ")
}
