package com.cycling.presentation.folder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.Song
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long, List<Song>) -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FolderEffect.NavigateToPlayer -> {
                    onNavigateToPlayer(effect.songId, effect.songs)
                }
                is FolderEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            M3DetailTopAppBar(
                title = uiState.title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = if (uiState.parentPath != null) {
                    { viewModel.handleIntent(FolderIntent.NavigateBack) }
                } else {
                    onNavigateBack
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyColors.Background,
                    titleContentColor = SpotifyColors.TextPrimary,
                    navigationIconContentColor = SpotifyColors.TextPrimary
                )
            )
        },
        containerColor = SpotifyColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyColors.Accent)
                }
            } else if (uiState.folders.isEmpty() && uiState.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = SpotifyColors.TextTertiary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无内容",
                            color = SpotifyColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "此文件夹中没有音乐文件",
                            color = SpotifyColors.TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(uiState.folders, key = { it.path }) { folder ->
                        FolderItem(
                            folder = folder,
                            onClick = {
                                viewModel.handleIntent(FolderIntent.NavigateToFolder(folder.path))
                            }
                        )
                    }

                    items(uiState.songs, key = { it.id }) { song ->
                        SongListItem(
                            song = song,
                            showDivider = true,
                            subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                            showDuration = false,
                            showMoreButton = false,
                            onClick = {
                                viewModel.handleIntent(FolderIntent.SongClick(song))
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
}

@Composable
private fun FolderItem(
    folder: FolderItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyColors.GridBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = SpotifyColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = SpotifyColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${folder.songCount} 首歌曲",
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SpotifyColors.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
