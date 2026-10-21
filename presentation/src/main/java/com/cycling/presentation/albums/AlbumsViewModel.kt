package com.cycling.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
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
        .catch { error ->
            // MediaStore 查询失败时退化成空列表：界面上就是个空态，下次进页面会重新查。
            // 不兜这一下的话，isLoading 会永远停在默认的 true。
            Timber.e(error, "getAllAlbums: failed")
            emit(AlbumsUiState(isLoading = false))
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
