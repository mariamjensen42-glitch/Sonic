package com.cycling.presentation.songdetail

import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.Song

data class SongDetailUiState(
    val song: Song? = null,
    val audioMetadata: AudioMetadata? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val editedTitle: String = "",
    val editedArtist: String = "",
    val editedAlbum: String = "",
    val editedYear: String = "",
    val editedGenre: String = "",
    val hasChanges: Boolean = false,
    val showDiscardDialog: Boolean = false
)

sealed interface SongDetailIntent {
    data class LoadSong(val songId: Long) : SongDetailIntent
    data object ToggleFavorite : SongDetailIntent
    data object PlaySong : SongDetailIntent
    data object AddToPlaylist : SongDetailIntent
    data class CopyPath(val path: String) : SongDetailIntent
    data object EnterEditMode : SongDetailIntent
    data object ExitEditMode : SongDetailIntent
    data class UpdateTitle(val title: String) : SongDetailIntent
    data class UpdateArtist(val artist: String) : SongDetailIntent
    data class UpdateAlbum(val album: String) : SongDetailIntent
    data class UpdateYear(val year: String) : SongDetailIntent
    data class UpdateGenre(val genre: String) : SongDetailIntent
    data object SaveChanges : SongDetailIntent
    data object DiscardChanges : SongDetailIntent
    data object KeepEditing : SongDetailIntent
}

sealed interface SongDetailEffect {
    data class NavigateToPlayer(val songId: Long) : SongDetailEffect
    data object ShowAddToPlaylistDialog : SongDetailEffect
    data class ShowCopiedMessage(val message: String) : SongDetailEffect
    data class ShowError(val message: String) : SongDetailEffect
    data class ShowToast(val message: String) : SongDetailEffect
}
