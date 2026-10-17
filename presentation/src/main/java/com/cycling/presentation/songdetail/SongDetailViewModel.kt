package com.cycling.presentation.songdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.Song
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetLyricsUseCase
import com.cycling.domain.usecase.SaveLyricsUseCase
import com.cycling.presentation.R
import com.cycling.presentation.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel(assistedFactory = SongDetailViewModel.Factory::class)
class SongDetailViewModel @AssistedInject constructor(
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioMetadataRepository: AudioMetadataRepository,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val saveLyricsUseCase: SaveLyricsUseCase,
    @Assisted private val route: Screen.SongDetail
) : ViewModel() {

    private val songId: Long = route.songId

    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SongDetailEffect>()
    val uiEffect: SharedFlow<SongDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadSong()
        loadPlaylists()
    }

    fun handleIntent(intent: SongDetailIntent) {
        when (intent) {
            is SongDetailIntent.LoadSong -> loadSong()
            is SongDetailIntent.ToggleFavorite -> toggleFavorite()
            is SongDetailIntent.PlaySong -> playSong()
            is SongDetailIntent.AddToPlaylist -> addToPlaylist()
            is SongDetailIntent.DismissAddToPlaylistDialog -> dismissAddToPlaylistDialog()
            is SongDetailIntent.AddToPlaylistTo -> addToPlaylist(intent.playlistId)
            is SongDetailIntent.CreatePlaylistAndAddSong -> createPlaylistAndAddSong(intent.name)
            is SongDetailIntent.CopyPath -> copyPath(intent.path)
            is SongDetailIntent.EnterEditMode -> enterEditMode()
            is SongDetailIntent.ExitEditMode -> exitEditMode()
            is SongDetailIntent.UpdateTitle -> updateTitle(intent.title)
            is SongDetailIntent.UpdateArtist -> updateArtist(intent.artist)
            is SongDetailIntent.UpdateAlbum -> updateAlbum(intent.album)
            is SongDetailIntent.UpdateYear -> updateYear(intent.year)
            is SongDetailIntent.UpdateGenre -> updateGenre(intent.genre)
            is SongDetailIntent.UpdateLyrics -> updateLyrics(intent.lyrics)
            is SongDetailIntent.SaveChanges -> saveChanges()
            is SongDetailIntent.DiscardChanges -> discardChanges()
            is SongDetailIntent.KeepEditing -> keepEditing()
        }
    }

    private fun loadSong() {
        Timber.d("loadSong: starting songId=$songId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val song = songRepository.getSongById(songId)
            if (song != null) {
                Timber.d("loadSong: loaded song ${song.title}")
                val audioMetadata = audioMetadataRepository.getAudioMetadata(song.path)
                _uiState.update { 
                    it.copy(
                        song = song, 
                        isLoading = false, 
                        isFavorite = song.isFavorite,
                        audioMetadata = audioMetadata,
                        editedTitle = song.title,
                        editedArtist = song.artist,
                        editedAlbum = song.album,
                        editedYear = audioMetadata?.year ?: "",
                        editedGenre = audioMetadata?.genre ?: ""
                    ) 
                }
                // Separate coroutine: a cache miss goes to the network, and the page should be
                // readable while that happens rather than waiting on it.
                loadLyrics(song)
            } else {
                Timber.d("loadSong: song not found")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Resolves the song's lyrics for the page, and seeds the editor with the same text.
     *
     * The editor gets the raw content, not the display lines: saving it back has to preserve LRC
     * timings, and stripping them for the sake of a prettier text field would quietly turn a timed
     * lyric into one the player can no longer follow.
     */
    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLyrics = true) }

            val lyrics = try {
                getLyricsUseCase(song)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "loadLyrics: failed for songId=${song.id}")
                _uiState.update { it.copy(isLoadingLyrics = false) }
                return@launch
            }

            _uiState.update { state ->
                val original = lyrics.rawContent.orEmpty()
                // An answer that arrives while the user is typing must not overwrite the draft;
                // only the comparison baseline moves.
                val draft = if (state.isEditMode && state.editedLyrics != state.originalLyrics) {
                    state.editedLyrics
                } else {
                    original
                }
                val next = state.copy(
                    lyrics = displayLines(lyrics),
                    lyricsSource = lyrics.source,
                    isLoadingLyrics = false,
                    originalLyrics = original,
                    editedLyrics = draft
                )
                next.copy(hasChanges = checkForChanges(next))
            }
        }
    }

    private fun updateLyrics(lyrics: String) {
        _uiState.update { state ->
            state.copy(
                editedLyrics = lyrics,
                hasChanges = checkForChanges(state.copy(editedLyrics = lyrics))
            )
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val currentSong = _uiState.value.song ?: return@launch
            val newFavoriteState = songRepository.toggleFavorite(currentSong.id)
            _uiState.update { it.copy(isFavorite = newFavoriteState) }
            val message = if (newFavoriteState) R.string.player_added_to_favorites else R.string.player_removed_from_favorites
            _uiEffect.emit(SongDetailEffect.ShowCopiedMessage(message))
        }
    }

    private fun playSong() {
        viewModelScope.launch {
            val currentSong = _uiState.value.song ?: return@launch
            playerRepository.playSong(currentSong, listOf(currentSong))
            _uiEffect.emit(SongDetailEffect.NavigateToPlayer(currentSong.id))
        }
    }

    private fun addToPlaylist() {
        _uiState.update { it.copy(showAddToPlaylistDialog = true) }
    }

    private fun dismissAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = false) }
    }

    private fun addToPlaylist(playlistId: Long) {
        val songId = _uiState.value.song?.id ?: return
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, songId)
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.playlist_added_to))
            dismissAddToPlaylistDialog()
        }
    }

    private fun createPlaylistAndAddSong(name: String) {
        val songId = _uiState.value.song?.id ?: return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongToPlaylist(playlistId, songId)
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.playlist_created_and_added))
            dismissAddToPlaylistDialog()
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun copyPath(path: String) {
        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowCopiedMessage(R.string.song_detail_vm_path_copied))
        }
    }

    private fun enterEditMode() {
        val song = _uiState.value.song ?: return
        val metadata = _uiState.value.audioMetadata
        _uiState.update { 
            it.copy(
                isEditMode = true,
                editedTitle = song.title,
                editedArtist = song.artist,
                editedAlbum = song.album,
                editedYear = metadata?.year ?: "",
                editedGenre = metadata?.genre ?: "",
                editedLyrics = it.originalLyrics,
                hasChanges = false
            )
        }
    }

    private fun exitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _uiState.update { it.copy(isEditMode = false) }
        }
    }

    private fun updateTitle(title: String) {
        val maxLength = 200
        val trimmedTitle = title.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedTitle = trimmedTitle,
                hasChanges = checkForChanges(state.copy(editedTitle = trimmedTitle))
            )
        }
    }

    private fun updateArtist(artist: String) {
        val maxLength = 200
        val trimmedArtist = artist.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedArtist = trimmedArtist,
                hasChanges = checkForChanges(state.copy(editedArtist = trimmedArtist))
            )
        }
    }

    private fun updateAlbum(album: String) {
        val maxLength = 200
        val trimmedAlbum = album.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedAlbum = trimmedAlbum,
                hasChanges = checkForChanges(state.copy(editedAlbum = trimmedAlbum))
            )
        }
    }

    private fun updateYear(year: String) {
        val filteredYear = year.filter { it.isDigit() }.take(4)
        _uiState.update { state ->
            state.copy(
                editedYear = filteredYear,
                hasChanges = checkForChanges(state.copy(editedYear = filteredYear))
            )
        }
    }

    private fun updateGenre(genre: String) {
        val maxLength = 100
        val trimmedGenre = genre.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedGenre = trimmedGenre,
                hasChanges = checkForChanges(state.copy(editedGenre = trimmedGenre))
            )
        }
    }

    private fun checkForChanges(state: SongDetailUiState): Boolean {
        val song = state.song ?: return false
        val metadata = state.audioMetadata

        return state.editedTitle != song.title ||
                state.editedArtist != song.artist ||
                state.editedAlbum != song.album ||
                state.editedYear != (metadata?.year ?: "") ||
                state.editedGenre != (metadata?.genre ?: "") ||
                state.editedLyrics != state.originalLyrics
    }

    /**
     * Turns resolved [lyrics] into what the page draws.
     *
     * Timed lyrics go through the parser's own lines, so karaoke documents lose their per-syllable
     * timing but keep their text and translation. Anything the parser could not read — plain text,
     * or a format it does not know — falls back to the raw text with its tags stripped, which is
     * the whole point of the detail page: it can show a lyric the player cannot follow.
     */
    private fun displayLines(lyrics: Lyrics): List<LyricsDisplayLine> {
        val synced = lyrics.syncedLyrics
        if (synced != null && synced.lines.isNotEmpty()) {
            return synced.lines.mapNotNull { line ->
                when (line) {
                    is SyncedLine -> LyricsDisplayLine(line.content, line.translation)
                    is KaraokeLine -> LyricsDisplayLine(
                        text = line.syllables.joinToString("") { it.content }.trim(),
                        translation = line.translation
                    )
                    else -> null
                }
            }.filter { it.text.isNotBlank() }
        }
        return plainLines(lyrics.rawContent)
    }

    /** Raw text as display lines: blank lines, `[ti:]`-style tags and timestamps removed. */
    private fun plainLines(rawContent: String?): List<LyricsDisplayLine> {
        if (rawContent.isNullOrBlank()) return emptyList()
        return rawContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { MetadataTag.matches(it) }
            .map { TimestampTag.replace(it, "").trim() }
            .filter { it.isNotEmpty() }
            .map { LyricsDisplayLine(it) }
            .toList()
    }

    private fun saveChanges() {
        val song = _uiState.value.song ?: return
        val snapshot = _uiState.value
        val metadata = snapshot.audioMetadata

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val titleChanged = snapshot.editedTitle != song.title
            val artistChanged = snapshot.editedArtist != song.artist
            val albumChanged = snapshot.editedAlbum != song.album
            val yearChanged = snapshot.editedYear != (metadata?.year ?: "")
            val genreChanged = snapshot.editedGenre != (metadata?.genre ?: "")
            val lyricsChanged = snapshot.editedLyrics != snapshot.originalLyrics

            if (titleChanged || artistChanged || albumChanged) {
                val updateSuccess = songRepository.updateSongInfo(
                    songId = song.id,
                    title = if (titleChanged) snapshot.editedTitle else null,
                    artist = if (artistChanged) snapshot.editedArtist else null,
                    album = if (albumChanged) snapshot.editedAlbum else null
                )
                if (!updateSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_update_failed))
                    return@launch
                }
            }

            if (yearChanged || genreChanged || titleChanged || artistChanged || albumChanged) {
                val tagUpdate = TagUpdate(
                    title = if (titleChanged) snapshot.editedTitle else null,
                    artist = if (artistChanged) snapshot.editedArtist else null,
                    album = if (albumChanged) snapshot.editedAlbum else null,
                    year = if (yearChanged) snapshot.editedYear.ifEmpty { null } else null,
                    genre = if (genreChanged) snapshot.editedGenre.ifEmpty { null } else null
                )

                val result = audioMetadataRepository.updateAudioTags(song.path, tagUpdate)
                result.exceptionOrNull()?.let { error ->
                    Timber.e(error, "Failed to update audio tags")
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_save_failed))
                    return@launch
                }
            }

            // Lyrics are stored in the database, not written into the file: the app holds a read
            // grant on the media library, and an edit that silently failed on a modern device
            // would be worse than one that is honestly kept on this side.
            var savedLyrics: Lyrics? = null
            if (lyricsChanged) {
                try {
                    savedLyrics = saveLyricsUseCase(song, snapshot.editedLyrics)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save lyrics for songId=${song.id}")
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_lyrics_save_failed))
                    return@launch
                }
            }

            _uiState.update { state ->
                val updatedSong = if (titleChanged || artistChanged || albumChanged) {
                    song.copy(
                        title = snapshot.editedTitle,
                        artist = snapshot.editedArtist,
                        album = snapshot.editedAlbum
                    )
                } else {
                    state.song
                }
                val updatedMetadata = if (yearChanged || genreChanged) {
                    metadata?.copy(
                        year = snapshot.editedYear.ifEmpty { null },
                        genre = snapshot.editedGenre.ifEmpty { null }
                    )
                } else {
                    state.audioMetadata
                }
                val saved = savedLyrics
                val base = state.copy(
                    isSaving = false,
                    hasChanges = false,
                    isEditMode = false,
                    song = updatedSong,
                    audioMetadata = updatedMetadata
                )
                if (saved == null) {
                    base
                } else {
                    // The save may have resolved to something other than what was typed — a
                    // cleared editor falls back to the automatic sources — so both the baseline
                    // and the draft are taken from the result, not from the input.
                    val raw = saved.rawContent.orEmpty()
                    base.copy(
                        lyrics = displayLines(saved),
                        lyricsSource = saved.source,
                        originalLyrics = raw,
                        editedLyrics = raw
                    )
                }
            }
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_vm_save_success))
        }
    }

    private fun discardChanges() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showDiscardDialog = false,
                    hasChanges = false,
                    isEditMode = false,
                    editedLyrics = it.originalLyrics
                )
            }
        }
    }

    private fun keepEditing() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    private companion object {
        /** `[00:12.34]` / `[1:02.5]` at the start of a line, stripped for display only. */
        val TimestampTag = Regex("\\[\\d{1,2}:\\d{1,2}(?:[.:]\\d{1,3})?]")

        /** `[ti:…]` / `[ar:…]` / `[offset:…]` — LRC header lines, never lyric text. */
        val MetadataTag = Regex("^\\[[A-Za-z#]+:[^]]*]$")
    }

    @AssistedFactory
    interface Factory {
        fun create(route: Screen.SongDetail): SongDetailViewModel
    }
}
