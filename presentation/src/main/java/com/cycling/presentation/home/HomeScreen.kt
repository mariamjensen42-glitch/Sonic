package com.cycling.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3LargeTopAppBar
import com.cycling.core.ui.components.M3ListItemWithTrailing
import com.cycling.core.ui.components.M3TrailingContentType
import com.cycling.core.ui.components.M3Container
import com.cycling.core.ui.components.M3ContainerStyle
import com.cycling.core.ui.theme.M3SemanticColors
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.PlayerState
import com.cycling.presentation.player.MiniPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
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
    val playerState by viewModel.playerState.collectAsStateWithLifecycle(
        initialValue = PlayerState()
    )
    val scrollState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSongs -> onNavigateToSongs()
                is HomeEffect.NavigateToAlbums -> onNavigateToAlbums()
                is HomeEffect.NavigateToArtists -> onNavigateToArtists()
                is HomeEffect.NavigateToPlaylists -> onNavigateToPlaylists()
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is HomeEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is HomeEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is HomeEffect.NavigateToMyMusic -> onNavigateToMyMusic()
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToFolders -> onNavigateToFolders()
                is HomeEffect.ShowToast -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3LargeTopAppBar(
                title = "音乐",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(HomeIntent.NavigateToSearch) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                    IconButton(onClick = { viewModel.handleIntent(HomeIntent.NavigateToSettings) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
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
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            state = scrollState,
                            verticalArrangement = Arrangement.spacedBy(M3Spacing.small),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                vertical = M3Spacing.medium
                            )
                        ) {
                            item {
                                QuickAccessSection(
                                    onSongsClick = { viewModel.handleIntent(HomeIntent.NavigateToSongs) },
                                    onAlbumsClick = { viewModel.handleIntent(HomeIntent.NavigateToAlbums) },
                                    onArtistsClick = { viewModel.handleIntent(HomeIntent.NavigateToArtists) },
                                    onPlaylistsClick = { viewModel.handleIntent(HomeIntent.NavigateToPlaylists) },
                                    onMyMusicClick = { viewModel.handleIntent(HomeIntent.NavigateToMyMusic) },
                                    onFoldersClick = { viewModel.handleIntent(HomeIntent.NavigateToFolders) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(bottomPadding))
                            }
                        }
                    }
                    if (playerState.currentSong != null) {
                        MiniPlayer(
                            currentSong = playerState.currentSong,
                            isPlaying = playerState.isPlaying,
                            playbackPosition = playerState.playbackPosition,
                            duration = playerState.duration,
                            onPlayPause = { viewModel.handleIntent(HomeIntent.PlayPause) },
                            onClick = { viewModel.handleIntent(HomeIntent.MiniPlayerClick) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessSection(
    onSongsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onMyMusicClick: () -> Unit,
    onFoldersClick: () -> Unit
) {
    M3Container(
        modifier = Modifier.fillMaxWidth(),
        style = M3ContainerStyle.Filled
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(M3Spacing.extraSmall)
        ) {
            M3ListItemWithTrailing(
                headlineText = "歌曲",
                onClick = onSongsClick,
                leadingIcon = Icons.Outlined.MusicNote,
                leadingIconBackgroundColor = M3SemanticColors.Red,
                trailingType = M3TrailingContentType.Chevron
            )
            M3ListItemWithTrailing(
                headlineText = "专辑",
                onClick = onAlbumsClick,
                leadingIcon = Icons.Outlined.Album,
                leadingIconBackgroundColor = M3SemanticColors.Teal,
                trailingType = M3TrailingContentType.Chevron
            )
            M3ListItemWithTrailing(
                headlineText = "歌手",
                onClick = onArtistsClick,
                leadingIcon = Icons.Outlined.Person,
                leadingIconBackgroundColor = M3SemanticColors.Blue,
                trailingType = M3TrailingContentType.Chevron
            )
            M3ListItemWithTrailing(
                headlineText = "播放列表",
                onClick = onPlaylistsClick,
                leadingIcon = Icons.AutoMirrored.Filled.QueueMusic,
                leadingIconBackgroundColor = M3SemanticColors.Green,
                trailingType = M3TrailingContentType.Chevron
            )
            M3ListItemWithTrailing(
                headlineText = "文件夹",
                onClick = onFoldersClick,
                leadingIcon = Icons.Default.Folder,
                leadingIconBackgroundColor = M3SemanticColors.Indigo,
                trailingType = M3TrailingContentType.Chevron
            )
            M3ListItemWithTrailing(
                headlineText = "我的音乐",
                supportingText = "喜欢、最近播放、最常播放",
                onClick = onMyMusicClick,
                leadingIcon = Icons.Default.Favorite,
                leadingIconBackgroundColor = M3SemanticColors.Pink,
                trailingType = M3TrailingContentType.Chevron
            )
        }
    }
}
