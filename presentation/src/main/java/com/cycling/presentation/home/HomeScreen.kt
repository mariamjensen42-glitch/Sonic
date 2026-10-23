package com.cycling.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleCard
import com.cycling.core.ui.apple.AppleCarousel
import com.cycling.core.ui.apple.AppleChevron
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleLargeTitle
import com.cycling.core.ui.apple.AppleSectionHeader
import com.cycling.core.ui.apple.AppleSongRow
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.presentation.R
import java.time.LocalTime

/**
 * 首页，按 Apple Music 的 Listen Now 结构重建。
 *
 * 大标题（34sp）+ 若干「分区标题 + 横滑卡组」；收藏与常播用 48dp 封面行。
 * 视觉令牌全部来自 [AppleTheme]（core/ui/apple），排版不走 Material 字阶。
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
    val colors = AppleTheme.colors

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            // 大标题自己不吃 inset：Material 的 Scaffold/TopAppBar 会代劳，换成自绘标题后必须补回来
            .statusBarsPadding()
    ) {
        when {
            // 加载中留空，避免空态文案闪一下
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize())

            isEmpty -> HomeMessage(
                text = stringResource(R.string.songs_empty_title),
                actionLabel = stringResource(R.string.mymusic_all_songs_label),
                onAction = onNavigateToSongs
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    // 悬浮胶囊不吃布局高度，这里要把导航栏 inset 一并让出来
                    bottom = appleBottomBarClearance() + bottomPadding
                )
            ) {
                item {
                    AppleLargeTitle(
                        title = homeGreeting(),
                        subtitle = stringResource(R.string.home_greeting_subtitle),
                        actions = {
                            AppleIconButton(
                                icon = SymbolIconName.Search,
                                contentDescription = stringResource(R.string.search_icon_desc),
                                onClick = onNavigateToSearch
                            )
                            AppleIconButton(
                                icon = SymbolIconName.Settings,
                                contentDescription = stringResource(R.string.home_settings_desc),
                                onClick = onNavigateToSettings
                            )
                        }
                    )
                }

                // ── 最近播放 ───────────────────────────────────────────
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        AppleSectionHeader(
                            title = stringResource(R.string.home_recent_plays),
                            onClick = { viewModel.handleIntent(HomeIntent.NavigateToSongs) }
                        )
                    }
                    item {
                        AppleCarousel {
                            items(uiState.recentlyPlayed, key = { "recentPlayed-${it.id}" }) { song ->
                                AppleCard(
                                    artwork = song.albumArt,
                                    title = song.title,
                                    subtitle = song.artist,
                                    onClick = { viewModel.handleIntent(HomeIntent.SongClick(song)) }
                                )
                            }
                        }
                    }
                }

                // ── 推荐专辑 ───────────────────────────────────────────
                if (uiState.topAlbums.isNotEmpty()) {
                    item {
                        AppleSectionHeader(
                            title = stringResource(R.string.home_recommended_albums),
                            onClick = { viewModel.handleIntent(HomeIntent.NavigateToAlbums) }
                        )
                    }
                    item {
                        AppleCarousel {
                            items(uiState.topAlbums, key = { "album-${it.id}" }) { album ->
                                AppleCard(
                                    artwork = album.albumArt,
                                    title = album.name,
                                    subtitle = album.artist,
                                    onClick = { viewModel.handleIntent(HomeIntent.AlbumClick(album)) }
                                )
                            }
                        }
                    }
                }

                // ── 推荐歌手（圆形封面） ────────────────────────────────
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        AppleSectionHeader(
                            title = stringResource(R.string.home_recommended_artists),
                            onClick = { viewModel.handleIntent(HomeIntent.NavigateToArtists) }
                        )
                    }
                    item {
                        AppleCarousel {
                            items(uiState.topArtists, key = { "artist-${it.id}" }) { artist ->
                                AppleCard(
                                    artwork = artist.artistArt,
                                    title = artist.name,
                                    subtitle = stringResource(R.string.home_artist_label),
                                    onClick = { viewModel.handleIntent(HomeIntent.ArtistClick(artist)) },
                                    width = 132.dp,
                                    circular = true
                                )
                            }
                        }
                    }
                }

                // ── 收藏歌曲 ───────────────────────────────────────────
                if (uiState.favoriteSongs.isNotEmpty()) {
                    item {
                        AppleSectionHeader(
                            title = stringResource(R.string.home_favorite_songs),
                            onClick = onNavigateToMyMusic
                        )
                    }
                    items(uiState.favoriteSongs.take(5), key = { "favorite-${it.id}" }) { song ->
                        AppleSongRow(
                            artwork = song.albumArt,
                            title = song.title,
                            subtitle = song.artist,
                            onClick = { viewModel.handleIntent(HomeIntent.SongClick(song)) },
                            trailing = { AppleChevron() }
                        )
                    }
                }

                // ── 最近添加 ───────────────────────────────────────────
                if (uiState.recentlyAdded.isNotEmpty()) {
                    item {
                        AppleSectionHeader(
                            title = stringResource(R.string.home_recently_added),
                            onClick = { viewModel.handleIntent(HomeIntent.NavigateToSongs) }
                        )
                    }
                    item {
                        AppleCarousel {
                            items(uiState.recentlyAdded, key = { "recentlyAdded-${it.id}" }) { song ->
                                AppleCard(
                                    artwork = song.albumArt,
                                    title = song.title,
                                    subtitle = song.artist,
                                    onClick = { viewModel.handleIntent(HomeIntent.SongClick(song)) }
                                )
                            }
                        }
                    }
                }

                // ── 常播歌曲 ───────────────────────────────────────────
                if (uiState.mostPlayed.isNotEmpty()) {
                    item {
                        AppleSectionHeader(title = stringResource(R.string.home_most_played))
                    }
                    items(uiState.mostPlayed.take(5), key = { "mostPlayed-${it.id}" }) { song ->
                        AppleSongRow(
                            artwork = song.albumArt,
                            title = song.title,
                            subtitle = song.artist,
                            onClick = { viewModel.handleIntent(HomeIntent.SongClick(song)) }
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

/** 加载 / 空态：居中一行说明，可选文字动作。 */
@Composable
private fun HomeMessage(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppleDims.padding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = AppleType.body,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = actionLabel,
                style = AppleType.body,
                color = colors.key,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}
