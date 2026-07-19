package com.cycling.presentation.artistdetail

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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.ArtColorExtractor
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.presentation.R
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerViewModel
import com.cycling.domain.model.Song

private val AppBackground = Color(0xFF0B0B0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToApiKeyConfig: () -> Unit = {},
    onNavigateToAiInfo: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: ArtistDetailViewModel,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    var dominantColor by remember { mutableStateOf(AppBackground) }
    LaunchedEffect(uiState.songs.firstOrNull()?.path) {
        uiState.songs.firstOrNull()?.let { song ->
            val color = ArtColorExtractor.extractColorFromMusicFile(song.path, AppBackground)
            dominantColor = color
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(dominantColor, Color.Black)
                )
            )
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // ── Header Section (460dp, Neptune/Spotify style) ──
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.padding(25.dp))

                            // Artist Image (240dp, rounded circle)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(RoundedCornerShape(120.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.artist?.artistArt != null) {
                                        AsyncImage(
                                            model = uiState.artist?.artistArt,
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
                                                painter = painterResource(id = com.cycling.presentation.R.drawable.ic_artists),
                                                contentDescription = null,
                                                modifier = Modifier.size(80.dp),
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.padding(20.dp))

                            // Artist Name + Play Button Row
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 25.dp)
                            ) {
                                Column(modifier = Modifier.width(200.dp)) {
                                    Text(
                                        text = uiState.artist?.name ?: stringResource(R.string.artist_detail_unknown),
                                        color = Color.White,
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.artist_detail_subtitle),
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Play Button (white circle) — only show when NOT currently playing
                                if (uiState.songs.isNotEmpty() && !uiState.isPlaying) {
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
                                                viewModel.handleIntent(ArtistDetailIntent.SongClick(uiState.songs.first()))
                                            }
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(25.dp),
                                            tint = Color.Black,
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.artist_detail_play_all_desc)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Song List (Neptune/Spotify style: cover + title + artist + add/added icon) ──
                    itemsIndexed(uiState.songs) { index, song ->
                        val isCurrentSong = song.id == uiState.currentSongId
                        val currentPlayingColor = if (isCurrentSong) SpotifyColors.Accent else Color.White
                        val isLiked = song.id in uiState.likedSongIds

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.handleIntent(ArtistDetailIntent.SongClick(song))
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Cover thumbnail
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

                            // Like icon (Spotify style: add/added)
                            Icon(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        viewModel.handleIntent(ArtistDetailIntent.ToggleFavorite(song.id))
                                    },
                                painter = if (isLiked) painterResource(id = com.cycling.presentation.R.drawable.added)
                                else painterResource(id = com.cycling.presentation.R.drawable.ic_add),
                                tint = if (isLiked) Color.White else Color.Gray,
                                contentDescription = if (isLiked) stringResource(R.string.player_unfavorite_desc) else stringResource(R.string.player_favorite_desc)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(bottomPadding + 80.dp))
                    }
                }
            }

        M3DetailTopAppBar(
        modifier = Modifier.align(Alignment.TopCenter),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        navigationIconContentDescription = stringResource(R.string.artist_detail_back_desc),
        onNavigationClick = onNavigateBack,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}}

