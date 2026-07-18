package com.cycling.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    val uiState = albumRepository.getAllAlbums()
        .map { albums ->
            AlbumsUiState(
                albums = albums,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AlbumsUiState()
        )

    private val _uiEffect = kotlinx.coroutines.channels.Channel<AlbumsEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: AlbumsIntent) {
        when (intent) {
            is AlbumsIntent.AlbumClick -> {
                Timber.d("handleIntent: AlbumClick albumId=${intent.album.id}")
                viewModelScope.launch {
                    _uiEffect.send(AlbumsEffect.NavigateToAlbumDetail(intent.album.id))
                }
            }
        }
    }
}
