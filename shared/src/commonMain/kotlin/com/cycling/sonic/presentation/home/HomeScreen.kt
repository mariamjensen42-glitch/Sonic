package com.cycling.sonic.presentation.home

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
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import coil3.compose.AsyncImage
import com.cycling.sonic.core.ui.components.M3ArtworkImage
import com.cycling.sonic.core.ui.components.M3CircularProgressIndicator
import com.cycling.sonic.core.ui.theme.SpotifyColors
import com.cycling.sonic.domain.model.Album
import com.cycling.sonic.domain.model.Artist
import com.cycling.sonic.domain.model.Song
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.home_greeting_subtitle
import sonic.shared.generated.resources.home_artist_label
import sonic.shared.generated.resources.home_view_all
import sonic.shared.generated.resources.ic_album
import sonic.shared.generated.resources.ic_artists
import sonic.shared.generated.resources.ic_settings
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
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
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

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
                is HomeEffect.ShowToast -> { }
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
                    item {
                        GreetingSection(onSettingsClick = onNavigateToSettings)
                    }

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

                    if (uiState.recentlyPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "最近播放",
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

                    if (uiState.topAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "推荐专辑",
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

                    if (uiState.topArtists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "热门艺术家",
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

                    if (uiState.favoriteSongs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "收藏歌曲",
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

                    if (uiState.recentlyAdded.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "最近添加",
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

                    if (uiState.mostPlayed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "常播歌曲",
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

                    if (uiState.topAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "发现音乐",
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

@Composable
private fun GreetingSection(
    name: String = "用户",
    onSettingsClick: () -> Unit = {}
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentHour = now.hour
    val greeting = when {
        currentHour < 12 -> "上午好"
        currentHour < 17 -> "下午好"
        else -> "晚上好"
    }

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
                text = stringResource(Res.string.home_greeting_subtitle),
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp
            )
        }
        Icon(
            painter = painterResource(Res.drawable.ic_settings),
            contentDescription = "设置",
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

@Composable
private fun QuickPlayGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    val gridAlbums = albums.take(8)
    val chunked = gridAlbums.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        chunked.forEach { rowAlbums ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowAlbums.forEach { album ->
                    QuickPlayItem(album = album, onClick = { onAlbumClick(album) }, modifier = Modifier.weight(1f))
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
        Box(modifier = Modifier.size(56.dp)) {
            if (album.albumArt != null) {
                AsyncImage(model = album.albumArt, contentDescription = album.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(SpotifyColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(24.dp), tint = SpotifyColors.TextSecondary)
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

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SpotifyColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            Text(
                text = stringResource(Res.string.home_view_all),
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSeeAll
                )
            )
        }
    }
}

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArt != null) {
                    M3ArtworkImage(artworkUrl = song.albumArt, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
            .width(140.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.albumArt != null) {
                    M3ArtworkImage(artworkUrl = album.albumArt, contentDescription = null)
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_album),
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(SpotifyColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artistArt != null) {
                    M3ArtworkImage(artworkUrl = artist.artistArt, contentDescription = null)
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_artists),
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
                text = stringResource(Res.string.home_artist_label),
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

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
                if (album.albumArt != null) {
                    AsyncImage(model = album.albumArt, contentDescription = album.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SpotifyColors.SurfaceVariant)
                    )
                }
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

@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SpotifyColors.Accent.copy(alpha = 0.6f),
                        SpotifyColors.Accent.copy(alpha = 0.3f)
                    )
                )
            )
    )
}
