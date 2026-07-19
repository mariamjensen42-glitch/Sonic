package com.cycling.sonic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.cycling.sonic.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.theme.SpotifyTheme
import com.cycling.presentation.navigation.AppNavGraph
import com.cycling.presentation.navigation.BottomNavTab
import com.cycling.presentation.navigation.Navigator
import com.cycling.presentation.navigation.Screen
import com.cycling.presentation.navigation.SpotifyBottomNavBar
import com.cycling.presentation.navigation.rememberNavigationState
import com.cycling.presentation.player.MiniPlayer
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.sonic.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()

            SpotifyTheme {
                val navigationState = rememberNavigationState(
                    startRoute = Screen.Home,
                    topLevelRoutes = setOf(Screen.Home, Screen.Search, Screen.Library)
                )

                val navigator = remember { Navigator(navigationState) }

                MainScreen(
                    navigator = navigator,
                    navigationState = navigationState,
                    playerViewModel = playerViewModel,
                    playerUiState = playerUiState
                )
            }
        }
    }
}

private val bottomNavTabs = listOf(
    BottomNavTab(
        screen = Screen.Home,
        label = "Home",
        icon = R.drawable.ic_spotify_home
    ),
    BottomNavTab(
        screen = Screen.Search,
        label = "Search",
        icon = R.drawable.ic_spotify_search
    ),
    BottomNavTab(
        screen = Screen.Library,
        label = "Library",
        icon = R.drawable.ic_spotify_library
    )
)

@Composable
private fun MainScreen(
    navigator: Navigator,
    navigationState: com.cycling.presentation.navigation.NavigationState,
    playerViewModel: PlayerViewModel,
    playerUiState: com.cycling.presentation.player.PlayerUiState
) {
    val currentSong = playerUiState.currentSong
    val hasMiniPlayer = currentSong != null
    val showBottomBar by navigationState.showBottomBar

    // Bottom nav overlays on top of content (Spotify style)
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavGraph(
            playerViewModel = playerViewModel,
            navigator = navigator,
            navigationState = navigationState
        )

        // Spotify-style bottom navigation overlaid at bottom
        SpotifyBottomNavBar(
            currentTab = navigationState.topLevelRoute,
            onTabSelected = { screen ->
                navigator.navigate(screen)
            },
            tabs = bottomNavTabs,
            visible = showBottomBar,
            miniPlayerVisible = hasMiniPlayer,
            miniPlayerContent = {
                MiniPlayer(
                    currentSong = currentSong,
                    isPlaying = playerUiState.isPlaying,
                    playbackPosition = playerUiState.playbackPosition,
                    duration = playerUiState.duration,
                    onPlayPause = { playerViewModel.handleIntent(PlayerIntent.PlayPause) },
                    onClick = { navigator.navigate(Screen.Player) }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
