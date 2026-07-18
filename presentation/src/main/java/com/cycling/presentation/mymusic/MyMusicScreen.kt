package com.cycling.presentation.mymusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3TopAppBar
import com.cycling.core.ui.theme.M3SemanticColors
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.domain.model.Song
import com.cycling.presentation.components.PlayActionButtons
import com.cycling.presentation.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMusicScreen(
    viewModel: MyMusicViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is MyMusicEffect.ShowToast -> {}
                is MyMusicEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
            }
        }
    }

    Scaffold(
        topBar = {
            M3TopAppBar(
                title = "我的音乐",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                actions = {
                    val currentSongs = when (uiState.currentTab) {
                        MyMusicTab.FAVORITES -> uiState.favoriteSongs
                        MyMusicTab.RECENTLY_PLAYED -> uiState.recentlyPlayedSongs
                        MyMusicTab.MOST_PLAYED -> uiState.mostPlayedSongs
                    }
                    if (currentSongs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.handleIntent(MyMusicIntent.PlayAll) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "全部播放",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.handleIntent(MyMusicIntent.ShuffleAll) }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "随机播放",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            M3SegmentedControl(
                tabs = listOf(
                    TabItem("喜欢", MyMusicTab.FAVORITES),
                    TabItem("最近播放", MyMusicTab.RECENTLY_PLAYED),
                    TabItem("最常播放", MyMusicTab.MOST_PLAYED)
                ),
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.handleIntent(MyMusicIntent.SelectTab(it)) }
            )

            when (uiState.currentTab) {
                MyMusicTab.FAVORITES -> {
                    FavoritesContent(
                        songs = uiState.favoriteSongs,
                        isLoading = uiState.isLoading,
                        onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) },
                        onToggleFavorite = { viewModel.handleIntent(MyMusicIntent.ToggleFavorite(it)) }
                    )
                }
                MyMusicTab.RECENTLY_PLAYED -> {
                    RecentlyPlayedContent(
                        songs = uiState.recentlyPlayedSongs,
                        isLoading = uiState.isLoading,
                        onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) }
                    )
                }
                MyMusicTab.MOST_PLAYED -> {
                    MostPlayedContent(
                        songs = uiState.mostPlayedSongs,
                        isLoading = uiState.isLoading,
                        onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) }
                    )
                }
            }
        }
    }
}

private data class TabItem(
    val label: String,
    val tab: MyMusicTab
)

@Composable
private fun M3SegmentedControl(
    tabs: List<TabItem>,
    selectedTab: MyMusicTab,
    onTabSelected: (MyMusicTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = M3Spacing.medium, vertical = M3Spacing.small)
            .clip(M3Shapes.cornerMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEach { tabItem ->
            val isSelected = selectedTab == tabItem.tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(M3Shapes.cornerSmall)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tabItem.tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabItem.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Favorite,
            iconTint = M3SemanticColors.Pink,
            title = "还没有喜欢的歌曲",
            subtitle = "在播放器中点击心形图标来收藏歌曲"
        )
    } else {
        SongList(
            songs = songs,
            onSongClick = onSongClick,
            trailingContent = { song ->
                IconButton(onClick = { onToggleFavorite(song) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "取消收藏",
                        tint = M3SemanticColors.Red
                    )
                }
            }
        )
    }
}

@Composable
private fun RecentlyPlayedContent(
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            iconTint = M3SemanticColors.Orange,
            title = "还没有播放记录",
            subtitle = "播放歌曲后会显示在这里"
        )
    } else {
        SongList(songs = songs, onSongClick = onSongClick)
    }
}

@Composable
private fun MostPlayedContent(
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (songs.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = M3SemanticColors.Purple,
            title = "还没有播放统计",
            subtitle = "播放歌曲后会显示在这里"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "${songs.size} 首歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song = song,
                    showDivider = true,
                    modifier = Modifier.clickable { onSongClick(song) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${song.playCount} 次",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    trailingContent: @Composable ((Song) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "${songs.size} 首歌曲",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                showDivider = true,
                modifier = Modifier.clickable { onSongClick(song) },
                trailingContent = trailingContent?.let { { it(song) } }
            )
        }
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .padding(bottom = 16.dp),
                tint = iconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
