package com.cycling.presentation.mymusic

import com.cycling.domain.model.Song

enum class MyMusicTab {
    FAVORITES,
    RECENTLY_PLAYED,
    MOST_PLAYED
}

data class MyMusicUiState(
    val currentTab: MyMusicTab = MyMusicTab.FAVORITES,
    val favoriteSongs: List<Song> = emptyList(),
    val recentlyPlayedSongs: List<Song> = emptyList(),
    val mostPlayedSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface MyMusicIntent {
    data class SelectTab(val tab: MyMusicTab) : MyMusicIntent
    data class SongClick(val song: Song) : MyMusicIntent
    data object PlayAll : MyMusicIntent
    data object ShuffleAll : MyMusicIntent
    data class ToggleFavorite(val song: Song) : MyMusicIntent
}

sealed interface MyMusicEffect {
    data class NavigateToPlayer(val songId: Long) : MyMusicEffect
    data class ShowToast(val message: String) : MyMusicEffect
}
