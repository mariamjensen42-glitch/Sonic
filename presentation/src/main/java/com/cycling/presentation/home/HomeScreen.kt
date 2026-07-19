package com.cycling.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.core.ui.components.M3CircularProgressIndicator
import com.cycling.core.ui.theme.SpotifyColors
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.cycling.presentation.R
import com.cycling.domain.model.Album
import com.cycling.domain.model.Artist
import com.cycling.domain.model.Song
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToMyMusic: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSongs -> onNavigateToSongs()
                is HomeEffect.NavigateToAlbums -> onNavigateToAlbums()
                is HomeEffect.NavigateToArtists -> onNavigateToArtists()
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.NavigateToAlbumDetail -> onNavigateToAlbumDetail(effect.albumId)
                is HomeEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
                is HomeEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is HomeEffect.NavigateToMyMusic -> onNavigateToMyMusic()
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToFolders -> onNavigateToFolders()
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyColors.Background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                M3CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // === Greeting ===
                    item {
                        GreetingSection(
                            onSettingsClick = onNavigateToSettings
                        )
                    }

                    // === Quick Play Grid (Spotify 2-column album grid) ===
                    if (uiState.topAlbums.isNotEmpty()) {
                        item {
                            QuickPlayGrid(
                                albums = uiState.topAlbums.take(8),
                                onAlbumClick = { album ->
                                    viewModel.handleIntent(HomeIntent.AlbumClick(album))
                                }
                            )
                        }
                    }

                    // === Recently Played ===
                    if (uiState.recentlyPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_recent_plays),
                                onSeeAll = null
                            )
                        }
                        item {
                            SongRow(
                                songs = uiState.recentlyPlayed,
                                onSongClick = { song ->
                                    viewModel.handleIntent(HomeIntent.SongClick(song))
                                }
                            )
                        }
                    }

                    // === Top Albums ===
                    if (uiState.topAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_recommended_albums),
                                onSeeAll = {
                                    viewModel.handleIntent(HomeIntent.NavigateToAlbums)
                                }
                            )
                        }
                        item {
                            AlbumRow(
                                albums = uiState.topAlbums,
                                onAlbumClick = { album ->
                                    viewModel.handleIntent(HomeIntent.AlbumClick(album))
                                }
                            )
                        }
                    }

                    // === Top Artists ===
                    if (uiState.topArtists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_recommended_artists),
                                onSeeAll = {
                                    viewModel.handleIntent(HomeIntent.NavigateToArtists)
                                }
                            )
                        }
                        item {
                            ArtistRow(
                                artists = uiState.topArtists,
                                onArtistClick = { artist ->
                                    viewModel.handleIntent(HomeIntent.ArtistClick(artist))
                                }
                            )
                        }
                    }

                    // === Favorite Songs ===
                    if (uiState.favoriteSongs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_favorite_songs),
                                onSeeAll = null
                            )
                        }
                        item {
                            SongRow(
                                songs = uiState.favoriteSongs,
                                onSongClick = { song ->
                                    viewModel.handleIntent(HomeIntent.SongClick(song))
                                }
                            )
                        }
                    }

                    // === Recently Added ===
                    if (uiState.recentlyAdded.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_recently_added),
                                onSeeAll = {
                                    viewModel.handleIntent(HomeIntent.NavigateToSongs)
                                }
                            )
                        }
                        item {
                            SongRow(
                                songs = uiState.recentlyAdded,
                                onSongClick = { song ->
                                    viewModel.handleIntent(HomeIntent.SongClick(song))
                                }
                            )
                        }
                    }

                    // === Most Played ===
                    if (uiState.mostPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_most_played),
                                onSeeAll = null
                            )
                        }
                        item {
                            SongRow(
                                songs = uiState.mostPlayed,
                                onSongClick = { song ->
                                    viewModel.handleIntent(HomeIntent.SongClick(song))
                                }
                            )
                        }
                    }

                    // === Discover Section (large cards) ===
                    if (uiState.topAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_discover),
                                onSeeAll = null
                            )
                        }
                        item {
                            DiscoverSection(
                                albums = uiState.topAlbums.takeLast(3),
                                onAlbumClick = { album ->
                                    viewModel.handleIntent(HomeIntent.AlbumClick(album))
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(bottomPadding + 80.dp))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Greeting Section — Spotify style
// ════════════════════════════════════════════════════════════════

@Composable
private fun GreetingSection(
    name: String = "用户",
    onSettingsClick: () -> Unit = {}
) {
    val currentHour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            currentHour < 12 -> R.string.home_greeting_morning
            currentHour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                color = SpotifyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp
            )
        }
        // Settings icon
        Icon(
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = stringResource(R.string.home_settings_desc),
            tint = SpotifyColors.TextSecondary,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSettingsClick
                )
                .padding(8.dp)
                .size(24.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Quick Play Grid — Spotify's 2-column album shortcut grid
// ════════════════════════════════════════════════════════════════

@Composable
private fun QuickPlayGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    val gridAlbums = albums.take(8)
    val chunked = gridAlbums.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        chunked.forEach { rowAlbums ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowAlbums.forEach { album ->
                    QuickPlayItem(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowAlbums.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPlayItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SpotifyColors.GridBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Album thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
        ) {
            if (album.albumArt != null) {
                AsyncImage(
                    model = album.albumArt,
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpotifyColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = SpotifyColors.TextSecondary
                    )
                }
            }
        }
        Text(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp),
            text = album.name,
            color = SpotifyColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Section Header — Spotify style
// ════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SpotifyColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            Text(
                text = stringResource(R.string.home_view_all),
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSeeAll
                    )
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Album Row (Horizontal Scrolling) — Spotify style
// ════════════════════════════════════════════════════════════════

@Composable
private fun AlbumRow(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            // Album Art — rounded square
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArt != null) {
                    AsyncImage(
                        model = album.albumArt,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_album),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SpotifyColors.TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.name,
                color = SpotifyColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Artist Row (Horizontal Scrolling) — Spotify style
// ════════════════════════════════════════════════════════════════

@Composable
private fun ArtistRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistCard(
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular artist art
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artistArt != null) {
                    AsyncImage(
                        model = artist.artistArt,
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_artists),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SpotifyColors.TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = artist.name,
                color = SpotifyColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.home_artist_label),
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Song Row (Horizontal Scrolling) — Spotify style
// ════════════════════════════════════════════════════════════════

@Composable
private fun SongRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongCard(
                song = song,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            // Song art — rounded square
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArt != null) {
                    AsyncImage(
                        model = song.albumArt,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SpotifyColors.TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                color = SpotifyColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Discover Section — large cards with gradient overlay
// ════════════════════════════════════════════════════════════════

@Composable
private fun DiscoverSection(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        albums.forEach { album ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onAlbumClick(album) }
                    )
            ) {
                // Background image
                if (album.albumArt != null) {
                    AsyncImage(
                        model = album.albumArt,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SpotifyColors.SurfaceVariant)
                    )
                }
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SpotifyColors.CardGradient.copy(alpha = 0.9f)
                                ),
                                startY = 50f
                            )
                        )
                )
                // Text at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = album.name,
                        color = SpotifyColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


