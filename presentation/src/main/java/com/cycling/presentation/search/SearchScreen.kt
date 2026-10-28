@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleLargeListRow
import com.cycling.core.ui.apple.AppleLargeTitle
import com.cycling.core.ui.apple.AppleSearchField
import com.cycling.core.ui.apple.AppleSongRow
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.components.SonGlyphIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.LyricsMatch
import com.cycling.domain.model.Song
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.toDownloadSong
import com.cycling.presentation.R
import com.cycling.presentation.download.LocalDownloadRequester

/*
 * 同一个 LazyColumn 里并列了在线结果 / 歌曲 / 歌词命中 / 专辑 / 艺人五个区块。
 * 这些区块的 id 空间彼此独立（MediaStore 的 songId、albumId、artistId 会撞号），
 * 而且同一首歌可以**同时**出现在「歌曲」与「歌词命中」两处（`SearchResult` 的文档写明了这一点），
 * 所以每个区块的 key 都必须带自己的前缀 —— 共用裸 `id` 会直接触发
 * `Key ... was already used` 崩溃。
 */

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToAlbumDetail: (Long) -> Unit = {},
    onNavigateToArtistDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is SearchEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is SearchEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is SearchEffect.ShowToast -> {
                    val text = effect.arg?.let { context.getString(effect.message, it) }
                        ?: context.getString(effect.message)
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val query = uiState.searchQuery
    val tab = uiState.selectedTab
    // 全部标签下三块都显示，单块标签下只显示对应的一块
    val showSongs = tab == SearchTab.ALL || tab == SearchTab.SONGS
    val showAlbums = tab == SearchTab.ALL || tab == SearchTab.ALBUMS
    val showArtists = tab == SearchTab.ALL || tab == SearchTab.ARTISTS
    // 歌词命中只在「全部」里出现：它不是第三个实体类型，而是「歌曲」的另一种命中方式。
    val showLyrics = tab == SearchTab.ALL
    val nothingFound = !uiState.isSearching && when (tab) {
        SearchTab.ALL -> uiState.songs.isEmpty() && uiState.albums.isEmpty() &&
            uiState.artists.isEmpty() && uiState.lyricsMatches.isEmpty()
        SearchTab.SONGS -> uiState.songs.isEmpty()
        SearchTab.ALBUMS -> uiState.albums.isEmpty()
        SearchTab.ARTISTS -> uiState.artists.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleTheme.colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleIconButton(
                icon = SymbolIconName.ArrowBack,
                contentDescription = stringResource(R.string.search_back_desc),
                onClick = onNavigateBack,
                tint = AppleTheme.colors.key
            )
        }
        AppleLargeTitle(title = stringResource(R.string.search_title))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = appleBottomBarClearance())
        ) {
            // 输入框吸顶：结果很长时也不必滚回顶部才能改词
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppleTheme.colors.background)
                        .padding(
                            horizontal = AppleDims.padding,
                            vertical = 8.dp
                        )
                ) {
                    AppleSearchField(
                        value = query,
                        onValueChange = {
                            viewModel.handleIntent(SearchIntent.SearchQueryChanged(it))
                        },
                        placeholder = stringResource(R.string.search_placeholder),
                        onSearch = {
                            viewModel.handleIntent(SearchIntent.SearchSubmitted(query))
                        },
                        onClear = { viewModel.handleIntent(SearchIntent.ClearSearch) }
                    )
                }
            }

            if (query.isBlank()) {
                // 没输入时显示历史记录：搜索过什么、清掉什么，本来就没有别的地方可以看到
                if (uiState.searchHistory.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = stringResource(R.string.search_history_title),
                            action = stringResource(R.string.search_clear_history),
                            onActionClick = { viewModel.handleIntent(SearchIntent.ClearHistory) }
                        )
                    }
                    item {
                        ChipRow(
                            items = uiState.searchHistory,
                            onItemClick = { viewModel.handleIntent(SearchIntent.HistoryItemClicked(it)) }
                        )
                    }
                }
            } else {
                // 输入过程中，命中历史的补全排在结果前面
                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_history_title),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = AppleType.sectionTitle,
                            color = AppleTheme.colors.label
                        )
                    }
                    item {
                        ChipRow(
                            items = uiState.suggestions,
                            onItemClick = { viewModel.handleIntent(SearchIntent.HistoryItemClicked(it)) }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = SearchTab.entries.map { stringResource(it.labelRes()) }
                        val selectedIndex = SearchTab.entries.indexOf(tab)
                        options.forEachIndexed { index, label ->
                            AppleChip(
                                label = label,
                                selected = index == selectedIndex,
                                onClick = {
                                    viewModel.handleIntent(
                                        SearchIntent.TabSelected(SearchTab.entries[index])
                                    )
                                }
                            )
                        }
                    }
                }

                if (showSongs) {
                    when {
                        uiState.onlineSearching -> item { OnlineSearchingRow() }

                        uiState.onlineResults.isNotEmpty() -> {
                            item {
                                Text(
                                    text = stringResource(R.string.search_online_section),
                                    modifier = Modifier.padding(
                                        start = SonSizes.listItemHorizontalPadding,
                                        top = SonSpacing.large,
                                        bottom = SonSpacing.small
                                    ),
                                    style = AppleType.footnoteEmphasized,
                                    color = AppleTheme.colors.key
                                )
                            }
                            // 同一首歌在各平台都有，混在一列里用户没法「只找网易云的版本」。
                            // 只有一个平台有结果时不显示切换条：一个 tab 的 tab 栏没有意义。
                            if (uiState.onlinePlatforms.size > 1) {
                                item {
                                    OnlinePlatformTabs(
                                        platforms = uiState.onlinePlatforms,
                                        selected = uiState.selectedOnlineSource,
                                        onSelect = {
                                            viewModel.handleIntent(
                                                SearchIntent.OnlineSourceSelected(it)
                                            )
                                        }
                                    )
                                }
                            }
                            items(uiState.visibleOnlineTracks, key = { "online-${it.onlineKey}" }) { track ->
                                OnlineSongRow(
                                    track = track,
                                    showSource = uiState.selectedOnlineSource == null,
                                    resolving = uiState.resolvingOnlineKey == track.onlineKey,
                                    onClick = {
                                        viewModel.handleIntent(SearchIntent.OnlineTrackClicked(track))
                                    }
                                )
                            }
                            if (uiState.onlineHasMore || uiState.onlineLoadingMore) {
                                item {
                                    OnlineLoadMoreRow(
                                        loading = uiState.onlineLoadingMore,
                                        onClick = {
                                            viewModel.handleIntent(SearchIntent.LoadMoreOnline)
                                        }
                                    )
                                }
                            }
                        }

                        // 空结果分两种：没装音源是「去装」，装了没搜到是「换个词」。
                        !uiState.onlineHasSources -> item {
                            OnlineHintRow(stringResource(R.string.search_online_no_sources))
                        }

                        else -> item {
                            OnlineHintRow(stringResource(R.string.search_online_empty))
                        }
                    }
                }

                if (showSongs && uiState.songs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_songs),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = AppleType.sectionTitle,
                            color = AppleTheme.colors.label
                        )
                    }
                    items(uiState.songs, key = { "song-${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            onClick = { viewModel.handleIntent(SearchIntent.SongClicked(song)) },
                            onToggleFavorite = { viewModel.handleIntent(SearchIntent.ToggleFavorite(song)) }
                        )
                    }
                }

                if (showLyrics && uiState.lyricsMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_lyrics_section),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = AppleType.sectionTitle,
                            color = AppleTheme.colors.label
                        )
                    }
                    items(uiState.lyricsMatches, key = { "lyrics-${it.song.id}" }) { match ->
                        LyricsMatchRow(
                            match = match,
                            onClick = {
                                viewModel.handleIntent(SearchIntent.SongClicked(match.song))
                            }
                        )
                    }
                }

                if (showAlbums && uiState.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_albums),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = AppleType.sectionTitle,
                            color = AppleTheme.colors.label
                        )
                    }
                    items(uiState.albums, key = { "album-${it.id}" }) { album ->
                        AlbumRow(
                            album = album,
                            onClick = { viewModel.handleIntent(SearchIntent.AlbumClicked(album)) }
                        )
                    }
                }

                if (showArtists && uiState.artists.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_tab_artists),
                            modifier = Modifier.padding(
                                start = SonSizes.listItemHorizontalPadding,
                                top = SonSpacing.large,
                                bottom = SonSpacing.small
                            ),
                            style = AppleType.sectionTitle,
                            color = AppleTheme.colors.label
                        )
                    }
                    items(uiState.artists, key = { "artist-${it.id}" }) { artist ->
                        ArtistRow(
                            artist = artist,
                            onClick = { viewModel.handleIntent(SearchIntent.ArtistClicked(artist)) }
                        )
                    }
                }

                if (nothingFound) {
                    item {
                        // 空态与列表族的模板同构（图标 + 强调标题 + 一行解释），
                        // 但放在 LazyColumn 里，所以不套整屏的那个组合。
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = SonLayout.screenMargin,
                                    vertical = SonSpacing.extraExtraLarge
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            M3SymbolIcon(
                                name = SymbolIconName.Search,
                                filled = false,
                                contentDescription = null,
                                modifier = Modifier.size(SonSizes.iconExtraLarge),
                                tint = AppleTheme.colors.secondaryLabel
                            )
                            Spacer(modifier = Modifier.height(SonSpacing.medium))
                            Text(
                                text = stringResource(R.string.search_no_results),
                                style = AppleType.headline,
                                color = AppleTheme.colors.label
                            )
                            Spacer(modifier = Modifier.height(SonSpacing.extraSmall))
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = AppleType.footnote,
                                color = AppleTheme.colors.secondaryLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SearchTab.labelRes(): Int = when (this) {
    SearchTab.ALL -> R.string.search_tab_all
    SearchTab.SONGS -> R.string.search_tab_songs
    SearchTab.ALBUMS -> R.string.search_tab_albums
    SearchTab.ARTISTS -> R.string.search_tab_artists
}

// ════════════════════════════════════════════════════════════════
// 区块标题 / 历史标签
// ════════════════════════════════════════════════════════════════

/** 区块标题：Apple 的 MusicTitle2（20sp Bold），操作文字用 key 色。 */
@Composable
private fun SearchSectionHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppleDims.padding,
                end = AppleDims.padding,
                top = AppleDims.sectionTitleTop,
                bottom = AppleDims.sectionTitleBottom
            )
    ) {
        Text(
            text = title,
            style = AppleType.sectionTitle,
            color = AppleTheme.colors.label
        )
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                style = AppleType.subhead,
                color = AppleTheme.colors.key,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

/** 历史与补全：Apple 的填充胶囊，32dp 高，横向滑动一排。 */
@Composable
private fun ChipRow(
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    val colors = AppleTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppleDims.padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleDims.cornerPill))
                    .background(colors.fill)
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = item,
                    style = AppleType.subhead,
                    color = colors.label,
                    maxLines = 1
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 结果行
// ════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════
// 在线结果
// ════════════════════════════════════════════════════════════════

/** 在线搜索走网络，比本地慢一个量级，所以这里真的会转圈（不是本地查询那种一帧闪）。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlineSearchingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SonLayout.screenMargin,
                vertical = SonSpacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoadingIndicator(modifier = Modifier.size(SonSizes.iconSmall))
        Spacer(modifier = Modifier.width(SonSpacing.small))
        Text(
            text = stringResource(R.string.search_online_loading),
            style = AppleType.caption1,
            color = AppleTheme.colors.secondaryLabel
        )
    }
}

/** 翻页入口：追加而不是替换，加载中把按钮换成指示器。 */
@Composable
private fun OnlineLoadMoreRow(
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SonLayout.screenMargin,
                vertical = SonSpacing.small
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            LoadingIndicator(modifier = Modifier.size(SonSizes.iconSmall))
            Spacer(modifier = Modifier.width(SonSpacing.small))
            Text(
                text = stringResource(R.string.search_online_loading_more),
                style = AppleType.caption1,
                color = AppleTheme.colors.secondaryLabel
            )
        } else {
            AppleTextAction(
                label = stringResource(R.string.search_online_load_more),
                onClick = onClick
            )
        }
    }
}

/** 在线区块的空态：一句解释，不占整屏。 */
@Composable
private fun OnlineHintRow(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = SonSizes.listItemHorizontalPadding,
            end = SonSizes.listItemHorizontalPadding,
            top = SonSpacing.small,
            bottom = SonSpacing.small
        ),
        style = AppleType.caption1,
        color = AppleTheme.colors.secondaryLabel
    )
}

/**
 * 在线结果的平台切换条：全部 + 本轮有结果的平台。
 *
 * 只列**真的有结果**的平台 —— 搜不到的平台给个空 tab 只会让人多点一次。
 */
@Composable
private fun OnlinePlatformTabs(
    platforms: List<OnlineSource>,
    selected: OnlineSource?,
    onSelect: (OnlineSource?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AppleDims.padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        item {
            AppleChip(
                label = stringResource(R.string.search_online_all_platforms),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(platforms, key = { it.lxKey }) { platform ->
            AppleChip(
                label = platform.displayName,
                selected = selected == platform,
                onClick = { onSelect(platform) }
            )
        }
    }
}

/**
 * 在线歌曲行：平台名放 overline（同一首歌在各平台都有，得先让用户看清是哪一家），
 * 取链期间行尾换成加载指示器。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlineSongRow(
    track: MusicInfo,
    showSource: Boolean,
    resolving: Boolean,
    onClick: () -> Unit
) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .applePress(onClick)
            .padding(horizontal = AppleDims.padding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = track.coverUrl,
            shape = RoundedCornerShape(AppleDims.cornerS),
            modifier = Modifier.size(AppleDims.listItemHeight)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // 来源压在最上面一行，用 key 色 —— 在线结果必须先说清"从哪来"。
            // 已经切到单平台 tab 时就不重复了。
            if (showSource) {
                Text(
                    text = track.source.displayName,
                    style = AppleType.caption2,
                    color = colors.key,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = track.title,
                style = AppleType.body,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(track.artist, track.album)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = AppleType.caption1,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (resolving) {
            Spacer(modifier = Modifier.width(8.dp))
            LoadingIndicator(modifier = Modifier.size(16.dp))
        }
        // 搜索结果常常是「听到一半才想存」，这里给一个直接入队的按钮：
        // 走的是全站同一个 LocalDownloadRequester（含音质选择），不另开一条下载路径。
        val downloadRequest = LocalDownloadRequester.current
        if (downloadRequest != null) {
            IconButton(
                onClick = { downloadRequest(track.toDownloadSong()) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colors.tertiaryLabel
                )
            ) {
                SonGlyphIcon(
                    glyph = SonGlyph.Download,
                    contentDescription = stringResource(R.string.download_menu_item),
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AppleSongRow(
        artwork = song.albumArt,
        title = song.title,
        subtitle = song.artist,
        onClick = onClick,
        // 红心跟着歌曲的真实收藏状态走，点了会写进数据库
        trailing = {
            IconButton(
                onClick = onToggleFavorite,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (song.isFavorite) {
                        AppleTheme.colors.systemRed
                    } else {
                        AppleTheme.colors.tertiaryLabel
                    }
                )
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = song.isFavorite,
                    contentDescription = stringResource(
                        if (song.isFavorite) R.string.player_unfavorite_desc
                        else R.string.player_favorite_desc
                    ),
                    size = 18.dp
                )
            }
        }
    )
}

@Composable
private fun LyricsMatchRow(match: LyricsMatch, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .applePress(onClick)
            .padding(horizontal = AppleDims.padding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = match.song.albumArt,
            shape = RoundedCornerShape(AppleDims.cornerS),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.song.title,
                style = AppleType.body,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // 命中原因比元数据更重要：这一行存在的意义就是让用户认出那句歌词。
                text = match.snippet,
                style = AppleType.caption1,
                color = colors.key,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    AppleLargeListRow(
        artwork = album.albumArt,
        title = album.name,
        subtitle = album.artist,
        onClick = onClick,
        imageSize = 56.dp
    )
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    AppleLargeListRow(
        artwork = artist.artistArt,
        title = artist.name,
        subtitle = stringResource(R.string.search_artist_song_count, artist.numberOfTracks),
        onClick = onClick,
        circular = true,
        imageSize = 56.dp
    )
}
