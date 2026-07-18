package com.cycling.presentation.artistdetail

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToApiKeyConfig: () -> Unit = {},
    onNavigateToAiInfo: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArtistDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
            }
        }
    }

    Scaffold(
        topBar = {
            M3LargeTopAppBar(
                title = "歌手详情",
                scrollBehavior = scrollBehavior,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            uiState.artist?.let { artist ->
                                onNavigateToAiInfo("artist", artist.name, "")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI 介绍",
                            tint = M3SemanticColors.Purple
                        )
                    }
                }
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
                    ArtistHeader(
                        artist = uiState.artist,
                        songCount = uiState.songs.size
                    )
                }

                item {
                    PlayActionButtons(
                        onPlayAll = {
                            uiState.songs.firstOrNull()?.let { song ->
                                viewModel.handleIntent(ArtistDetailIntent.SongClick(song))
                            }
                        },
                        onShuffle = {
                            uiState.songs.randomOrNull()?.let { song ->
                                viewModel.handleIntent(ArtistDetailIntent.SongClick(song))
                            }
                        }
                    )
                }

                itemsIndexed(uiState.songs) { index, song ->
                    M3SongListItem(
                        song = song,
                        onClick = {
                            viewModel.handleIntent(ArtistDetailIntent.SongClick(song))
                        }
                    )
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
        horizontalArrangement = Arrangement.Center
    ) {
        M3FilledButton(
            onClick = onPlayAll,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(1f)
        ) {
            Text("播放全部")
        }
        Spacer(modifier = Modifier.width(M3Spacing.small))
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
private fun M3SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    M3ListItemWithAvatar(
        headlineText = song.title,
        supportingText = song.album ?: formatDuration(song.duration),
        onClick = onClick,
        modifier = modifier,
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
        }
    )
}

@Composable
private fun ArtistHeader(
    artist: com.cycling.domain.model.Artist?,
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
                .size(M3ComponentSize.artistCardSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist?.artistArt != null) {
                AsyncImage(
                    model = artist.artistArt,
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = M3SemanticColors.Blue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(M3Spacing.medium))

        Text(
            text = artist?.name ?: "未知歌手",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(M3Spacing.small))

        val albumCount = artist?.numberOfAlbums ?: 0
        Text(
            text = "$albumCount 张专辑 · $songCount 首歌曲",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(M3Spacing.medium))
    }
}

private fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}
