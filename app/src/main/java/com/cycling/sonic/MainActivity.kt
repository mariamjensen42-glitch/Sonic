
package com.cycling.sonic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cycling.presentation.navigation.AppleBottomNavigationBar
import com.cycling.presentation.navigation.Navigator
import com.cycling.presentation.navigation.Screen
import com.cycling.presentation.navigation.rememberBottomNavTabs
import com.cycling.presentation.navigation.rememberNavigationState
import com.cycling.presentation.permissions.MediaLibraryPermission
import com.cycling.presentation.player.ArtworkFlightEnd
import com.cycling.presentation.player.LocalPlayerArtworkTransition
import com.cycling.presentation.player.MiniPlayer
import com.cycling.presentation.player.PlayerArtworkFlight
import com.cycling.presentation.player.PlayerArtworkTransition
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerMotion
import com.cycling.presentation.player.PlayerOverlay
import com.cycling.presentation.player.PlayerViewMode
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.presentation.player.rememberArtworkBackdrop
import com.cycling.sonic.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 应用级引导（接播放器、首次灌库）加上前台增量刷新。
     *
     * 用 Activity 的委托而不是只靠组合里的 `hiltViewModel()`：媒体库变更发生在应用之外，
     * 回到前台是最自然的触发点，那时还没有任何页面要求过一次重组。
     */
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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

            // 封面只喂播放页 / 歌词页的动态色面；应用主题本身跟随系统壁纸，
            // 不再按当前歌曲的封面重新着色（换歌时全应用重新着色没有意义）。
            // filePath 是兜底：MediaStore 的 albums 行读不到封面时，从音频文件里读内嵌图。
            val artworkBackdrop = rememberArtworkBackdrop(
                imageUrl = playerUiState.currentSong?.albumArt,
                filePath = playerUiState.currentSong?.path
            )

            SonicTheme(
                backdropColors = artworkBackdrop.colors,
                backdropArtwork = artworkBackdrop.cells
            ) {
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

    /**
     * 回到前台时增量刷新媒体库。
     *
     * 外部拷进来的歌、别处删掉的歌，不该逼用户跑去设置里手动按一次扫描。没有媒体权限时
     * 绝不进来 —— 那时 MediaStore 查不到东西，扫描的剪枝逻辑会把整个库清空。
     */
    override fun onStart() {
        super.onStart()
        if (MediaLibraryPermission.isGranted(this)) {
            mainViewModel.refreshLibraryOnForeground()
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
    // 派生值：路由本身允许（栈顶是 tab 根）且页面没有主动收起（如「我喜欢」）。
    val showBottomBar = navigationState.showBottomBar
    val miniPlayerVisible = hasMiniPlayer && showBottomBar

    // ── 播放页浮层 ────────────────────────────────────────────────────────
    // 跟 MeloX 一样，播放页不再是导航目的地，而是盖在整个 app 上的浮层：
    // 底下的页面、底栏、迷你播放器都还在，下拉时露出的就是它们本身。
    // 0 = 已收进迷你播放器，1 = 完全展开；手势直接改这个值，所以打开 / 收起是同一条时间线。
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    // 旋屏后如果浮层还开着，进度要直接落回 1，否则会留一层透明但仍吃手势的空壳。
    var playerProgress by remember { mutableFloatStateOf(if (playerOpen) 1f else 0f) }
    val scope = rememberCoroutineScope()
    var playerSettleJob by remember { mutableStateOf<Job?>(null) }

    fun animatePlayerTo(target: Float, onDone: (() -> Unit)? = null) {
        playerSettleJob?.cancel()
        // 已经拖到终点（比如一把拖到底才松手）：别再等一段空动画，白白吃 280ms 的手势。
        if (playerProgress == target) {
            onDone?.invoke()
            return
        }
        playerSettleJob = scope.launch {
            animate(
                initialValue = playerProgress,
                targetValue = target,
                // 曲线与 MeloX 同一条（临界阻尼弹簧、无过冲）：点击打开 / 松手收尾走它，
                // 手势拖动不经过这里 —— 那条路径由手指直接写进度。
                animationSpec = tween(
                    durationMillis = if (target >= 1f) {
                        PlayerMotion.OPEN_MS
                    } else {
                        PlayerMotion.CLOSE_MS
                    },
                    easing = PlayerMotion.easing
                )
            ) { value, _ -> playerProgress = value }
            onDone?.invoke()
        }
    }

    fun openPlayer() {
        playerOpen = true
        animatePlayerTo(1f)
    }

    fun closePlayer() {
        if (!playerOpen) return
        animatePlayerTo(0f) { playerOpen = false }
    }

    // 系统返回先收播放页，再往下才是路由返回。
    BackHandler(enabled = playerOpen) { closePlayer() }

    // 封面飞行：两端（迷你播放器 / 播放页封面）各自把图藏起来，中间由这一份在根坐标里连续
    // 变形。歌词页收起时不飞 —— 那一页没有封面节点，从"没有"到迷你播放器只能硬切。
    // 矩形还没测到时也不飞（走原来的淡入淡出），否则封面会被藏起来又没有人补位。
    val artworkTransition = remember { PlayerArtworkTransition() }
    val miniArtworkBounds = artworkTransition.miniBounds
    val playerArtworkBounds = artworkTransition.coverBounds
    val artworkFlying = playerOpen &&
        playerProgress < ArtworkFlightEnd &&
        playerUiState.viewMode == PlayerViewMode.COVER &&
        miniArtworkBounds.width > 1f &&
        playerArtworkBounds.width > 1f

    // 导航栏与迷你播放器浮在内容之上，内容区自行预留底部留白
    CompositionLocalProvider(LocalPlayerArtworkTransition provides artworkTransition) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavGraph(
                playerViewModel = playerViewModel,
                navigator = navigator,
                navigationState = navigationState,
                bottomContentPadding = if (miniPlayerVisible) {
                    SonLayout.miniPlayerClearance
                } else {
                    0.dp
                },
                onOpenPlayer = { openPlayer() }
            )

            AppleBottomNavigationBar(
                currentTab = navigationState.topLevelRoute,
                onTabSelected = { screen -> navigator.navigate(screen) },
                tabs = rememberBottomNavTabs(),
                visible = showBottomBar,
                miniPlayerVisible = miniPlayerVisible,
                miniPlayerContent = {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = playerUiState.isPlaying,
                        onPlayPause = { playerViewModel.handleIntent(PlayerIntent.PlayPause) },
                        onSkipNext = { playerViewModel.handleIntent(PlayerIntent.SkipToNext) },
                        onClick = { openPlayer() },
                        artworkAlpha = if (artworkFlying) 0f else 1f
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                onSearchClick = { navigator.navigate(Screen.Search) }
            )

            // 浮层画在最后 = 压住底栏与迷你播放器；把播放页拉下来时它们就从底下露出来。
            if (playerOpen) {
                PlayerOverlay(
                    progress = playerProgress,
                    uiState = playerUiState,
                    onIntent = playerViewModel::handleIntent,
                    onProgressChange = { progress ->
                        // 手势接管，正在跑的收尾动画立刻停手，否则两处同时写进度会抖。
                        playerSettleJob?.cancel()
                        playerProgress = progress
                    },
                    onSettle = { close -> if (close) closePlayer() else animatePlayerTo(1f) },
                    beatFlow = playerViewModel.beat,
                    artworkOwned = artworkFlying
                )
            }

            // 飞行中的封面压在浮层之上：浮层缩下去的时候，这张图才是观众视线跟着走的那个。
            if (artworkFlying) {
                PlayerArtworkFlight(
                    artwork = currentSong?.albumArt,
                    rect = artworkTransition.flightRect(playerProgress),
                    progress = playerProgress
                )
            }
        }
    }
}
