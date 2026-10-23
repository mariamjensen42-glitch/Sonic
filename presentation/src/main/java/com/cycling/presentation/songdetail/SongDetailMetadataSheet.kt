@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.song_detail_metadata_online_title),
            subtitle = stringResource(R.string.song_detail_metadata_online_hint)
        )

        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            SheetSearchAction(
                label = stringResource(R.string.song_detail_cover_search),
                onClick = onRetry
            )
        }

        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.song_detail_metadata_online_searching),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
            }

            candidates.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.song_detail_metadata_online_empty),
                    style = AppleType.body,
                    color = AppleTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.song_detail_metadata_online_empty_hint),
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
                    MetadataCandidateItem(
                        candidate = candidate,
                        onClick = { onSelect(candidate.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSearchAction(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = SymbolIconName.Refresh,
            contentDescription = null,
            filled = true,
            tint = AppleTheme.colors.key,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = label, style = AppleType.body, color = AppleTheme.colors.key)
    }
}

@Composable
private fun MetadataCandidateItem(
    candidate: CoverCandidate,
    onClick: () -> Unit
) {
    AppleSheetRow(
        title = candidate.title ?: candidate.album.orEmpty(),
        subtitle = buildString {
            val artistAlbum = listOfNotNull(candidate.artist, candidate.album)
                .joinToString(" · ")
            if (artistAlbum.isNotEmpty()) append(artistAlbum)
            val facts = candidateFacts(candidate)
            if (facts.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(facts)
            }
        },
        onClick = onClick,
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
