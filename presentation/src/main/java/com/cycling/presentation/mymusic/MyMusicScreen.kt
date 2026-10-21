@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.mymusic

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.CollectionActions
import com.cycling.presentation.components.CollectionHero
import com.cycling.presentation.components.MenuItem
import com.cycling.presentation.components.MenuItemGroup
import com.cycling.presentation.components.PlaylistDescription
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.SonSectionHeader
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.SongQueueActions
import com.cycling.presentation.components.songMenuItems

/** AI 预览列表在底部弹层内的限定高度，保证确认按钮始终可达。 */
private val previewListHeight = 280.dp

@Composable
fun MyMusicScreen(
    viewModel: MyMusicViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToPlaylistDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSongs: () -> Unit = {},
    /** 听歌足迹：统计与排行榜。两者都是 pushed 页面，由外壳负责导航。 */
    onNavigateToStats: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    bottomPadding: Dp = 0.dp,
    /** 队列与详情动作，由外壳接线；收藏页每首歌的菜单因此也有「下一首播放 / 加入队列」。 */
    queueActions: SongQueueActions? = null,
    /**
     * 底栏让位回调。「我喜欢」是 tab 内部的全屏子视图而不是一条导航路由，导航层看不出
     * 它和 tab 根的区别，所以由这一页在进入/退出子视图时告诉外壳。
     */
    onBottomBarHiddenChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    // 音乐库是工具型页面：标题时刻只由 large flexible 顶栏承担，内容区保持安静。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 收藏列表有自己的顶栏，是盖住整个 tab 的一页：底栏在这一页让位。
    // onDispose 里必须放回来 —— 页面被移出组合时若还留着 true，底栏就再也回不来了。
    val favoritesOpen = uiState.showFavoriteSongs
    DisposableEffect(favoritesOpen) {
        onBottomBarHiddenChange(favoritesOpen)
        onDispose { onBottomBarHiddenChange(false) }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is MyMusicEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is MyMusicEffect.NavigateToPlaylistDetail -> onNavigateToPlaylistDetail(effect.playlistId)
                is MyMusicEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 收藏列表是「音乐库」里的子视图，不是独立导航目的地：系统返回要回到歌单列表。
    // 不拦的话一按就离开整个 tab，而 ViewModel 的 showFavoriteSongs 还是 true，
    // 下次再进「音乐库」仍然停在这一页。
    BackHandler(enabled = uiState.showFavoriteSongs) {
        viewModel.handleIntent(MyMusicIntent.BackToPlaylists)
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            uiState = uiState,
            onManualCreate = { name ->
                viewModel.handleIntent(MyMusicIntent.CreatePlaylist(name))
                showCreateDialog = false
            },
            onIntent = { viewModel.handleIntent(it) },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (uiState.showRenameDialog && uiState.playlistToRename != null) {
        RenamePlaylistDialog(
            playlist = uiState.playlistToRename!!,
            onDismiss = { viewModel.handleIntent(MyMusicIntent.DismissRenameDialog) },
            onRename = { newName ->
                viewModel.handleIntent(MyMusicIntent.RenamePlaylist(uiState.playlistToRename!!.id, newName))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!uiState.showFavoriteSongs) {
            // 标题时刻：display 级「音乐库」+ 一行歌单数量；搜索与新建降为 actions。
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mymusic_title_library),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.mymusic_subtitle_library, uiState.playlists.size),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        M3SymbolIcon(
                            name = SymbolIconName.Search,
                            filled = true,
                            contentDescription = stringResource(R.string.mymusic_search_desc)
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        M3SymbolIcon(
                            name = SymbolIconName.Add,
                            filled = true,
                            contentDescription = stringResource(R.string.mymusic_create_playlist_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        }

        // ── 内容 ──
        if (uiState.showFavoriteSongs) {
            FavoritesContent(
                songs = uiState.favoriteSongs,
                isLoading = uiState.isLoading,
                description = uiState.favoriteDescription,
                isDescriptionExpanded = uiState.isFavoriteDescriptionExpanded,
                isDescriptionGenerating = uiState.isFavoriteDescriptionGenerating,
                currentSongId = uiState.currentSongId,
                bottomPadding = bottomPadding,
                onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) },
                onNavigateBack = { viewModel.handleIntent(MyMusicIntent.BackToPlaylists) },
                onShuffle = { viewModel.handleIntent(MyMusicIntent.ShuffleFavoriteSongs) },
                onToggleDescription = { viewModel.handleIntent(MyMusicIntent.ToggleFavoriteDescription) },
                onToggleFavorite = { viewModel.handleIntent(MyMusicIntent.ToggleFavorite(it)) },
                queueActions = queueActions
            )
        } else {
            PlaylistsContent(
                playlists = uiState.playlists,
                isLoading = uiState.isLoading,
                onAllSongsClick = onNavigateToSongs,
                onFavoriteSongsClick = { viewModel.handleIntent(MyMusicIntent.FavoriteSongsClick) },
                onStatsClick = onNavigateToStats,
                onLeaderboardClick = onNavigateToLeaderboard,
                onPlaylistClick = { viewModel.handleIntent(MyMusicIntent.PlaylistClick(it)) },
                onRename = { viewModel.handleIntent(MyMusicIntent.ShowRenameDialog(it)) },
                onDelete = { viewModel.handleIntent(MyMusicIntent.DeletePlaylist(it)) },
                bottomPadding = bottomPadding,
                contentModifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            )
        }
    }
}

// ════════════════════════════════════════════════════════
// 收藏歌曲
// ════════════════════════════════════════════════════════

@Composable
private fun FavoritesContent(
    songs: List<Song>,
    isLoading: Boolean,
    description: String,
    isDescriptionExpanded: Boolean,
    isDescriptionGenerating: Boolean,
    currentSongId: Long?,
    bottomPadding: Dp,
    onSongClick: (Song) -> Unit,
    onNavigateBack: () -> Unit,
    onShuffle: () -> Unit,
    onToggleDescription: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    queueActions: SongQueueActions? = null
) {
    when {
        isLoading -> FavoritesStandaloneScaffold(
            title = stringResource(R.string.mymusic_title_favorites),
            onNavigateBack = onNavigateBack
        ) { SonLoadingState() }

        songs.isEmpty() -> FavoritesStandaloneScaffold(
            title = stringResource(R.string.mymusic_title_favorites),
            onNavigateBack = onNavigateBack
        ) {
            SonEmptyState(
                icon = SymbolIconName.Favorite,
                title = stringResource(R.string.mymusic_empty_favorites_title),
                subtitle = stringResource(R.string.mymusic_empty_favorites_subtitle)
            )
        }

        else -> {
            val listState = rememberLazyListState()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val heroSide = maxWidth
                val density = LocalDensity.current
                val collapse = remember(heroSide, density, listState) {
                    derivedStateOf {
                        val sidePx = with(density) { heroSide.toPx() }
                        if (sidePx <= 0f) {
                            0f
                        } else {
                            val offsetPx = if (listState.firstVisibleItemIndex == 0) {
                                listState.firstVisibleItemScrollOffset
                            } else {
                                sidePx.toInt()
                            }
                            (offsetPx.toFloat() / sidePx).coerceIn(0f, 1f)
                        }
                    }
                }

                val extended = LocalSonExtendedColors.current
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                val titleAlpha = ((collapse.value - 0.45f) / 0.25f).coerceIn(0f, 1f)
                                if (titleAlpha > 0f) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        M3Artwork(
                                            artwork = songs.firstOrNull()?.albumArt,
                                            placeholderIcon = SymbolIconName.Favorite,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier
                                                .size(SonSizes.appBarArtwork)
                                                .graphicsLayer { alpha = titleAlpha }
                                        )
                                        Spacer(Modifier.size(SonSpacing.small))
                                        Text(
                                            text = stringResource(R.string.mymusic_title_favorites),
                                            style = MaterialTheme.typography.titleMediumEmphasized,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                FilledTonalIconButton(
                                    onClick = onNavigateBack,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        // 同 CollectionDetail：压在封面上的图标要有 0.60 的托底，
                                        // 亮封面上的白图标才看得清。
                                        containerColor = extended.artworkScrim.copy(alpha = 0.60f),
                                        contentColor = extended.onArtwork
                                    )
                                ) {
                                    M3SymbolIcon(
                                        name = SymbolIconName.ArrowBack,
                                        filled = true,
                                        contentDescription = stringResource(R.string.mymusic_back_desc)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = lerp(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                    (collapse.value * 1.6f).coerceIn(0f, 1f)
                                ),
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                navigationIconContentColor = lerp(
                                    extended.onArtwork,
                                    MaterialTheme.colorScheme.onSurface,
                                    collapse.value
                                ),
                                titleContentColor = lerp(
                                    extended.onArtwork,
                                    MaterialTheme.colorScheme.onSurface,
                                    collapse.value
                                )
                            )
                        )
                    }
                ) { _ ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = SonLayout.bottomBarClearance + bottomPadding
                        )
                    ) {
                        item(key = "favorites-hero") {
                            CollectionHero(
                                name = stringResource(R.string.mymusic_title_favorites),
                                subtitle = stringResource(
                                    R.string.collection_detail_song_count,
                                    songs.size
                                ),
                                artwork = songs.firstOrNull()?.albumArt,
                                placeholderIcon = SymbolIconName.Favorite,
                                side = heroSide,
                                collapse = collapse
                            )
                        }

                        item(key = "favorites-actions") {
                            // 收藏曲目不属于任何歌单，「添加到歌单」不挂。
                            CollectionActions(
                                onPlayAll = { songs.firstOrNull()?.let(onSongClick) },
                                onShuffle = onShuffle,
                                onAddToPlaylist = null
                            )
                        }

                        item(key = "favorites-description") {
                            PlaylistDescription(
                                description = description,
                                expanded = isDescriptionExpanded,
                                isGenerating = isDescriptionGenerating,
                                onToggle = onToggleDescription,
                                modifier = Modifier.padding(
                                    horizontal = SonLayout.screenMargin,
                                    vertical = SonSpacing.small
                                )
                            )
                        }

                        itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                            // 每行自己的展开状态：菜单开着时点别处就关，开第二行时第一行自动收起。
                            var menuExpanded by remember(song.id) { mutableStateOf(false) }
                            SongListItem(
                                song = song,
                                subtitle = song.artist,
                                isCurrentSong = song.id == currentSongId,
                                // quiet surface：收藏列表同样不画分割线。
                                showDivider = false,
                                // 心形已经在行尾，菜单里不再重复一条「收藏」。
                                showMoreButton = queueActions != null,
                                moreMenuExpanded = menuExpanded,
                                onMoreClick = { menuExpanded = true },
                                onMoreDismiss = { menuExpanded = false },
                                moreMenuItems = songMenuItems(song, queueActions),
                                onClick = { onSongClick(song) },
                                trailingContent = {
                                    IconButton(
                                        onClick = { onToggleFavorite(song) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        M3SymbolIcon(
                                            name = SymbolIconName.Favorite,
                                            filled = true,
                                            contentDescription = stringResource(R.string.mymusic_unfavorite_desc)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 收藏页「没有封面可压」时的形态：载入中、或一首都没喜欢。
 *
 * 这里的顶栏必须是实底、标题常显。内容态那种透明顶栏是**压在 hero 封面上的**，标题与返回
 * 按钮靠封面托住；一旦没有 hero，页面就只剩一段空态文字 —— 没有标题、没有返回，用户只能
 * 用系统手势退出整个「音乐库」。而「音乐库」自己的大标题栏在收藏子视图里是隐藏的，所以
 * 这个缺口没人补。
 */
@Composable
private fun FavoritesStandaloneScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        // 与收藏页内容态同一套：状态栏交给顶栏，内容从顶栏下沿开始。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.mymusic_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    bottomPadding: Dp,
    onSongClick: (Song) -> Unit,
    trailingContent: @Composable ((Song) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = SonLayout.contentTopPadding,
            bottom = SonLayout.bottomBarClearance + bottomPadding
        )
    ) {
        items(songs, key = { it.id }) { song ->
            val trailingSlot: (@Composable () -> Unit)? = trailingContent?.let { content ->
                { content(song) }
            }
            ListItem(
                onClick = { onSongClick(song) },
                content = {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingContent = trailingSlot
            )
        }
    }
}

// ════════════════════════════════════════════════════════
// 播放列表
// ════════════════════════════════════════════════════════

@Composable
private fun PlaylistsContent(
    playlists: List<Playlist>,
    isLoading: Boolean,
    onAllSongsClick: () -> Unit,
    onFavoriteSongsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    bottomPadding: Dp,
    contentModifier: Modifier = Modifier
) {
    when {
        isLoading -> SonLoadingState()
        playlists.isEmpty() -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(contentModifier),
            contentPadding = PaddingValues(
                bottom = SonLayout.bottomBarClearance + bottomPadding
            )
        ) {
            item { AllSongsItem(onClick = onAllSongsClick) }
            item { FavoriteSongsItem(onClick = onFavoriteSongsClick) }
            item { ListeningSection(onStatsClick = onStatsClick, onLeaderboardClick = onLeaderboardClick) }
            item { SonSectionHeader(title = stringResource(R.string.mymusic_section_playlists)) }
            item { EmptyPlaylistsHint() }
        }
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(contentModifier),
            contentPadding = PaddingValues(
                bottom = SonLayout.bottomBarClearance + bottomPadding
            )
        ) {
            item { AllSongsItem(onClick = onAllSongsClick) }
            item { FavoriteSongsItem(onClick = onFavoriteSongsClick) }
            item { ListeningSection(onStatsClick = onStatsClick, onLeaderboardClick = onLeaderboardClick) }
            item { SonSectionHeader(title = stringResource(R.string.mymusic_section_playlists)) }
            items(playlists, key = { it.id }) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onRename = { onRename(playlist) },
                    onDelete = { onDelete(playlist) }
                )
            }
        }
    }
}

/**
 * 听歌足迹的两个入口。
 *
 * 和「全部歌曲 / 我喜欢」并排放在音乐库顶部，而不是塞进设置页：统计是"我的音乐"的一部分，
 * 用户是在看自己的收藏和歌单时顺手想到"我到底听了多少"。
 */
@Composable
private fun ListeningSection(
    onStatsClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column {
        SonSectionHeader(title = stringResource(R.string.mymusic_section_listening))
        LibraryShortcutItem(
            icon = SymbolIconName.BarChart,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            title = stringResource(R.string.mymusic_stats_label),
            subtitle = stringResource(R.string.mymusic_stats_subtitle),
            onClick = onStatsClick
        )
        LibraryShortcutItem(
            icon = SymbolIconName.GraphicEq,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            title = stringResource(R.string.mymusic_leaderboard_label),
            subtitle = stringResource(R.string.mymusic_leaderboard_subtitle),
            onClick = onLeaderboardClick
        )
    }
}

/** 全部歌曲：本地曲库的完整列表，点进去是 SongsScreen（排序 / 多选 / 播放全部都在那儿）。 */
@Composable
private fun AllSongsItem(onClick: () -> Unit) {
    LibraryShortcutItem(
        icon = SymbolIconName.MusicNote,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        title = stringResource(R.string.mymusic_all_songs_label),
        subtitle = stringResource(R.string.mymusic_all_songs_subtitle),
        onClick = onClick
    )
}

@Composable
private fun FavoriteSongsItem(onClick: () -> Unit) {
    LibraryShortcutItem(
        icon = SymbolIconName.Favorite,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        title = stringResource(R.string.mymusic_favorites_label),
        subtitle = stringResource(R.string.mymusic_favorites_subtitle),
        onClick = onClick
    )
}

/**
 * 音乐库里的固定入口行：图标放在同尺寸的圆角容器里，保证两个入口的左侧节奏完全一致。
 */
@Composable
private fun LibraryShortcutItem(
    icon: SymbolIconName,
    containerColor: Color,
    contentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(SonSizes.thumbnail)
                    .clip(MaterialTheme.shapes.small)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = icon,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.iconMedium),
                    tint = contentColor
                )
            }
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun EmptyPlaylistsHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SonSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(SonSpacing.extraLarge))
        M3SymbolIcon(
            name = SymbolIconName.QueueMusic,
            filled = false,
            contentDescription = null,
            modifier = Modifier.size(SonSizes.iconExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(SonSpacing.medium))
        Text(
            text = stringResource(R.string.mymusic_empty_playlists),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(SonSpacing.small))
        Text(
            text = stringResource(R.string.mymusic_empty_playlists_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        onClick = onClick,
        leadingContent = {
            M3Artwork(
                artwork = playlist.albumArt,
                placeholderIcon = SymbolIconName.QueueMusic,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        },
        content = {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.mymusic_song_count, playlist.numberOfSongs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    M3SymbolIcon(
                        name = SymbolIconName.MoreVert,
                        filled = true,
                        contentDescription = stringResource(R.string.mymusic_more_desc)
                    )
                }
                DropdownMenuPopup(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    MenuItemGroup(
                        items = listOf(
                            MenuItem(
                                name = stringResource(R.string.mymusic_rename),
                                icon = SymbolIconName.Edit,
                                onClick = onRename
                            ),
                            MenuItem(
                                name = stringResource(R.string.mymusic_delete),
                                isDestructive = true,
                                icon = SymbolIconName.Delete,
                                onClick = onDelete
                            )
                        ),
                        onDismiss = { showMenu = false }
                    )
                }
            }
        }
    )
}

// ════════════════════════════════════════════════════════
// 新建播放列表（手动 + AI）
// ════════════════════════════════════════════════════════

private enum class CreateMode { MANUAL, AI }
private enum class AiMode { RANDOM, THEME }

@Composable
private fun CreatePlaylistDialog(
    uiState: MyMusicUiState,
    onManualCreate: (String) -> Unit,
    onIntent: (MyMusicIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var createMode by remember { mutableStateOf(CreateMode.MANUAL) }
    var manualName by remember { mutableStateOf("") }
    var aiMode by remember { mutableStateOf(AiMode.RANDOM) }
    var sliderValue by remember { mutableFloatStateOf(uiState.selectedSongCount.toFloat()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SonSpacing.large)
                .padding(bottom = SonSpacing.extraLarge)
        ) {
            val createModeOptions = listOf(
                stringResource(R.string.mymusic_create_manual),
                stringResource(R.string.mymusic_create_ai)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                createModeOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = (if (createMode == CreateMode.MANUAL) 0 else 1) == index,
                        onClick = {
                            createMode = if (index == 0) CreateMode.MANUAL else CreateMode.AI
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = createModeOptions.size),
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(SonSpacing.medium))

            when (createMode) {
                CreateMode.MANUAL -> {
                    Text(
                        text = stringResource(R.string.mymusic_new_playlist_dialog_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.mymusic_new_playlist_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Spacer(Modifier.height(SonSpacing.large))
                    Button(
                        onClick = {
                            if (manualName.isNotBlank()) {
                                onManualCreate(manualName.trim())
                            }
                        },
                        enabled = manualName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.mymusic_create)) }
                    Spacer(Modifier.height(SonSpacing.small))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mymusic_cancel))
                    }
                }

                CreateMode.AI -> {
                    val minCount = 5
                    val maxCount = if (aiMode == AiMode.RANDOM) 50 else 30
                    val showResults = uiState.isAiGenerating ||
                        uiState.aiGeneratedSongs.isNotEmpty() ||
                        uiState.aiCreateError != null

                    Text(
                        text = stringResource(R.string.mymusic_ai_dialog_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    Text(
                        text = stringResource(R.string.mymusic_ai_dialog_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(SonSpacing.mediumSmall))
                    val aiModeOptions = listOf(
                        stringResource(R.string.mymusic_ai_random),
                        stringResource(R.string.mymusic_ai_theme)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        aiModeOptions.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = (if (aiMode == AiMode.RANDOM) 0 else 1) == index,
                                onClick = {
                                    aiMode = if (index == 0) AiMode.RANDOM else AiMode.THEME
                                    onIntent(
                                        MyMusicIntent.SetAiCreateMode(
                                            if (aiMode == AiMode.RANDOM) AiCreateMode.RANDOM else AiCreateMode.THEME
                                        )
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = aiModeOptions.size),
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(SonSpacing.small))
                    Text(
                        text = if (aiMode == AiMode.RANDOM) {
                            stringResource(R.string.mymusic_ai_random_desc)
                        } else {
                            stringResource(R.string.mymusic_ai_theme_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showResults) {
                        Spacer(Modifier.height(SonSpacing.medium))
                        Text(
                            text = uiState.aiGeneratedPlaylistName.ifEmpty {
                                if (aiMode == AiMode.RANDOM) stringResource(R.string.mymusic_ai_fallback_name) else uiState.themeInput
                            },
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(SonSpacing.mediumSmall))
                        Column(Modifier.fillMaxWidth().height(previewListHeight)) {
                            when {
                                uiState.isAiGenerating -> Box(
                                    Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        LoadingIndicator()
                                        Spacer(Modifier.height(SonSpacing.medium))
                                        Text(
                                            text = stringResource(R.string.mymusic_ai_generating),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                uiState.aiCreateError != null -> Box(
                                    Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.mymusic_ai_generate_failed),
                                            style = MaterialTheme.typography.titleMediumEmphasized,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(SonSpacing.small))
                                        Text(
                                            text = stringResource(uiState.aiCreateError),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                uiState.aiGeneratedSongs.isEmpty() -> Box(
                                    Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.mymusic_ai_no_songs),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                                    items(uiState.aiGeneratedSongs, key = { it.id }) { song ->
                                        PreviewSongItem(song)
                                    }
                                }
                            }
                            if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                                Spacer(Modifier.height(SonSpacing.small))
                                Text(
                                    text = stringResource(R.string.mymusic_ai_song_count_total, uiState.aiGeneratedSongs.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(SonSpacing.medium))
                        if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                            Button(
                                onClick = { onIntent(MyMusicIntent.ConfirmAiPlaylist); onDismiss() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.mymusic_ai_create_confirm)) }
                            Spacer(Modifier.height(SonSpacing.small))
                            TextButton(
                                onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.mymusic_ai_regenerate)) }
                        }
                    } else {
                        Spacer(Modifier.height(SonSpacing.medium))
                        if (aiMode == AiMode.THEME) {
                            OutlinedTextField(
                                value = uiState.themeInput,
                                onValueChange = { onIntent(MyMusicIntent.SetThemeInput(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.mymusic_ai_theme_label)) },
                                placeholder = { Text(stringResource(R.string.mymusic_ai_theme_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                            )
                            Spacer(Modifier.height(SonSpacing.medium))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = stringResource(R.string.mymusic_ai_song_count_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.mymusic_ai_song_count_value, sliderValue.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(SonSpacing.small))
                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                sliderValue = it
                                onIntent(MyMusicIntent.SetSelectedSongCount(it.toInt()))
                            },
                            valueRange = minCount.toFloat()..maxCount.toFloat(),
                            steps = maxCount - minCount - 1
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "$minCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$maxCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(SonSpacing.large))
                        Button(
                            onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                            enabled = !(aiMode == AiMode.THEME && uiState.themeInput.isBlank()),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.mymusic_ai_generate)) }
                    }

                    Spacer(Modifier.height(SonSpacing.small))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mymusic_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSongItem(song: Song, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            M3Artwork(
                artwork = song.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        },
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun RenamePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mymusic_rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.mymusic_rename_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onRename(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.mymusic_rename_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mymusic_rename_cancel))
            }
        }
    )
}
