package com.cycling.presentation.songdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
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
import javax.inject.Inject

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    private val audioMetadataRepository: AudioMetadataRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: -1L

    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SongDetailEffect>()
    val uiEffect: SharedFlow<SongDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadSong()
    }

    fun handleIntent(intent: SongDetailIntent) {
        when (intent) {
            is SongDetailIntent.LoadSong -> loadSong()
            is SongDetailIntent.ToggleFavorite -> toggleFavorite()
            is SongDetailIntent.PlaySong -> playSong()
            is SongDetailIntent.AddToPlaylist -> addToPlaylist()
            is SongDetailIntent.CopyPath -> copyPath(intent.path)
            is SongDetailIntent.EnterEditMode -> enterEditMode()
            is SongDetailIntent.ExitEditMode -> exitEditMode()
            is SongDetailIntent.UpdateTitle -> updateTitle(intent.title)
            is SongDetailIntent.UpdateArtist -> updateArtist(intent.artist)
            is SongDetailIntent.UpdateAlbum -> updateAlbum(intent.album)
            is SongDetailIntent.UpdateYear -> updateYear(intent.year)
            is SongDetailIntent.UpdateGenre -> updateGenre(intent.genre)
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
            } else {
                Timber.d("loadSong: song not found")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val currentSong = _uiState.value.song ?: return@launch
            val newFavoriteState = songRepository.toggleFavorite(currentSong.id)
            _uiState.update { it.copy(isFavorite = newFavoriteState) }
            val message = if (newFavoriteState) "已添加到喜欢" else "已从喜欢移除"
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
        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowAddToPlaylistDialog)
        }
    }

    private fun copyPath(path: String) {
        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowCopiedMessage("路径已复制到剪贴板"))
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
                state.editedGenre != (metadata?.genre ?: "")
    }

    private fun saveChanges() {
        val song = _uiState.value.song ?: return
        val metadata = _uiState.value.audioMetadata

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val titleChanged = _uiState.value.editedTitle != song.title
            val artistChanged = _uiState.value.editedArtist != song.artist
            val albumChanged = _uiState.value.editedAlbum != song.album

            if (titleChanged || artistChanged || albumChanged) {
                val updateSuccess = songRepository.updateSongInfo(
                    songId = song.id,
                    title = if (titleChanged) _uiState.value.editedTitle else null,
                    artist = if (artistChanged) _uiState.value.editedArtist else null,
                    album = if (albumChanged) _uiState.value.editedAlbum else null
                )
                if (!updateSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError("更新歌曲信息失败"))
                    return@launch
                }
            }

            val yearChanged = _uiState.value.editedYear != (metadata?.year ?: "")
            val genreChanged = _uiState.value.editedGenre != (metadata?.genre ?: "")

            if (yearChanged || genreChanged || titleChanged || artistChanged || albumChanged) {
                val tagUpdate = TagUpdate(
                    title = if (titleChanged) _uiState.value.editedTitle else null,
                    artist = if (artistChanged) _uiState.value.editedArtist else null,
                    album = if (albumChanged) _uiState.value.editedAlbum else null,
                    year = if (yearChanged) _uiState.value.editedYear.ifEmpty { null } else null,
                    genre = if (genreChanged) _uiState.value.editedGenre.ifEmpty { null } else null
                )

                val result = audioMetadataRepository.updateAudioTags(song.path, tagUpdate)
                result.fold(
                    onSuccess = {
                        val updatedSong = song.copy(
                            title = _uiState.value.editedTitle,
                            artist = _uiState.value.editedArtist,
                            album = _uiState.value.editedAlbum
                        )
                        val updatedMetadata = metadata?.copy(
                            year = _uiState.value.editedYear.ifEmpty { null },
                            genre = _uiState.value.editedGenre.ifEmpty { null }
                        )
                        _uiState.update { 
                            it.copy(
                                isSaving = false, 
                                hasChanges = false, 
                                isEditMode = false,
                                song = updatedSong,
                                audioMetadata = updatedMetadata
                            ) 
                        }
                        _uiEffect.emit(SongDetailEffect.ShowToast("保存成功"))
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to update audio tags")
                        _uiState.update { it.copy(isSaving = false) }
                        _uiEffect.emit(SongDetailEffect.ShowError("保存标签失败"))
                    }
                )
            } else {
                _uiState.update { it.copy(isSaving = false, hasChanges = false, isEditMode = false) }
            }
        }
    }

    private fun discardChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDiscardDialog = false, hasChanges = false, isEditMode = false) }
        }
    }

    private fun keepEditing() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }
}
