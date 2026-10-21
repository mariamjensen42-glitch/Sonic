package com.cycling.domain.lyrics.model

/**
 * LRC 文件头部的元数据标签（`[ti:]` / `[ar:]` / `[al:]` / `[offset:]` / `[length:]` / `[by:]`）。
 *
 * 这是歌词文档的属性，不是歌曲文件的标签：它描述的是这一份歌词认为自己属于哪首歌。
 * 解析后的 `null` 表示文档里没有该标签，写回时也就不会凭空补一条。
 */
data class Attributes(
    val artist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val offset: Int? = null,
    val duration: Int? = null,
    /** `[by:]` —— 歌词的制作/上传者。 */
    val by: String? = null,
)
