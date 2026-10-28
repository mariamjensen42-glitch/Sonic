package com.cycling.data.repository

import com.cycling.data.api.playlist.QqPlaylistApi
import com.cycling.data.api.qqmusic.QQMusicUserPlaylistApi
import com.cycling.domain.model.QQMusicPlaylistSummary
import com.cycling.domain.online.OnlinePlaylistRepository
import com.cycling.domain.online.PlaylistFetchResult
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * 目前只接了 QQ 音乐 —— 它的公开歌单接口不要登录态就能读，别的平台（网易云要 `__csrf`、
 * 酷狗要签名）成本高得多。要加平台就在这里按链接域名分派。
 *
 * 两个数据源分工明确：[QqPlaylistApi] 读公开链接（不需要登录），
 * [QQMusicUserPlaylistApi] 读登录账号名下的歌单（必须登录）。前者认文本，后者认 id。
 */
@Singleton
class OnlinePlaylistRepositoryImpl @Inject constructor(
    private val qqPlaylistApi: QqPlaylistApi,
    private val qqUserPlaylistApi: QQMusicUserPlaylistApi
) : OnlinePlaylistRepository {

    override suspend fun fetchPlaylist(shareText: String): PlaylistFetchResult =
        qqPlaylistApi.fetchPlaylist(shareText)

    override suspend fun fetchMyPlaylists(): List<QQMusicPlaylistSummary> =
        try {
            qqUserPlaylistApi.myPlaylists()
        } catch (e: Exception) {
            // 列表是增强项：读不到就当作「没有歌单」，不该让整个导入页报错。
            Timber.w(e, "OnlinePlaylistRepository: 读取我的歌单失败")
            emptyList()
        }

    override suspend fun fetchPlaylistById(playlistId: String): PlaylistFetchResult =
        if (playlistId.isBlank()) {
            PlaylistFetchResult.InvalidLink
        } else {
            try {
                qqUserPlaylistApi.playlistDetail(playlistId)
                    ?.let { PlaylistFetchResult.Success(it) }
                    ?: PlaylistFetchResult.NotAccessible
            } catch (e: Exception) {
                Timber.w(e, "OnlinePlaylistRepository: 读取歌单 %s 失败", playlistId)
                PlaylistFetchResult.Failed(e.message)
            }
        }
}
