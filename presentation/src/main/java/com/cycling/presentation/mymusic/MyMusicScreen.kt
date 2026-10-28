@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.mymusic

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleLargeListRow
import com.cycling.core.ui.apple.AppleLargeTitle
import com.cycling.core.ui.apple.AppleMenuRow
import com.cycling.core.ui.apple.AppleSectionHeader
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.MeloXPlaylistBackdrop
import com.cycling.presentation.components.MeloXPlaylistDescription
import com.cycling.presentation.components.MeloXPlaylistHero
import com.cycling.presentation.components.MeloXPlaylistToolbar
import com.cycling.presentation.components.MenuItem
import com.cycling.presentation.components.MenuItemGroup
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.SongQueueActions
import com.cycling.presentation.components.formatCollectionDuration
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
                is MyMusicEffect.NavigateToPlaylistDetail -> {
                    // 在线歌单导入成功后会直接跳到新歌单；对话框是本地状态，不在这里关掉的话
                    // 用户从详情页返回时会看到它还盖在列表上
                    showCreateDialog = false
                    onNavigateToPlaylistDetail(effect.playlistId)
                }
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
            .background(AppleTheme.colors.background)
            // 收藏子页自带工具条与整屏模糊背景，状态栏由它自己吃；
            // 只有歌单列表（自绘大标题）需要在这里补状态栏间距。
            .then(if (uiState.showFavoriteSongs) Modifier else Modifier.statusBarsPadding())
    ) {
        if (!uiState.showFavoriteSongs) {
            AppleLargeTitle(
                title = stringResource(R.string.mymusic_title_library),
                subtitle = stringResource(R.string.mymusic_subtitle_library, uiState.playlists.size),
                actions = {
                    AppleIconButton(
                        icon = SymbolIconName.Search,
                        contentDescription = stringResource(R.string.mymusic_search_desc),
                        onClick = onNavigateToSearch
                    )
                    AppleIconButton(
                        icon = SymbolIconName.Add,
                        contentDescription = stringResource(R.string.mymusic_create_playlist_desc),
                        onClick = { showCreateDialog = true }
                    )
                }
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
                contentModifier = Modifier
            )
        }
    }
}

// ════════════════════════════════════════════════════════
// 收藏歌曲
// ════════════════════════════════════════════════════════

/**
 * 收藏页：整页照「歌单详情」的 MeloX 版式实现 ——
 * 封面模糊铺满 + 58dp 玻璃工具条 + 居中封面英雄区 + 整段可点的简介 + 封面行曲目表。
 *
 * 这一页没有歌单名可读，标题恒为「我喜欢」，封面取第一首收藏曲目的专辑封面；
 * 一首都没有时英雄区照样渲染（0 首歌曲、动作禁用），标题与返回就不会消失。
 */
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
    val title = stringResource(R.string.mymusic_title_favorites)
    val artwork = songs.firstOrNull()?.albumArt
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppleTheme.colors.background,
        // 与歌单详情同一套：状态栏交给工具条，模糊背景一路铺到屏幕顶端。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MeloXPlaylistToolbar(
                onBack = onNavigateBack,
                backContentDescription = stringResource(R.string.mymusic_back_desc),
                backIcon = SymbolIconName.ArrowBack,
                contentColor = AppleTheme.colors.label
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MeloXPlaylistBackdrop(
                artwork = artwork,
                placeholderIcon = SymbolIconName.Favorite
            )

            if (isLoading) {
                // 收藏曲目来自 Room，几乎瞬间就到：只有真慢了才画转圈（见 SonLoadingState）。
                SonLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = appleBottomBarClearance() + bottomPadding
                    )
                ) {
                    item(key = "favorites-hero") {
                        MeloXPlaylistHero(
                            title = title,
                            // 收藏集没有创作者一栏，副标题留空（与本地歌单一致）。
                            subtitle = "",
                            meta = favoritesMeta(songs),
                            artwork = artwork,
                            placeholderIcon = SymbolIconName.Favorite,
                            onPlay = { songs.firstOrNull()?.let(onSongClick) },
                            onShuffle = onShuffle,
                            hasSongs = songs.isNotEmpty(),
                            // 收藏曲目本身不属于任何歌单，「添加到歌单」不挂。
                            footer = {
                                MeloXPlaylistDescription(
                                    description = description,
                                    expanded = isDescriptionExpanded,
                                    isGenerating = isDescriptionGenerating,
                                    onToggle = onToggleDescription
                                )
                            }
                        )
                    }

                    if (songs.isEmpty()) {
                        item(key = "favorites-empty") { FavoritesEmptyHint() }
                    } else {
                        itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                            // 每行自己的展开状态：菜单开着时点别处就关，开第二行时第一行自动收起。
                            var menuExpanded by remember(song.id) { mutableStateOf(false) }
                            SongListItem(
                                song = song,
                                subtitle = song.artist,
                                isCurrentSong = song.id == currentSongId,
                                // quiet surface：与歌单详情一致，不画分割线、行首不显序号。
                                showDivider = false,
                                showMoreButton = queueActions != null,
                                moreMenuExpanded = menuExpanded,
                                onMoreClick = { menuExpanded = true },
                                onMoreDismiss = { menuExpanded = false },
                                // 取消收藏收进 ⋮ 菜单：行尾不再单独挂一颗心，行宽与歌单详情对齐。
                                moreMenuItems = songMenuItems(
                                    song = song,
                                    queueActions = queueActions,
                                    onToggleFavorite = onToggleFavorite
                                ),
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 英雄区元数据行：`42 首歌曲 · 2 小时 30 分钟`，与歌单详情同一句。 */
@Composable
private fun favoritesMeta(songs: List<Song>): String {
    if (songs.isEmpty()) return ""
    return listOf(
        stringResource(R.string.collection_detail_song_count, songs.size),
        formatCollectionDuration(songs.sumOf { it.duration })
    ).joinToString(" · ")
}

/** 一首都没喜欢时的提示块，排在英雄区之下（标题与返回由英雄区/工具条保住）。 */
@Composable
private fun FavoritesEmptyHint() {
    val colors = AppleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppleDims.padding, vertical = AppleDims.topMargin),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        M3SymbolIcon(
            name = SymbolIconName.Favorite,
            filled = false,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.tertiaryLabel
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.mymusic_empty_favorites_title),
            style = AppleType.body,
            color = colors.secondaryLabel
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.mymusic_empty_favorites_subtitle),
            style = AppleType.footnote,
            color = colors.tertiaryLabel
        )
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
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(contentModifier),
        contentPadding = PaddingValues(
            // 悬浮胶囊不吃布局高度，导航栏 inset 要自己让出来
            bottom = appleBottomBarClearance() + bottomPadding
        )
    ) {
        // ── 固定入口：Apple 资料库的图标菜单行 ──
        item {
            Column {
                AppleMenuRow(
                    icon = SymbolIconName.MusicNote,
                    label = stringResource(R.string.mymusic_all_songs_label),
                    onClick = onAllSongsClick
                )
                AppleSeparator(leadingInset = 58.dp)
                AppleMenuRow(
                    icon = SymbolIconName.Favorite,
                    label = stringResource(R.string.mymusic_favorites_label),
                    onClick = onFavoriteSongsClick
                )
                AppleSeparator(leadingInset = 58.dp)
                AppleMenuRow(
                    icon = SymbolIconName.BarChart,
                    label = stringResource(R.string.mymusic_stats_label),
                    onClick = onStatsClick
                )
                AppleSeparator(leadingInset = 58.dp)
                AppleMenuRow(
                    icon = SymbolIconName.GraphicEq,
                    label = stringResource(R.string.mymusic_leaderboard_label),
                    onClick = onLeaderboardClick
                )
                AppleSeparator(leadingInset = 58.dp)
            }
        }

        item {
            AppleSectionHeader(title = stringResource(R.string.mymusic_section_playlists))
        }

        if (playlists.isEmpty()) {
            item { EmptyPlaylistsHint() }
        } else {
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


@Composable
private fun EmptyPlaylistsHint() {
    val colors = AppleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppleDims.padding * 2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(AppleDims.topMargin))
        M3SymbolIcon(
            name = SymbolIconName.QueueMusic,
            filled = false,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.tertiaryLabel
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.mymusic_empty_playlists),
            style = AppleType.body,
            color = colors.secondaryLabel
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.mymusic_empty_playlists_subtitle),
            style = AppleType.footnote,
            color = colors.tertiaryLabel
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
    AppleLargeListRow(
        artwork = playlist.albumArt,
        title = playlist.name,
        subtitle = stringResource(R.string.mymusic_song_count, playlist.numberOfSongs),
        onClick = onClick,
        trailing = {
            Box {
                AppleIconButton(
                    icon = SymbolIconName.MoreVert,
                    contentDescription = stringResource(R.string.mymusic_more_desc),
                    onClick = { showMenu = true },
                    tint = AppleTheme.colors.secondaryLabel,
                    size = 20.dp
                )
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

private enum class CreateMode { MANUAL, AI, ONLINE }
private enum class AiMode { RANDOM, THEME }

/**
 * 「我的歌单」列表的高度。
 *
 * 必须给死：外层是 `verticalScroll` 的 Column，LazyColumn 在这种无限高约束下无法测量。
 * 四行半的高度既能让用户看出「这里可以滚」，又不会把下面的导入按钮挤出屏幕。
 */
private val MY_PLAYLIST_LIST_HEIGHT = 208.dp

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

    // 「我的歌单」只有切到导入页时才问：另外两个 tab 不需要它，提前请求纯属浪费一次往返。
    LaunchedEffect(createMode) {
        if (createMode == CreateMode.ONLINE) onIntent(MyMusicIntent.LoadMyPlaylists)
    }

    AppleSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = SonSpacing.extraLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreateMode.entries.forEach { mode ->
                    AppleChip(
                        label = stringResource(
                            when (mode) {
                                CreateMode.MANUAL -> R.string.mymusic_create_manual
                                CreateMode.AI -> R.string.mymusic_create_ai
                                CreateMode.ONLINE -> R.string.mymusic_create_import
                            }
                        ),
                        selected = createMode == mode,
                        onClick = { createMode = mode }
                    )
                }
            }
            Spacer(Modifier.height(SonSpacing.medium))

            when (createMode) {
                CreateMode.MANUAL -> {
                    Text(
                        text = stringResource(R.string.mymusic_new_playlist_dialog_title),
                        style = AppleType.headline,
                        color = AppleTheme.colors.label
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    AppleTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        placeholder = stringResource(R.string.mymusic_new_playlist_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(SonSpacing.large))
                    AppleActionButton(
                        label = stringResource(R.string.mymusic_create),
                        onClick = {
                            if (manualName.isNotBlank()) {
                                onManualCreate(manualName.trim())
                            }
                        },
                        enabled = manualName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(SonSpacing.small))
                    AppleActionButton(
                        label = stringResource(R.string.mymusic_cancel),
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CreateMode.AI -> {
                    val minCount = 5
                    val maxCount = if (aiMode == AiMode.RANDOM) 50 else 30
                    val showResults = uiState.isAiGenerating ||
                        uiState.aiGeneratedSongs.isNotEmpty() ||
                        uiState.aiCreateError != null

                    Text(
                        text = stringResource(R.string.mymusic_ai_dialog_title),
                        style = AppleType.headline,
                        color = AppleTheme.colors.label
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    Text(
                        text = stringResource(R.string.mymusic_ai_dialog_subtitle),
                        style = AppleType.footnote,
                        color = AppleTheme.colors.secondaryLabel
                    )
                    Spacer(Modifier.height(SonSpacing.mediumSmall))
                    val aiModeOptions = listOf(
                        stringResource(R.string.mymusic_ai_random),
                        stringResource(R.string.mymusic_ai_theme)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aiModeOptions.forEachIndexed { index, label ->
                            AppleChip(
                                label = label,
                                selected = (if (aiMode == AiMode.RANDOM) 0 else 1) == index,
                                onClick = {
                                    aiMode = if (index == 0) AiMode.RANDOM else AiMode.THEME
                                    onIntent(
                                        MyMusicIntent.SetAiCreateMode(
                                            if (aiMode == AiMode.RANDOM) AiCreateMode.RANDOM else AiCreateMode.THEME
                                        )
                                    )
                                }
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
                        style = AppleType.caption1,
                        color = AppleTheme.colors.secondaryLabel
                    )

                    if (showResults) {
                        Spacer(Modifier.height(SonSpacing.medium))
                        Text(
                            text = uiState.aiGeneratedPlaylistName.ifEmpty {
                                if (aiMode == AiMode.RANDOM) stringResource(R.string.mymusic_ai_fallback_name) else uiState.themeInput
                            },
                            style = AppleType.headline,
                            color = AppleTheme.colors.label,
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
                                            style = AppleType.footnote,
                                            color = AppleTheme.colors.secondaryLabel
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
                                            style = AppleType.headline,
                                            color = AppleTheme.colors.systemRed
                                        )
                                        Spacer(Modifier.height(SonSpacing.small))
                                        Text(
                                            text = stringResource(uiState.aiCreateError),
                                            style = AppleType.caption1,
                                            color = AppleTheme.colors.secondaryLabel
                                        )
                                    }
                                }
                                uiState.aiGeneratedSongs.isEmpty() -> Box(
                                    Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.mymusic_ai_no_songs),
                                        style = AppleType.footnote,
                                        color = AppleTheme.colors.secondaryLabel
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
                                    style = AppleType.caption1,
                                    color = AppleTheme.colors.secondaryLabel
                                )
                            }
                        }
                        Spacer(Modifier.height(SonSpacing.medium))
                        if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                            AppleActionButton(
                                label = stringResource(R.string.mymusic_ai_create_confirm),
                                onClick = {
                                    onIntent(MyMusicIntent.ConfirmAiPlaylist)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(SonSpacing.small))
                            AppleActionButton(
                                label = stringResource(R.string.mymusic_ai_regenerate),
                                onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                                filled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Spacer(Modifier.height(SonSpacing.medium))
                        if (aiMode == AiMode.THEME) {
                            Text(
                                text = stringResource(R.string.mymusic_ai_theme_label),
                                style = AppleType.footnote,
                                color = AppleTheme.colors.secondaryLabel
                            )
                            Spacer(Modifier.height(4.dp))
                            AppleTextField(
                                value = uiState.themeInput,
                                onValueChange = { onIntent(MyMusicIntent.SetThemeInput(it)) },
                                placeholder = stringResource(R.string.mymusic_ai_theme_placeholder)
                            )
                            Spacer(Modifier.height(SonSpacing.medium))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = stringResource(R.string.mymusic_ai_song_count_label),
                                style = AppleType.footnote,
                                color = AppleTheme.colors.label
                            )
                            Text(
                                text = stringResource(R.string.mymusic_ai_song_count_value, sliderValue.toInt()),
                                style = AppleType.footnote,
                                color = AppleTheme.colors.key
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
                                style = AppleType.caption1,
                                color = AppleTheme.colors.secondaryLabel
                            )
                            Text(
                                text = "$maxCount",
                                style = AppleType.caption1,
                                color = AppleTheme.colors.secondaryLabel
                            )
                        }
                        Spacer(Modifier.height(SonSpacing.large))
                        AppleActionButton(
                            label = stringResource(R.string.mymusic_ai_generate),
                            onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                            enabled = !(aiMode == AiMode.THEME && uiState.themeInput.isBlank()),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(SonSpacing.small))
                    AppleActionButton(
                        label = stringResource(R.string.mymusic_cancel),
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CreateMode.ONLINE -> {
                    Text(
                        text = stringResource(R.string.mymusic_import_title),
                        style = AppleType.headline,
                        color = AppleTheme.colors.label
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    Text(
                        text = stringResource(R.string.mymusic_import_subtitle),
                        style = AppleType.footnote,
                        color = AppleTheme.colors.secondaryLabel
                    )
                    Spacer(Modifier.height(SonSpacing.medium))
                    AppleTextField(
                        value = uiState.importInput,
                        onValueChange = { onIntent(MyMusicIntent.SetImportInput(it)) },
                        placeholder = stringResource(R.string.mymusic_import_placeholder),
                        singleLine = false,
                        minHeight = 64.dp
                    )
                    // 登录账号名下的歌单：比粘贴链接短一步，点一下直接导入。
                    // 未登录或读取失败时列表为空，这一整块（含标题）都不出现。
                    if (uiState.myPlaylists.isNotEmpty()) {
                        Spacer(Modifier.height(SonSpacing.large))
                        Text(
                            text = stringResource(R.string.mymusic_import_my_playlists),
                            style = AppleType.headline,
                            color = AppleTheme.colors.label
                        )
                        Spacer(Modifier.height(SonSpacing.small))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MY_PLAYLIST_LIST_HEIGHT)
                        ) {
                            items(uiState.myPlaylists, key = { it.id }) { playlist ->
                                ListItem(
                                    // 整行可点：点一下就直接导入这张歌单。
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onIntent(MyMusicIntent.ImportMyPlaylist(playlist.id))
                                        },
                                    leadingContent = {
                                        M3Artwork(
                                            artwork = playlist.coverUrl,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = playlist.title,
                                            style = AppleType.body,
                                            color = AppleTheme.colors.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = stringResource(
                                                R.string.mymusic_import_my_playlist_tracks,
                                                playlist.trackCount
                                            ),
                                            style = AppleType.caption1,
                                            color = AppleTheme.colors.secondaryLabel,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                    // 进度和错误共用一行：空着的时候不该多出两行空白把按钮往下推
                    val importError = uiState.importError
                    val importStatus = when {
                        importError != null -> stringResource(importError)
                        uiState.isImporting && uiState.importTotal > 0 -> stringResource(
                            R.string.mymusic_import_progress,
                            uiState.importDone,
                            uiState.importTotal
                        )
                        uiState.isImporting -> stringResource(R.string.mymusic_import_fetching)
                        else -> null
                    }
                    if (importStatus != null) {
                        Spacer(Modifier.height(SonSpacing.small))
                        Text(
                            text = importStatus,
                            style = AppleType.footnote,
                            color = if (importError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                AppleTheme.colors.secondaryLabel
                            }
                        )
                    }
                    Spacer(Modifier.height(SonSpacing.large))
                    AppleActionButton(
                        label = stringResource(R.string.mymusic_import_action),
                        onClick = { onIntent(MyMusicIntent.ImportOnlinePlaylist) },
                        enabled = uiState.importInput.isNotBlank() && !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(SonSpacing.small))
                    AppleActionButton(
                        label = stringResource(R.string.mymusic_cancel),
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSongItem(song: Song, modifier: Modifier = Modifier) {
    AppleSheetRow(
        modifier = modifier,
        title = song.title,
        subtitle = song.artist,
        leading = {
            M3Artwork(
                artwork = song.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = RoundedCornerShape(AppleDims.cornerS),
                modifier = Modifier.size(40.dp)
            )
        }
    )
}

@Composable
private fun RenamePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(playlist.name) }
    AppleAlert(
        title = stringResource(R.string.mymusic_rename_dialog_title),
        confirmLabel = stringResource(R.string.mymusic_rename_confirm),
        onConfirm = { if (name.isNotBlank()) onRename(name.trim()) },
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.mymusic_rename_cancel),
        confirmEnabled = name.isNotBlank()
    ) {
        AppleTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.mymusic_rename_placeholder)
        )
    }
}
