package com.cycling.sonic.presentation.artistdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.sonic.core.ui.components.M3ArtworkImage
import com.cycling.sonic.core.ui.components.M3DetailTopAppBar
import com.cycling.sonic.core.ui.theme.SpotifyColors
import com.cycling.sonic.core.ui.theme.rememberDominantColor
import com.cycling.sonic.presentation.player.PlayerIntent
import com.cycling.sonic.presentation.player.PlayerViewModel
import com.cycling.sonic.presentation.components.PlaylistPickerDialog
import com.cycling.sonic.presentation.components.SongMoreMenu
import org.jetbrains.compose.resources.painterResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.added
import sonic.shared.generated.resources.ic_add
import sonic.shared.generated.resources.ic_artists

private val AppBackground = Color(0xFF0B0B0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    viewModel: ArtistDetailViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSongDetail: (Long) -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuIndex by remember { mutableIntStateOf(-1) }
    val dominantColor = rememberDominantColor(
        uiState.songs.firstOrNull()?.albumArt,
        filePath = uiState.songs.firstOrNull()?.path,
        AppBackground
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArtistDetailEffect.NavigateToPlayer -> {
                    val song = uiState.songs.find { it.id == effect.songId } ?: return@collect
                    playerViewModel.handleIntent(PlayerIntent.PlaySong(song, uiState.songs))
                    onNavigateToPlayer(effect.songId)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(dominantColor, Color.Black)))) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Color.White) }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.fillMaxWidth().height(460.dp), verticalArrangement = Arrangement.Center) {
                        Spacer(Modifier.padding(25.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(240.dp).clip(RoundedCornerShape(120.dp)), contentAlignment = Alignment.Center) {
                                M3ArtworkImage(
                                    artworkUrl = uiState.songs.firstOrNull()?.albumArt,
                                    contentDescription = uiState.artist?.name,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = {
                                        Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                                            Icon(painter = painterResource(Res.drawable.ic_artists), null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.padding(20.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(200.dp)) {
                                Text(uiState.artist?.name ?: "未知歌手", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("为你推荐", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            if (uiState.songs.isNotEmpty() && !uiState.isPlaying) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(100.dp)).background(Color.White).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.handleIntent(ArtistDetailIntent.SongClick(uiState.songs.first())) }) {
                                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "播放全部", modifier = Modifier.size(25.dp), tint = Color.Black)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(uiState.songs) { index, song ->
                    val isCurrentSong = song.id == uiState.currentSongId
                    val isLiked = song.id in uiState.likedSongIds
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.handleIntent(ArtistDetailIntent.SongClick(song)) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(50.dp).padding(end = 10.dp).clip(RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(24.dp), tint = Color.Gray)
                                }
                            }
                            Column {
                                Text(song.title, color = if (isCurrentSong) SpotifyColors.Accent else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Icon(
                            painter = if (isLiked) painterResource(Res.drawable.added) else painterResource(Res.drawable.ic_add),
                            contentDescription = if (isLiked) "取消收藏" else "收藏",
                            modifier = Modifier.size(20.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.handleIntent(ArtistDetailIntent.ToggleFavorite(song.id)) },
                            tint = if (isLiked) Color.White else Color.Gray)
                        SongMoreMenu(expanded = menuIndex == index, onMoreClick = { menuIndex = index }, onDismiss = { menuIndex = -1 }, onPlayNext = { playerViewModel.handleIntent(PlayerIntent.PlayNext(song)) }, onViewDetail = { onNavigateToSongDetail(song.id) }, onAddToPlaylist = { viewModel.handleIntent(ArtistDetailIntent.ShowAddToPlaylistDialog(song)) }, tint = Color.Gray)
                    }
                }
                item { Spacer(Modifier.height(bottomPadding + 80.dp)) }
            }
        }

        M3DetailTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationIconContentDescription = "返回",
            onNavigationClick = onNavigateBack,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
        )

        PlaylistPickerDialog(
            visible = uiState.showAddToPlaylistDialog,
            playlists = uiState.playlists,
            onDismiss = { viewModel.handleIntent(ArtistDetailIntent.DismissAddToPlaylistDialog) },
            onPlaylistSelected = { viewModel.handleIntent(ArtistDetailIntent.AddToPlaylist(it)) },
            onCreateNewPlaylist = { viewModel.handleIntent(ArtistDetailIntent.CreatePlaylistAndAddSong(it)) }
        )
    }
}


