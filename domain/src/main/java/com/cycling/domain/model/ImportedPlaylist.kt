package com.cycling.domain.model

import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.OnlineSource

/**
 * 从音乐平台抓下来的歌单，还没写进本地库。
 *
 * 只是**搬运单**：名称、封面、曲目指针。真正落库是 [com.cycling.domain.usecase.ImportOnlinePlaylistUseCase]
 * 的事 —— 抓取与写入分开，抓失败不会留下半张空歌单。
 */
data class ImportedPlaylist(
    val source: OnlineSource,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val tracks: List<MusicInfo> = emptyList()
)
