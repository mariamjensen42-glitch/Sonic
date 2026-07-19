package com.cycling.presentation.playlistdetail

import androidx.compose.ui.graphics.Color
import com.cycling.domain.model.Playlist
import androidx.annotation.StringRes
import com.cycling.domain.model.Song

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val accentColor: Color = Color(0xFF1A1A2E),
    val currentSongId: Long? = null
)

sealed interface PlaylistDetailIntent {
    data class SongClick(val song: Song) : PlaylistDetailIntent
    data class RemoveSong(val songId: Long) : PlaylistDetailIntent
}

sealed interface PlaylistDetailEffect {
    data class NavigateToPlayer(val songId: Long) : PlaylistDetailEffect
    data class ShowToast(@StringRes val message: Int) : PlaylistDetailEffect
}
