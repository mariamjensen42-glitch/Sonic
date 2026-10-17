package com.cycling.presentation.albumdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R
import com.cycling.presentation.components.CollectionDetailScreen
import com.cycling.presentation.components.CollectCollectionDetailEffects

@Composable
fun AlbumDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onPlayShuffled: (List<com.cycling.domain.model.Song>) -> Unit,
    viewModel: AlbumDetailViewModel,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectCollectionDetailEffects(viewModel.uiEffect, onNavigateToPlayer, onPlayShuffled)

    CollectionDetailScreen(
        uiState = uiState,
        titleFallback = stringResource(R.string.album_detail_unknown),
        // 头图副标题展示歌手名；曲目数量由列表本身可见，不再单独占一行。
        subtitle = uiState.subtitle ?: stringResource(R.string.album_detail_unknown_artist),
        placeholderIcon = SymbolIconName.Album,
        backContentDescription = stringResource(R.string.album_detail_back_desc),
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::handleCollectionIntent,
        bottomPadding = bottomPadding
    )
}
