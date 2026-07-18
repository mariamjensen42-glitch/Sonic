package com.cycling.presentation.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import com.cycling.core.ui.components.M3ArtistCard
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.formatDuration
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3SemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: com.cycling.presentation.player.PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToPlayer -> {
                    val songs = uiState.songs
                    val song = songs.find { it.id == effect.songId }
                    if (song != null) {
                        playerViewModel.handleIntent(
                            com.cycling.presentation.player.PlayerIntent.PlaySong(song, songs)
                        )
                    }
                    onNavigateToPlayer(effect.songId)
                }
                is SearchEffect.NavigateToAlbumDetail -> {
                    onNavigateToAlbumDetail(effect.albumId)
                }
                is SearchEffect.NavigateToArtistDetail -> {
                    onNavigateToArtistDetail(effect.artistId)
                }
                is SearchEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            M3SearchBar(
                searchQuery = uiState.searchQuery,
                onQueryChange = { viewModel.handleIntent(SearchIntent.SearchQueryChanged(it)) },
                onSearch = { viewModel.handleIntent(SearchIntent.SearchSubmitted(it)) },
                onClear = { viewModel.handleIntent(SearchIntent.ClearSearch) },
                onCancel = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.searchQuery.isEmpty()) {
                SearchHistorySection(
                    history = uiState.searchHistory,
                    onHistoryItemClick = { viewModel.handleIntent(SearchIntent.HistoryItemClicked(it)) },
                    onClearHistory = { viewModel.handleIntent(SearchIntent.ClearHistory) }
                )
            } else {
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    SearchResultsSection(
                        songs = uiState.songs,
                        albums = uiState.albums,
                        artists = uiState.artists,
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.handleIntent(SearchIntent.TabSelected(it)) },
                        onSongClick = { viewModel.handleIntent(SearchIntent.SongClicked(it)) },
                        onAlbumClick = { viewModel.handleIntent(SearchIntent.AlbumClicked(it)) },
                        onArtistClick = { viewModel.handleIntent(SearchIntent.ArtistClicked(it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3SearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = M3Spacing.medium,
                vertical = M3Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .clip(M3Shapes.cornerMedium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "搜索",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        AnimatedVisibility(
            visible = isFocused || searchQuery.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = M3Spacing.small)
                    .clickable(onClick = onCancel)
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(M3Spacing.medium))
                Text(
                    text = "搜索音乐",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(M3Spacing.small))
                Text(
                    text = "输入歌曲、专辑或歌手名称进行搜索",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = M3Spacing.medium,
                        vertical = M3Spacing.small
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3SectionHeader(title = "搜索历史")
                Text(
                    text = "清除",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClearHistory)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = M3Spacing.medium)
            ) {
                history.forEachIndexed { index, query ->
                    HistoryItem(
                        query = query,
                        onClick = { onHistoryItemClick(query) },
                        showDivider = index < history.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun M3SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = M3Spacing.small)
    )
}

@Composable
private fun HistoryItem(
    query: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = M3Spacing.small,
                    vertical = M3Spacing.small + M3Spacing.extraSmall
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(M3Spacing.small + M3Spacing.extraSmall))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = M3Spacing.medium + M3Spacing.extraSmall)
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun SearchResultsSection(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    val hasResults = songs.isNotEmpty() || albums.isNotEmpty() || artists.isNotEmpty()

    if (!hasResults) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(M3Spacing.medium))
                Text(
                    text = "未找到结果",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(M3Spacing.small))
                Text(
                    text = "尝试使用不同的关键词搜索",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column {
            SearchTabRow(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                songsCount = songs.size,
                albumsCount = albums.size,
                artistsCount = artists.size
            )

            when (selectedTab) {
                SearchTab.ALL -> {
                    AllResultsContent(
                        songs = songs,
                        albums = albums,
                        artists = artists,
                        onSongClick = onSongClick,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick
                    )
                }
                SearchTab.SONGS -> {
                    SongsResultList(
                        songs = songs,
                        onSongClick = onSongClick
                    )
                }
                SearchTab.ALBUMS -> {
                    AlbumsResultList(
                        albums = albums,
                        onAlbumClick = onAlbumClick
                    )
                }
                SearchTab.ARTISTS -> {
                    ArtistsResultList(
                        artists = artists,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTabRow(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    songsCount: Int,
    albumsCount: Int,
    artistsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = M3Spacing.medium,
                vertical = M3Spacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(M3Spacing.small)
    ) {
        SearchTabChip(
            label = "全部",
            selected = selectedTab == SearchTab.ALL,
            onClick = { onTabSelected(SearchTab.ALL) }
        )
        SearchTabChip(
            label = "歌曲 ($songsCount)",
            selected = selectedTab == SearchTab.SONGS,
            onClick = { onTabSelected(SearchTab.SONGS) }
        )
        SearchTabChip(
            label = "专辑 ($albumsCount)",
            selected = selectedTab == SearchTab.ALBUMS,
            onClick = { onTabSelected(SearchTab.ALBUMS) }
        )
        SearchTabChip(
            label = "歌手 ($artistsCount)",
            selected = selectedTab == SearchTab.ARTISTS,
            onClick = { onTabSelected(SearchTab.ARTISTS) }
        )
    }
}

@Composable
private fun SearchTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(M3Shapes.cornerLarge)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = M3Spacing.small + M3Spacing.extraSmall,
                vertical = M3Spacing.extraSmall + 2.dp
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AllResultsContent(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (songs.isNotEmpty()) {
            item {
                M3SectionHeader(
                    title = "歌曲",
                    modifier = Modifier.padding(
                        top = M3Spacing.small,
                        start = M3Spacing.medium
                    )
                )
            }
            items(songs.take(3)) { song ->
                SongListItem(
                    song = song,
                    subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                    showDuration = false,
                    showMoreButton = false,
                    showDivider = true,
                    modifier = Modifier.clickable { onSongClick(song) }
                )
            }
        }

        if (albums.isNotEmpty()) {
            item {
                M3SectionHeader(
                    title = "专辑",
                    modifier = Modifier.padding(
                        top = M3Spacing.medium,
                        start = M3Spacing.medium
                    )
                )
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = M3Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(M3Spacing.small + M3Spacing.extraSmall)
                ) {
                    items(albums.take(5)) { album ->
                        M3MediaCard(
                            title = album.name,
                            subtitle = album.artist,
                            artwork = album.albumArt,
                            placeholderIcon = Icons.Default.Album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(M3Spacing.medium)) }
        }

        if (artists.isNotEmpty()) {
            item {
                M3SectionHeader(
                    title = "歌手",
                    modifier = Modifier.padding(
                        top = M3Spacing.medium,
                        start = M3Spacing.medium
                    )
                )
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = M3Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(M3Spacing.small + M3Spacing.extraSmall)
                ) {
                    items(artists.take(5)) { artist ->
                        M3ArtistCard(
                            name = artist.name,
                            artwork = artist.artistArt,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(M3Spacing.medium)) }
        }
    }
}

@Composable
private fun SongsResultList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(songs) { index, song ->
            SongListItem(
                song = song,
                subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                showDuration = false,
                showMoreButton = false,
                showDivider = index < songs.size - 1,
                modifier = Modifier.clickable { onSongClick(song) }
            )
        }
    }
}

@Composable
private fun AlbumsResultList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(M3Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(M3Spacing.small + M3Spacing.extraSmall)
    ) {
        items(albums) { album ->
            AlbumResultItem(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
private fun AlbumResultItem(
    album: Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = M3Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(M3Shapes.cornerSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.albumArt != null) {
                coil3.compose.AsyncImage(
                    model = album.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = M3SemanticColors.Teal
                )
            }
        }
        Spacer(modifier = Modifier.width(M3Spacing.small + M3Spacing.extraSmall))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} · ${album.numberOfSongs} 首歌曲",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistsResultList(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(M3Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(M3Spacing.small + M3Spacing.extraSmall)
    ) {
        items(artists) { artist ->
            ArtistResultItem(
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
private fun ArtistResultItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = M3Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(M3Shapes.cornerFull)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artistArt != null) {
                coil3.compose.AsyncImage(
                    model = artist.artistArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = M3SemanticColors.Blue
                )
            }
        }
        Spacer(modifier = Modifier.width(M3Spacing.small + M3Spacing.extraSmall))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.numberOfAlbums} 张专辑 · ${artist.numberOfTracks} 首歌曲",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
