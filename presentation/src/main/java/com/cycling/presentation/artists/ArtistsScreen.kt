package com.cycling.presentation.artists

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3ArtistCard
import com.cycling.core.ui.components.M3LargeTopAppBar
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArtistsEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            M3LargeTopAppBar(
                title = "歌手",
                scrollBehavior = scrollBehavior,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.cycling.core.ui.components.M3CircularProgressIndicator()
                }
            } else if (uiState.artists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(M3Spacing.medium))
                    Text(
                        text = "暂无歌手",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "您的音乐库中没有歌手信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = M3ComponentSize.artistCardSize + M3Spacing.large),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = M3Spacing.medium,
                        end = M3Spacing.medium,
                        top = M3Spacing.small,
                        bottom = M3Spacing.extraExtraLarge + bottomPadding
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
