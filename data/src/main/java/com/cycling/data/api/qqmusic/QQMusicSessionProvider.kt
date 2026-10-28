package com.cycling.data.api.qqmusic

import com.cycling.data.store.QQMusicSessionStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 当前登录的 QQ 音乐会话。
 *
 * 所有「这个 QQ 接口要不要带登录态」的取值都走这里，避免每个调用方各自去读 DataStore 并各自
 * 决定读失败怎么办。策略只有一条：**登录是增强项**，读不到（文件损坏、尚未落盘）就按游客处理，
 * 不能因为登录态缺失把接口本身拖垮。
 */
@Singleton
class QQMusicSessionProvider @Inject constructor(
    private val store: QQMusicSessionStore
) {

    suspend fun current(): QQMusicSession? = try {
        store.session.first()?.takeIf { it.isLoggedIn }
    } catch (e: IOException) {
        Timber.w(e, "QQMusicSessionProvider: session read failed")
        null
    }
}
