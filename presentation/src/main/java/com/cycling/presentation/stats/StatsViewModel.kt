package com.cycling.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.ClearListeningStatsUseCase
import com.cycling.domain.usecase.GetListeningStatsUseCase
import com.cycling.domain.usecase.GetSongsOfDayUseCase
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
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

/**
 * 听歌统计。
 *
 * 统计本身是 Room 的 Flow，播放事件一落库就会重新发射，因此这里没有"刷新"动作；
 * 界面只需要跟着状态走。点开某一天时另开一个短期收集（[dayCollectJob]），
 * 关掉弹层就取消 —— 免得离开页面前还挂着一个没人看的查询。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getListeningStats: GetListeningStatsUseCase,
    private val getSongsOfDay: GetSongsOfDayUseCase,
    private val clearListeningStats: ClearListeningStatsUseCase,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<StatsEffect>()
    val uiEffect: SharedFlow<StatsEffect> = _uiEffect.asSharedFlow()

    private var dayCollectJob: Job? = null

    init {
        observeStats()
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.SelectDay -> selectDay(intent.date)
            StatsIntent.DismissDay -> dismissDay()
            is StatsIntent.PlaySong -> playSong(intent.songId)
            StatsIntent.RequestClearStats -> _uiState.update { it.copy(showClearConfirm = true) }
            StatsIntent.DismissClearStats -> _uiState.update { it.copy(showClearConfirm = false) }
            StatsIntent.ConfirmClearStats -> clearStats()
            StatsIntent.NavigateToLeaderboard -> emit(StatsEffect.NavigateToLeaderboard)
            StatsIntent.NavigateBack -> emit(StatsEffect.NavigateBack)
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            getListeningStats()
                .catch { error ->
                    Timber.e(error, "stats: collect failed")
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(StatsEffect.ShowToast(STATS_ERROR_RES))
                }
                .collectLatest { stats ->
                    _uiState.update { it.copy(stats = stats, isLoading = false) }
                }
        }
    }

    private fun selectDay(date: LocalDate) {
        dayCollectJob?.cancel()
        _uiState.update {
            it.copy(selectedDay = date, daySongs = emptyList(), isLoadingDay = true)
        }
        dayCollectJob = viewModelScope.launch {
            getSongsOfDay(date, DAY_DETAIL_LIMIT)
                .catch { error ->
                    Timber.e(error, "stats: day detail failed for %s", date)
                    _uiState.update { it.copy(isLoadingDay = false) }
                }
                .collectLatest { songs ->
                    _uiState.update { state ->
                        // 收集期间用户可能已经改选了别的日期，迟到的一批要丢掉。
                        if (state.selectedDay != date) {
                            state
                        } else {
                            state.copy(daySongs = songs, isLoadingDay = false)
                        }
                    }
                }
        }
    }

    private fun dismissDay() {
        dayCollectJob?.cancel()
        dayCollectJob = null
        _uiState.update { it.copy(selectedDay = null, daySongs = emptyList(), isLoadingDay = false) }
    }

    private fun clearStats() {
        viewModelScope.launch {
            clearListeningStats()
            _uiState.update { it.copy(showClearConfirm = false) }
            _uiEffect.emit(StatsEffect.ShowToast(R.string.stats_cleared))
        }
    }

    private fun playSong(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            if (song == null) {
                _uiEffect.emit(StatsEffect.ShowToast(R.string.stats_song_missing))
                return@launch
            }
            playerRepository.playSong(song)
            _uiEffect.emit(StatsEffect.NavigateToPlayer(songId))
        }
    }

    private fun emit(effect: StatsEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        /** 当天详情最多列多少首。一天听几百首是极端情况，这里只做展示。 */
        const val DAY_DETAIL_LIMIT = 50

        const val STATS_ERROR_RES = R.string.stats_error
    }
}
