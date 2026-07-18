package com.cycling.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.AiInfoType
import com.cycling.domain.repository.AiRepository
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
class AiInfoViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiInfoUiState())
    val uiState: StateFlow<AiInfoUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AiInfoEffect>()
    val uiEffect: SharedFlow<AiInfoEffect> = _uiEffect.asSharedFlow()

    private var lastRequest: AiInfoIntent? = null

    init {
        checkApiKey()
    }

    private fun checkApiKey() {
        viewModelScope.launch {
            val hasKey = aiRepository.hasApiKey()
            _uiState.update { it.copy(hasApiKey = hasKey) }
        }
    }

    fun handleIntent(intent: AiInfoIntent) {
        Timber.d("handleIntent: $intent")
        lastRequest = intent
        when (intent) {
            is AiInfoIntent.LoadSongInfo -> loadSongInfo(intent.songTitle, intent.artist)
            is AiInfoIntent.LoadArtistInfo -> loadArtistInfo(intent.artistName)
            is AiInfoIntent.LoadAlbumInfo -> loadAlbumInfo(intent.albumTitle, intent.artist)
            is AiInfoIntent.Dismiss -> dismiss()
            is AiInfoIntent.Retry -> retry()
        }
    }

    private fun loadSongInfo(songTitle: String, artist: String) {
        Timber.d("loadSongInfo: songTitle=$songTitle, artist=$artist")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            aiRepository.getSongInfo(songTitle, artist)
                .onSuccess { info ->
                    Timber.d("loadSongInfo: success")
                    _uiState.update { it.copy(isLoading = false, info = info, error = null) }
                }
                .onFailure { exception ->
                    Timber.e(exception, "loadSongInfo: failed")
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    private fun loadArtistInfo(artistName: String) {
        Timber.d("loadArtistInfo: artistName=$artistName")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            aiRepository.getArtistInfo(artistName)
                .onSuccess { info ->
                    Timber.d("loadArtistInfo: success")
                    _uiState.update { it.copy(isLoading = false, info = info, error = null) }
                }
                .onFailure { exception ->
                    Timber.e(exception, "loadArtistInfo: failed")
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    private fun loadAlbumInfo(albumTitle: String, artist: String) {
        Timber.d("loadAlbumInfo: albumTitle=$albumTitle, artist=$artist")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            aiRepository.getAlbumInfo(albumTitle, artist)
                .onSuccess { info ->
                    Timber.d("loadAlbumInfo: success")
                    _uiState.update { it.copy(isLoading = false, info = info, error = null) }
                }
                .onFailure { exception ->
                    Timber.e(exception, "loadAlbumInfo: failed")
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    private fun dismiss() {
        _uiState.update { AiInfoUiState(hasApiKey = _uiState.value.hasApiKey) }
    }

    private fun retry() {
        lastRequest?.let { handleIntent(it) }
    }
}
