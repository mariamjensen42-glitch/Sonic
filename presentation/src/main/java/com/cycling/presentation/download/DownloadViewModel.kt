package com.cycling.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.DownloadState
import com.cycling.domain.repository.DownloadRepository
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 下载管理。
 *
 * 这里没有轮询：进度由 [DownloadRepository] 写进 Room，Flow 一变界面就跟着变。
 * 所有动作都是「改状态」，失败原因由任务行自己显示（[com.cycling.domain.model.DownloadTask.errorMessage]），
 * 不在这里攒一份错误列表。
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DownloadEffect>()
    val uiEffect: SharedFlow<DownloadEffect> = _uiEffect.asSharedFlow()

    init {
        observeTasks()
    }

    fun handleIntent(intent: DownloadIntent) {
        when (intent) {
            is DownloadIntent.Pause -> act { downloadRepository.pause(intent.taskId) }
            is DownloadIntent.Resume -> act { downloadRepository.resume(intent.taskId) }
            is DownloadIntent.Cancel -> act {
                downloadRepository.cancel(intent.taskId)
                _uiEffect.emit(DownloadEffect.ShowToast(R.string.download_cancelled))
            }

            is DownloadIntent.Remove -> act { downloadRepository.remove(intent.taskId) }

            DownloadIntent.RequestClearCompleted -> _uiState.update { it.copy(showClearConfirm = true) }
            DownloadIntent.DismissClearCompleted -> _uiState.update { it.copy(showClearConfirm = false) }
            DownloadIntent.ConfirmClearCompleted -> {
                _uiState.update { it.copy(showClearConfirm = false) }
                act { downloadRepository.clearCompleted() }
            }

            DownloadIntent.NavigateBack -> viewModelScope.launch {
                _uiEffect.emit(DownloadEffect.NavigateBack)
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            downloadRepository.observeTasks().collect { tasks ->
                _uiState.update {
                    it.copy(
                        tasks = tasks.sortedWith(
                            compareBy<com.cycling.domain.model.DownloadTask> { task ->
                                when (task.state) {
                                    DownloadState.RUNNING -> 0
                                    DownloadState.QUEUED -> 1
                                    DownloadState.PAUSED -> 2
                                    DownloadState.FAILED -> 3
                                    DownloadState.COMPLETED -> 4
                                }
                            }.thenByDescending { task -> task.createdAt }
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure {
                Timber.w(it, "Download: 操作失败")
                _uiEffect.emit(DownloadEffect.ShowToast(R.string.download_action_failed))
            }
        }
    }
}
