package com.cycling.presentation.mymusic

import com.cycling.domain.model.Playlist
import androidx.annotation.StringRes
import com.cycling.domain.model.QQMusicPlaylistSummary
import com.cycling.domain.model.Song

data class MyMusicUiState(
    val favoriteSongs: List<Song> = emptyList(),
    val favoriteDescription: String = "",
    val isFavoriteDescriptionExpanded: Boolean = false,
    val isFavoriteDescriptionGenerating: Boolean = false,
    val currentSongId: Long? = null,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val showFavoriteSongs: Boolean = false,
    // AI create dialog
    val showAiCreateDialog: Boolean = false,
    val aiCreateMode: AiCreateMode = AiCreateMode.RANDOM,
    val selectedSongCount: Int = 10,
    val themeInput: String = "",
    val isAiGenerating: Boolean = false,
    val aiGeneratedSongs: List<Song> = emptyList(),
    val aiGeneratedPlaylistName: String = "",
    val aiCreateError: Int? = null,
    val hasApiKey: Boolean = false,
    // Rename
    val playlistToRename: Playlist? = null,
    val showRenameDialog: Boolean = false,
    // 在线歌单导入（目前只有 QQ 音乐）
    /** 用户粘贴的分享文本 / 链接 / 裸 id。 */
    val importInput: String = "",
    val isImporting: Boolean = false,
    /** 已处理曲目数 / 总曲目数，`importTotal == 0` 时表示还在抓歌单。 */
    val importDone: Int = 0,
    val importTotal: Int = 0,
    /** 失败原因，取值见 `mymusic_import_error_*`。 */
    @StringRes val importError: Int? = null,
    /** 登录账号名下的 QQ 音乐歌单；未登录或读取失败时为空，界面据此隐藏这一块。 */
    val myPlaylists: List<QQMusicPlaylistSummary> = emptyList(),
    /** 「我的歌单」是否已经问过一次 —— 空列表也要算问过，否则每次开对话框都会重打一次接口。 */
    val myPlaylistsLoaded: Boolean = false,
    val isLoadingMyPlaylists: Boolean = false
)

enum class AiCreateMode { RANDOM, THEME }

sealed interface MyMusicIntent {
    data object FavoriteSongsClick : MyMusicIntent
    data object BackToPlaylists : MyMusicIntent
    data object ToggleFavoriteDescription : MyMusicIntent
    data object ShuffleFavoriteSongs : MyMusicIntent
    data class SongClick(val song: Song) : MyMusicIntent
    data class ToggleFavorite(val song: Song) : MyMusicIntent
    data class PlaylistClick(val playlist: Playlist) : MyMusicIntent
    data class CreatePlaylist(val name: String) : MyMusicIntent
    data class DeletePlaylist(val playlist: Playlist) : MyMusicIntent
    data class ShowRenameDialog(val playlist: Playlist) : MyMusicIntent
    data class RenamePlaylist(val playlistId: Long, val newName: String) : MyMusicIntent
    data object DismissRenameDialog : MyMusicIntent
    // AI
    data object ShowAiCreateDialog : MyMusicIntent
    data object DismissAiCreateDialog : MyMusicIntent
    data class SetAiCreateMode(val mode: AiCreateMode) : MyMusicIntent
    data class SetSelectedSongCount(val count: Int) : MyMusicIntent
    data class SetThemeInput(val theme: String) : MyMusicIntent
    data object GenerateAiPlaylist : MyMusicIntent
    data object ConfirmAiPlaylist : MyMusicIntent
    // 在线歌单导入
    data class SetImportInput(val text: String) : MyMusicIntent
    data object ImportOnlinePlaylist : MyMusicIntent

    /** 打开导入对话框时拉一次「我的歌单」；已拉过就不再重复请求。 */
    data object LoadMyPlaylists : MyMusicIntent

    /** 直接导入账号里的某一张歌单。 */
    data class ImportMyPlaylist(val playlistId: String) : MyMusicIntent
}

sealed interface MyMusicEffect {
    data class NavigateToPlayer(val songId: Long) : MyMusicEffect
    data class NavigateToPlaylistDetail(val playlistId: Long) : MyMusicEffect
    data class ShowToast(@StringRes val message: Int) : MyMusicEffect
}
