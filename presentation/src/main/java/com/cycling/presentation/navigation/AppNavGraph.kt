package com.cycling.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.cycling.presentation.ai.AiInfoScreen
import com.cycling.presentation.albumdetail.AlbumDetailScreen
import com.cycling.presentation.albumdetail.AlbumDetailViewModel
import com.cycling.presentation.albums.AlbumsScreen
import com.cycling.presentation.albums.AlbumsViewModel
import com.cycling.presentation.artistdetail.ArtistDetailScreen
import com.cycling.presentation.artistdetail.ArtistDetailViewModel
import com.cycling.presentation.artists.ArtistsScreen
import com.cycling.presentation.artists.ArtistsViewModel
import com.cycling.presentation.folder.FolderScreen
import com.cycling.presentation.folder.FolderViewModel
import com.cycling.presentation.home.HomeScreen
import com.cycling.presentation.home.HomeViewModel
import com.cycling.presentation.mymusic.MyMusicScreen
import com.cycling.presentation.mymusic.MyMusicViewModel
import com.cycling.presentation.player.PlayerScreen
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.presentation.playlistdetail.PlaylistDetailScreen
import com.cycling.presentation.playlistdetail.PlaylistDetailViewModel
import com.cycling.presentation.search.SearchScreen
import com.cycling.presentation.search.SearchViewModel
import com.cycling.presentation.settings.SettingsScreen
import com.cycling.presentation.settings.SettingsViewModel
import com.cycling.presentation.songdetail.SongDetailScreen
import com.cycling.presentation.songdetail.SongDetailViewModel
import com.cycling.presentation.songs.SongsScreen
import com.cycling.presentation.songs.SongsViewModel

@Composable
fun AppNavGraph(
    playerViewModel: PlayerViewModel,
    navigator: Navigator,
    navigationState: NavigationState
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        playerViewModel.uiEffect.collect { effect ->
            when (effect) {
                is com.cycling.presentation.player.PlayerEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val entryProvider = entryProvider<NavKey> {
        entry<Screen.Home> { _ ->
            val homeViewModel = hiltViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSongs = { navigator.navigate(LibraryDestination.Songs) },
                onNavigateToAlbums = { navigator.navigate(LibraryDestination.Albums) },
                onNavigateToArtists = { navigator.navigate(LibraryDestination.Artists) },
                onNavigateToSettings = { navigator.navigate(SettingsDestination.Main) },
                onNavigateToAlbumDetail = { albumId ->
                    navigator.navigate(LibraryDestination.AlbumDetail(albumId))
                },
                onNavigateToArtistDetail = { artistId ->
                    navigator.navigate(LibraryDestination.ArtistDetail(artistId))
                },
                onNavigateToPlayer = { navigator.navigate(Screen.Player) },
                onNavigateToMyMusic = { navigator.navigate(LibraryDestination.MyMusic) },
                onNavigateToSearch = { navigator.navigate(Screen.Search) },
                onNavigateToFolders = { navigator.navigate(LibraryDestination.Folders) }
            )
        }

        entry<Screen.Search> { _ ->
            val searchViewModel = hiltViewModel<SearchViewModel>()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToPlayer = { songId -> navigator.navigate(Screen.Player) },
                onNavigateBack = { navigator.goBack() }
            )
        }

        entry<Screen.Library> { _ ->
            val myMusicViewModel = hiltViewModel<MyMusicViewModel>()
            MyMusicScreen(
                viewModel = myMusicViewModel,
                onNavigateBack = { },  // top-level tab, no back
                onNavigateToPlayer = { navigator.navigate(Screen.Player) },
                onNavigateToPlaylistDetail = { playlistId ->
                    navigator.navigate(LibraryDestination.PlaylistDetail(playlistId))
                }
            )
        }

        entry<Screen.Player> { _ ->
            val uiState = playerViewModel.uiState.collectAsStateWithLifecycle()
            PlayerScreen(
                uiState = uiState.value,
                onIntent = playerViewModel::handleIntent,
                onNavigateBack = { navigator.goBack() }
            )
        }

        entry<Screen.SongDetail> { key ->
            val viewModel = hiltViewModel<SongDetailViewModel, SongDetailViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) }
            )
            SongDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.goBack() },
                onNavigateToPlayer = { navigator.navigate(Screen.Player) }
            )
        }

        entry<Screen.AiInfo> { key ->
            AiInfoScreen(
                type = key.type,
                name = key.name,
                artist = key.artist,
                onNavigateBack = { navigator.goBack() },
                onNavigateToApiKeyConfig = { navigator.navigate(SettingsDestination.Main) }
            )
        }

        libraryEntries(navigator, playerViewModel)
        settingsEntries(navigator)
    }

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() }
    )
}

private fun EntryProviderScope<NavKey>.libraryEntries(
    navigator: Navigator,
    playerViewModel: com.cycling.presentation.player.PlayerViewModel
) {
    entry<LibraryDestination.Songs> { _ ->
        val songsViewModel: SongsViewModel = hiltViewModel()
        val songsState by songsViewModel.uiState.collectAsStateWithLifecycle()
        SongsScreen(
            viewModel = songsViewModel,
            playerViewModel = playerViewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId ->
                val queue = songsState.songs
                val song = queue.find { it.id == songId } ?: return@SongsScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(song, queue)
                )
                navigator.navigate(Screen.Player)
            },
            onNavigateToSongDetail = { songId ->
                navigator.navigate(Screen.SongDetail(songId))
            }
        )
    }

    entry<LibraryDestination.Albums> { _ ->
        AlbumsScreen(
            viewModel = hiltViewModel(),
            onNavigateBack = { navigator.goBack() },
            onNavigateToAlbumDetail = { albumId ->
                navigator.navigate(LibraryDestination.AlbumDetail(albumId))
            }
        )
    }

    entry<LibraryDestination.Artists> { _ ->
        ArtistsScreen(
            viewModel = hiltViewModel(),
            onNavigateBack = { navigator.goBack() },
            onNavigateToArtistDetail = { artistId ->
                navigator.navigate(LibraryDestination.ArtistDetail(artistId))
            }
        )
    }

    entry<LibraryDestination.MyMusic> { _ ->
        MyMusicScreen(
            viewModel = hiltViewModel(),
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToPlaylistDetail = { playlistId ->
                navigator.navigate(LibraryDestination.PlaylistDetail(playlistId))
            }
        )
    }

    entry<LibraryDestination.Folders> { _ ->
        FolderScreen(
            viewModel = hiltViewModel(),
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId, songs ->
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(
                        songs.find { it.id == songId } ?: return@FolderScreen,
                        songs
                    )
                )
                navigator.navigate(Screen.Player)
            }
        )
    }

    entry<LibraryDestination.AlbumDetail> { key ->
        val viewModel = hiltViewModel<AlbumDetailViewModel, AlbumDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }
        )
        AlbumDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToApiKeyConfig = { navigator.navigate(SettingsDestination.Main) },
            onNavigateToAiInfo = { type, name, artist ->
                navigator.navigate(Screen.AiInfo(type, name, artist))
            }
        )
    }

    entry<LibraryDestination.ArtistDetail> { key ->
        val viewModel = hiltViewModel<ArtistDetailViewModel, ArtistDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }
        )
        ArtistDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToApiKeyConfig = { navigator.navigate(SettingsDestination.Main) },
            onNavigateToAiInfo = { type, name, artist ->
                navigator.navigate(Screen.AiInfo(type, name, artist))
            }
        )
    }

    entry<LibraryDestination.PlaylistDetail> { key ->
        val viewModel = hiltViewModel<PlaylistDetailViewModel, PlaylistDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }
        )
        val playlistState by viewModel.uiState.collectAsStateWithLifecycle()
        PlaylistDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId ->
                val queue = playlistState.songs
                val song = queue.find { it.id == songId } ?: return@PlaylistDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(song, queue)
                )
                navigator.navigate(Screen.Player)
            }
        )
    }
}

private fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsDestination.Main> { _ ->
        SettingsScreen(
            viewModel = hiltViewModel(),
            onNavigateBack = { navigator.goBack() },
            onShowToast = { message -> /* Toast handled in AppNavGraph */ }
        )
    }
}
