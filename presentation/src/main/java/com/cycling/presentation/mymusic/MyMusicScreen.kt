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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.presentation.R
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

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is MyMusicEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is MyMusicEffect.NavigateToPlaylistDetail -> onNavigateToPlaylistDetail(effect.playlistId)
                is MyMusicEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            uiState = uiState,
            onManualCreate = { name ->
                viewModel.handleIntent(MyMusicIntent.CreatePlaylist(name))
                showCreateDialog = false
            },
            onIntent = { viewModel.handleIntent(it) },
            onDismiss = { showCreateDialog = false }
        )
    }

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
        // ── Header: Your Library ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.showFavoriteSongs) {
                IconButton(onClick = { viewModel.handleIntent(MyMusicIntent.BackToPlaylists) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.mymusic_back_desc), tint = SpotifyColors.TextPrimary)
                }
            }
            Text(
                text = if (uiState.showFavoriteSongs) stringResource(R.string.mymusic_title_favorites) else stringResource(R.string.mymusic_title_library),
                color = SpotifyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!uiState.showFavoriteSongs) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        modifier = Modifier.size(28.dp).clickable { /* TODO: search */ },
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.mymusic_search_desc),
                        tint = SpotifyColors.TextSecondary
                    )
                    Icon(
                        modifier = Modifier.size(28.dp).clickable { showCreateDialog = true },
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.mymusic_create_playlist_desc),
                        tint = SpotifyColors.TextSecondary
                    )
                }
            }
        }

        // ── Content: Playlists only ──
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
                onDelete = { viewModel.handleIntent(MyMusicIntent.DeletePlaylist(it)) },
                bottomPadding = bottomPadding
            )
        }
    }
}

// ════════════════════════════════════════════════════════
// Favorites Content
// ════════════════════════════════════════════════════════

@Composable
private fun FavoritesContent(songs: List<Song>, isLoading: Boolean, onSongClick: (Song) -> Unit, onToggleFavorite: (Song) -> Unit) {
    if (isLoading) {
        LoadingBox()
    } else if (songs.isEmpty()) {
        EmptyState(Icons.Default.Favorite, SpotifyColors.Accent, stringResource(R.string.mymusic_empty_favorites_title), stringResource(R.string.mymusic_empty_favorites_subtitle))
    } else {
        SongList(songs, onSongClick, trailingContent = { song ->
            Icon(modifier = Modifier.size(20.dp).clickable { onToggleFavorite(song) }, imageVector = Icons.Default.Favorite, contentDescription = stringResource(R.string.mymusic_unfavorite_desc), tint = SpotifyColors.Accent)
        })
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
    onDelete: (Playlist) -> Unit,
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
                    onRename = { onRename(playlist) },
                    onDelete = { onDelete(playlist) }
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
            Text(stringResource(R.string.mymusic_favorites_label), color = SpotifyColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(stringResource(R.string.mymusic_favorites_subtitle), color = SpotifyColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyPlaylistsHint() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(64.dp), tint = SpotifyColors.TextTertiary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.mymusic_empty_playlists), color = SpotifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.mymusic_empty_playlists_subtitle), color = SpotifyColors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyColors.GridBackground), contentAlignment = Alignment.Center) {
            if (playlist.albumArt != null) {
                AsyncImage(
                    model = playlist.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SpotifyColors.TextSecondary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, color = SpotifyColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(stringResource(R.string.mymusic_song_count, playlist.numberOfSongs), color = SpotifyColors.TextTertiary, fontSize = 12.sp)
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.mymusic_more_desc), tint = SpotifyColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = SpotifyColors.Surface) {
                DropdownMenuItem(text = { Text(stringResource(R.string.mymusic_rename), color = SpotifyColors.TextPrimary) }, onClick = { showMenu = false; onRename() })
                DropdownMenuItem(text = { Text(stringResource(R.string.mymusic_delete), color = Color(0xFFFF6B6B)) }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Combined Create Playlist Dialog (Manual + AI)
// ════════════════════════════════════════════════════════

private enum class CreateMode { MANUAL, AI }
private enum class AiMode { RANDOM, THEME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlaylistDialog(
    uiState: MyMusicUiState,
    onManualCreate: (String) -> Unit,
    onIntent: (MyMusicIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var createMode by remember { mutableStateOf(CreateMode.MANUAL) }
    var manualName by remember { mutableStateOf("") }
    var aiMode by remember { mutableStateOf(AiMode.RANDOM) }
    var sliderValue by remember { mutableFloatStateOf(uiState.selectedSongCount.toFloat()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyColors.Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            M3SegmentedButtonRow(
                options = listOf(stringResource(R.string.mymusic_create_manual), stringResource(R.string.mymusic_create_ai)),
                selectedIndex = if (createMode == CreateMode.MANUAL) 0 else 1,
                onOptionSelected = { index ->
                    createMode = if (index == 0) CreateMode.MANUAL else CreateMode.AI
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            when (createMode) {
                CreateMode.MANUAL -> {
                    Text(stringResource(R.string.mymusic_new_playlist_dialog_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotifyColors.TextPrimary)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = manualName, onValueChange = { manualName = it },
                        placeholder = { Text(stringResource(R.string.mymusic_new_playlist_placeholder), color = SpotifyColors.TextTertiary) },
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SpotifyColors.TextPrimary, unfocusedTextColor = SpotifyColors.TextPrimary,
                            focusedBorderColor = SpotifyColors.Accent, unfocusedBorderColor = SpotifyColors.Divider, cursorColor = SpotifyColors.Accent
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    M3FilledButton(
                        onClick = {
                            if (manualName.isNotBlank()) {
                                onManualCreate(manualName.trim())
                            }
                        },
                        enabled = manualName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.mymusic_create)) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mymusic_cancel), color = SpotifyColors.TextSecondary)
                    }
                }

                CreateMode.AI -> {
                    val minCount = 5
                    val maxCount = if (aiMode == AiMode.RANDOM) 50 else 30
                    val showResults = uiState.isAiGenerating || uiState.aiGeneratedSongs.isNotEmpty() || uiState.aiCreateError != null

                    Text(stringResource(R.string.mymusic_ai_dialog_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotifyColors.TextPrimary)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.mymusic_ai_dialog_subtitle), fontSize = 14.sp, color = SpotifyColors.TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    M3SegmentedButtonRow(
                        options = listOf(stringResource(R.string.mymusic_ai_random), stringResource(R.string.mymusic_ai_theme)),
                        selectedIndex = if (aiMode == AiMode.RANDOM) 0 else 1,
                        onOptionSelected = { index ->
                            aiMode = if (index == 0) AiMode.RANDOM else AiMode.THEME
                            onIntent(MyMusicIntent.SetAiCreateMode(if (aiMode == AiMode.RANDOM) AiCreateMode.RANDOM else AiCreateMode.THEME))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (aiMode == AiMode.RANDOM) stringResource(R.string.mymusic_ai_random_desc) else stringResource(R.string.mymusic_ai_theme_desc),
                        fontSize = 13.sp, color = SpotifyColors.TextTertiary
                    )

                    if (showResults) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            uiState.aiGeneratedPlaylistName.ifEmpty {
                                if (aiMode == AiMode.RANDOM) stringResource(R.string.mymusic_ai_fallback_name) else uiState.themeInput
                            },
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotifyColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.fillMaxWidth().height(280.dp)) {
                            when {
                                uiState.isAiGenerating -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = SpotifyColors.Accent)
                                        Spacer(Modifier.height(16.dp))
                                        Text(stringResource(R.string.mymusic_ai_generating), fontSize = 14.sp, color = SpotifyColors.TextSecondary)
                                    }
                                }
                                uiState.aiCreateError != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stringResource(R.string.mymusic_ai_generate_failed), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                                        Spacer(Modifier.height(8.dp))
                                        Text(stringResource(uiState.aiCreateError), fontSize = 13.sp, color = SpotifyColors.TextTertiary)
                                    }
                                }
                                uiState.aiGeneratedSongs.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.mymusic_ai_no_songs), fontSize = 14.sp, color = SpotifyColors.TextSecondary)
                                }
                                else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                                    items(uiState.aiGeneratedSongs, key = { it.id }) { song ->
                                        PreviewSongItem(song)
                                    }
                                }
                            }
                            if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.mymusic_ai_song_count_total, uiState.aiGeneratedSongs.size), fontSize = 13.sp, color = SpotifyColors.TextTertiary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (!uiState.isAiGenerating && uiState.aiCreateError == null && uiState.aiGeneratedSongs.isNotEmpty()) {
                            M3FilledButton(
                                onClick = { onIntent(MyMusicIntent.ConfirmAiPlaylist); onDismiss() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.mymusic_ai_create_confirm)) }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.mymusic_ai_regenerate), color = SpotifyColors.Accent) }
                        }
                    } else {
                        Spacer(Modifier.height(20.dp))
                        if (aiMode == AiMode.THEME) {
                            OutlinedTextField(
                                value = uiState.themeInput,
                                onValueChange = { onIntent(MyMusicIntent.SetThemeInput(it)) },
                                placeholder = { Text(stringResource(R.string.mymusic_ai_theme_placeholder)) },
                                label = { Text(stringResource(R.string.mymusic_ai_theme_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SpotifyColors.TextPrimary, unfocusedTextColor = SpotifyColors.TextPrimary,
                                    focusedBorderColor = SpotifyColors.Accent, unfocusedBorderColor = SpotifyColors.Divider, cursorColor = SpotifyColors.Accent
                                )
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.mymusic_ai_song_count_label), fontSize = 14.sp, color = SpotifyColors.TextPrimary)
                            Text(stringResource(R.string.mymusic_ai_song_count_value, sliderValue.toInt()), fontSize = 14.sp, color = SpotifyColors.Accent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                sliderValue = it
                                onIntent(MyMusicIntent.SetSelectedSongCount(it.toInt()))
                            },
                            valueRange = minCount.toFloat()..maxCount.toFloat(),
                            steps = maxCount - minCount - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = SpotifyColors.Accent, activeTrackColor = SpotifyColors.Accent,
                                inactiveTrackColor = SpotifyColors.SurfaceVariant
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$minCount", fontSize = 12.sp, color = SpotifyColors.TextTertiary)
                            Text("$maxCount", fontSize = 12.sp, color = SpotifyColors.TextTertiary)
                        }
                        Spacer(Modifier.height(28.dp))
                        M3FilledButton(
                            onClick = { onIntent(MyMusicIntent.GenerateAiPlaylist) },
                            enabled = !(aiMode == AiMode.THEME && uiState.themeInput.isBlank()),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.mymusic_ai_generate)) }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mymusic_cancel), color = SpotifyColors.TextSecondary)
                    }
                }
            }
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

@Composable
private fun RenamePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mymusic_rename_dialog_title), color = SpotifyColors.TextPrimary) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.mymusic_rename_placeholder), color = SpotifyColors.TextTertiary) },
                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SpotifyColors.TextPrimary, unfocusedTextColor = SpotifyColors.TextPrimary,
                    focusedBorderColor = SpotifyColors.Accent, unfocusedBorderColor = SpotifyColors.Divider, cursorColor = SpotifyColors.Accent)
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.mymusic_rename_confirm), color = SpotifyGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.mymusic_rename_cancel), color = SpotifyColors.TextSecondary) } },
        containerColor = SpotifyColors.Surface, titleContentColor = SpotifyColors.TextPrimary, textContentColor = SpotifyColors.TextSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}
