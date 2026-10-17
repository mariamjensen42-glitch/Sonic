package com.cycling.presentation.songs

import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song
import com.cycling.domain.model.SortOrder
import androidx.annotation.StringRes
import com.cycling.domain.model.ViewMode

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val sortAscending: Boolean = true,
    val currentSongId: Long? = null,
    val songToAdd: Song? = null,
    val showAddToPlaylistDialog: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val isScanning: Boolean = false,
    /** 多选模式。开启后单击是勾选，不再是播放。 */
    val selectionMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    /** 多选状态下「加入歌单」的弹窗可见性。 */
    val showBatchAddToPlaylistDialog: Boolean = false
)

sealed interface SongsIntent {
    data object LoadSongs : SongsIntent
    data object ScanLibrary : SongsIntent
    data object ToggleViewMode : SongsIntent
    data class ChangeSortOrder(val sortOrder: SortOrder) : SongsIntent
    data class SongClick(val song: Song) : SongsIntent
    data class ShowAddToPlaylistDialog(val song: Song) : SongsIntent
    data class AddToPlaylist(val playlistId: Long) : SongsIntent
    data object DismissAddToPlaylistDialog : SongsIntent
    data class CreatePlaylistAndAddSong(val name: String) : SongsIntent

    // ── 多选 ──
    data object EnterSelectionMode : SongsIntent
    data object ExitSelectionMode : SongsIntent
    data class ToggleSelection(val songId: Long) : SongsIntent
    data object SelectAll : SongsIntent
    data object ClearSelection : SongsIntent
    data object ShowBatchAddToPlaylistDialog : SongsIntent
    data object DismissBatchAddToPlaylistDialog : SongsIntent
    data class BatchAddToPlaylist(val playlistId: Long) : SongsIntent
    data class CreatePlaylistAndAddSelected(val name: String) : SongsIntent
    data class BatchSetFavorite(val isFavorite: Boolean) : SongsIntent
}

sealed interface SongsEffect {
    data class NavigateToPlayer(val songId: Long) : SongsEffect

    /**
     * [arg] 是插入 `%1$d` 的数量。批量操作的提示都要说清楚动了几首 ——
     * 只说"已加入歌单"的话，用户没法确认自己勾的那 3 首是不是都进去了。
     */
    data class ShowToast(@StringRes val message: Int, val arg: Int? = null) : SongsEffect
}
