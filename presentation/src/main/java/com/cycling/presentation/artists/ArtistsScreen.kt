@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleTopBar
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3ArtistCard
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState

@Composable
fun ArtistsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToArtistDetail: (Long) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArtistsEffect.NavigateToArtistDetail -> onNavigateToArtistDetail(effect.artistId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppleTheme.colors.background,
        topBar = {
            AppleTopBar(
                title = stringResource(R.string.artists_title),
                subtitle = stringResource(R.string.artists_count, uiState.artists.size),
                onNavigateBack = onNavigateBack,
                backContentDescription = stringResource(R.string.artists_back_desc),
                actions = {

                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> SonLoadingState(modifier = Modifier.padding(paddingValues))
            uiState.artists.isEmpty() -> SonEmptyState(
                icon = SymbolIconName.Person,
                title = stringResource(R.string.artists_empty_title),
                subtitle = stringResource(R.string.artists_empty_subtitle),
                modifier = Modifier.padding(paddingValues)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = SonSizes.artistCardSize + SonSpacing.large),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = gridState,
                contentPadding = PaddingValues(
                    start = SonLayout.screenMargin,
                    end = SonLayout.screenMargin,
                    top = SonLayout.contentTopPadding,
                    bottom = appleBottomBarClearance() + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(SonLayout.gridGap),
                horizontalArrangement = Arrangement.spacedBy(SonLayout.gridGap)
            ) {
                items(uiState.artists, key = { it.id }) { artist ->
                    M3ArtistCard(
                        name = artist.name,
                        artwork = artist.artistArt,
                        onClick = { viewModel.handleIntent(ArtistsIntent.ArtistClick(artist)) },
                        size = null
                    )
                }
            }
        }
    }
}
