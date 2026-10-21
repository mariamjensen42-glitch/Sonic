package com.cycling.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetListeningLeaderboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 听歌排行榜。
 *
 * 窗口与口径合成一条选择流再 `flatMapLatest`：切换 tab 时上一轮查询会被取消，
 * 不会出现"快速连点后先回来的旧结果把新结果盖掉"。
 *
 * 数据是本地 Room 的 Flow，落库即刷新，因此没有下拉刷新这类动作。
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getListeningLeaderboard: GetListeningLeaderboardUseCase,
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<LeaderboardEffect>()
    val uiEffect: SharedFlow<LeaderboardEffect> = _uiEffect.asSharedFlow()

    /** 用户当前的窗口 + 口径选择；界面状态里的两个字段只是它的投影。 */
    private val selection = MutableStateFlow(
        LeaderboardRange.WEEK to LeaderboardMetric.DURATION
    )

    private var collectJob: Job? = null

    init {
        observeBoard()
    }

    fun handleIntent(intent: LeaderboardIntent) {
        when (intent) {
            is LeaderboardIntent.SelectRange -> {
                selection.update { (_, metric) -> intent.range to metric }
                _uiState.update { it.copy(range = intent.range) }
            }

            is LeaderboardIntent.SelectMetric -> {
                selection.update { (range, _) -> range to intent.metric }
                _uiState.update { it.copy(metric = intent.metric) }
            }

            is LeaderboardIntent.PlaySong -> playSong(intent.songId)
            LeaderboardIntent.NavigateToStats -> emit(LeaderboardEffect.NavigateToStats)
            LeaderboardIntent.NavigateBack -> emit(LeaderboardEffect.NavigateBack)
        }
    }

    private fun observeBoard() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            selection
                .flatMapLatest { (range, metric) ->
                    _uiState.update { it.copy(isLoading = true) }
                    getListeningLeaderboard(range, metric)
                }
                .catch { error ->
                    Timber.e(error, "leaderboard: collect failed")
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.emit(LeaderboardEffect.ShowToast(ERROR_MESSAGE_RES))
                }
                .collectLatest { board ->
                    _uiState.update { state ->
                        state.copy(
                            board = board,
                            range = board.range,
                            metric = board.metric,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * 点一行就播这一首。
     *
     * 榜单行只有记录当时的快照（标题/歌手/封面），真正起播要用当前曲库里的 Song：
     * 歌可能已经被重新扫描过、id 还在但文件换了，快照不该被当成播放源。
     */
    private fun playSong(songId: Long) {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            if (song == null) {
                // 歌被删掉了：榜单还留着历史快照，但已经播不了。
                _uiEffect.emit(LeaderboardEffect.ShowToast(SONG_MISSING_MESSAGE_RES))
                return@launch
            }
            playerRepository.playSong(song)
            _uiEffect.emit(LeaderboardEffect.NavigateToPlayer(songId))
        }
    }

    private fun emit(effect: LeaderboardEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val ERROR_MESSAGE_RES = com.cycling.presentation.R.string.leaderboard_error
        const val SONG_MISSING_MESSAGE_RES = com.cycling.presentation.R.string.leaderboard_song_missing
    }
}
