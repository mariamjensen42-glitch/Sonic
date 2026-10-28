package com.cycling.sonic

import android.app.Application
import android.util.Log
import com.cycling.core.ui.theme.SonBrandSchemes
import com.cycling.data.cache.CacheJanitor
import com.cycling.domain.repository.DownloadRepository
import com.cycling.domain.repository.PlayUrlCacheRepository
import com.cycling.presentation.widget.SonicWidgetUpdater
import com.cycling.sonic.log.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/** 缓存上限的检查间隔。 */
private const val CACHE_CHECK_INTERVAL_MS = 60L * 60 * 1000

@HiltAndroidApp
class SonicApplication : Application() {

    @Inject
    lateinit var cacheJanitor: CacheJanitor

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var playUrlCacheRepository: PlayUrlCacheRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // 开发/测试：控制台输出全部级别，文件记录 INFO 及以上
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(logDir(), setOf(Log.INFO, Log.WARN, Log.ERROR)))
        } else {
            // 生产：仅 WARNING/ERROR 写入文件
            Timber.plant(FileLoggingTree(logDir(), setOf(Log.WARN, Log.ERROR)))
        }

        // 动态取色不可用时（Android 12 以下，或系统没提供壁纸取色），首帧会回落品牌色
        // 展开配色方案，而展开要做上千次颜色空间迭代。这里先把它算在后台线程上，
        // 首帧通常就能直接命中缓存。
        SonBrandSchemes.prewarm()

        // 上次进程被杀时还在跑的任务打回队列；取链缓存顺手清一遍过期条目。
        appScope.launch {
            runCatching { downloadRepository.recoverPending() }
                .onFailure { Timber.w(it, "下载队列恢复失败") }
            runCatching { playUrlCacheRepository.purgeExpired() }
                .onFailure { Timber.w(it, "取链缓存清理失败") }
        }

        // 小组件只在已添加到桌面时才启动订阅（内部会检查），
        // 保证 App 在后台放歌时桌面封面/播放键仍能跟着变。
        SonicWidgetUpdater.start(this)

        // 启动即查一次（补上关闭期间涨过头的量），之后每小时一次。进程级循环，
        // 不申请闹钟权限，也就不会在后台被唤醒。
        appScope.launch {
            while (true) {
                runCatching { cacheJanitor.cleanIfNeeded() }
                    .onFailure { Timber.w(it, "缓存上限检查失败") }
                delay(CACHE_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun logDir(): File = getExternalFilesDir("logs") ?: File(filesDir, "logs")
}
