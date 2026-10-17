package com.cycling.presentation.navigation.prototype

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.navigation.AppNavGraph
import com.cycling.presentation.navigation.BottomNavTab
import com.cycling.presentation.navigation.M3BottomNavigationBar
import com.cycling.presentation.navigation.Navigator
import com.cycling.presentation.navigation.NavigationState
import com.cycling.presentation.navigation.Screen
import com.cycling.presentation.navigation.rememberBottomNavTabs
import com.cycling.presentation.player.MiniPlayer
import com.cycling.presentation.player.MiniPlayerContainer
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel

/**
 * PROTOTYPE —— 导航外壳三变体，问题来自 Sonic#5。
 *
 * 问题：底部导航继续用标准 NavigationBar，还是换成 ShortNavigationBar /
 * FlexibleBottomAppBar + FAB？
 *
 * 运行（Debug 构建，先由人执行 installDebug）：
 *   adb shell am start -S -n com.cycling.sonic/.MainActivity \
 *     --ez prototype_nav_shell true \
 *     --es prototype_variant A|B|C \
 *     --es prototype_mini on|off \
 *     --es prototype_theme system|light|dark
 *
 * 进入后底部悬浮的是调试条，不是被评估的设计：左右箭头切换 A/B/C，
 * 展开后能看到并切换 mini player、滚动策略、B 的标签策略、C 的 FAB 位置和明暗。
 *
 * 三个变体共用真实的 Home / 音乐库内容与 Navigation 3 backstack；只换外壳渲染。
 * 这是抛弃式代码，结论落进 #17 后整包删除，不要把它当生产实现。
 */

const val PROTOTYPE_NAV_SHELL_EXTRA = "prototype_nav_shell"
const val PROTOTYPE_VARIANT_EXTRA = "prototype_variant"
const val PROTOTYPE_MINI_EXTRA = "prototype_mini"
const val PROTOTYPE_THEME_EXTRA = "prototype_theme"

private val SwitcherBottomOffset =
    SonLayout.bottomBarClearance + SonLayout.miniPlayerClearance + SonSpacing.large +
        SonSpacing.huge

private enum class ShellVariant(val key: String, val title: String) {
    STANDARD("A", "标准 NavigationBar"),
    SHORT("B", "ShortNavigationBar 悬浮"),
    FLEX_FAB("C", "FlexibleBottomAppBar + FAB")
}

private enum class ScrollMode(val label: String) {
    ALWAYS("常显"),
    HIDE_MINI("藏迷你"),
    HIDE_CHROME("藏底栏")
}

private enum class FabPlacement(val label: String) {
    FLOATING("FAB 悬浮"),
    DOCKED("FAB 贴边")
}

/** 没有正在播放的歌时，也让迷你播放器有内容可评估。 */
private val DemoSong = Song(
    id = -1L,
    title = "演示曲目 · 夜航",
    artist = "Prototype",
    album = "Nav Shell Demo",
    duration = 215_000L,
    path = "",
    albumId = -1L,
    artistId = -1L,
    dateAdded = 0L,
    dateModified = 0L,
    size = 0L,
    mimeType = "audio/mpeg",
    albumArt = null,
    isFavorite = true
)

@Composable
fun PrototypeNavigationShell(
    playerViewModel: PlayerViewModel,
    navigationState: NavigationState,
    navigator: Navigator,
    initialVariant: String?,
    initialMiniPlayer: Boolean?,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onExit: () -> Unit
) {
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentTab = navigationState.topLevelRoute
    val showBottomBar by navigationState.showBottomBar
    val tabs = rememberBottomNavTabs()

    var variantKey by rememberSaveable { mutableStateOf(shellVariantOf(initialVariant).key) }
    val variant = shellVariantOf(variantKey)
    var scrollModeName by rememberSaveable { mutableStateOf(ScrollMode.ALWAYS.name) }
    val scrollMode = scrollModeOf(scrollModeName)
    var fabPlacementName by rememberSaveable { mutableStateOf(FabPlacement.FLOATING.name) }
    val fabPlacement = fabPlacementOf(fabPlacementName)
    var showMiniPlayer by rememberSaveable { mutableStateOf(initialMiniPlayer ?: true) }
    var shortBarAllLabels by rememberSaveable { mutableStateOf(false) }
    var demoPlaying by rememberSaveable { mutableStateOf(true) }
    var demoFavorite by rememberSaveable { mutableStateOf(true) }
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var debugExpanded by rememberSaveable { mutableStateOf(false) }
    var lastFabAction by remember { mutableStateOf("") }
    var scrolledAway by remember { mutableStateOf(false) }
    var scrollDirection by remember { mutableIntStateOf(0) }
    var scrollAccum by remember { mutableFloatStateOf(0f) }

    val realSong = playerUiState.currentSong
    val miniSong = if (showMiniPlayer) realSong ?: DemoSong else null
    val miniSource = when {
        miniSong == null -> "off"
        realSong != null -> "real"
        else -> "demo"
    }
    val miniPlaying = if (realSong != null) playerUiState.isPlaying else demoPlaying
    val miniFavorite = if (realSong != null) playerUiState.isFavorite else demoFavorite
    val miniPosition = if (realSong != null) playerUiState.playbackPosition else 72_000L
    val miniDuration = if (realSong != null) playerUiState.duration else DemoSong.duration

    val hideMini = scrollMode == ScrollMode.HIDE_MINI && scrolledAway
    val hideChrome = scrollMode == ScrollMode.HIDE_CHROME && scrolledAway
    val chromeVisible = showBottomBar && !hideChrome
    val miniVisible = chromeVisible && showMiniPlayer && miniSong != null && !hideMini

    val density = LocalDensity.current
    val hideThresholdPx = with(density) { 32.dp.toPx() }
    val scrollConnection = remember(scrollMode, hideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (scrollMode == ScrollMode.ALWAYS) return Offset.Zero
                if (consumed.y < -1f) {
                    scrollDirection = 1
                    scrollAccum = (scrollAccum - consumed.y).coerceAtMost(hideThresholdPx)
                    if (scrollAccum >= hideThresholdPx) {
                        scrolledAway = true
                        scrollAccum = 0f
                    }
                } else if (consumed.y > 1f) {
                    scrollDirection = -1
                    scrollAccum = (scrollAccum - consumed.y).coerceAtLeast(-hideThresholdPx)
                    if (scrollAccum <= -hideThresholdPx) {
                        scrolledAway = false
                        scrollAccum = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(variant.key) {
        fabExpanded = false
        scrolledAway = false
        scrollAccum = 0f
        scrollDirection = 0
    }
    LaunchedEffect(scrollMode.name) {
        scrolledAway = false
        scrollAccum = 0f
        scrollDirection = 0
    }
    LaunchedEffect(currentTab) {
        if (currentTab != Screen.Home) fabExpanded = false
    }
    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) fabExpanded = false
    }

    val miniPlayerContent: @Composable (MiniPlayerContainer) -> Unit = { container ->
        MiniPlayer(
            currentSong = miniSong,
            isPlaying = miniPlaying,
            playbackPosition = miniPosition,
            duration = miniDuration,
            isFavorite = miniFavorite,
            onPlayPause = {
                if (realSong != null) {
                    playerViewModel.handleIntent(PlayerIntent.PlayPause)
                } else {
                    demoPlaying = !demoPlaying
                }
            },
            onToggleFavorite = {
                if (realSong != null) {
                    playerViewModel.handleIntent(PlayerIntent.ToggleFavorite)
                } else {
                    demoFavorite = !demoFavorite
                }
            },
            onClick = {
                if (realSong != null) navigator.navigate(Screen.Player)
            },
            container = container
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollConnection)
        ) {
            AppNavGraph(
                playerViewModel = playerViewModel,
                navigator = navigator,
                navigationState = navigationState,
                bottomContentPadding = if (showMiniPlayer && miniSong != null && showBottomBar) {
                    SonLayout.miniPlayerClearance
                } else {
                    0.dp
                }
            )
        }

        when (variant) {
            ShellVariant.STANDARD -> StandardChrome(
                currentTab = currentTab,
                onTabSelected = { navigator.navigate(it) },
                tabs = tabs,
                visible = chromeVisible,
                miniPlayerVisible = miniVisible,
                miniPlayerContent = { miniPlayerContent(MiniPlayerContainer.FLOATING) }
            )

            ShellVariant.SHORT -> ShortChrome(
                currentTab = currentTab,
                onTabSelected = { navigator.navigate(it) },
                tabs = tabs,
                visible = chromeVisible,
                miniPlayerVisible = miniVisible,
                miniPlayerContent = { miniPlayerContent(MiniPlayerContainer.FLOATING) },
                allLabels = shortBarAllLabels
            )

            ShellVariant.FLEX_FAB -> FlexFabChrome(
                currentTab = currentTab,
                onTabSelected = { navigator.navigate(it) },
                tabs = tabs,
                visible = chromeVisible,
                miniPlayerVisible = miniVisible,
                miniPlayerContent = { miniPlayerContent(MiniPlayerContainer.ATTACHED) },
                showFab = currentTab == Screen.Home,
                fabExpanded = fabExpanded,
                onFabExpandedChange = { fabExpanded = it },
                onFabAction = { lastFabAction = it },
                fabPlacement = fabPlacement
            )
        }

        if (!fabExpanded) {
            PrototypeSwitcher(
                variant = variant,
                onPrevious = {
                    val next = (variant.ordinal - 1 + ShellVariant.entries.size) %
                        ShellVariant.entries.size
                    variantKey = ShellVariant.entries[next].key
                },
                onNext = {
                    val next = (variant.ordinal + 1) % ShellVariant.entries.size
                    variantKey = ShellVariant.entries[next].key
                },
                expanded = debugExpanded,
                onExpandedChange = { debugExpanded = it },
                miniState = miniSource,
                scrollMode = scrollMode,
                scrolledAway = scrolledAway,
                scrollDirection = scrollDirection,
                showMiniPlayer = showMiniPlayer,
                onShowMiniPlayerChange = { showMiniPlayer = it },
                onScrollModeChange = { scrollModeName = it.name },
                shortBarAllLabels = shortBarAllLabels,
                onShortBarAllLabelsChange = { shortBarAllLabels = it },
                fabPlacement = fabPlacement,
                onFabPlacementChange = { fabPlacementName = it.name },
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                lastFabAction = lastFabAction,
                onExit = onExit
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// A — 现状对照：标准 NavigationBar，标签常显，迷你播放器浮在上面
// ════════════════════════════════════════════════════════════════

@Composable
private fun StandardChrome(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean,
    miniPlayerVisible: Boolean,
    miniPlayerContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        M3BottomNavigationBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            tabs = tabs,
            visible = visible,
            miniPlayerVisible = miniPlayerVisible,
            miniPlayerContent = miniPlayerContent,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// B — ShortNavigationBar：悬浮容器、64dp 高、未选中只留图标
// ════════════════════════════════════════════════════════════════

@Composable
private fun ShortChrome(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean,
    miniPlayerVisible: Boolean,
    miniPlayerContent: @Composable () -> Unit,
    allLabels: Boolean
) {
    val motionScheme = MaterialTheme.motionScheme
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = motionScheme.slowSpatialSpec()) { it } +
                fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
            exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it } +
                fadeOut(animationSpec = motionScheme.fastEffectsSpec())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(SonSpacing.small)
            ) {
                AnimatedVisibility(
                    visible = miniPlayerVisible,
                    enter = slideInVertically(animationSpec = motionScheme.defaultSpatialSpec()) { it } +
                        fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                    exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it } +
                        fadeOut(animationSpec = motionScheme.fastEffectsSpec())
                ) {
                    miniPlayerContent()
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SonSpacing.medium)
                        .padding(bottom = SonSpacing.small),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = SonSpacing.small
                ) {
                    ShortNavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        arrangement = ShortNavigationBarArrangement.EqualWeight
                    ) {
                        val first = tabs.getOrNull(0)
                        val second = tabs.getOrNull(1)
                        if (first != null) {
                            ShortBarItem(
                                tab = first,
                                selected = first.screen == currentTab,
                                showLabel = first.screen == currentTab || allLabels,
                                onClick = { onTabSelected(first.screen) }
                            )
                        }
                        if (second != null) {
                            ShortBarItem(
                                tab = second,
                                selected = second.screen == currentTab,
                                showLabel = second.screen == currentTab || allLabels,
                                onClick = { onTabSelected(second.screen) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortBarItem(
    tab: BottomNavTab,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val label: (@Composable () -> Unit)? = if (showLabel) {
        { Text(text = tab.label) }
    } else {
        null
    }
    ShortNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            M3SymbolIcon(
                name = if (selected) tab.selectedIcon else tab.icon,
                filled = selected,
                contentDescription = tab.label
            )
        },
        label = label
    )
}

// ════════════════════════════════════════════════════════════════
// C — FlexibleBottomAppBar + 首页 FAB menu，迷你播放器贴附在底栏上
// ════════════════════════════════════════════════════════════════

@Composable
private fun FlexFabChrome(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean,
    miniPlayerVisible: Boolean,
    miniPlayerContent: @Composable () -> Unit,
    showFab: Boolean,
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    onFabAction: (String) -> Unit,
    fabPlacement: FabPlacement
) {
    val motionScheme = MaterialTheme.motionScheme
    val density = LocalDensity.current
    var chromeHeightPx by remember { mutableIntStateOf(0) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val chromeHeight = with(density) { chromeHeightPx.toDp() }
    val barHeight = with(density) { barHeightPx.toDp() }
    val fabBottom = when (fabPlacement) {
        FabPlacement.FLOATING ->
            (chromeHeight + SonSpacing.medium).coerceAtLeast(SonSpacing.extraLarge)

        FabPlacement.DOCKED ->
            (barHeight - SonSpacing.extraLarge).coerceAtLeast(SonSpacing.small)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = motionScheme.slowSpatialSpec()) { it } +
                fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
            exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it } +
                fadeOut(animationSpec = motionScheme.fastEffectsSpec())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { chromeHeightPx = it.height }
            ) {
                AnimatedVisibility(
                    visible = miniPlayerVisible,
                    enter = slideInVertically(animationSpec = motionScheme.defaultSpatialSpec()) { it } +
                        fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                    exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it } +
                        fadeOut(animationSpec = motionScheme.fastEffectsSpec())
                ) {
                    miniPlayerContent()
                }

                FlexibleBottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height }
                ) {
                    SingleChoiceSegmentedButtonRow {
                        tabs.forEachIndexed { index, tab ->
                            val selected = tab.screen == currentTab
                            SegmentedButton(
                                selected = selected,
                                onClick = { onTabSelected(tab.screen) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = tabs.size
                                ),
                                icon = {
                                    M3SymbolIcon(
                                        name = if (selected) tab.selectedIcon else tab.icon,
                                        filled = selected,
                                        contentDescription = null,
                                        modifier = Modifier.size(SonSizes.iconSmall)
                                    )
                                },
                                label = { Text(text = tab.label) }
                            )
                        }
                    }
                }
            }
        }

        if (showFab && visible) {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    FloatingActionButton(onClick = { onFabExpandedChange(!fabExpanded) }) {
                        M3SymbolIcon(
                            name = if (fabExpanded) SymbolIconName.Close else SymbolIconName.AutoAwesome,
                            filled = true,
                            contentDescription = if (fabExpanded) "关闭菜单" else "首页快捷操作"
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = SonSpacing.medium, bottom = fabBottom)
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        onFabExpandedChange(false)
                        onFabAction("AI 创建歌单")
                    },
                    text = { Text(stringResource(R.string.mymusic_ai_dialog_title)) },
                    icon = { M3SymbolIcon(SymbolIconName.AutoAwesome, contentDescription = null) }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        onFabExpandedChange(false)
                        onFabAction("随机播放")
                    },
                    text = { Text(stringResource(R.string.player_shuffle_desc)) },
                    icon = { M3SymbolIcon(SymbolIconName.Shuffle, contentDescription = null) }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        onFabExpandedChange(false)
                        onFabAction("扫描本地音乐")
                    },
                    text = { Text(stringResource(R.string.settings_scan_local)) },
                    icon = { M3SymbolIcon(SymbolIconName.Refresh, contentDescription = null) }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 调试条（不属于被评估的设计）
// ════════════════════════════════════════════════════════════════

@Composable
private fun BoxScope.PrototypeSwitcher(
    variant: ShellVariant,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    miniState: String,
    scrollMode: ScrollMode,
    scrolledAway: Boolean,
    scrollDirection: Int,
    showMiniPlayer: Boolean,
    onShowMiniPlayerChange: (Boolean) -> Unit,
    onScrollModeChange: (ScrollMode) -> Unit,
    shortBarAllLabels: Boolean,
    onShortBarAllLabelsChange: (Boolean) -> Unit,
    fabPlacement: FabPlacement,
    onFabPlacementChange: (FabPlacement) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    lastFabAction: String,
    onExit: () -> Unit
) {
    val detail = when (variant) {
        ShellVariant.STANDARD -> "labels=all"
        ShellVariant.SHORT -> "labels=${if (shortBarAllLabels) "all" else "selected"}"
        ShellVariant.FLEX_FAB -> fabPlacement.label
    }
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = SonSpacing.extraLarge)
            .padding(bottom = SwitcherBottomOffset),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = SonSpacing.extraSmall,
        shadowElevation = SonSpacing.small
    ) {
        Column(modifier = Modifier.padding(SonSpacing.small)) {
            Text(
                text = "PROTOTYPE · 导航外壳",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = SonSpacing.small))
                Text(
                    text = "variant: ${variant.key} ${variant.title}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "mini=$miniState · scroll=${scrollMode.label}" +
                        (if (scrolledAway) " (away)" else "") +
                        " · dir=$scrollDirection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "detail=$detail" +
                        (if (lastFabAction.isNotEmpty()) " · last=$lastFabAction" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.padding(top = SonSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                        FilterChip(
                            selected = showMiniPlayer,
                            onClick = { onShowMiniPlayerChange(!showMiniPlayer) },
                            label = { Text("迷你播放器") }
                        )
                        FilterChip(
                            selected = isDark,
                            onClick = onToggleTheme,
                            label = { Text("夜间") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                        ScrollMode.entries.forEach { mode ->
                            FilterChip(
                                selected = scrollMode == mode,
                                onClick = { onScrollModeChange(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    if (variant == ShellVariant.SHORT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                            FilterChip(
                                selected = shortBarAllLabels,
                                onClick = {
                                    onShortBarAllLabelsChange(!shortBarAllLabels)
                                },
                                label = { Text("B 全部标签") }
                            )
                        }
                    }
                    if (variant == ShellVariant.FLEX_FAB) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                            FabPlacement.entries.forEach { placement ->
                                FilterChip(
                                    selected = fabPlacement == placement,
                                    onClick = { onFabPlacementChange(placement) },
                                    label = { Text(placement.label) }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "重现：adb shell am start -S -n com.cycling.sonic/.MainActivity " +
                        "--ez $PROTOTYPE_NAV_SHELL_EXTRA true " +
                        "--es $PROTOTYPE_VARIANT_EXTRA ${variant.key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onExit) {
                    Text("退出原型")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    M3SymbolIcon(
                        name = SymbolIconName.ArrowBack,
                        contentDescription = "上一个变体"
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${variant.key} · ${variant.title}",
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "mini=$miniState · ${scrollMode.label}" +
                            (if (scrolledAway) "↓" else "") +
                            " · $detail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onNext) {
                    M3SymbolIcon(
                        name = SymbolIconName.KeyboardArrowRight,
                        contentDescription = "下一个变体"
                    )
                }
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    M3SymbolIcon(
                        name = if (expanded) SymbolIconName.ExpandMore else SymbolIconName.MoreVert,
                        contentDescription = if (expanded) "收起调试条" else "展开调试条"
                    )
                }
            }
        }
    }
}

private fun shellVariantOf(key: String?): ShellVariant =
    ShellVariant.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        ?: ShellVariant.STANDARD

private fun scrollModeOf(name: String): ScrollMode =
    ScrollMode.entries.firstOrNull { it.name == name } ?: ScrollMode.ALWAYS

private fun fabPlacementOf(name: String): FabPlacement =
    FabPlacement.entries.firstOrNull { it.name == name } ?: FabPlacement.FLOATING
