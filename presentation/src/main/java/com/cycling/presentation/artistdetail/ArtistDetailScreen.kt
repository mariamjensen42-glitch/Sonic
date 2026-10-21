package com.cycling.presentation.artistdetail

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonSizes
import com.cycling.presentation.R
import com.cycling.presentation.components.CollectionDetailScreen
import com.cycling.presentation.components.CollectCollectionDetailEffects
import com.cycling.presentation.components.SongQueueActions

@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onPlayShuffled: (List<com.cycling.domain.model.Song>) -> Unit,
    viewModel: ArtistDetailViewModel,
    bottomPadding: Dp = 0.dp,
    queueActions: SongQueueActions? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectCollectionDetailEffects(viewModel.uiEffect, onNavigateToPlayer, onPlayShuffled)

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
        // 歌手页的特有差异：行尾心形收藏，直连歌手自己的意图。
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
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            }
        }
    )
}
