package com.cycling.sonic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonicTheme
import com.cycling.presentation.navigation.AppNavGraph
import com.cycling.presentation.navigation.M3BottomNavigationBar
import com.cycling.presentation.navigation.Navigator
import com.cycling.presentation.navigation.Screen
import com.cycling.presentation.navigation.rememberBottomNavTabs
import com.cycling.presentation.navigation.rememberNavigationState
import com.cycling.presentation.permissions.MediaLibraryPermission
import com.cycling.presentation.player.MiniPlayer
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.presentation.player.rememberArtworkTheme
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
            val context = LocalContext.current

            val mediaPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.values.all { it }) mainViewModel.scanIfLibraryEmpty()
            }

            // 第一次打开就问媒体权限：本地播放器真正需要的就只有它，晚问只会先给用户一个空库。
            // 被拒绝也不拦路，设置页和空态里都还有再问一次的入口。
            LaunchedEffect(Unit) {
                if (MediaLibraryPermission.isGranted(context)) {
                    mainViewModel.scanIfLibraryEmpty()
                } else {
                    mediaPermissionLauncher.launch(MediaLibraryPermission.names)
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            // 通知权限等到真的开始放歌再问。和媒体权限一起弹，用户更可能顺手拒绝，
            // 而一旦拒绝，通知栏的媒体控制和锁屏控制就再也不会出现 —— 那是这个应用的主要卖点。
            var askedForNotificationPermission by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(playerUiState.currentSong) {
                if (!askedForNotificationPermission &&
                    playerUiState.currentSong != null &&
                    needsNotificationPermission(context)
                ) {
                    askedForNotificationPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // 封面种子色驱动的整套配色方案；取色与色调映射都在后台线程完成。
            val artworkTheme = rememberArtworkTheme(
                imageUrl = playerUiState.currentSong?.albumArt,
                darkTheme = isSystemInDarkTheme()
            )

            SonicTheme(colorScheme = artworkTheme.colorScheme) {
                val navigationState = rememberNavigationState(
                    startRoute = Screen.Home,
                    topLevelRoutes = setOf(Screen.Home, Screen.Library)
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

/**
 * 只有在 Android 13 及以上，通知权限才是一个要单独申请的运行时权限。更早的版本上
 * `POST_NOTIFICATIONS` 这个常量本身没有意义，问了也没有对话框可弹。
 */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

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
    val miniPlayerVisible = hasMiniPlayer && showBottomBar

    // 导航栏与迷你播放器浮在内容之上，内容区自行预留底部留白
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavGraph(
            playerViewModel = playerViewModel,
            navigator = navigator,
            navigationState = navigationState,
            bottomContentPadding = if (miniPlayerVisible) {
                SonLayout.miniPlayerClearance
            } else {
                0.dp
            }
        )

        M3BottomNavigationBar(
            currentTab = navigationState.topLevelRoute,
            onTabSelected = { screen -> navigator.navigate(screen) },
            tabs = rememberBottomNavTabs(),
            visible = showBottomBar,
            miniPlayerVisible = miniPlayerVisible,
            miniPlayerContent = {
                MiniPlayer(
                    currentSong = currentSong,
                    isPlaying = playerUiState.isPlaying,
                    playbackPosition = playerUiState.playbackPosition,
                    duration = playerUiState.duration,
                    isFavorite = playerUiState.isFavorite,
                    onPlayPause = { playerViewModel.handleIntent(PlayerIntent.PlayPause) },
                    onToggleFavorite = { playerViewModel.handleIntent(PlayerIntent.ToggleFavorite) },
                    onClick = { navigator.navigate(Screen.Player) }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
