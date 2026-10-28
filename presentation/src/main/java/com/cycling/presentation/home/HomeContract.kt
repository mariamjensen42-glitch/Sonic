package com.cycling.presentation.home

import androidx.annotation.StringRes
import com.cycling.domain.model.Song

/**
 * 首页 = 全部歌曲。
 *
 * 原先的「推荐专辑 / 推荐歌手 / 最近添加」三个分区连同分页跳转一起删掉了，
 * 状态里只剩一份全库歌曲列表。
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    /** 当前播放曲目，用来把那一行染成品牌红。 */
    val currentSongId: Long? = null
)

sealed interface HomeIntent {
    data object LoadData : HomeIntent
    data class SongClick(val song: Song) : HomeIntent
    data class PlayNext(val song: Song) : HomeIntent
    data class AddToQueue(val song: Song) : HomeIntent
    data class ToggleFavorite(val song: Song) : HomeIntent
    data object NavigateToSettings : HomeIntent
}

sealed interface HomeEffect {
    data class ShowToast(@StringRes val message: Int) : HomeEffect
    data class NavigateToPlayer(val songId: Long) : HomeEffect
    data object NavigateToSettings : HomeEffect
}
