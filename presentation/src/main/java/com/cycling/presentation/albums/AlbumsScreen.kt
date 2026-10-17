@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Album
import com.cycling.domain.model.SortOrder
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState

@Composable
fun AlbumsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AlbumsEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
            }
        }
    }

    val sortedAlbums = remember(uiState.albums, sortOrder) {
        sortAlbums(uiState.albums, sortOrder)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.albums_title),
                        maxLines = 1
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.albums_count, sortedAlbums.size),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.albums_back_desc)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            M3SymbolIcon(
                                name = SymbolIconName.Sort,
                                filled = true,
                                contentDescription = stringResource(R.string.albums_sort_desc)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(SortOrder.TITLE, SortOrder.ARTIST).forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(text = getSortOrderName(order)) },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> SonLoadingState(modifier = Modifier.padding(paddingValues))
            sortedAlbums.isEmpty() -> SonEmptyState(
                icon = SymbolIconName.Album,
                title = stringResource(R.string.albums_empty_title),
                subtitle = stringResource(R.string.albums_empty_subtitle),
                modifier = Modifier.padding(paddingValues)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = gridState,
                contentPadding = PaddingValues(
                    start = SonLayout.screenMargin,
                    end = SonLayout.screenMargin,
                    top = SonLayout.contentTopPadding,
                    bottom = SonLayout.bottomBarClearance + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(SonLayout.gridGap),
                horizontalArrangement = Arrangement.spacedBy(SonLayout.gridGap)
            ) {
                items(sortedAlbums, key = { it.id }) { album ->
                    M3MediaCard(
                        title = album.name,
                        subtitle = album.artist,
                        artwork = album.albumArt,
                        onClick = { viewModel.handleIntent(AlbumsIntent.AlbumClick(album)) },
                        placeholderIcon = SymbolIconName.Album,
                        width = null
                    )
                }
            }
        }
    }
}

private fun sortAlbums(albums: List<Album>, sortOrder: SortOrder): List<Album> {
    return when (sortOrder) {
        SortOrder.TITLE -> albums.sortedBy { it.name.lowercase() }
        SortOrder.ARTIST -> albums.sortedBy { it.artist.lowercase() }
        else -> albums.sortedBy { it.name.lowercase() }
    }
}

@Composable
private fun getSortOrderName(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.TITLE -> stringResource(R.string.albums_sort_name)
        SortOrder.ARTIST -> stringResource(R.string.albums_sort_artist)
        else -> stringResource(R.string.albums_sort_name)
    }
}
