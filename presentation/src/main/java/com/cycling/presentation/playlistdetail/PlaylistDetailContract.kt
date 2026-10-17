package com.cycling.presentation.playlistdetail

/**
 * 歌单详情页特有的用户意图。
 *
 * 公共意图（点击、随机、多选、批量收藏、添加到歌单等）见
 * [com.cycling.presentation.collectiondetail.SongCollectionIntent]，
 * 由基类 [com.cycling.presentation.collectiondetail.CollectionDetailViewModel] 分发。
 */
sealed interface PlaylistDetailIntent {
    /** 从歌单移出单曲。 */
    data class RemoveSong(val songId: Long) : PlaylistDetailIntent

    /** 把勾选的曲目一次移出这张歌单。 */
    data object RemoveSelectedSongs : PlaylistDetailIntent

    /** 拖拽排序后落库；索引都是当前列表里的位置。 */
    data class MoveSong(val fromIndex: Int, val toIndex: Int) : PlaylistDetailIntent

    /** 把这张歌单写成 M3U8。[targetUri] 来自 SAF 的 CreateDocument。 */
    data class ExportM3u(val targetUri: String) : PlaylistDetailIntent

    /** 展开 / 收起歌单介绍。 */
    data object ToggleDescription : PlaylistDetailIntent

    /** 该歌单还没有介绍，现场生成一次并落库。 */
    data object GenerateDescription : PlaylistDetailIntent
}
