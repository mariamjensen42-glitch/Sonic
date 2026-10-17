package com.cycling.presentation.songdetail

import com.cycling.domain.model.AudioMetadata
import androidx.annotation.StringRes
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

/**
 * One line of lyrics as the detail page renders it.
 *
 * The page shows whatever it has, timed or not — unlike the player, which can only follow a
 * [com.cycling.domain.lyrics.model.SyncedLyrics] — so the state carries display-ready lines rather
 * than the parser's model. [translation] is kept apart from [text] so the second language can be
 * set in a weaker style instead of being glued onto the line.
 */
data class LyricsDisplayLine(
    val text: String,
    val translation: String? = null
)

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
    val showDiscardDialog: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val showAddToPlaylistDialog: Boolean = false,
    /** Lines to display, already stripped of timestamps and metadata tags. */
    val lyrics: List<LyricsDisplayLine> = emptyList(),
    val lyricsSource: LyricsSource = LyricsSource.NONE,
    val isLoadingLyrics: Boolean = false,
    /** Text in the editor, verbatim: it is saved back as-is so timings survive a round trip. */
    val editedLyrics: String = "",
    /** What the editor started from, so "did the user touch the lyrics" is one comparison. */
    val originalLyrics: String = ""
)

sealed interface SongDetailIntent {
    data class LoadSong(val songId: Long) : SongDetailIntent
    data object ToggleFavorite : SongDetailIntent
    data object PlaySong : SongDetailIntent
    data object AddToPlaylist : SongDetailIntent
    data object DismissAddToPlaylistDialog : SongDetailIntent
    data class AddToPlaylistTo(val playlistId: Long) : SongDetailIntent
    data class CreatePlaylistAndAddSong(val name: String) : SongDetailIntent
    data class CopyPath(val path: String) : SongDetailIntent
    data object EnterEditMode : SongDetailIntent
    data object ExitEditMode : SongDetailIntent
    data class UpdateTitle(val title: String) : SongDetailIntent
    data class UpdateArtist(val artist: String) : SongDetailIntent
    data class UpdateAlbum(val album: String) : SongDetailIntent
    data class UpdateYear(val year: String) : SongDetailIntent
    data class UpdateGenre(val genre: String) : SongDetailIntent
    data class UpdateLyrics(val lyrics: String) : SongDetailIntent
    data object SaveChanges : SongDetailIntent
    data object DiscardChanges : SongDetailIntent
    data object KeepEditing : SongDetailIntent
}

sealed interface SongDetailEffect {
    data class NavigateToPlayer(val songId: Long) : SongDetailEffect
    data class ShowCopiedMessage(@StringRes val message: Int) : SongDetailEffect
    data class ShowError(@StringRes val message: Int) : SongDetailEffect
    data class ShowToast(@StringRes val message: Int) : SongDetailEffect
}
