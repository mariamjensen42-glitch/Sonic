package com.cycling.presentation.collectiondetail

import androidx.annotation.StringRes
import com.cycling.domain.model.Playlist
import com.cycling.domain.model.Song

/**
 * 歌曲集合详情页的公共状态。歌单 / 专辑 / 歌手三个详情页共用同一份形状，
 * 页面特有的字段（介绍、收藏集）以可空/默认值并入，避免三份状态定义各自漂移。
 */
data class CollectionDetailUiState(
    /** 集合名称：歌单名 / 专辑名 / 歌手名。 */
    val title: String? = null,
    /** 头图副标题：专辑页放歌手名，其余页留空由界面方展示曲目数。 */
    val subtitle: String? = null,
    val artwork: String? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val currentSongId: Long? = null,
    /** 多选模式。开启后单击是勾选，不再是播放。 */
    val selectionMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    /** 「添加到歌单」弹窗可见性。 */
    val showAddToPlaylistDialog: Boolean = false,
    /** 弹窗里的候选歌单，随弹窗一起加载。 */
    val playlists: List<Playlist> = emptyList(),
    /** 介绍文案（仅歌单有，存量歌单会现场生成后落库）。 */
    val description: String? = null,
    /** 介绍是否展开。折叠态只露出开头几行。 */
    val isDescriptionExpanded: Boolean = false,
    /** 介绍正在生成中。 */
    val isDescriptionGenerating: Boolean = false,
    /** 已收藏曲目 id（歌手页实时维护，供行尾心形使用）。 */
    val likedSongIds: Set<Long> = emptySet()
)

/**
 * 三个详情页共用的用户意图。
 *
 * 页面特有的意图（移出歌单、展开介绍、行尾收藏等）留在各自 feature 的 Contract 里，
 * 由页面自行分发，不进这份公共集合。
 */
sealed interface SongCollectionIntent {
    data class SongClick(val song: Song) : SongCollectionIntent
    /** 随机播放整个集合。 */
    data object ShufflePlay : SongCollectionIntent
    data object AddSongsToPlaylist : SongCollectionIntent
    data object DismissAddToPlaylistDialog : SongCollectionIntent
    data class AddSongsToPlaylistTo(val playlistId: Long) : SongCollectionIntent
    /** 在弹窗里直接新建歌单，并把整个集合塞进去。 */
    data class CreatePlaylistAndAddSongs(val name: String) : SongCollectionIntent

    // ── 多选 ──
    data object EnterSelectionMode : SongCollectionIntent
    data object ExitSelectionMode : SongCollectionIntent
    data class ToggleSelection(val songId: Long) : SongCollectionIntent
    data object SelectAll : SongCollectionIntent
    data object ClearSelection : SongCollectionIntent
    data class BatchSetFavorite(val isFavorite: Boolean) : SongCollectionIntent

    /** 行菜单里的收藏 / 取消收藏，作用于 [song] 自己。 */
    data class ToggleFavorite(val song: Song) : SongCollectionIntent
}

sealed interface CollectionDetailEffect {
    data class NavigateToPlayer(val songId: Long) : CollectionDetailEffect

    /** 随机播放：下发整条打乱后的队列。 */
    data class PlayShuffled(val queue: List<Song>) : CollectionDetailEffect

    /** [arg] 是插入 `%1$d` 的数量，批量操作要报出到底动了几首。 */
    data class ShowToast(@StringRes val message: Int, val arg: Int? = null) : CollectionDetailEffect
}
