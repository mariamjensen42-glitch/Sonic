package com.cycling.presentation.navigation

import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import com.cycling.presentation.ai.AiInfoScreen
import com.cycling.presentation.albumdetail.AlbumDetailScreen
import com.cycling.presentation.albumdetail.AlbumDetailViewModel
import com.cycling.presentation.albums.AlbumsScreen
import com.cycling.presentation.artistdetail.ArtistDetailScreen
import com.cycling.presentation.artistdetail.ArtistDetailViewModel
import com.cycling.presentation.artists.ArtistsScreen
import com.cycling.presentation.folder.FolderScreen
import com.cycling.presentation.home.HomeScreen
import com.cycling.presentation.home.HomeViewModel
import com.cycling.presentation.lyrics.LyricsScreen
import com.cycling.presentation.mymusic.MyMusicScreen
import com.cycling.presentation.mymusic.MyMusicViewModel
import com.cycling.presentation.player.PlayerScreen
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.presentation.playlistdetail.PlaylistDetailScreen
import com.cycling.presentation.playlistdetail.PlaylistDetailViewModel
import com.cycling.presentation.search.SearchScreen
import com.cycling.presentation.search.SearchViewModel
import com.cycling.presentation.settings.SettingsScreen
import com.cycling.presentation.songdetail.SongDetailScreen
import com.cycling.presentation.songdetail.SongDetailViewModel
import com.cycling.presentation.songs.SongsScreen
import com.cycling.presentation.songs.SongsViewModel

/**
 * 主壳导航图。
 *
 * [bottomContentPadding] 只传给 Home / 音乐库两个 tab root：迷你播放器可见时由 MainScreen 传
 * `SonLayout.miniPlayerClearance`，避免最后一行被迷你播放器盖住。pushed screens 的底栏已经由
 * [NavigationState] 隐藏，不需要这份额外留白。
 */
@Composable
fun AppNavGraph(
    playerViewModel: PlayerViewModel,
    navigator: Navigator,
    navigationState: NavigationState,
    bottomContentPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val motionScheme = MaterialTheme.motionScheme
    val tabRoutes = navigationState.topLevelRoutes

    // 设置里关掉「点击播放时进入播放页」后，各处点歌起播都不再跳转；迷你播放器的点击是显式
    // 打开播放页，走 MainActivity 那边的 navigate，不受这个开关管。
    val openPlayerOnPlay = playerViewModel.openPlayerOnPlay.collectAsStateWithLifecycle()
    fun navigateToPlayer() {
        if (openPlayerOnPlay.value) navigator.navigate(Screen.Player)
    }

    LaunchedEffect(Unit) {
        playerViewModel.uiEffect.collect { effect ->
            when (effect) {
                is com.cycling.presentation.player.PlayerEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
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
                onNavigateToPlayer = { navigateToPlayer() },
                onNavigateToMyMusic = { navigator.navigate(LibraryDestination.MyMusic) },
                onNavigateToSearch = { navigator.navigate(Screen.Search) },
                onNavigateToFolders = { navigator.navigate(LibraryDestination.Folders) },
                bottomPadding = bottomContentPadding
            )
        }

        entry<Screen.Search> { _ ->
            val searchViewModel = hiltViewModel<SearchViewModel>()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToPlayer = { navigateToPlayer() },
                onNavigateBack = { navigator.goBack() },
                onNavigateToAlbumDetail = { albumId ->
                    navigator.navigate(LibraryDestination.AlbumDetail(albumId))
                },
                onNavigateToArtistDetail = { artistId ->
                    navigator.navigate(LibraryDestination.ArtistDetail(artistId))
                }
            )
        }

        entry<Screen.Library> { _ ->
            val myMusicViewModel = hiltViewModel<MyMusicViewModel>()
            MyMusicScreen(
                viewModel = myMusicViewModel,
                onNavigateBack = { },  // top-level tab, no back
                onNavigateToPlayer = { navigateToPlayer() },
                onNavigateToPlaylistDetail = { playlistId ->
                    navigator.navigate(LibraryDestination.PlaylistDetail(playlistId))
                },
                onNavigateToSearch = { navigator.navigate(Screen.Search) },
                onNavigateToSongs = { navigator.navigate(LibraryDestination.Songs) },
                bottomPadding = bottomContentPadding
            )
        }

        entry<Screen.Player>(
            metadata = metadata {
                // Slide up from bottom when entering (mini player -> full player)
                put(NavDisplay.TransitionKey) {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = motionScheme.defaultSpatialSpec()
                    ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                }
                // Slide down to bottom when going back
                put(NavDisplay.PopTransitionKey) {
                    EnterTransition.None togetherWith
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = motionScheme.defaultSpatialSpec()
                        )
                }
                put(NavDisplay.PredictivePopTransitionKey) {
                    EnterTransition.None togetherWith
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = motionScheme.defaultSpatialSpec()
                        )
                }
            }
        ) { _ ->
            val uiState = playerViewModel.uiState.collectAsStateWithLifecycle()
            PlayerScreen(
                uiState = uiState.value,
                onIntent = playerViewModel::handleIntent,
                onNavigateBack = { navigator.goBack() },
                onNavigateToLyrics = { navigator.navigate(Screen.Lyrics) }
            )
        }

        entry<Screen.Lyrics> { _ ->
            val uiState = playerViewModel.uiState.collectAsStateWithLifecycle()
            LyricsScreen(
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
                onNavigateToPlayer = { navigateToPlayer() }
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

        libraryEntries(navigator, playerViewModel) { navigateToPlayer() }
        settingsEntries(navigator)
    }

    fun isTabRoute(key: Any): Boolean = tabRoutes.any { it == key }

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        // push：从右侧推入，上一屏反向退出四分之一并淡出
        transitionSpec = {
            if (isTabRoute(initialState.key) && isTabRoute(targetState.key)) {
                // tab 之间不加位移：转场语法归 #7，这里只保证不被横向滑动带跑
                fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                    fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
            } else {
                (
                    slideInHorizontally(animationSpec = motionScheme.defaultSpatialSpec()) { it } +
                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
                    ) togetherWith (
                    slideOutHorizontally(animationSpec = motionScheme.defaultSpatialSpec()) { -it / 4 } +
                        fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                    )
            }
        },
        // pop：方向相反；tab 根之间同样不加位移
        popTransitionSpec = {
            if (isTabRoute(initialState.key) && isTabRoute(targetState.key)) {
                fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                    fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
            } else {
                (
                    slideInHorizontally(animationSpec = motionScheme.defaultSpatialSpec()) { -it / 4 } +
                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
                    ) togetherWith (
                    slideOutHorizontally(animationSpec = motionScheme.defaultSpatialSpec()) { it } +
                        fadeOut(animationSpec = motionScheme.defaultEffectsSpec())
                    )
            }
        },
        // predictive back：保留库的缩放收束形态，只换 spec
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                scaleOut(
                    targetScale = 0.7f,
                    animationSpec = motionScheme.defaultSpatialSpec()
                )
        }
    )
}

private fun EntryProviderScope<NavKey>.libraryEntries(
    navigator: Navigator,
    playerViewModel: com.cycling.presentation.player.PlayerViewModel,
    navigateToPlayer: () -> Unit
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
                navigateToPlayer()
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
            onNavigateToPlayer = { navigateToPlayer() },
            onNavigateToPlaylistDetail = { playlistId ->
                navigator.navigate(LibraryDestination.PlaylistDetail(playlistId))
            },
            onNavigateToSearch = { navigator.navigate(Screen.Search) },
            onNavigateToSongs = { navigator.navigate(LibraryDestination.Songs) }
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
                    navigateToPlayer()
                }
        )
    }

    entry<LibraryDestination.AlbumDetail> { key ->
        val viewModel = hiltViewModel<AlbumDetailViewModel, AlbumDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }
        )
        val albumState by viewModel.uiState.collectAsStateWithLifecycle()
        AlbumDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId ->
                val queue = albumState.songs
                val song = queue.find { it.id == songId } ?: return@AlbumDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(song, queue)
                )
                navigateToPlayer()
            },
            // 随机播放：队列已在上游打乱，这里直接以首曲起播并开启随机模式。
            onPlayShuffled = { queue ->
                val first = queue.firstOrNull() ?: return@AlbumDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(first, queue)
                )
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.ToggleShuffleMode
                )
                navigateToPlayer()
            }
        )
    }

    entry<LibraryDestination.ArtistDetail> { key ->
        val viewModel = hiltViewModel<ArtistDetailViewModel, ArtistDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }
        )
        val artistState by viewModel.uiState.collectAsStateWithLifecycle()
        ArtistDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onNavigateToPlayer = { songId ->
                val queue = artistState.songs
                val song = queue.find { it.id == songId } ?: return@ArtistDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(song, queue)
                )
                navigateToPlayer()
            },
            // 随机播放：队列已在上游打乱，这里直接以首曲起播并开启随机模式。
            onPlayShuffled = { queue ->
                val first = queue.firstOrNull() ?: return@ArtistDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(first, queue)
                )
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.ToggleShuffleMode
                )
                navigateToPlayer()
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
                navigateToPlayer()
            },
            // 随机播放：队列已在上游打乱，这里直接以首曲起播并开启随机模式。
            onPlayShuffled = { queue ->
                val first = queue.firstOrNull() ?: return@PlaylistDetailScreen
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.PlaySong(first, queue)
                )
                playerViewModel.handleIntent(
                    com.cycling.presentation.player.PlayerIntent.ToggleShuffleMode
                )
                navigateToPlayer()
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
