package com.cycling.sonic.presentation.playlistdetail

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
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.DropdownMenuItem
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
import com.cycling.sonic.core.ui.LocalM3ToastState
import com.cycling.sonic.core.ui.components.M3ArtworkImage
import com.cycling.sonic.core.ui.components.M3DetailTopAppBar
import com.cycling.sonic.core.ui.theme.SpotifyColors
import com.cycling.sonic.core.ui.theme.rememberDominantColor
import com.cycling.sonic.presentation.components.PlaylistPickerDialog
import com.cycling.sonic.presentation.components.SongMoreMenu
import com.cycling.sonic.presentation.player.PlayerIntent
import com.cycling.sonic.presentation.player.PlayerViewModel
import org.jetbrains.compose.resources.stringResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.playlist_detail_back_desc
import sonic.shared.generated.resources.playlist_detail_empty
import sonic.shared.generated.resources.playlist_detail_play_all
import sonic.shared.generated.resources.playlist_detail_remove
import sonic.shared.generated.resources.playlist_detail_song_count
import sonic.shared.generated.resources.playlist_detail_unknown

private val AppBackground = Color(0xFF0B0B0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSongDetail: (Long) -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val dominantColor = rememberDominantColor(
        uiState.songs.firstOrNull()?.albumArt,
        filePath = uiState.songs.firstOrNull()?.path,
        AppBackground
    )
    var menuIndex by remember { mutableIntStateOf(-1) }

    val toastState = LocalM3ToastState.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlaylistDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is PlaylistDetailEffect.ShowToast -> toastState?.show(effect.message)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(dominantColor, Color.Black))
        )
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(460.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.padding(25.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                                val art = uiState.songs.firstOrNull()?.albumArt
                                M3ArtworkImage(
                                    artworkUrl = art,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = {
                                        if (uiState.playlist?.isSystem == true) {
                                            Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(80.dp), tint = SpotifyColors.Accent)
                                        } else {
                                            Icon(imageVector = Icons.AutoMirrored.Outlined.QueueMusic, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(5.dp))

                        Text(
                            modifier = Modifier.padding(start = 20.dp, top = 5.dp),
                            text = uiState.playlist?.name ?: stringResource(Res.string.playlist_detail_unknown),
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = stringResource(Res.string.playlist_detail_song_count, uiState.songs.size),
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.width(75.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbnailArt = uiState.songs.firstOrNull()?.albumArt
                                    M3ArtworkImage(artworkUrl = thumbnailArt, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }

                            if (uiState.songs.isNotEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(100.dp)).background(Color.White)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.handleIntent(PlaylistDetailIntent.SongClick(uiState.songs.first()))
                                        }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(25.dp),
                                        tint = Color.Black,
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(Res.string.playlist_detail_play_all)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.playlist_detail_empty),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(uiState.songs) { index, song ->
                    val isCurrentSong = song.id == uiState.currentSongId
                    val currentPlayingColor = if (isCurrentSong) SpotifyColors.Accent else Color.White

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.handleIntent(PlaylistDetailIntent.SongClick(song)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).padding(end = 10.dp).clip(RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                M3ArtworkImage(
                                    artworkUrl = song.albumArt,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = { Icon(imageVector = Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray) }
                                )
                            }

                            Column {
                                Text(
                                    text = song.title,
                                    color = currentPlayingColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SongMoreMenu(
                            expanded = menuIndex == index,
                            onMoreClick = { menuIndex = index },
                            onDismiss = { menuIndex = -1 },
                            onPlayNext = { playerViewModel.handleIntent(PlayerIntent.PlayNext(song)) },
                            onViewDetail = { onNavigateToSongDetail(song.id) },
                            onAddToPlaylist = { viewModel.handleIntent(PlaylistDetailIntent.ShowAddToPlaylistDialog(song)) },
                            tint = Color.Gray,
                            extraItems = {
                                DropdownMenuItem(
                                    text = {
                                        val removeText = if (uiState.playlist?.isSystem == true) "取消喜欢"
                                        else stringResource(Res.string.playlist_detail_remove)
                                        Text(removeText, color = Color(0xFFFF6B6B))
                                    },
                                    onClick = {
                                        viewModel.handleIntent(PlaylistDetailIntent.RemoveSong(song.id))
                                        menuIndex = -1
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(bottomPadding + 80.dp)) }
        }

        PlaylistPickerDialog(
            visible = uiState.showAddToPlaylistDialog,
            playlists = uiState.allPlaylists,
            onDismiss = { viewModel.handleIntent(PlaylistDetailIntent.DismissAddToPlaylistDialog) },
            onPlaylistSelected = { viewModel.handleIntent(PlaylistDetailIntent.AddToPlaylist(it)) },
            onCreateNewPlaylist = { viewModel.handleIntent(PlaylistDetailIntent.CreatePlaylistAndAddSong(it)) }
        )

        M3DetailTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            navigationIconContentDescription = stringResource(Res.string.playlist_detail_back_desc),
            onNavigationClick = onNavigateBack,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }
}


