package com.cycling.presentation.playlistdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.core.ui.components.M3CircularProgressIndicator
import com.cycling.core.ui.components.M3FilledButton
import com.cycling.core.ui.components.M3LargeTopAppBar
import com.cycling.core.ui.components.M3ListItemWithAvatar
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3SemanticColors
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlaylistDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is PlaylistDetailEffect.ShowToast -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            M3LargeTopAppBar(
                title = "播放列表详情",
                scrollBehavior = scrollBehavior,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                M3CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    PlaylistHeader(
                        playlist = uiState.playlist,
                        songCount = uiState.songs.size
                    )
                }

                item {
                    PlayActionButtons(
                        onPlayAll = {
                            uiState.songs.firstOrNull()?.let { song ->
                                viewModel.handleIntent(PlaylistDetailIntent.SongClick(song))
                            }
                        },
                        onShuffle = {
                            uiState.songs.randomOrNull()?.let { song ->
                                viewModel.handleIntent(PlaylistDetailIntent.SongClick(song))
                            }
                        }
                    )
                }

                if (uiState.songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(M3Spacing.extraLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "播放列表为空",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(uiState.songs) { index, song ->
                        M3SongListItemWithMenu(
                            song = song,
                            onSongClick = { song ->
                                viewModel.handleIntent(PlaylistDetailIntent.SongClick(song))
                            },
                            onRemoveSong = { songId ->
                                viewModel.handleIntent(PlaylistDetailIntent.RemoveSong(songId))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(bottomPadding))
                }
            }
        }
    }
}

@Composable
private fun PlayActionButtons(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = M3Spacing.medium, vertical = M3Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(M3Spacing.small)
    ) {
        M3FilledButton(
            onClick = onPlayAll,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(1f)
        ) {
            Text("播放全部")
        }
        M3FilledButton(
            onClick = onShuffle,
            icon = Icons.Default.Shuffle,
            modifier = Modifier.weight(1f)
        ) {
            Text("随机播放")
        }
    }
    Spacer(modifier = Modifier.height(M3Spacing.small))
}

@Composable
private fun PlaylistHeader(
    playlist: com.cycling.domain.model.Playlist?,
    songCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(M3Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(M3ComponentSize.mediaCardWidth)
                .clip(M3Shapes.cornerLarge),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = M3SemanticColors.Teal
                )
            }
        }

        Spacer(modifier = Modifier.height(M3Spacing.medium))

        Text(
            text = playlist?.name ?: "未知播放列表",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(M3Spacing.small))

        Text(
            text = "$songCount 首歌曲",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(M3Spacing.medium))
    }
}

@Composable
private fun M3SongListItemWithMenu(
    song: Song,
    onSongClick: (Song) -> Unit,
    onRemoveSong: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        M3ListItemWithAvatar(
            headlineText = song.title,
            supportingText = formatDuration(song.duration),
            onClick = { onSongClick(song) },
            avatarContent = {
                if (song.albumArt != null) {
                    AsyncImage(
                        model = song.albumArt,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = M3SemanticColors.Red
                        )
                    }
                }
            },
            trailingContent = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
            offset = DpOffset(x = (-8).dp, y = 8.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "从播放列表移除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onRemoveSong(song.id)
                    menuExpanded = false
                }
            )
        }
    }
}

private fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}
