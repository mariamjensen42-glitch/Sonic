package com.cycling.sonic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cycling.sonic.core.ui.theme.SpotifyTheme
import com.cycling.sonic.presentation.navigation.AppNavGraph
import com.cycling.sonic.presentation.navigation.BottomNavTab
import com.cycling.sonic.presentation.navigation.Navigator
import com.cycling.sonic.presentation.navigation.Screen
import com.cycling.sonic.presentation.navigation.SpotifyBottomNavBar
import com.cycling.sonic.presentation.navigation.rememberNavigationState
import com.cycling.sonic.presentation.player.MiniPlayer
import com.cycling.sonic.presentation.player.PlayerIntent
import com.cycling.sonic.presentation.player.PlayerViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.ic_home_filled
import sonic.shared.generated.resources.ic_search_big
import sonic.shared.generated.resources.ic_library_small

@Composable
fun App() {
    SpotifyTheme {
        val topLevelRoutes = setOf<Screen>(Screen.Home, Screen.Search, Screen.Library)
        val navigationState = rememberNavigationState(Screen.Home, topLevelRoutes)
        val navigator = remember { Navigator(navigationState) }
        val tabs = remember {
            listOf(
                BottomNavTab(Screen.Home, "首页") { Icon(painter = painterResource(Res.drawable.ic_home_filled), contentDescription = "首页") },
                BottomNavTab(Screen.Search, "搜索") { Icon(painter = painterResource(Res.drawable.ic_search_big), contentDescription = "搜索") },
                BottomNavTab(Screen.Library, "音乐库") { Icon(painter = painterResource(Res.drawable.ic_library_small), contentDescription = "音乐库") },
            )
        }

        val playerViewModel = koinInject<PlayerViewModel>()
        val playerUiState by playerViewModel.uiState.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            AppNavGraph(
                navigator = navigator,
                navigationState = navigationState
            )

            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SpotifyBottomNavBar(
                    currentTab = navigationState.topLevelRoute,
                    onTabSelected = { navigator.navigate(it) },
                    tabs = tabs,
                    visible = navigationState.showBottomBar.value,
                    miniPlayerVisible = playerUiState.currentSong != null,
                    miniPlayerContent = {
                        MiniPlayer(
                            uiState = playerUiState,
                            onPlayPause = { playerViewModel.handleIntent(PlayerIntent.PlayPause) },
                            onClick = { navigator.navigate(Screen.Player) },
                            onToggleFavorite = { playerViewModel.handleIntent(PlayerIntent.ToggleFavorite) }
                        )
                    }
                )
            }
        }
    }
}
