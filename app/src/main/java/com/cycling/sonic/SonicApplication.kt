package com.cycling.sonic

import android.app.Application
import android.util.Log
import com.cycling.presentation.widget.SonicWidgetUpdater
import com.cycling.sonic.log.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File

@HiltAndroidApp
class SonicApplication : Application() {
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

        // 小组件只在已添加到桌面时才启动订阅（内部会检查），
        // 保证 App 在后台放歌时桌面封面/播放键仍能跟着变。
        SonicWidgetUpdater.start(this)
    }

    private fun logDir(): File = getExternalFilesDir("logs") ?: File(filesDir, "logs")
}
