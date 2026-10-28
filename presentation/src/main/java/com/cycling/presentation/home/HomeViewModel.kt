package com.cycling.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.usecase.SongQueryUseCase
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 首页只做一件事：把本地库的全部歌曲摊开。
 *
 * 曾经的三个数据源（歌曲 / 专辑 / 歌手）与「谁先到谁先画」的编排都随之删除 ——
 * 页面上只剩一份列表，不需要等任何一方。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songQueryUseCase: SongQueryUseCase,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeEffect>()
    val uiEffect: SharedFlow<HomeEffect> = _uiEffect.asSharedFlow()

    private var loadJob: Job? = null

    init {
        loadSongs()
        observePlayerState()
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadSongs()

            is HomeIntent.SongClick -> {
                Timber.d("handleIntent: SongClick songId=%d", intent.song.id)
                play(intent.song)
            }

            is HomeIntent.PlayNext -> playerRepository.playNext(intent.song)

            is HomeIntent.AddToQueue -> playerRepository.addToQueue(intent.song)

            is HomeIntent.ToggleFavorite -> playerRepository.setSongFavorite(
                songId = intent.song.id,
                isFavorite = !intent.song.isFavorite
            )

            is HomeIntent.NavigateToSettings -> emitEffect(HomeEffect.NavigateToSettings)
        }
    }

    private fun loadSongs() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }

        loadJob = viewModelScope.launch {
            songQueryUseCase.all()
                .catch { error ->
                    Timber.e(error, "loadSongs failed")
                    // 失败也要退出加载态，否则页面会一直空着，看不出是没歌还是挂了。
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(HomeEffect.ShowToast(R.string.home_vm_load_failed))
                }
                .collectLatest { songs ->
                    _uiState.update { it.copy(isLoading = false, songs = songs) }
                }
        }
    }

    /** 播放态只取当前曲目 id，用来把列表里那一行染红。 */
    private fun observePlayerState() {
        viewModelScope.launch {
            playerRepository.playerState.collect { state ->
                _uiState.update { it.copy(currentSongId = state.currentSong?.id) }
            }
        }
    }

    private fun play(song: Song) {
        viewModelScope.launch {
            playerRepository.playSong(song)
            _uiEffect.emit(HomeEffect.NavigateToPlayer(song.id))
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
