package com.cycling.presentation.artistdetail

/**
 * 歌手详情页特有的用户意图。
 *
 * 公共意图（点击、随机、多选、批量收藏、添加到歌单等）见
 * [com.cycling.presentation.collectiondetail.SongCollectionIntent]，
 * 由基类 [com.cycling.presentation.collectiondetail.CollectionDetailViewModel] 分发。
 */
sealed interface ArtistDetailIntent {
    /** 切换单曲收藏。 */
    data class ToggleFavorite(val songId: Long) : ArtistDetailIntent
}
