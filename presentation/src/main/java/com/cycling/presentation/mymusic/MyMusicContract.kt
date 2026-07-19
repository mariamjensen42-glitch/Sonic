package com.cycling.presentation.mymusic

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

enum class MyMusicTab {
    RECENTLY_PLAYED,
    MOST_PLAYED,
    PLAYLISTS
}

data class MyMusicUiState(
    val currentTab: MyMusicTab = MyMusicTab.RECENTLY_PLAYED,
    val favoriteSongs: List<Song> = emptyList(),
    val recentlyPlayedSongs: List<Song> = emptyList(),
    val mostPlayedSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    // Playlists
    val playlists: List<Playlist> = emptyList(),
    val playlistToRename: Playlist? = null,
    val showRenameDialog: Boolean = false,
    val showFavoriteSongs: Boolean = false,
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

enum class AiCreateMode { RANDOM, THEME }

enum class AiCreateStep { SELECT_MODE, INPUT_DETAILS, PREVIEW }

sealed interface MyMusicIntent {
    // Songs
    data class SelectTab(val tab: MyMusicTab) : MyMusicIntent
    data class SongClick(val song: Song) : MyMusicIntent
    data object PlayAll : MyMusicIntent
    data object ShuffleAll : MyMusicIntent
    data class ToggleFavorite(val song: Song) : MyMusicIntent
    // Playlists
    data object FavoriteSongsClick : MyMusicIntent
    data object BackToPlaylists : MyMusicIntent
    data class PlaylistClick(val playlist: Playlist) : MyMusicIntent
    data class CreatePlaylist(val name: String) : MyMusicIntent
    data class DeletePlaylist(val playlistId: Long) : MyMusicIntent
    data class ShowRenameDialog(val playlist: Playlist) : MyMusicIntent
    data class RenamePlaylist(val playlistId: Long, val newName: String) : MyMusicIntent
    data object DismissRenameDialog : MyMusicIntent
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
    data class ShowToast(val message: String) : MyMusicEffect
}
