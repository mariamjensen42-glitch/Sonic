package com.cycling.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.cycling.presentation.ai.AiInfoScreen
import com.cycling.presentation.albumdetail.AlbumDetailScreen
import com.cycling.presentation.albums.AlbumsScreen
import com.cycling.presentation.artistdetail.ArtistDetailScreen
import com.cycling.presentation.artists.ArtistsScreen
import com.cycling.presentation.folder.FolderScreen
import com.cycling.presentation.home.HomeScreen
import com.cycling.presentation.mymusic.MyMusicScreen
import com.cycling.presentation.player.PlayerScreen
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.presentation.playlistdetail.PlaylistDetailScreen
import com.cycling.presentation.playlists.PlaylistsScreen
import com.cycling.presentation.search.SearchScreen
import com.cycling.presentation.settings.SettingsScreen
import com.cycling.presentation.settings.ThemeSettingsScreen
import com.cycling.presentation.songdetail.SongDetailScreen
import com.cycling.presentation.songs.SongsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onShowToast: (String) -> Unit,
    playerViewModel: PlayerViewModel
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
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToSongs = { navController.navigate(LibraryDestination.Songs) },
                onNavigateToAlbums = { navController.navigate(LibraryDestination.Albums) },
                onNavigateToArtists = { navController.navigate(LibraryDestination.Artists) },
                onNavigateToPlaylists = { navController.navigate(LibraryDestination.Playlists) },
                onNavigateToSettings = { navController.navigate(SettingsDestination.Main) },
                onNavigateToAlbumDetail = { albumId -> navController.navigate(LibraryDestination.AlbumDetail(albumId)) },
                onNavigateToArtistDetail = { artistId -> navController.navigate(LibraryDestination.ArtistDetail(artistId)) },
                onNavigateToPlayer = { songId -> navController.navigate(Screen.Player) },
                onNavigateToMyMusic = { navController.navigate(LibraryDestination.MyMusic) },
                onNavigateToSearch = { navController.navigate(Screen.Search) },
                onNavigateToFolders = { navController.navigate(LibraryDestination.Folders) }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId -> },
                onNavigateToAlbumDetail = { albumId -> navController.navigate(LibraryDestination.AlbumDetail(albumId)) },
                onNavigateToArtistDetail = { artistId -> navController.navigate(LibraryDestination.ArtistDetail(artistId)) },
                playerViewModel = playerViewModel
            )
        }

        composable<Screen.Player> {
            val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
            PlayerScreen(
                uiState = uiState,
                onIntent = playerViewModel::handleIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.SongDetail> {
            SongDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate(Screen.Player) }
            )
        }

        composable<Screen.AiInfo> {
            val route = it.toRoute<Screen.AiInfo>()
            AiInfoScreen(
                type = route.type,
                name = route.name,
                artist = route.artist,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApiKeyConfig = { }
            )
        }

        libraryNavGraph(
            navController = navController,
            playerViewModel = playerViewModel
        )

        settingsNavGraph(
            navController = navController,
            onShowToast = onShowToast
        )
    }
}

private fun NavGraphBuilder.libraryNavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel
) {
    navigation<NavGraph.Library>(
        startDestination = LibraryDestination.Songs
    ) {
        composable<LibraryDestination.Songs> {
            SongsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { },
                onNavigateToSongDetail = { songId -> navController.navigate(Screen.SongDetail(songId)) },
                playerViewModel = playerViewModel
            )
        }

        composable<LibraryDestination.Albums> {
            AlbumsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlbumDetail = { albumId -> navController.navigate(LibraryDestination.AlbumDetail(albumId)) }
            )
        }

        composable<LibraryDestination.Artists> {
            ArtistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToArtistDetail = { artistId -> navController.navigate(LibraryDestination.ArtistDetail(artistId)) }
            )
        }

        composable<LibraryDestination.Playlists> {
            PlaylistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylistDetail = { playlistId -> navController.navigate(LibraryDestination.PlaylistDetail(playlistId)) }
            )
        }

        composable<LibraryDestination.MyMusic> {
            MyMusicScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId -> navController.navigate(Screen.Player) }
            )
        }

        composable<LibraryDestination.Folders> {
            FolderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId, songs ->
                    playerViewModel.handleIntent(
                        com.cycling.presentation.player.PlayerIntent.PlaySong(
                            songs.find { it.id == songId } ?: return@FolderScreen,
                            songs
                        )
                    )
                    navController.navigate(Screen.Player)
                }
            )
        }

        composable<LibraryDestination.AlbumDetail> {
            AlbumDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId -> },
                onNavigateToApiKeyConfig = { },
                onNavigateToAiInfo = { type, name, artist -> navController.navigate(Screen.AiInfo(type, name, artist)) }
            )
        }

        composable<LibraryDestination.ArtistDetail> {
            ArtistDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId -> },
                onNavigateToApiKeyConfig = { },
                onNavigateToAiInfo = { type, name, artist -> navController.navigate(Screen.AiInfo(type, name, artist)) }
            )
        }

        composable<LibraryDestination.PlaylistDetail> {
            PlaylistDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId -> }
            )
        }
    }
}

private fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    onShowToast: (String) -> Unit
) {
    composable<SettingsDestination.Main> {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onShowToast = onShowToast,
            onNavigateToThemeSettings = { navController.navigate(SettingsDestination.Theme) }
        )
    }
    
    composable<SettingsDestination.Theme> {
        ThemeSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
