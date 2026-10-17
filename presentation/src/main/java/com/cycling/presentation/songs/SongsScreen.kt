@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import com.cycling.presentation.R
import com.cycling.presentation.components.MenuItem
import com.cycling.presentation.components.PlaylistPickerDialog
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.formatDuration
import com.cycling.presentation.permissions.MediaLibraryPermission
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel

/** 多选操作栏的高度，用来给列表补底部留白。 */
private val SelectionBarHeight = 72.dp

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

    // 空列表上的按钮要区分"没权限"和"没歌"：没权限时先去要权限，要到手就直接扫，
    // 否则用户按下去只会得到一个依然空着的列表。
    var hasMediaPermission by remember { mutableStateOf(MediaLibraryPermission.isGranted(context)) }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMediaPermission = permissions.values.all { it }
        if (hasMediaPermission) viewModel.handleIntent(SongsIntent.ScanLibrary)
    }
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
                    val message = effect.arg
                        ?.let { context.getString(effect.message, it) }
                        ?: context.getString(effect.message)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val sortedSongs = remember(uiState.songs, uiState.sortOrder, uiState.sortAscending) {
        sortSongs(uiState.songs, uiState.sortOrder, uiState.sortAscending)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectionMode) {
                            stringResource(R.string.songs_selected_count, uiState.selectedSongIds.size)
                        } else {
                            stringResource(R.string.songs_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.selectionMode) {
                                viewModel.handleIntent(SongsIntent.ExitSelectionMode)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        M3SymbolIcon(
                            name = if (uiState.selectionMode) {
                                SymbolIconName.Close
                            } else {
                                SymbolIconName.ArrowBack
                            },
                            filled = true,
                            contentDescription = if (uiState.selectionMode) {
                                stringResource(R.string.songs_exit_selection_desc)
                            } else {
                                stringResource(R.string.songs_back_desc)
                            }
                        )
                    }
                },
                actions = {
                    if (uiState.selectionMode) {
                        val allSelected = sortedSongs.isNotEmpty() &&
                            uiState.selectedSongIds.size == sortedSongs.size
                        IconButton(
                            onClick = {
                                viewModel.handleIntent(
                                    if (allSelected) SongsIntent.ClearSelection else SongsIntent.SelectAll
                                )
                            }
                        ) {
                            M3SymbolIcon(
                                name = SymbolIconName.SelectAll,
                                filled = true,
                                contentDescription = if (allSelected) {
                                    stringResource(R.string.songs_clear_selection_desc)
                                } else {
                                    stringResource(R.string.songs_select_all_desc)
                                }
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.handleIntent(SongsIntent.EnterSelectionMode) }
                        ) {
                            M3SymbolIcon(
                                name = SymbolIconName.Checklist,
                                filled = true,
                                contentDescription = stringResource(R.string.songs_select_desc)
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                M3SymbolIcon(
                                    name = SymbolIconName.Sort,
                                    filled = true,
                                    contentDescription = stringResource(R.string.songs_sort_desc)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { sortOrder ->
                                    DropdownMenuItem(
                                        text = { Text(text = getSortOrderName(sortOrder)) },
                                        onClick = {
                                            viewModel.handleIntent(SongsIntent.ChangeSortOrder(sortOrder))
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                LoadingState()
            } else if (sortedSongs.isEmpty()) {
                EmptySongsState(
                    isScanning = uiState.isScanning,
                    hasMediaPermission = hasMediaPermission,
                    onScan = { viewModel.handleIntent(SongsIntent.ScanLibrary) },
                    onRequestPermission = { mediaPermissionLauncher.launch(MediaLibraryPermission.names) }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        contentPadding = PaddingValues(
                            bottom = SonLayout.bottomBarClearance +
                                if (uiState.selectionMode) SelectionBarHeight else 0.dp
                        )
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.songs_count, sortedSongs.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = SonSpacing.medium,
                                    vertical = SonSpacing.small
                                )
                            )
                        }
                        itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                            SongListItem(
                                song = song,
                                isCurrentSong = song.id == uiState.currentSongId,
                                showDivider = index < sortedSongs.size - 1,
                                subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                                showDuration = false,
                                showMoreButton = !uiState.selectionMode,
                                selected = if (uiState.selectionMode) {
                                    song.id in uiState.selectedSongIds
                                } else {
                                    null
                                },
                                moreMenuExpanded = expandedSongId == song.id,
                                onMoreClick = { expandedSongId = song.id },
                                onMoreDismiss = { expandedSongId = null },
                                onClick = {
                                    // 多选模式下单击是勾选。播放要退出多选再做，否则用户会在
                                    // "我只是想挑几首" 的过程中把当前队列换掉。
                                    if (uiState.selectionMode) {
                                        viewModel.handleIntent(SongsIntent.ToggleSelection(song.id))
                                    } else {
                                        viewModel.handleIntent(SongsIntent.SongClick(song))
                                    }
                                },
                                moreMenuItems = listOf(
                                    MenuItem(name = stringResource(R.string.songs_play_next), onClick = {
                                        playerViewModel.handleIntent(PlayerIntent.PlayNext(song))
                                    }),
                                    MenuItem(name = stringResource(R.string.songs_view_detail), onClick = { onNavigateToSongDetail(song.id) }),
                                    MenuItem(name = stringResource(R.string.songs_add_to_playlist), onClick = {
                                        viewModel.handleIntent(SongsIntent.ShowAddToPlaylistDialog(song))
                                    })
                                )
                            )
                        }
                    }

                    if (uiState.selectionMode) {
                        SongSelectionBar(
                            selectedCount = uiState.selectedSongIds.size,
                            onAddToPlaylist = {
                                viewModel.handleIntent(SongsIntent.ShowBatchAddToPlaylistDialog)
                            },
                            onFavorite = { viewModel.handleIntent(SongsIntent.BatchSetFavorite(true)) },
                            onUnfavorite = { viewModel.handleIntent(SongsIntent.BatchSetFavorite(false)) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    } else {
                        ExtendedFloatingActionButton(
                            text = { Text(text = stringResource(R.string.songs_play_all)) },
                            icon = {
                                M3SymbolIcon(
                                    name = SymbolIconName.PlayArrow,
                                    filled = true,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                if (sortedSongs.isNotEmpty()) {
                                    playerViewModel.handleIntent(
                                        PlayerIntent.PlaySong(sortedSongs.first(), sortedSongs)
                                    )
                                    onNavigateToPlayer(sortedSongs.first().id)
                                }
                            },
                            expanded = true,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(SonSpacing.medium)
                        )
                    }
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
        onCreateNewPlaylist = { viewModel.handleIntent(SongsIntent.CreatePlaylistAndAddSong(it)) }
    )

    PlaylistPickerDialog(
        visible = uiState.showBatchAddToPlaylistDialog,
        playlists = uiState.playlists,
        onDismiss = { viewModel.handleIntent(SongsIntent.DismissBatchAddToPlaylistDialog) },
        onPlaylistSelected = { playlistId ->
            viewModel.handleIntent(SongsIntent.BatchAddToPlaylist(playlistId))
        },
        onCreateNewPlaylist = { viewModel.handleIntent(SongsIntent.CreatePlaylistAndAddSelected(it)) }
    )
}

/**
 * 多选时替换掉右下角播放按钮的操作栏。
 *
 * 放在底部而不是顶部：这三个动作都会改动选中的歌，靠近拇指、且离"取消"（左上角）足够远，
 * 不会误触。
 */
@Composable
private fun SongSelectionBar(
    selectedCount: Int,
    onAddToPlaylist: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = selectedCount > 0
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onAddToPlaylist,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.PlaylistAdd,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.buttonIcon)
                )
                Spacer(modifier = Modifier.width(SonSpacing.extraSmall))
                Text(text = stringResource(R.string.songs_batch_add_to_playlist))
            }
            FilledTonalButton(onClick = onFavorite, enabled = enabled) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = true,
                    contentDescription = stringResource(R.string.songs_batch_favorite)
                )
            }
            FilledTonalButton(onClick = onUnfavorite, enabled = enabled) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = false,
                    contentDescription = stringResource(R.string.songs_batch_unfavorite)
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun EmptySongsState(
    isScanning: Boolean,
    hasMediaPermission: Boolean,
    onScan: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        M3SymbolIcon(
            name = SymbolIconName.MusicNote,
            filled = false,
            contentDescription = null,
            modifier = Modifier.size(SonSizes.iconExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SonSpacing.medium))
        Text(
            text = stringResource(R.string.songs_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = stringResource(R.string.songs_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SonSpacing.large))
        when {
            isScanning -> LoadingIndicator()
            hasMediaPermission -> Button(onClick = onScan) {
                Text(text = stringResource(R.string.scan_start))
            }
            else -> Button(onClick = onRequestPermission) {
                Text(text = stringResource(R.string.grant_permission))
            }
        }
    }
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

@Composable
private fun getSortOrderName(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.TITLE -> stringResource(R.string.songs_sort_title)
        SortOrder.ARTIST -> stringResource(R.string.songs_sort_artist)
        SortOrder.ALBUM -> stringResource(R.string.songs_sort_album)
        SortOrder.DURATION -> stringResource(R.string.songs_sort_duration)
        SortOrder.DATE_ADDED -> stringResource(R.string.songs_sort_date)
    }
}
