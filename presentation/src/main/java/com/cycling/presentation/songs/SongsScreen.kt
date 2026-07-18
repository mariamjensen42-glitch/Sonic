package com.cycling.presentation.songs

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3ExtendedFAB
import com.cycling.core.ui.components.M3LargeTopAppBar
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import com.cycling.presentation.components.MenuItem
import com.cycling.presentation.components.PlaylistPickerDialog
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.formatDuration
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSongDetail: (Long) -> Unit = {},
    viewModel: SongsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var expandedSongId by remember { mutableStateOf<Long?>(null) }
    val scrollState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SongsEffect.NavigateToPlayer -> {
                    val songs = uiState.songs
                    val song = songs.find { it.id == effect.songId }
                    if (song != null) {
                        playerViewModel.handleIntent(
                            PlayerIntent.PlaySong(song, songs)
                        )
                    }
                    onNavigateToPlayer(effect.songId)
                }
                is SongsEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val sortedSongs = remember(uiState.songs, uiState.sortOrder, uiState.sortAscending) {
        sortSongs(uiState.songs, uiState.sortOrder, uiState.sortAscending)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3LargeTopAppBar(
                title = "歌曲",
                scrollBehavior = scrollBehavior,
                navigationIcon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "排序"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { sortOrder ->
                                DropdownMenuItem(
                                    text = { Text(getSortOrderName(sortOrder)) },
                                    onClick = {
                                        viewModel.handleIntent(SongsIntent.ChangeSortOrder(sortOrder))
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.cycling.core.ui.components.M3CircularProgressIndicator()
                }
            } else if (sortedSongs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(M3Spacing.medium))
                    Text(
                        text = "暂无歌曲",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "扫描本地音乐以添加歌曲",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState
                    ) {
                        item {
                            Text(
                                text = "${sortedSongs.size} 首歌曲",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = M3Spacing.medium,
                                    vertical = M3Spacing.small
                                )
                            )
                        }
                        itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                            SongListItem(
                                song = song,
                                showDivider = index < sortedSongs.size - 1,
                                subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                                showDuration = false,
                                showMoreButton = true,
                                moreMenuExpanded = expandedSongId == song.id,
                                onMoreClick = { expandedSongId = song.id },
                                onMoreDismiss = { expandedSongId = null },
                                onClick = { viewModel.handleIntent(SongsIntent.SongClick(song)) },
                                moreMenuItems = listOf(
                                    MenuItem(
                                        name = "下一首播放",
                                        onClick = {
                                            playerViewModel.handleIntent(
                                                PlayerIntent.PlayNext(song)
                                            )
                                        }
                                    ),
                                    MenuItem(
                                        name = "查看详情",
                                        onClick = {
                                            onNavigateToSongDetail(song.id)
                                        }
                                    ),
                                    MenuItem(
                                        name = "添加到播放列表",
                                        onClick = {
                                            viewModel.handleIntent(
                                                SongsIntent.ShowAddToPlaylistDialog(song)
                                            )
                                        }
                                    )
                                )
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(M3Spacing.extraExtraLarge)
                            )
                        }
                    }

                    M3ExtendedFAB(
                        text = "播放全部",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (sortedSongs.isNotEmpty()) {
                                playerViewModel.handleIntent(
                                    PlayerIntent.PlaySong(sortedSongs.first(), sortedSongs)
                                )
                                onNavigateToPlayer(sortedSongs.first().id)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(M3Spacing.medium)
                    )
                }
            }
        }
    }

    PlaylistPickerDialog(
        visible = uiState.showAddToPlaylistDialog,
        playlists = uiState.playlists,
        onDismiss = { viewModel.handleIntent(SongsIntent.DismissAddToPlaylistDialog) },
        onPlaylistSelected = { playlistId ->
            viewModel.handleIntent(SongsIntent.AddToPlaylist(playlistId))
        },
        onCreateNewPlaylist = { name ->
            viewModel.handleIntent(SongsIntent.CreatePlaylistAndAddSong(name))
        }
    )
}

private fun sortSongs(songs: List<Song>, sortOrder: SortOrder, ascending: Boolean): List<Song> {
    val sorted = when (sortOrder) {
        SortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
        SortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
        SortOrder.ALBUM -> songs.sortedBy { it.album.lowercase() }
        SortOrder.DURATION -> songs.sortedBy { it.duration }
        SortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
    }
    return if (ascending) sorted else sorted.reversed()
}

private fun getSortOrderName(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.TITLE -> "按标题"
        SortOrder.ARTIST -> "按艺术家"
        SortOrder.ALBUM -> "按专辑"
        SortOrder.DURATION -> "按时长"
        SortOrder.DATE_ADDED -> "按添加日期"
    }
}
