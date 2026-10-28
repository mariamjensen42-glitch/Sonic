package com.cycling.data.online

import com.cycling.data.api.qqmusic.QQMusicPlaybackVkeyClient
import com.cycling.domain.online.model.MusicInfo
import com.cycling.domain.online.model.MusicQuality
import com.cycling.domain.online.model.OnlineSource
import com.cycling.domain.online.model.PlaySource
import com.cycling.domain.online.model.PlaySourceResolution
import com.cycling.domain.repository.PlayUrlCacheRepository
import com.cycling.domain.online.usecase.ResolveOnlinePlaySourceUseCase
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * 取链的统一入口：先查短期缓存，未命中再走脚本，脚本给不出时用 QQ 音乐官方接口兜底。
 *
 * 播放与下载都必须走这里 —— 两条路各自取链会让同一首歌一分钟内解析两遍，
 * 而且缓存里只会留下其中一次的结果。
 *
 * 缓存只在拿到可播放地址之后写入：脚本那条路已经在 `LxPluginManager.fetchPlaySource` 里过了
 * [com.cycling.data.online.PlayUrlProbe]，vkey 这条路的地址本身就是官方直链，所以命中缓存
 * 不必再探一次。
 */
@Singleton
class CachedPlaySourceResolver @Inject constructor(
    private val cache: PlayUrlCacheRepository,
    private val resolveOnlinePlaySource: ResolveOnlinePlaySourceUseCase,
    private val qqMusicVkey: QQMusicPlaybackVkeyClient
) {

    /**
     * @param songId 缓存键的一部分；同一首歌换平台/换档位都不会误命中。
     * @return 可播放地址（含取链要求的请求头）；取链失败返回 null，原因只打日志 ——
     *         「放不出来」由上层统一处理。
     */
    suspend fun resolve(
        songId: Long,
        track: MusicInfo,
        quality: MusicQuality
    ): PlaySource? {
        cache.get(songId, quality)?.let {
            Timber.d("CachedPlaySourceResolver: 命中缓存 song=%d quality=%s", songId, quality.name)
            return it
        }

        val resolution = resolveOnlinePlaySource(track, quality)
        val scripted = (resolution as? PlaySourceResolution.Success)?.source
        if (scripted != null) {
            cache.put(songId, quality, scripted)
            return scripted
        }

        // 脚本这条路走不通：没装 tx 脚本、脚本被风控挡住、或插件没声明该档位。
        // QQ 音乐曲目还能用官方 vkey 试一次 —— 登录态有效时这是最稳的一条路。
        nativeFallback(track, quality)?.let {
            Timber.d("CachedPlaySourceResolver: 官方 vkey 兜底成功 song=%d", songId)
            cache.put(songId, quality, it)
            return it
        }

        when (resolution) {
            is PlaySourceResolution.Failure -> {
                Timber.w(
                    "CachedPlaySourceResolver: 取链失败 %s —— %s",
                    track.title,
                    resolution.message
                )
            }

            PlaySourceResolution.NotConfigured -> {
                Timber.d("CachedPlaySourceResolver: 没有可用音源脚本，%s 未取到地址", track.title)
            }

            is PlaySourceResolution.Success -> Unit
        }
        return null
    }

    /**
     * QQ 音乐的原生取链兜底。
     *
     * 只处理 QQ 平台：其它平台的官方接口各自要一套签名协议，宿主这边统一交给音源脚本。
     * 未登录时客户端自己返回 `null`（vkey 必须要 `musicKey`），这里不再判断登录态 ——
     * 登录态是客户端读一次的，重复读只会多一次 DataStore 访问。
     */
    private suspend fun nativeFallback(track: MusicInfo, quality: MusicQuality): PlaySource? {
        if (track.source != OnlineSource.QQ) return null
        val songMid = track.extras["mid"] ?: track.id
        val url = qqMusicVkey.resolve(
            songMid = songMid,
            mediaMid = track.extras["media_mid"],
            quality = quality
        ) ?: return null
        return PlaySource(url)
    }
}
