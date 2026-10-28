package com.cycling.presentation.download

import androidx.annotation.StringRes
import com.cycling.domain.model.DownloadTask

/** 下载管理页的界面状态。任务列表是 Room 的 Flow，播放/暂停后无需手动刷新。 */
data class DownloadUiState(
    val tasks: List<DownloadTask> = emptyList(),
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false
) {
    val hasCompleted: Boolean get() = tasks.any { it.state.isFinished }
}

sealed interface DownloadIntent {
    data class Pause(val taskId: Long) : DownloadIntent
    data class Resume(val taskId: Long) : DownloadIntent
    data class Cancel(val taskId: Long) : DownloadIntent
    data class Remove(val taskId: Long) : DownloadIntent

    data object RequestClearCompleted : DownloadIntent
    data object DismissClearCompleted : DownloadIntent
    data object ConfirmClearCompleted : DownloadIntent

    data object NavigateBack : DownloadIntent
}

sealed interface DownloadEffect {
    data object NavigateBack : DownloadEffect
    data class ShowToast(@StringRes val message: Int) : DownloadEffect
}
