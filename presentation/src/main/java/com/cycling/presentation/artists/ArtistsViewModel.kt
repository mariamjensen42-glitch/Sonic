package com.cycling.presentation.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.ArtistRepository
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
class ArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    val uiState = artistRepository.getAllArtists()
        .map { artists ->
            ArtistsUiState(
                artists = artists,
                isLoading = false
            )
        }
        .catch { error ->
            // MediaStore 查询失败时退化成空列表：界面上就是个空态，下次进页面会重新查。
            // 不兜这一下的话，isLoading 会永远停在默认的 true。
            Timber.e(error, "getAllArtists: failed")
            emit(ArtistsUiState(isLoading = false))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ArtistsUiState()
        )

    private val _uiEffect = kotlinx.coroutines.channels.Channel<ArtistsEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: ArtistsIntent) {
        when (intent) {
            is ArtistsIntent.ArtistClick -> {
                Timber.d("handleIntent: ArtistClick artistId=${intent.artist.id}")
                viewModelScope.launch {
                    _uiEffect.send(ArtistsEffect.NavigateToArtistDetail(intent.artist.id))
                }
            }
        }
    }
}
