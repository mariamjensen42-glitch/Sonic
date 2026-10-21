@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3ArtistCard
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonColorFieldBackground
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.SonSectionHeader
import java.time.LocalTime

/**
 * 首页（试点 2，#19）。
 *
 * 构图语法：一个 hero（最近播放的非对称大卡 + primaryContainer 光晕）+ 一个
 * 官方 `ButtonGroup` 组成的快速入口簇 + 若干安静的分区横滑；标题时刻由
 * `LargeFlexibleTopAppBar` 的 display 级问候语承担。没有 FAB —— 导航外壳已定标准
 * `NavigationBar` 且不引入 FAB（#5），主页面的动作簇落在内容里而不是右下角。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToMyMusic: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val greeting = homeGreeting()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSongs -> onNavigateToSongs()
                is HomeEffect.NavigateToAlbums -> onNavigateToAlbums()
                is HomeEffect.NavigateToArtists -> onNavigateToArtists()
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is HomeEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is HomeEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is HomeEffect.NavigateToMyMusic -> onNavigateToMyMusic()
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToFolders -> onNavigateToFolders()
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isEmpty = !uiState.isLoading &&
        uiState.recentlyPlayed.isEmpty() &&
        uiState.topAlbums.isEmpty() &&
        uiState.topArtists.isEmpty() &&
        uiState.favoriteSongs.isEmpty() &&
        uiState.recentlyAdded.isEmpty() &&
        uiState.mostPlayed.isEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // 标题时刻：LargeFlexibleTopAppBar 的 display 级问候语 + 一行副标题；
            // 搜索与设置降为 actions，不占用首屏的内容高度。
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = greeting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.home_greeting_subtitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        M3SymbolIcon(
                            name = SymbolIconName.Search,
                            filled = true,
                            contentDescription = stringResource(R.string.search_icon_desc)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        M3SymbolIcon(
                            name = SymbolIconName.Settings,
                            filled = true,
                            contentDescription = stringResource(R.string.home_settings_desc)
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
    ) { innerPadding ->
        when {
            uiState.isLoading -> SonLoadingState(modifier = Modifier.padding(innerPadding))
            isEmpty -> SonEmptyState(
                icon = SymbolIconName.MusicNote,
                title = stringResource(R.string.songs_empty_title),
                subtitle = stringResource(R.string.songs_empty_subtitle),
                modifier = Modifier.padding(innerPadding),
                actionLabel = stringResource(R.string.mymusic_all_songs_label),
                onAction = onNavigateToSongs
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    bottom = SonLayout.bottomBarClearance + bottomPadding
                )
            ) {
                // === hero：最近播放（左大右小） ===
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        HomeRecentHero(
                            songs = uiState.recentlyPlayed,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 快速入口：官方 ButtonGroup，按下时按钮横向扩张 ===
                item {
                    HomeQuickActions(
                        onNavigateToSongs = { viewModel.handleIntent(HomeIntent.NavigateToSongs) },
                        onNavigateToAlbums = { viewModel.handleIntent(HomeIntent.NavigateToAlbums) },
                        onNavigateToArtists = { viewModel.handleIntent(HomeIntent.NavigateToArtists) }
                    )
                }

                // === 推荐专辑 ===
                if (uiState.topAlbums.isNotEmpty()) {
                    item {
                        SonSectionHeader(
                            title = stringResource(R.string.home_recommended_albums),
                            actionLabel = stringResource(R.string.home_view_all),
                            onAction = { viewModel.handleIntent(HomeIntent.NavigateToAlbums) }
                        )
                    }
                    item {
                        AlbumMediaRow(
                            albums = uiState.topAlbums,
                            onAlbumClick = { album ->
                                viewModel.handleIntent(HomeIntent.AlbumClick(album))
                            }
                        )
                    }
                }

                // === 推荐歌手 ===
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        SonSectionHeader(
                            title = stringResource(R.string.home_recommended_artists),
                            actionLabel = stringResource(R.string.home_view_all),
                            onAction = { viewModel.handleIntent(HomeIntent.NavigateToArtists) }
                        )
                    }
                    item {
                        ArtistMediaRow(
                            artists = uiState.topArtists,
                            onArtistClick = { artist ->
                                viewModel.handleIntent(HomeIntent.ArtistClick(artist))
                            }
                        )
                    }
                }

                // === 收藏歌曲 ===
                if (uiState.favoriteSongs.isNotEmpty()) {
                    item {
                        SonSectionHeader(
                            title = stringResource(R.string.home_favorite_songs),
                            actionLabel = stringResource(R.string.home_view_all),
                            onAction = onNavigateToMyMusic
                        )
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.favoriteSongs,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 最近添加 ===
                if (uiState.recentlyAdded.isNotEmpty()) {
                    item {
                        SonSectionHeader(
                            title = stringResource(R.string.home_recently_added),
                            actionLabel = stringResource(R.string.home_view_all),
                            onAction = { viewModel.handleIntent(HomeIntent.NavigateToSongs) }
                        )
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.recentlyAdded,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }

                // === 常播歌曲 ===
                if (uiState.mostPlayed.isNotEmpty()) {
                    item {
                        SonSectionHeader(title = stringResource(R.string.home_most_played))
                    }
                    item {
                        SongMediaRow(
                            songs = uiState.mostPlayed,
                            onSongClick = { song ->
                                viewModel.handleIntent(HomeIntent.SongClick(song))
                            }
                        )
                    }
                }
            }
        }
    }
}

/** 按当前时段取问候语。 */
@Composable
private fun homeGreeting(): String {
    val hour = LocalTime.now().hour
    return stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    )
}

// ════════════════════════════════════════════════════════════════
// hero：最近播放
// ════════════════════════════════════════════════════════════════

/**
 * 非对称 hero：左边一张大的最近播放卡（封面方形、`titleMediumEmphasized` 标题节奏），
 * 右边两行紧凑条目。背景是这一屏唯一的大面积色面：主题的 **primaryContainer**
 * 光晕，到大卡下沿收干净（`SonColorFieldBackground` 的 B 规则）。
 *
 * 这里刻意**不**为 hero 单独换一套封面配色：色面会画一层自己的 surface，若与页面
 * surface 不同就会在 hero 区留下一块可见的矩形。色面跟着页面主题走（当前播放封面或
 * 品牌色），封面自己的颜色由封面图像提供。
 */
@Composable
private fun HomeRecentHero(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val heroSong = songs.first()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val contentWidth = maxWidth - SonLayout.screenMargin * 2
        val bigWidth = (contentWidth - SonLayout.cardGap) * HeroBigCardRatio
        val smallWidth = contentWidth - SonLayout.cardGap - bigWidth
        // 大卡高度 = 方形封面 + 两行文字 + 上下留白；色面刚好在这里归零。
        val heroHeight = bigWidth + SonSpacing.extraExtraLarge + SonSpacing.extraLarge

        Box(modifier = Modifier.fillMaxWidth()) {
            SonColorFieldBackground(
                heroBottom = heroHeight,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SonLayout.screenMargin,
                        end = SonLayout.screenMargin,
                        top = SonSpacing.medium,
                        bottom = SonSpacing.medium
                    ),
                horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
            ) {
                HomeHeroCard(
                    song = heroSong,
                    width = bigWidth,
                    onClick = { onSongClick(heroSong) }
                )

                Column(modifier = Modifier.width(smallWidth)) {
                    songs.drop(1).take(HeroRowCount).forEach { song ->
                        HomeHeroRow(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                        Spacer(modifier = Modifier.height(SonSpacing.small))
                    }
                }
            }
        }
    }
}

/** 大卡在 hero 里占的比例：非对称的来源，改这一处就能调构图。 */
private const val HeroBigCardRatio = 0.58f

/** 右侧紧凑条目数；和 0.58 的比例配起来刚好与左侧大卡的封面高度接近。 */
private const val HeroRowCount = 2

@Composable
private fun HomeHeroCard(
    song: Song,
    width: Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
    ) {
        M3Artwork(
            artwork = song.albumArt,
            placeholderIcon = SymbolIconName.MusicNote,
            shape = MaterialTheme.shapes.largeIncreased,
            modifier = Modifier
                .fillMaxWidth()
                .height(width),
            contentDescription = song.title
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeHeroRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SonSpacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = song.albumArt,
            placeholderIcon = SymbolIconName.MusicNote,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(SonSizes.thumbnail),
            contentDescription = song.title
        )
        Spacer(modifier = Modifier.width(SonSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 快速入口
// ════════════════════════════════════════════════════════════════

/**
 * 官方 `ButtonGroup`：三个入口共享容器，按下时被按的那一项横向扩张，其余压缩。
 *
 * `overflowIndicator` 是 alpha29 的必填参数，这里不需要溢出菜单，传空实现。
 */
@Composable
private fun HomeQuickActions(
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit
) {
    // 标签必须在进入 ButtonGroup 之前取好：它的 content 是普通 lambda，
    // 里面不能调用 @Composable（stringResource 只能在外面算）。
    val songsLabel = stringResource(R.string.mymusic_all_songs_label)
    val albumsLabel = stringResource(R.string.albums_title)
    val artistsLabel = stringResource(R.string.artists_title)

    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SonLayout.screenMargin,
                end = SonLayout.screenMargin,
                top = SonSpacing.small,
                bottom = SonSpacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        clickableItem(
            onClick = onNavigateToSongs,
            label = songsLabel,
            icon = {
                M3SymbolIcon(
                    name = SymbolIconName.MusicNote,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            },
            weight = 1f
        )
        clickableItem(
            onClick = onNavigateToAlbums,
            label = albumsLabel,
            icon = {
                M3SymbolIcon(
                    name = SymbolIconName.Album,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            },
            weight = 1f
        )
        clickableItem(
            onClick = onNavigateToArtists,
            label = artistsLabel,
            icon = {
                M3SymbolIcon(
                    name = SymbolIconName.Person,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            },
            weight = 1f
        )
    }
}

// ════════════════════════════════════════════════════════════════
// 横向媒体列表
// ════════════════════════════════════════════════════════════════

@Composable
private fun SongMediaRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(songs, key = { it.id }) { song ->
            M3MediaCard(
                title = song.title,
                subtitle = song.artist,
                artwork = song.albumArt,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
private fun AlbumMediaRow(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(albums, key = { it.id }) { album ->
            M3MediaCard(
                title = album.name,
                subtitle = album.artist,
                artwork = album.albumArt,
                onClick = { onAlbumClick(album) },
                placeholderIcon = SymbolIconName.Album
            )
        }
    }
}

@Composable
private fun ArtistMediaRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.cardGap)
    ) {
        items(artists, key = { it.id }) { artist ->
            M3ArtistCard(
                name = artist.name,
                artwork = artist.artistArt,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}
