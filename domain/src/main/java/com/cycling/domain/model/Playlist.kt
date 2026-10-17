package com.cycling.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val dateAdded: Long,
    val dateModified: Long,
    val numberOfSongs: Int,
    val albumArt: String? = null,
    /** 歌单介绍。AI 创建时生成，手动创建的歌单为 null —— 详情页据此决定是否显示。 */
    val description: String? = null
)
