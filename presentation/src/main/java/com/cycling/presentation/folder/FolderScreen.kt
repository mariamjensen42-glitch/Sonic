package com.cycling.presentation.folder

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3ListItemTwoLine
import com.cycling.core.ui.components.M3TopAppBar
import com.cycling.core.ui.theme.M3SemanticColors
import com.cycling.core.ui.theme.M3Spacing
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
            M3TopAppBar(
                title = uiState.title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = if (uiState.parentPath != null) {
                    { viewModel.handleIntent(FolderIntent.NavigateBack) }
                } else {
                    onNavigateBack
                }
            )
        }
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.folders.isEmpty() && uiState.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = M3SemanticColors.Blue,
                            modifier = Modifier.height(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无内容",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "此文件夹中没有音乐文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(M3Spacing.small))
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
    M3ListItemTwoLine(
        headlineText = folder.name,
        supportingText = "${folder.songCount} 首歌曲",
        leadingIcon = Icons.Default.Folder,
        leadingIconBackgroundColor = M3SemanticColors.Blue.copy(alpha = 0.2f),
        onClick = onClick
    )
}
