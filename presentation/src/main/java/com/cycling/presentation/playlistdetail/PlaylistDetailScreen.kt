package com.cycling.presentation.playlistdetail

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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.cycling.presentation.R
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: PlaylistDetailViewModel,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlaylistDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is PlaylistDetailEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val dominantColor = uiState.accentColor

    // Full screen gradient background (extends behind status bar)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(dominantColor, Color.Black)
                )
            )
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.padding(25.dp))

                        // Cover Art
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(230.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val art = uiState.songs.firstOrNull()?.albumArt
                                if (art != null) {
                                    AsyncImage(
                                        model = art,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF333333)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp),
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.padding(5.dp))

                        // Playlist Name
                        Text(
                            modifier = Modifier.padding(start = 20.dp, top = 5.dp),
                            text = uiState.playlist?.name ?: stringResource(R.string.playlist_detail_unknown),
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Song count
                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = stringResource(R.string.playlist_detail_song_count, uiState.songs.size),
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Play Action Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.width(75.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .height(60.dp)
                                        .padding(top = 5.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbnailArt = uiState.songs.firstOrNull()?.albumArt
                                    if (thumbnailArt != null) {
                                        AsyncImage(
                                            model = thumbnailArt,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            if (uiState.songs.isNotEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color.White)
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
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.playlist_detail_play_all)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.playlist_detail_empty),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.handleIntent(PlaylistDetailIntent.SongClick(song))
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(end = 10.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
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
                                            .background(Color(0xFF333333)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Gray
                                        )
                                    }
                                }
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

                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.playlist_detail_more_desc),
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = SpotifyColors.Surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playlist_detail_remove), color = Color(0xFFFF6B6B)) },
                                onClick = {
                                    viewModel.handleIntent(PlaylistDetailIntent.RemoveSong(song.id))
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(bottomPadding + 80.dp))
            }
        }

        // Top bar overlaid on top
        M3DetailTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationIconContentDescription = stringResource(R.string.playlist_detail_back_desc),
            onNavigationClick = onNavigateBack,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }
}
