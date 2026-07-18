package com.cycling.presentation.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3LargeTopAppBar
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.Album
import com.cycling.domain.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }

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
        topBar = {
            M3LargeTopAppBar(
                title = "专辑",
                scrollBehavior = scrollBehavior,
                navigationIcon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "排序"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(SortOrder.TITLE, SortOrder.ARTIST).forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(getSortOrderName(order)) },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.cycling.core.ui.components.M3CircularProgressIndicator()
                }
            } else if (sortedAlbums.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.Album,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(M3Spacing.medium))
                    Text(
                        text = "暂无专辑",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "扫描本地音乐以添加专辑",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = M3Spacing.medium,
                        end = M3Spacing.medium,
                        top = M3Spacing.small,
                        bottom = M3Spacing.extraExtraLarge + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(M3Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(M3Spacing.medium)
                ) {
                    items(sortedAlbums, key = { it.id }) { album ->
                        M3MediaCard(
                            title = album.name,
                            subtitle = album.artist,
                            artwork = album.albumArt,
                            onClick = { viewModel.handleIntent(AlbumsIntent.AlbumClick(album)) },
                            placeholderIcon = Icons.Default.Album
                        )
                    }
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

private fun getSortOrderName(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.TITLE -> "按专辑名"
        SortOrder.ARTIST -> "按艺术家"
        else -> "按专辑名"
    }
}
