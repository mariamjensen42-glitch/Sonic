package com.cycling.presentation.mymusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.core.ui.components.M3FilledButton
import com.cycling.core.ui.components.M3SegmentedButtonRow
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

private val SpotifyGreen = Color(0xFF1DB954)

@Composable
fun MyMusicScreen(
    viewModel: MyMusicViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToPlaylistDetail: (Long) -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is MyMusicEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is MyMusicEffect.NavigateToPlaylistDetail -> onNavigateToPlaylistDetail(effect.playlistId)
                is MyMusicEffect.ShowToast -> {}
            }
        }
    }

    // Create playlist dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.handleIntent(MyMusicIntent.CreatePlaylist(name))
                showCreateDialog = false
            }
        )
    }

    // AI create playlist dialog
    if (uiState.showAiCreateDialog) {
        AiPlaylistCreationDialog(
            uiState = uiState,
            onIntent = { viewModel.handleIntent(it) },
            onDismiss = { viewModel.handleIntent(MyMusicIntent.DismissAiCreateDialog) }
        )
    }

    // Rename dialog
    if (uiState.showRenameDialog && uiState.playlistToRename != null) {
        RenamePlaylistDialog(
            playlist = uiState.playlistToRename!!,
            onDismiss = { viewModel.handleIntent(MyMusicIntent.DismissRenameDialog) },
            onRename = { newName ->
                viewModel.handleIntent(MyMusicIntent.RenamePlaylist(uiState.playlistToRename!!.id, newName))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyColors.Background)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.currentTab == MyMusicTab.PLAYLISTS && uiState.showFavoriteSongs) {
                IconButton(onClick = { viewModel.handleIntent(MyMusicIntent.BackToPlaylists) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = SpotifyColors.TextPrimary)
                }
            }
            Text(
                text = if (uiState.showFavoriteSongs) "我喜欢" else "Library",
                color = SpotifyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.currentTab == MyMusicTab.PLAYLISTS && !uiState.showFavoriteSongs) {
                    IconButton(onClick = { viewModel.handleIntent(MyMusicIntent.ShowAiCreateDialog) }) {
                        Icon(Icons.Default.AutoAwesome, "AI 创建歌单", tint = Color(0xFFBB86FC), modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "创建播放列表", tint = SpotifyColors.TextSecondary, modifier = Modifier.size(28.dp))
                    }
                } else if (uiState.currentTab != MyMusicTab.PLAYLISTS) {
                    val currentSongs = when (uiState.currentTab) {
                        MyMusicTab.RECENTLY_PLAYED -> uiState.recentlyPlayedSongs
                        MyMusicTab.MOST_PLAYED -> uiState.mostPlayedSongs
                        MyMusicTab.PLAYLISTS -> emptyList()
                    }
                    if (currentSongs.isNotEmpty()) {
                        Icon(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { viewModel.handleIntent(MyMusicIntent.ShuffleAll) },
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "随机播放",
                            tint = SpotifyColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── Segmented Control ──
        SpotifySegmentedControl(
            tabs = listOf(
                TabItem("最近播放", MyMusicTab.RECENTLY_PLAYED),
                TabItem("最常播放", MyMusicTab.MOST_PLAYED),
                TabItem("歌单", MyMusicTab.PLAYLISTS)
            ),
            selectedTab = uiState.currentTab,
            onTabSelected = { viewModel.handleIntent(MyMusicIntent.SelectTab(it)) }
        )

        // ── Content ──
        when (uiState.currentTab) {
            MyMusicTab.RECENTLY_PLAYED -> RecentlyPlayedContent(
                songs = uiState.recentlyPlayedSongs, isLoading = uiState.isLoading,
                onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) }
            )
            MyMusicTab.MOST_PLAYED -> MostPlayedContent(
                songs = uiState.mostPlayedSongs, isLoading = uiState.isLoading,
                onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) }
            )
            MyMusicTab.PLAYLISTS -> {
                if (uiState.showFavoriteSongs) {
                    FavoritesContent(
                        songs = uiState.favoriteSongs, isLoading = uiState.isLoading,
                        onSongClick = { viewModel.handleIntent(MyMusicIntent.SongClick(it)) },
                        onToggleFavorite = { viewModel.handleIntent(MyMusicIntent.ToggleFavorite(it)) }
                    )
                } else {
                    PlaylistsContent(
                        playlists = uiState.playlists, isLoading = uiState.isLoading,
                        onFavoriteSongsClick = { viewModel.handleIntent(MyMusicIntent.FavoriteSongsClick) },
                        onPlaylistClick = { viewModel.handleIntent(MyMusicIntent.PlaylistClick(it)) },
                        onRename = { viewModel.handleIntent(MyMusicIntent.ShowRenameDialog(it)) },
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Segmented Control
// ════════════════════════════════════════════════════════

private data class TabItem(val label: String, val tab: MyMusicTab)

@Composable
private fun SpotifySegmentedControl(
    tabs: List<TabItem>,
    selectedTab: MyMusicTab,
    onTabSelected: (MyMusicTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tabItem ->
            val isSelected = selectedTab == tabItem.tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) SpotifyColors.TextPrimary else SpotifyColors.SurfaceVariant)
                    .clickable { onTabSelected(tabItem.tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabItem.label,
                    color = if (isSelected) SpotifyColors.Background else SpotifyColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Song Content tabs (unchanged)
// ════════════════════════════════════════════════════════

@Composable
private fun FavoritesContent(songs: List<Song>, isLoading: Boolean, onSongClick: (Song) -> Unit, onToggleFavorite: (Song) -> Unit) {
    if (isLoading) LoadingBox() else if (songs.isEmpty()) EmptyState(Icons.Default.Favorite, SpotifyColors.Accent, "还没有喜欢的歌曲", "在播放器中点击心形图标来收藏歌曲")
    else SongList(songs, onSongClick, trailingContent = { song ->
        Icon(modifier = Modifier.size(20.dp).clickable { onToggleFavorite(song) }, imageVector = Icons.Default.Favorite, contentDescription = "取消收藏", tint = SpotifyColors.Accent)
    })
}

@Composable
private fun RecentlyPlayedContent(songs: List<Song>, isLoading: Boolean, onSongClick: (Song) -> Unit) {
    if (isLoading) LoadingBox() else if (songs.isEmpty()) EmptyState(Icons.Default.History, SpotifyColors.TextSecondary, "还没有播放记录", "播放歌曲后会显示在这里")
    else SongList(songs, onSongClick)
}

@Composable
private fun MostPlayedContent(songs: List<Song>, isLoading: Boolean, onSongClick: (Song) -> Unit) {
    if (isLoading) LoadingBox() else if (songs.isEmpty()) EmptyState(Icons.AutoMirrored.Filled.TrendingUp, SpotifyColors.TextSecondary, "还没有播放统计", "播放歌曲后会显示在这里")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSongClick(song) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${song.playCount}次", color = SpotifyColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(song.title, color = SpotifyColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(song.artist, color = SpotifyColors.TextTertiary, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SongList(songs: List<Song>, onSongClick: (Song) -> Unit, trailingContent: @Composable ((Song) -> Unit)? = null) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        items(songs, key = { it.id }) { song ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onSongClick(song) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, color = SpotifyColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(song.artist, color = SpotifyColors.TextTertiary, fontSize = 12.sp, maxLines = 1)
                }
                trailingContent?.let { it(song) }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SpotifyColors.Accent) }
}

@Composable
private fun EmptyState(icon: ImageVector, iconTint: Color, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(60.dp).padding(bottom = 16.dp), tint = iconTint)
            Text(title, color = SpotifyColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = SpotifyColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

// ════════════════════════════════════════════════════════
// Playlists Content
// ════════════════════════════════════════════════════════

@Composable
private fun PlaylistsContent(
    playlists: List<Playlist>,
    isLoading: Boolean,
    onFavoriteSongsClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    bottomPadding: Dp
) {
    if (isLoading) {
        LoadingBox()
    } else if (playlists.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                FavoriteSongsItem(onClick = onFavoriteSongsClick)
                Spacer(Modifier.height(16.dp))
                EmptyPlaylistsHint()
            }
            item { Spacer(Modifier.height(80.dp + bottomPadding)) }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                FavoriteSongsItem(onClick = onFavoriteSongsClick)
                Spacer(Modifier.height(8.dp))
            }
            items(playlists, key = { it.id }) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onRename = { onRename(playlist) }
                )
            }
            item { Spacer(Modifier.height(80.dp + bottomPadding)) }
        }
    }
}

@Composable
private fun FavoriteSongsItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyColors.Accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Favorite, null, tint = SpotifyColors.Accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("我喜欢", color = SpotifyColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("收藏的歌曲", color = SpotifyColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyPlaylistsHint() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(64.dp), tint = SpotifyColors.TextTertiary)
        Spacer(Modifier.height(16.dp))
        Text("暂无播放列表", color = SpotifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("创建一个播放列表来整理你的音乐", color = SpotifyColors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist, onClick: () -> Unit, onRename: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyColors.GridBackground), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SpotifyColors.TextSecondary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, color = SpotifyColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${playlist.numberOfSongs} 首歌曲", color = SpotifyColors.TextTertiary, fontSize = 12.sp)
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "更多选项", tint = SpotifyColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SpotifyColors.Surface) {
                DropdownMenuItem(text = { Text("重命名", color = SpotifyColors.TextPrimary) }, onClick = { showMenu = false; onRename() })
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Dialogs
// ════════════════════════════════════════════════════════

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建播放列表", color = SpotifyColors.TextPrimary) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("输入播放列表名称", color = SpotifyColors.TextTertiary) },
                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SpotifyColors.TextPrimary, unfocusedTextColor = SpotifyColors.TextPrimary,
                    focusedBorderColor = SpotifyColors.Accent, unfocusedBorderColor = SpotifyColors.Divider, cursorColor = SpotifyColors.Accent)
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("创建", color = SpotifyGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = SpotifyColors.TextSecondary) } },
        containerColor = SpotifyColors.Surface, titleContentColor = SpotifyColors.TextPrimary, textContentColor = SpotifyColors.TextSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun RenamePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名播放列表", color = SpotifyColors.TextPrimary) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("输入播放列表名称", color = SpotifyColors.TextTertiary) },
                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SpotifyColors.TextPrimary, unfocusedTextColor = SpotifyColors.TextPrimary,
                    focusedBorderColor = SpotifyColors.Accent, unfocusedBorderColor = SpotifyColors.Divider, cursorColor = SpotifyColors.Accent)
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }, enabled = name.isNotBlank()) { Text("确定", color = SpotifyGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = SpotifyColors.TextSecondary) } },
        containerColor = SpotifyColors.Surface, titleContentColor = SpotifyColors.TextPrimary, textContentColor = SpotifyColors.TextSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}

// ════════════════════════════════════════════════════════
// AI Playlist Creation Dialog (integrated)
// ════════════════════════════════════════════════════════

private data class AiDialogState(
    val step: AiPlaylistCreationStep = AiPlaylistCreationStep.SELECT_MODE,
    val mode: AiPlaylistMode = AiPlaylistMode.RANDOM,
    val songCount: Int = 10,
    val theme: String = "",
    val playlistName: String = ""
)

private enum class AiPlaylistCreationStep { SELECT_MODE, INPUT_DETAILS, PREVIEW }
private enum class AiPlaylistMode { RANDOM, THEME }

@Composable
private fun AiPlaylistCreationDialog(
    uiState: MyMusicUiState,
    onIntent: (MyMusicIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var dialogState by remember { mutableStateOf(AiDialogState()) }
    var sliderValue by remember { mutableFloatStateOf(uiState.selectedSongCount.toFloat()) }

    LaunchedEffect(uiState.aiGeneratedSongs, uiState.isAiGenerating, uiState.aiCreateError) { /* sync handled via state */ }

    when (dialogState.step) {
        AiPlaylistCreationStep.SELECT_MODE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("AI 创建歌单", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("选择创建方式", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        M3SegmentedButtonRow(options = listOf("随机创建", "主题创建"),
                            selectedIndex = if (dialogState.mode == AiPlaylistMode.RANDOM) 0 else 1,
                            onOptionSelected = { index ->
                                val mode = if (index == 0) AiPlaylistMode.RANDOM else AiPlaylistMode.THEME
                                dialogState = dialogState.copy(mode = mode)
                                onIntent(MyMusicIntent.SetAiCreateMode(mode.toCreate()))
                            }, modifier = Modifier.fillMaxWidth())
                        Text(if (dialogState.mode == AiPlaylistMode.RANDOM) "随机从您的音乐库中选择歌曲" else "根据主题描述智能推荐歌曲",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { M3FilledButton(onClick = { dialogState = dialogState.copy(step = AiPlaylistCreationStep.INPUT_DETAILS) }) { Text("下一步") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        AiPlaylistCreationStep.INPUT_DETAILS -> {
            val minCount = 5
            val maxCount = if (dialogState.mode == AiPlaylistMode.RANDOM) 50 else 30
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (dialogState.mode == AiPlaylistMode.RANDOM) "随机创建" else "主题创建", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (dialogState.mode == AiPlaylistMode.THEME) {
                            OutlinedTextField(value = uiState.themeInput, onValueChange = { onIntent(MyMusicIntent.SetThemeInput(it)) },
                                placeholder = { Text("例如: 适合跑步的音乐、安静的夜晚") }, label = { Text("主题描述") },
                                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth())
                        }
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("歌曲数量", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("${sliderValue.toInt()} 首", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Slider(value = sliderValue, onValueChange = { sliderValue = it; onIntent(MyMusicIntent.SetSelectedSongCount(it.toInt())) },
                                valueRange = minCount.toFloat()..maxCount.toFloat(), steps = maxCount - minCount - 1,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$minCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$maxCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = { M3FilledButton(onClick = {
                    dialogState = dialogState.copy(songCount = sliderValue.toInt(), step = AiPlaylistCreationStep.PREVIEW)
                    onIntent(MyMusicIntent.GenerateAiPlaylist)
                }, enabled = !(dialogState.mode == AiPlaylistMode.THEME && uiState.themeInput.isBlank())) { Text("生成") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
                shape = RoundedCornerShape(16.dp),
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
            )
        }

        AiPlaylistCreationStep.PREVIEW -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(uiState.aiGeneratedPlaylistName.ifEmpty { if (dialogState.mode == AiPlaylistMode.RANDOM) "随机歌单" else uiState.themeInput },
                        style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                text = {
                    Column(Modifier.fillMaxWidth().height(300.dp)) {
                        when {
                            uiState.isAiGenerating -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(16.dp))
                                    Text("正在生成歌单...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            uiState.aiCreateError != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("生成失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(8.dp))
                                    Text(uiState.aiCreateError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            uiState.aiGeneratedSongs.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("暂无歌曲", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                                items(uiState.aiGeneratedSongs, key = { it.id }) { song -> PreviewSongItem(song) }
                            }
                        }
                        if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("共 ${uiState.aiGeneratedSongs.size} 首歌曲", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                        M3FilledButton(onClick = { onIntent(MyMusicIntent.ConfirmAiPlaylist); onDismiss() }) { Text("创建") }
                    }
                },
                dismissButton = {
                    Row {
                        if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                            TextButton(onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) }) { Text("重新生成") }
                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(onClick = onDismiss) { Text("取消") }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                properties = DialogProperties(dismissOnBackPress = !uiState.isAiGenerating, dismissOnClickOutside = false)
            )
        }
    }
}

@Composable
private fun PreviewSongItem(song: Song, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            if (song.albumArt != null) AsyncImage(model = song.albumArt, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, Modifier.size(20.dp), tint = Color(0xFFFF453A))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun AiPlaylistMode.toCreate(): AiCreateMode = when (this) {
    AiPlaylistMode.RANDOM -> AiCreateMode.RANDOM
    AiPlaylistMode.THEME -> AiCreateMode.THEME
}
