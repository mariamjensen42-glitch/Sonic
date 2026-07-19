package com.cycling.presentation.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.presentation.R
import com.cycling.presentation.lyrics.components.KaraokeLyricsView
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val positionRef = remember { intArrayOf(0) }
    val lineTrigger = remember { mutableIntStateOf(0) }
    val latestPosition by rememberUpdatedState(uiState.playbackPosition.toInt())
    val lyricsLines = uiState.lyrics?.lines ?: emptyList()

    LaunchedEffect(Unit) {
        var lastLineIndex = -1
        while (true) {
            val pos = latestPosition
            positionRef[0] = pos
            val currentLineIndex = lyricsLines.indexOfLast { it.start <= pos }
            if (currentLineIndex != lastLineIndex) {
                lastLineIndex = currentLineIndex
                lineTrigger.intValue = currentLineIndex
            }
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color.Black)
                )
            )
            .statusBarsPadding()
    ) {
        val lyrics = uiState.lyrics
        if (lyrics != null) {
            val listState = rememberLazyListState()
            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = { lineTrigger.intValue; positionRef[0] },
                onLineClicked = { line ->
                    onIntent(PlayerIntent.SeekTo(line.start.toLong()))
                },
                onLinePressed = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, bottom = 48.dp)
            )
        } else if (uiState.isLoadingLyrics) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        } else if (uiState.currentSong != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.player_lyrics_unavailable),
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        M3DetailTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = uiState.currentSong?.title ?: "",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationIconContentDescription = stringResource(R.string.lyrics_back_desc),
            onNavigationClick = onNavigateBack,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }
}
