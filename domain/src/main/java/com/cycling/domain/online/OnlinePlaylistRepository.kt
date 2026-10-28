package com.cycling.domain.online

import com.cycling.domain.model.ImportedPlaylist
import com.cycling.domain.model.QQMusicPlaylistSummary

/**
 * 从音乐平台的分享链接导入歌单。
 *
 * 只负责「链接 → 歌单信息」，不写库：抓取成功之后怎么落地（建歌单、把曲目变成在线曲目、
 * 撞上本地文件时改指本地）由用例决定，这样换平台只加一个实现。
 *
 * 另外提供「账号名下有哪些歌单」这一层 —— 链接导入解决的是「别人分享的那张」，
 * 而用户自己的歌单只有登录后才看得到。
 */
interface OnlinePlaylistRepository {

    /** 接受分享文本、链接或裸 id —— 用户从 QQ 音乐复制出来的东西长什么样都得能认。 */
    suspend fun fetchPlaylist(shareText: String): PlaylistFetchResult

    /**
     * 登录账号名下的歌单（含「我喜欢」）。
     *
     * 未登录返回空列表：这不是错误，界面据此隐藏入口即可。
     */
    suspend fun fetchMyPlaylists(): List<QQMusicPlaylistSummary>

    /**
     * 按平台歌单 id 拉整张歌单。
     *
     * 与 [fetchPlaylist] 的区别只在入口：一个从文本里挖 id，一个 id 已经拿到了
     * （列表里点的那一行）。两者产出同一种 [PlaylistFetchResult]。
     */
    suspend fun fetchPlaylistById(playlistId: String): PlaylistFetchResult
}

sealed interface PlaylistFetchResult {

    data class Success(val playlist: ImportedPlaylist) : PlaylistFetchResult

    /** 文本里没有任何歌单 id，或者那条链接压根不是 QQ 音乐的。 */
    data object InvalidLink : PlaylistFetchResult

    /** 私密歌单、已删除、或者被平台折叠掉的歌单：接口不会告诉我们具体是哪种。 */
    data object NotAccessible : PlaylistFetchResult

    /** 网络或解析失败；[message] 只在平台回了可读原因时才有值。 */
    data class Failed(val message: String? = null) : PlaylistFetchResult
}
