@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.SongListeningStat
import com.cycling.presentation.R

/**
 * 听歌足迹里的通用歌曲行：名次 → 封面 → 标题/歌手 → 成绩，下面一条占比条。
 *
 * 统计页的常听榜单与当天详情共用它，保证同一种"成绩行"在两处看起来完全一样。
 * 名次为空时不画数字槽（当天详情按次数排但没有"榜单"的语境）。
 */
@Composable
fun ListeningSongRow(
    song: SongListeningStat,
    valueText: String,
    rank: Int? = null,
    share: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = SonLayout.screenMargin,
                vertical = SonSpacing.mediumSmall
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rank != null) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = if (rank <= TOP_RANK_COUNT) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(RankSlotWidth)
                )
            }
            M3Artwork(
                artwork = song.coverUrl,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
            Spacer(modifier = Modifier.width(SonSpacing.mediumSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(SonSpacing.small))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.stats_plays_count, song.playCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (share > 0f) {
            Spacer(modifier = Modifier.height(SonSpacing.small))
            ShareMeter(fraction = share)
        }
    }
}

/** 名次数字的固定槽宽：让所有行的封面左边缘对齐。 */
private val RankSlotWidth = 32.dp

/** 前三名用强调色。 */
private const val TOP_RANK_COUNT = 3
