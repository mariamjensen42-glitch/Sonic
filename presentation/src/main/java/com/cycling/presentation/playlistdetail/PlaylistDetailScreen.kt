package com.cycling.presentation.playlistdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.CollectionDetailScreen
import com.cycling.presentation.components.CollectCollectionDetailEffects
import com.cycling.presentation.components.MenuItem
import com.cycling.presentation.components.PlaylistDescription

@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onPlayShuffled: (List<Song>) -> Unit,
    viewModel: PlaylistDetailViewModel,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectCollectionDetailEffects(viewModel.uiEffect, onNavigateToPlayer, onPlayShuffled)

    // 文件名取歌单名，去掉 SAF/文件系统不接受的字符。
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        uri?.let { viewModel.handleIntent(PlaylistDetailIntent.ExportM3u(it.toString())) }
    }

    // songMoreMenu 是普通 lambda（不是 @Composable），stringResource 必须在外层取好值。
    val removeSongLabel = stringResource(R.string.playlist_detail_remove)

    CollectionDetailScreen(
        uiState = uiState,
        titleFallback = stringResource(R.string.playlist_detail_unknown),
        subtitle = stringResource(R.string.collection_detail_song_count, uiState.songs.size),
        placeholderIcon = SymbolIconName.QueueMusic,
        backContentDescription = stringResource(R.string.playlist_detail_back_desc),
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::handleCollectionIntent,
        bottomPadding = bottomPadding,
        // 仅歌单有"移出"语义：多选栏多出一个移出按钮。
        onRemoveSelected = { viewModel.handleIntent(PlaylistDetailIntent.RemoveSelectedSongs) },
        removeLabel = stringResource(R.string.playlist_detail_batch_remove),
        // 歌单可混合不同歌手，副标题保留歌手名；组件据此同时展示时长。
        songSubtitle = { it.artist },
        // 只有歌单有可变的用户顺序；专辑/歌手详情保持数据库排序。
        onMoveSong = { from, to ->
            viewModel.handleIntent(PlaylistDetailIntent.MoveSong(from, to))
        },
        onExport = {
            val name = uiState.title
                ?.replace(Regex("""[\\/:*?"<>|]"""), "_")
                ?.takeIf { it.isNotBlank() }
                ?: "playlist"
            exportLauncher.launch("$name.m3u")
        },
        songMoreMenu = { song ->
            listOf(
                MenuItem(
                    name = removeSongLabel,
                    isDestructive = true,
                    onClick = { viewModel.handleIntent(PlaylistDetailIntent.RemoveSong(song.id)) }
                )
            )
        },
        headerExtra = {
            PlaylistDescription(
                description = uiState.description.orEmpty(),
                expanded = uiState.isDescriptionExpanded,
                isGenerating = uiState.isDescriptionGenerating,
                onToggle = { viewModel.handleIntent(PlaylistDetailIntent.ToggleDescription) }
            )
        },
        emptyText = stringResource(R.string.playlist_detail_empty)
    )
}
