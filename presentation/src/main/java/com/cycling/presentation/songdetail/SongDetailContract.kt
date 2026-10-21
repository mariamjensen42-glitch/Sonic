package com.cycling.presentation.songdetail

import androidx.annotation.StringRes
import com.cycling.domain.lyrics.model.Attributes
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsProvider
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

/**
 * What the "歌词信息" dialog shows about the current lyrics.
 *
 * Everything except [source] and [provider] is derived from the text the page currently holds —
 * the draft in edit mode — so the dialog describes the lyrics the user is looking at (or editing)
 * rather than whatever was resolved when the page opened.
 */
data class LyricsInfo(
    val source: LyricsSource = LyricsSource.NONE,
    val provider: LyricsProvider? = null,
    /** Guessed document format, e.g. `LRC` / `YRC` / `TTML`; null when nothing matched. */
    val format: String? = null,
    val isWordByWord: Boolean = false,
    /** Whether the text carries timings the player can follow at all. */
    val hasTimings: Boolean = false,
    val lineCount: Int = 0,
    val hasTranslation: Boolean = false,
    /** The LRC header tags of the draft, so the editable dialog can round-trip them. */
    val metadata: Attributes = Attributes()
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
    val editedAlbumArtist: String = "",
    val editedYear: String = "",
    val editedGenre: String = "",
    val editedTrackNumber: String = "",
    val editedDiscNumber: String = "",
    val editedComposer: String = "",
    val editedPublisher: String = "",
    val editedCopyright: String = "",
    val editedComment: String = "",
    val hasChanges: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val showAddToPlaylistDialog: Boolean = false,
    /** Lines to display, already stripped of timestamps and metadata tags. */
    val lyrics: List<LyricsDisplayLine> = emptyList(),
    val lyricsSource: LyricsSource = LyricsSource.NONE,
    /** Which online source supplied the current copy; null for local and user-written lyrics. */
    val lyricsProvider: LyricsProvider? = null,
    val isLoadingLyrics: Boolean = false,
    /** Text in the editor, verbatim: it is saved back as-is so timings survive a round trip. */
    val editedLyrics: String = "",
    /** What the editor started from, so "did the user touch the lyrics" is one comparison. */
    val originalLyrics: String = "",
    /** Online versions offered by the picker; empty until it has been opened once. */
    val lyricsCandidates: List<LyricsCandidate> = emptyList(),
    val showLyricsCandidates: Boolean = false,
    val isLoadingLyricsCandidates: Boolean = false,
    val showLyricsInfoDialog: Boolean = false,
    val lyricsInfo: LyricsInfo = LyricsInfo(),
    /** Online cover search state; the sheet is the only consumer. */
    val showCoverPicker: Boolean = false,
    val isApplyingCover: Boolean = false,
    /** Online metadata fill state; reuses the same catalogue search results as the cover picker. */
    val showMetadataCandidates: Boolean = false,
    /** Catalogue matches from the last search — cover and metadata sheets both read this. */
    val onlineCandidates: List<CoverCandidate> = emptyList(),
    val isLoadingOnlineCandidates: Boolean = false
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
    data class UpdateAlbumArtist(val albumArtist: String) : SongDetailIntent
    data class UpdateYear(val year: String) : SongDetailIntent
    data class UpdateGenre(val genre: String) : SongDetailIntent
    data class UpdateTrackNumber(val trackNumber: String) : SongDetailIntent
    data class UpdateDiscNumber(val discNumber: String) : SongDetailIntent
    data class UpdateComposer(val composer: String) : SongDetailIntent
    data class UpdatePublisher(val publisher: String) : SongDetailIntent
    data class UpdateCopyright(val copyright: String) : SongDetailIntent
    data class UpdateComment(val comment: String) : SongDetailIntent
    data class UpdateLyrics(val lyrics: String) : SongDetailIntent
    data object SaveChanges : SongDetailIntent
    data object DiscardChanges : SongDetailIntent
    data object KeepEditing : SongDetailIntent
    data object OpenLyricsCandidates : SongDetailIntent
    data object DismissLyricsCandidates : SongDetailIntent
    data class SelectLyricsCandidate(val candidateId: String) : SongDetailIntent
    data object OpenLyricsInfo : SongDetailIntent
    data object DismissLyricsInfo : SongDetailIntent
    /** Writes the edited header tags back into the lyrics text; saving the page persists them. */
    data class ApplyLyricsMetadata(val metadata: Attributes) : SongDetailIntent
    /** Copies `[ti:]` / `[ar:]` / `[al:]` into the editable song fields. */
    data class FillSongInfoFromLyricsMetadata(val metadata: Attributes) : SongDetailIntent
    data object OpenCoverPicker : SongDetailIntent
    data object DismissCoverPicker : SongDetailIntent
    data object RetryOnlineSearch : SongDetailIntent
    data class SelectCoverCandidate(val candidateId: String) : SongDetailIntent
    data class ApplyLocalCover(val imageUri: String) : SongDetailIntent
    data object ClearCustomCover : SongDetailIntent
    data object OpenMetadataCandidates : SongDetailIntent
    data object DismissMetadataCandidates : SongDetailIntent
    data class SelectMetadataCandidate(val candidateId: String) : SongDetailIntent
}

sealed interface SongDetailEffect {
    data class NavigateToPlayer(val songId: Long) : SongDetailEffect
    data class ShowCopiedMessage(@StringRes val message: Int) : SongDetailEffect
    data class ShowError(@StringRes val message: Int) : SongDetailEffect
    data class ShowToast(@StringRes val message: Int) : SongDetailEffect
}
