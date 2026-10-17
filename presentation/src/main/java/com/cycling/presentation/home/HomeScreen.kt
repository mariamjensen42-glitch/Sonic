@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3ArtistCard
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.core.ui.theme.SonLayout
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToMyMusic: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val greeting = homeGreeting()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSongs -> onNavigateToSongs()
                is HomeEffect.NavigateToAlbums -> onNavigateToAlbums()
                is HomeEffect.NavigateToArtists -> onNavigateToArtists()
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is HomeEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is HomeEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is HomeEffect.NavigateToMyMusic -> onNavigateToMyMusic()
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToFolders -> onNavigateToFolders()
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            // 与音乐库一致：普通高度顶栏，不随滚动折叠
            TopAppBar(
                title = {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // 搜索不再是底部 tab，入口收在首页顶栏 —— 顶部最靠外的位置最容易被找到
                    IconButton(onClick = onNavigateToSearch) {
                        M3SymbolIcon(
                            name = SymbolIconName.Search,
                            filled = true,
                            contentDescription = stringResource(R.string.search_icon_desc)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        M3SymbolIcon(
                            name = SymbolIconName.Settings,
                            filled = true,
                            contentDescription = stringResource(R.string.home_settings_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    bottom = SonLayout.bottomBarClearance + bottomPadding
                )
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_greeting_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = SonLayout.screenMargin,
                            vertical = SonLayout.contentTopPadding
                        )
                    )
                }

                // === 最近播放 ===
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        HomeSectionHeader(title = stringResource(R.string.home_recent_plays))
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.recentlyPlayed,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 推荐专辑 ===
                if (uiState.topAlbums.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_recommended_albums),
                            onSeeAll = { viewModel.handleIntent(HomeIntent.NavigateToAlbums) }
                        )
                    }
                    item {
                        AlbumMediaRow(
                            albums = uiState.topAlbums,
                            onAlbumClick = { album ->
                                viewModel.handleIntent(HomeIntent.AlbumClick(album))
                            }
                        )
                    }
                }

                // === 推荐歌手 ===
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_recommended_artists),
                            onSeeAll = { viewModel.handleIntent(HomeIntent.NavigateToArtists) }
                        )
                    }
                    item {
                        ArtistMediaRow(
                            artists = uiState.topArtists,
                            onArtistClick = { artist ->
                                viewModel.handleIntent(HomeIntent.ArtistClick(artist))
                            }
                        )
                    }
                }

                // === 收藏歌曲 ===
                if (uiState.favoriteSongs.isNotEmpty()) {
                    item {
                        HomeSectionHeader(title = stringResource(R.string.home_favorite_songs))
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.favoriteSongs,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 最近添加 ===
                if (uiState.recentlyAdded.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_recently_added),
                            onSeeAll = { viewModel.handleIntent(HomeIntent.NavigateToSongs) }
                        )
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.recentlyAdded,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 最常播放 ===
                if (uiState.mostPlayed.isNotEmpty()) {
                    item {
                        HomeSectionHeader(title = stringResource(R.string.home_most_played))
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.mostPlayed,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }
            }
        }
    }
}

/** 按当前时段取问候语。 */
@Composable
private fun homeGreeting(): String {
    val hour = LocalTime.now().hour
    return stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    )
}

// ════════════════════════════════════════════════════════════════
// 区块标题
// ════════════════════════════════════════════════════════════════

/** 区块标题用强调变体与正文拉开层级；"查看全部"是真正的按钮，保留点击态层。 */
@Composable
private fun HomeSectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SonLayout.screenMargin,
                end = SonLayout.screenMargin,
                top = SonLayout.cardGap,
                bottom = SonLayout.sectionTitleGap
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.home_view_all))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 横向媒体列表
// ════════════════════════════════════════════════════════════════

@Composable
private fun SongMediaRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(songs, key = { it.id }) { song ->
            M3MediaCard(
                title = song.title,
                subtitle = song.artist,
                artwork = song.albumArt,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
private fun AlbumMediaRow(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(albums, key = { it.id }) { album ->
            M3MediaCard(
                title = album.name,
                subtitle = album.artist,
                artwork = album.albumArt,
                onClick = { onAlbumClick(album) },
                placeholderIcon = SymbolIconName.Album
            )
        }
    }
}

@Composable
private fun ArtistMediaRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(artists, key = { it.id }) { artist ->
            M3ArtistCard(
                name = artist.name,
                artwork = artist.artistArt,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}
