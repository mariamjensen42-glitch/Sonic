@file:OptIn(ExperimentalMaterial3Api::class)

package com.cycling.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song
import com.cycling.presentation.R

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToArtistDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is SearchEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is SearchEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is SearchEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val query = uiState.searchQuery
    val tab = uiState.selectedTab
    // 全部标签下三块都显示，单块标签下只显示对应的一块
    val showSongs = tab == SearchTab.ALL || tab == SearchTab.SONGS
    val showAlbums = tab == SearchTab.ALL || tab == SearchTab.ALBUMS
    val showArtists = tab == SearchTab.ALL || tab == SearchTab.ARTISTS
    // 歌词命中只在「全部」里出现：它不是第三个实体类型，而是「歌曲」的另一种命中方式。
    val showLyrics = tab == SearchTab.ALL
    val nothingFound = !uiState.isSearching && when (tab) {
        SearchTab.ALL -> uiState.songs.isEmpty() && uiState.albums.isEmpty() &&
            uiState.artists.isEmpty() && uiState.lyricsMatches.isEmpty()
        SearchTab.SONGS -> uiState.songs.isEmpty()
        SearchTab.ALBUMS -> uiState.albums.isEmpty()
        SearchTab.ARTISTS -> uiState.artists.isEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.search_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = SonLayout.bottomBarClearance)
        ) {
            // 输入框吸顶：结果很长时也不必滚回顶部才能改词
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            horizontal = SonLayout.screenMargin,
                            vertical = SonSpacing.small
                        )
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            viewModel.handleIntent(SearchIntent.SearchQueryChanged(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = {
                            M3SymbolIcon(
                                name = SymbolIconName.Search,
                                filled = true,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        viewModel.handleIntent(SearchIntent.ClearSearch)
                                    }
                                ) {
                                    M3SymbolIcon(
                                        name = SymbolIconName.Close,
                                        filled = true,
                                        contentDescription = stringResource(R.string.search_clear_history)
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.handleIntent(SearchIntent.SearchSubmitted(query))
                            }
                        )
                    )
                }
            }

            if (query.isBlank()) {
                // 没输入时显示历史记录：搜索过什么、清掉什么，本来就没有别的地方可以看到
                if (uiState.searchHistory.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = stringResource(R.string.search_history_title),
                            action = stringResource(R.string.search_clear_history),
                            onActionClick = { viewModel.handleIntent(SearchIntent.ClearHistory) }
                        )
                    }
                    item {
                        ChipRow(
                            items = uiState.searchHistory,
                            onItemClick = { viewModel.handleIntent(SearchIntent.HistoryItemClicked(it)) }
                        )
                    }
                }
            } else {
                // 输入过程中，命中历史的补全排在结果前面
                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_history_title),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        ChipRow(
                            items = uiState.suggestions,
                            onItemClick = { viewModel.handleIntent(SearchIntent.HistoryItemClicked(it)) }
                        )
                    }
                }

                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SonLayout.screenMargin,
                                vertical = SonSpacing.small
                            ),
                        space = SonSpacing.small
                    ) {
                        val options = SearchTab.entries.map { stringResource(it.labelRes()) }
                        val selectedIndex = SearchTab.entries.indexOf(tab)
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = index == selectedIndex,
                                onClick = {
                                    viewModel.handleIntent(
                                        SearchIntent.TabSelected(SearchTab.entries[index])
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                label = { Text(text = label) }
                            )
                        }
                    }
                }

                if (showSongs && uiState.songs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_songs),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.songs) { song ->
                        SongRow(
                            song = song,
                            onClick = { viewModel.handleIntent(SearchIntent.SongClicked(song)) },
                            onToggleFavorite = { viewModel.handleIntent(SearchIntent.ToggleFavorite(song)) }
                        )
                    }
                }

                if (showLyrics && uiState.lyricsMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_lyrics_section),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.lyricsMatches) { match ->
                        LyricsMatchRow(
                            match = match,
                            onClick = {
                                viewModel.handleIntent(SearchIntent.SongClicked(match.song))
                            }
                        )
                    }
                }

                if (showAlbums && uiState.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_albums),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.albums) { album ->
                        AlbumRow(
                            album = album,
                            onClick = { viewModel.handleIntent(SearchIntent.AlbumClicked(album)) }
                        )
                    }
                }

                if (showArtists && uiState.artists.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_artists),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.artists) { artist ->
                        ArtistRow(
                            artist = artist,
                            onClick = { viewModel.handleIntent(SearchIntent.ArtistClicked(artist)) }
                        )
                    }
                }

                if (nothingFound) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SonSpacing.extraExtraLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_no_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SearchTab.labelRes(): Int = when (this) {
    SearchTab.ALL -> R.string.search_tab_all
    SearchTab.SONGS -> R.string.search_tab_songs
    SearchTab.ALBUMS -> R.string.search_tab_albums
    SearchTab.ARTISTS -> R.string.search_tab_artists
}

// ════════════════════════════════════════════════════════════════
// 区块标题 / 历史标签
// ════════════════════════════════════════════════════════════════

/** 区块标题用强调变体；带操作时操作是真正的按钮，保留点击态层。 */
@Composable
private fun SearchSectionHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SonLayout.screenMargin,
                end = SonLayout.screenMargin,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        if (action != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(action)
            }
        }
    }
}

/** 历史与补全都是一行可点的建议芯片。 */
@Composable
private fun ChipRow(
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        items(items) { item ->
            SuggestionChip(
                onClick = { onItemClick(item) },
                label = { Text(text = item) }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 结果行
// ════════════════════════════════════════════════════════════════

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                text = song.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            M3Artwork(
                artwork = song.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        },
        // 红心跟着歌曲的真实收藏状态走，点了会写进数据库
        trailingContent = {
            IconButton(
                onClick = onToggleFavorite,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (song.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = song.isFavorite,
                    contentDescription = stringResource(
                        if (song.isFavorite) R.string.player_unfavorite_desc
                        else R.string.player_favorite_desc
                    )
                )
            }
        }
    )
}

@Composable
private fun LyricsMatchRow(match: LyricsMatch, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        overlineContent = {
            Text(
                text = match.song.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        content = {
            Text(
                text = match.song.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                // 命中原因比元数据更重要：这一行存在的意义就是让用户认出那句歌词。
                text = match.snippet,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            M3Artwork(
                artwork = match.song.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        }
    )
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                text = album.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = album.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            M3Artwork(
                artwork = album.albumArt,
                placeholderIcon = SymbolIconName.Album,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        }
    )
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                text = artist.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.search_artist_song_count, artist.numberOfTracks),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        },
        leadingContent = {
            M3Artwork(
                artwork = artist.artistArt,
                placeholderIcon = SymbolIconName.Person,
                shape = CircleShape,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        }
    )
}
