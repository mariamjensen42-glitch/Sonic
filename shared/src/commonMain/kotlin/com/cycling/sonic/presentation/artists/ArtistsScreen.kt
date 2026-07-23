package com.cycling.sonic.presentation.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.cycling.sonic.core.ui.components.M3Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.sonic.core.ui.components.M3ArtistCard
import com.cycling.sonic.core.ui.components.M3CircularProgressIndicator
import com.cycling.sonic.core.ui.components.M3DetailTopAppBar
import com.cycling.sonic.core.ui.theme.M3ComponentSize
import com.cycling.sonic.core.ui.theme.M3Spacing
import com.cycling.sonic.core.ui.theme.SpotifyColors
import org.jetbrains.compose.resources.painterResource
import sonic.shared.generated.resources.Res
import sonic.shared.generated.resources.ic_artists

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArtistsEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
            }
        }
    }

    M3Scaffold(
        topBar = {
            M3DetailTopAppBar(
                title = "歌手",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyColors.Background,
                    titleContentColor = SpotifyColors.TextPrimary,
                    navigationIconContentColor = SpotifyColors.TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    M3CircularProgressIndicator()
                }
            } else if (uiState.artists.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(Res.drawable.ic_artists),
                        contentDescription = null, modifier = Modifier.size(64.dp),
                        tint = SpotifyColors.TextTertiary
                    )
                    Spacer(Modifier.height(M3Spacing.medium))
                    Text("暂无歌手", style = MaterialTheme.typography.titleMedium, color = SpotifyColors.TextPrimary)
                    Text("您的音乐库中没有歌手信息", style = MaterialTheme.typography.bodyMedium, color = SpotifyColors.TextSecondary)
                    Spacer(Modifier.weight(1f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = M3ComponentSize.artistCardSize + M3Spacing.large),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = M3Spacing.medium, end = M3Spacing.medium,
                        top = M3Spacing.small, bottom = M3Spacing.extraExtraLarge + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(M3Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(M3Spacing.medium)
                ) {
                    items(uiState.artists, key = { it.id }) { artist ->
                        M3ArtistCard(
                            name = artist.name,
                            artwork = artist.artistArt,
                            onClick = { viewModel.handleIntent(ArtistsIntent.ArtistClick(artist)) }
                        )
                    }
                }
            }
        }
    }
}
