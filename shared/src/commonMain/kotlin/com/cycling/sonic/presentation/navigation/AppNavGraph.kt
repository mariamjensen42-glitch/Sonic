package com.cycling.sonic.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import com.cycling.sonic.presentation.albums.AlbumsScreen
import com.cycling.sonic.presentation.artistdetail.ArtistDetailScreen
import com.cycling.sonic.presentation.artists.ArtistsScreen
import com.cycling.sonic.presentation.folder.FolderScreen
import com.cycling.sonic.presentation.home.HomeScreen
import com.cycling.sonic.presentation.lyrics.LyricsScreen
import com.cycling.sonic.presentation.mymusic.MyMusicScreen
import com.cycling.sonic.presentation.player.PlayerIntent
import com.cycling.sonic.presentation.player.PlayerScreen
import com.cycling.sonic.presentation.search.SearchScreen
import com.cycling.sonic.presentation.settings.SettingsScreen
import com.cycling.sonic.presentation.songs.SongsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.cycling.sonic.presentation.player.PlayerViewModel
import com.cycling.sonic.presentation.home.HomeViewModel
import com.cycling.sonic.presentation.search.SearchViewModel
import com.cycling.sonic.presentation.mymusic.MyMusicViewModel
import com.cycling.sonic.presentation.songs.SongsViewModel
import com.cycling.sonic.presentation.albums.AlbumsViewModel
import com.cycling.sonic.presentation.artists.ArtistsViewModel
import com.cycling.sonic.presentation.folder.FolderViewModel
import com.cycling.sonic.presentation.settings.SettingsViewModel
import com.cycling.sonic.presentation.albumdetail.AlbumDetailScreen
import com.cycling.sonic.presentation.ai.AiInfoScreen
import com.cycling.sonic.presentation.playlistdetail.PlaylistDetailScreen

@Composable
fun AppNavGraph(
    navigator: Navigator,
    navigationState: NavigationState
) {
    val playerViewModel = koinInject<PlayerViewModel>()
    val playerUiState by playerViewModel.uiState.collectAsState()

    val entryProvider = entryProvider<NavKey> {
        entry<Screen.Home>(
            metadata = tabTransitionMetadata()
        ) { _ ->
            val vm = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = vm,
                onNavigateToSongs = { navigator.navigate(LibraryDestination.Songs) },
                onNavigateToAlbums = { navigator.navigate(LibraryDestination.Albums) },
                onNavigateToArtists = { navigator.navigate(LibraryDestination.Artists) },
                onNavigateToSettings = { navigator.navigate(SettingsDestination.Main) },
                onNavigateToAlbumDetail = { navigator.navigate(LibraryDestination.AlbumDetail(it)) },
                onNavigateToArtistDetail = { navigator.navigate(LibraryDestination.ArtistDetail(it)) },
                onNavigateToPlayer = { navigator.navigate(Screen.Player) },
                onNavigateToMyMusic = { navigator.navigate(LibraryDestination.MyMusic) },
                onNavigateToSearch = { navigator.navigate(Screen.Search) },
                onNavigateToFolders = { navigator.navigate(LibraryDestination.Folders) }
            )
        }

        entry<Screen.Search>(
            metadata = tabTransitionMetadata()
        ) { _ ->
            val vm = koinViewModel<SearchViewModel>()
            SearchScreen(
                viewModel = vm,
                onNavigateToPlayer = { navigator.navigate(Screen.Player) },
                onNavigateToAlbumDetail = { navigator.navigate(LibraryDestination.AlbumDetail(it)) },
                onNavigateToArtistDetail = { navigator.navigate(LibraryDestination.ArtistDetail(it)) },
                onNavigateBack = { navigator.goBack() }
            )
        }

        entry<Screen.Library>(
            metadata = tabTransitionMetadata()
        ) { _ ->
            val vm = koinViewModel<MyMusicViewModel>()
            MyMusicScreen(
                viewModel = vm,
                onNavigateToPlayer = { navigator.navigate(Screen.Player) },
                onNavigateToPlaylistDetail = { navigator.navigate(LibraryDestination.PlaylistDetail(it)) },
                onNavigateToSearch = { navigator.navigate(Screen.Search) }
            )
        }

        entry<Screen.Player>(
            metadata = metadata {
                put(NavDisplay.TransitionKey) {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) togetherWith ExitTransition.KeepUntilTransitionsFinished
                }
                put(NavDisplay.PopTransitionKey) {
                    EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350))
                }
                put(NavDisplay.PredictivePopTransitionKey) {
                    EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350))
                }
            }
        ) { _ ->
            PlayerScreen(
                uiState = playerUiState,
                onIntent = playerViewModel::handleIntent,
                onNavigateBack = { navigator.goBack() },
                onNavigateToLyrics = { navigator.navigate(Screen.Lyrics) }
            )
        }

        entry<Screen.Lyrics> { _ ->
            LyricsScreen(
                uiState = playerUiState,
                onIntent = playerViewModel::handleIntent,
                onNavigateBack = { navigator.goBack() }
            )
        }

        entry<Screen.AiInfo> { key ->
            AiInfoScreen(
                viewModel = koinViewModel<com.cycling.sonic.presentation.ai.AiInfoViewModel> { parametersOf(key.type, key.name, key.artist) },
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
    playerViewModel: PlayerViewModel
) {
    entry<LibraryDestination.Songs> { _ ->
        val vm = koinViewModel<SongsViewModel>()
        val songsState by vm.uiState.collectAsState()
        SongsScreen(
            viewModel = vm,
            playerViewModel = playerViewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId ->
                val song = songsState.songs.find { it.id == songId }
                if (song != null) {
                    playerViewModel.handleIntent(PlayerIntent.PlaySong(song, songsState.songs))
                }
                navigator.navigate(Screen.Player)
            },
            onNavigateToSongDetail = { navigator.navigate(Screen.SongDetail(it)) }
        )
    }

    entry<LibraryDestination.Albums> { _ ->
        val vm = koinViewModel<AlbumsViewModel>()
        AlbumsScreen(
            viewModel = vm,
            onNavigateBack = { navigator.goBack() },
            onNavigateToAlbumDetail = { navigator.navigate(LibraryDestination.AlbumDetail(it)) }
        )
    }

    entry<LibraryDestination.Artists> { _ ->
        val vm = koinViewModel<ArtistsViewModel>()
        ArtistsScreen(
            viewModel = vm,
            onNavigateBack = { navigator.goBack() },
            onNavigateToArtistDetail = { navigator.navigate(LibraryDestination.ArtistDetail(it)) }
        )
    }

    entry<LibraryDestination.MyMusic> { _ ->
        val vm = koinViewModel<MyMusicViewModel>()
        MyMusicScreen(
            viewModel = vm,
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToPlaylistDetail = { navigator.navigate(LibraryDestination.PlaylistDetail(it)) },
            onNavigateToSearch = { navigator.navigate(Screen.Search) }
        )
    }

    entry<LibraryDestination.Folders> { _ ->
        val vm = koinViewModel<FolderViewModel>()
        FolderScreen(
            viewModel = vm,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId, songs ->
                playerViewModel.handleIntent(PlayerIntent.PlaySong(
                    songs.find { it.id == songId } ?: return@FolderScreen,
                    songs
                ))
                navigator.navigate(Screen.Player)
            }
        )
    }

    entry<LibraryDestination.AlbumDetail> { key ->
        val vm = koinViewModel<com.cycling.sonic.presentation.albumdetail.AlbumDetailViewModel> { parametersOf(key.albumId) }
        AlbumDetailScreen(
            viewModel = vm,
            playerViewModel = playerViewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToSongDetail = { navigator.navigate(Screen.SongDetail(it)) }
        )
    }

    entry<LibraryDestination.ArtistDetail> { key ->
        val vm = koinViewModel<com.cycling.sonic.presentation.artistdetail.ArtistDetailViewModel> { parametersOf(key.artistId) }
        ArtistDetailScreen(
            viewModel = vm,
            playerViewModel = playerViewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToSongDetail = { navigator.navigate(Screen.SongDetail(it)) }
        )
    }

    entry<LibraryDestination.PlaylistDetail> { key ->
        val vm = koinViewModel<com.cycling.sonic.presentation.playlistdetail.PlaylistDetailViewModel> { parametersOf(key.playlistId) }
        PlaylistDetailScreen(
            viewModel = vm,
            playerViewModel = playerViewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { navigator.navigate(Screen.Player) },
            onNavigateToSongDetail = { navigator.navigate(Screen.SongDetail(it)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsDestination.Main> { _ ->
        val vm = koinViewModel<SettingsViewModel>()
        SettingsScreen(
            viewModel = vm,
            onNavigateBack = { navigator.goBack() }
        )
    }
}

private fun tabTransitionMetadata() = metadata {
    put(NavDisplay.TransitionKey) {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) togetherWith
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(200))
    }
    put(NavDisplay.PopTransitionKey) {
        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(200)) togetherWith
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280))
    }
}
