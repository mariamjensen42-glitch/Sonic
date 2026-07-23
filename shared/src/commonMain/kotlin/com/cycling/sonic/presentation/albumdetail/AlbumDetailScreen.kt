package com.cycling.sonic.presentation.albumdetail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.cycling.sonic.presentation.components.PlaylistPickerDialog
import com.cycling.sonic.presentation.components.SongMoreMenu
import com.cycling.sonic.presentation.player.PlayerIntent
import com.cycling.sonic.presentation.player.PlayerViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.added
import sonic.shared.generated.resources.album_detail_back_desc
import sonic.shared.generated.resources.album_detail_info
import sonic.shared.generated.resources.album_detail_play_all_desc
import sonic.shared.generated.resources.album_detail_unknown
import sonic.shared.generated.resources.album_detail_unknown_artist
import sonic.shared.generated.resources.ic_add
import sonic.shared.generated.resources.ic_album
import sonic.shared.generated.resources.player_favorite_desc
import sonic.shared.generated.resources.player_unfavorite_desc

private val AppBackground = Color(0xFF0B0B0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToSongDetail: (Long) -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    var dominantColor by remember { mutableStateOf(AppBackground) }
    var menuIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AlbumDetailEffect.NavigateToPlayer -> {
                    val song = uiState.songs.find { it.id == effect.songId } ?: return@collect
                    playerViewModel.handleIntent(PlayerIntent.PlaySong(song, uiState.songs))
                    onNavigateToPlayer(effect.songId)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(dominantColor, Color.Black))
        )
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(460.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.padding(25.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                                M3ArtworkImage(
                                    artworkUrl = uiState.album?.albumArt,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = { Icon(painter = painterResource(Res.drawable.ic_album), contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(5.dp))

                        Text(
                            modifier = Modifier.padding(start = 20.dp, top = 5.dp),
                            text = uiState.album?.name ?: stringResource(Res.string.album_detail_unknown),
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = uiState.album?.artist ?: stringResource(Res.string.album_detail_unknown_artist),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = stringResource(Res.string.album_detail_info, uiState.songs.size),
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
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(5.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    M3ArtworkImage(artworkUrl = uiState.album?.albumArt, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    painter = painterResource(Res.drawable.ic_add),
                                    tint = Color.White,
                                    contentDescription = stringResource(Res.string.player_favorite_desc),
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            if (uiState.songs.isNotEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(100.dp)).background(Color.White)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.handleIntent(AlbumDetailIntent.SongClick(uiState.songs.first()))
                                        }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(25.dp),
                                        tint = Color.Black,
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(Res.string.album_detail_play_all_desc)
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(uiState.songs) { index, song ->
                    val isCurrentSong = song.id == uiState.currentSongId
                    val currentPlayingColor = if (isCurrentSong) Color(0xFF618DFF) else Color.White
                    val isLiked = song.id in uiState.likedSongIds

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.handleIntent(AlbumDetailIntent.SongClick(song)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).padding(end = 10.dp).clip(RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                M3ArtworkImage(
                                    artworkUrl = song.albumArt,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = { Icon(painter = painterResource(Res.drawable.ic_album), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray) }
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

                        Icon(
                            modifier = Modifier.size(20.dp).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.handleIntent(AlbumDetailIntent.ToggleFavorite(song.id)) },
                            painter = if (isLiked) painterResource(Res.drawable.added) else painterResource(Res.drawable.ic_add),
                            tint = if (isLiked) Color.White else Color.Gray,
                            contentDescription = if (isLiked) stringResource(Res.string.player_unfavorite_desc) else stringResource(Res.string.player_favorite_desc)
                        )

                        SongMoreMenu(
                            expanded = menuIndex == index,
                            onMoreClick = { menuIndex = index },
                            onDismiss = { menuIndex = -1 },
                            onPlayNext = { playerViewModel.handleIntent(PlayerIntent.PlayNext(song)) },
                            onViewDetail = { onNavigateToSongDetail(song.id) },
                            onAddToPlaylist = { viewModel.handleIntent(AlbumDetailIntent.ShowAddToPlaylistDialog(song)) },
                            tint = Color.Gray
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(bottomPadding + 80.dp)) }
            }
        }

        PlaylistPickerDialog(
            visible = uiState.showAddToPlaylistDialog,
            playlists = uiState.playlists,
            onDismiss = { viewModel.handleIntent(AlbumDetailIntent.DismissAddToPlaylistDialog) },
            onPlaylistSelected = { viewModel.handleIntent(AlbumDetailIntent.AddToPlaylist(it)) },
            onCreateNewPlaylist = { viewModel.handleIntent(AlbumDetailIntent.CreatePlaylistAndAddSong(it)) }
        )

        M3DetailTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationIconContentDescription = stringResource(Res.string.album_detail_back_desc),
            onNavigationClick = onNavigateBack,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }
}
