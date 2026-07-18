package com.cycling.presentation.playlists

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

enum class AiCreateMode {
    RANDOM, THEME
}

enum class AiCreateStep {
    SELECT_MODE,
    INPUT_DETAILS,
    PREVIEW
}

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val playlistToRename: Playlist? = null,
    val showRenameDialog: Boolean = false,
    val showAiCreateDialog: Boolean = false,
    val aiCreateMode: AiCreateMode = AiCreateMode.RANDOM,
    val aiCreateStep: AiCreateStep = AiCreateStep.SELECT_MODE,
    val selectedSongCount: Int = 10,
    val themeInput: String = "",
    val isAiGenerating: Boolean = false,
    val aiGeneratedSongs: List<Song> = emptyList(),
    val aiGeneratedPlaylistName: String = "",
    val aiCreateError: String? = null,
    val hasApiKey: Boolean = false
)

sealed interface PlaylistsIntent {
    data class PlaylistClick(val playlist: Playlist) : PlaylistsIntent
    data class CreatePlaylist(val name: String) : PlaylistsIntent
    data class DeletePlaylist(val playlistId: Long) : PlaylistsIntent
    data class ShowRenameDialog(val playlist: Playlist) : PlaylistsIntent
    data class RenamePlaylist(val playlistId: Long, val newName: String) : PlaylistsIntent
    data object DismissRenameDialog : PlaylistsIntent
    data object ShowAiCreateDialog : PlaylistsIntent
    data object DismissAiCreateDialog : PlaylistsIntent
    data class SetAiCreateMode(val mode: AiCreateMode) : PlaylistsIntent
    data class SetSelectedSongCount(val count: Int) : PlaylistsIntent
    data class SetThemeInput(val theme: String) : PlaylistsIntent
    data object GenerateAiPlaylist : PlaylistsIntent
    data object ConfirmAiPlaylist : PlaylistsIntent
}

sealed interface PlaylistsEffect {
    data class NavigateToPlaylistDetail(val playlistId: Long) : PlaylistsEffect
    data class ShowToast(val message: String) : PlaylistsEffect
}
