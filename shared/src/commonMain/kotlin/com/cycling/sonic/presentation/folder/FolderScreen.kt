package com.cycling.sonic.presentation.folder

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import com.cycling.sonic.core.ui.components.M3Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.sonic.core.ui.components.M3CircularProgressIndicator
import com.cycling.sonic.core.ui.components.M3DetailTopAppBar
import com.cycling.sonic.core.ui.theme.M3Spacing
import com.cycling.sonic.core.ui.theme.SpotifyColors

@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long, List<com.cycling.sonic.domain.model.Song>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FolderEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId, effect.songs)
                is FolderEffect.ShowToast -> { }
            }
        }
    }

    M3Scaffold(
        topBar = {
            M3DetailTopAppBar(
                title = uiState.currentPath.split("/").lastOrNull()?.ifEmpty { "文件" } ?: "文件",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { M3CircularProgressIndicator() }
            } else if (uiState.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error ?: "", color = SpotifyColors.TextSecondary) }
            } else {
                val content = uiState.folderContent ?: return
                LazyColumn(Modifier.fillMaxSize()) {
                    if (uiState.currentPath != "/") {
                        item {
                            Row(
                                Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { viewModel.handleIntent(FolderIntent.NavigateUp) }).padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("..", color = SpotifyColors.Accent, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    items(content.folders) { folder ->
                        Row(
                            Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { viewModel.handleIntent(FolderIntent.NavigateToFolder(folder.path)) }).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = SpotifyColors.TextSecondary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(folder.name, color = SpotifyColors.TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${folder.songCount} 首歌曲", color = SpotifyColors.TextTertiary, fontSize = 12.sp)
                            }
                        }
                    }
                    items(content.songs) { song ->
                        Row(
                            Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { viewModel.handleIntent(FolderIntent.SongClick(song.id)) }).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).background(SpotifyColors.SurfaceVariant, shape = MaterialTheme.shapes.small))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = SpotifyColors.TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = SpotifyColors.TextTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
