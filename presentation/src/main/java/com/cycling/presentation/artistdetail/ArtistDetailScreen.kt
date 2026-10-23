package com.cycling.presentation.artistdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleCard
import com.cycling.core.ui.apple.AppleCarousel
import com.cycling.core.ui.apple.AppleSectionHeader
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonSizes
import com.cycling.presentation.R
import com.cycling.presentation.components.CollectionDetailScreen
import com.cycling.presentation.components.CollectCollectionDetailEffects
import com.cycling.presentation.components.SongQueueActions

/**
 * 歌手页，按 Apple Music 的艺人页结构排：头部大图 + 「热门歌曲」曲目表 + 页尾「专辑」横滑。
 *
 * 与专辑页的差别只有这两段附加区块与行尾心形 —— 主体仍是共享的 [CollectionDetailScreen]，
 * 多选、拖拽、折叠头部等行为因此与另外两个详情页完全一致。
 */
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onPlayShuffled: (List<com.cycling.domain.model.Song>) -> Unit,
    viewModel: ArtistDetailViewModel,
    bottomPadding: Dp = 0.dp,
    queueActions: SongQueueActions? = null,
    onNavigateToAlbumDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectCollectionDetailEffects(viewModel.uiEffect, onNavigateToPlayer, onPlayShuffled)

    // 页尾「专辑」横滑；没有专辑时整块不占位。显式标注类型，避免 lambda 推断成无 receiver 的函数。
    val albumsFooter: (@Composable () -> Unit)? = if (uiState.albums.isEmpty()) {
        null
    } else {
        {
            Column {
                AppleSectionHeader(title = stringResource(R.string.albums_title))
                AppleCarousel {
                    items(uiState.albums, key = { "artistAlbum-${it.id}" }) { album ->
                        AppleCard(
                            artwork = album.albumArt,
                            title = album.name,
                            subtitle = album.firstYear.takeIf { it > 0 }?.toString(),
                            onClick = { onNavigateToAlbumDetail(album.id) }
                        )
                    }
                }
            }
        }
    }

    CollectionDetailScreen(
        uiState = uiState,
        titleFallback = stringResource(R.string.artist_detail_unknown),
        subtitle = stringResource(R.string.collection_detail_song_count, uiState.songs.size),
        placeholderIcon = SymbolIconName.Person,
        backContentDescription = stringResource(R.string.artist_detail_back_desc),
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::handleCollectionIntent,
        bottomPadding = bottomPadding,
        queueActions = queueActions,
        // 「热门歌曲」区块标题：曲目表紧跟其后
        headerExtra = {
            AppleSectionHeader(title = stringResource(R.string.artist_detail_top_songs))
        },
        songsFooter = albumsFooter,
        // 行尾心形，直连歌手自己的意图
        songTrailing = { song ->
            val isLiked = song.id in uiState.likedSongIds
            IconButton(
                onClick = { viewModel.handleIntent(ArtistDetailIntent.ToggleFavorite(song.id)) }
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = isLiked,
                    contentDescription = stringResource(
                        if (isLiked) R.string.player_unfavorite_desc else R.string.player_favorite_desc
                    ),
                    modifier = Modifier.size(SonSizes.iconSmall),
                    tint = if (isLiked) AppleTheme.colors.systemRed else AppleTheme.colors.tertiaryLabel
                )
            }
        }
    )
}
