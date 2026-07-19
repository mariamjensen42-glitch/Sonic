package com.cycling.presentation.mymusic

import com.cycling.domain.model.Playlist
import androidx.annotation.StringRes
import com.cycling.domain.model.Song

enum class MyMusicTab {
    RECENTLY_PLAYED,
    MOST_PLAYED,
    PLAYLISTS
}

data class MyMusicUiState(
    val favoriteSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val showFavoriteSongs: Boolean = false,
    // AI create dialog
    val showAiCreateDialog: Boolean = false,
    val aiCreateMode: AiCreateMode = AiCreateMode.RANDOM,
    val selectedSongCount: Int = 10,
    val themeInput: String = "",
    val isAiGenerating: Boolean = false,
    val aiGeneratedSongs: List<Song> = emptyList(),
    val aiGeneratedPlaylistName: String = "",
    val aiCreateError: Int? = null,
    val hasApiKey: Boolean = false,
    // Rename
    val playlistToRename: Playlist? = null,
    val showRenameDialog: Boolean = false
)

enum class AiCreateMode { RANDOM, THEME }

sealed interface MyMusicIntent {
    data object FavoriteSongsClick : MyMusicIntent
    data object BackToPlaylists : MyMusicIntent
    data class SongClick(val song: Song) : MyMusicIntent
    data class ToggleFavorite(val song: Song) : MyMusicIntent
    data class PlaylistClick(val playlist: Playlist) : MyMusicIntent
    data class CreatePlaylist(val name: String) : MyMusicIntent
    data class DeletePlaylist(val playlist: Playlist) : MyMusicIntent
    data class ShowRenameDialog(val playlist: Playlist) : MyMusicIntent
    data class RenamePlaylist(val playlistId: Long, val newName: String) : MyMusicIntent
    data object DismissRenameDialog : MyMusicIntent
    // AI
    data object ShowAiCreateDialog : MyMusicIntent
    data object DismissAiCreateDialog : MyMusicIntent
    data class SetAiCreateMode(val mode: AiCreateMode) : MyMusicIntent
    data class SetSelectedSongCount(val count: Int) : MyMusicIntent
    data class SetThemeInput(val theme: String) : MyMusicIntent
    data object GenerateAiPlaylist : MyMusicIntent
    data object ConfirmAiPlaylist : MyMusicIntent
}

sealed interface MyMusicEffect {
    data class NavigateToPlayer(val songId: Long) : MyMusicEffect
    data class NavigateToPlaylistDetail(val playlistId: Long) : MyMusicEffect
    data class ShowToast(@StringRes val message: Int) : MyMusicEffect
}
