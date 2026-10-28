package com.cycling.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * 下载专用的前台服务：只做一件事 —— 让系统不要在我们写文件时把进程回收掉。
 *
 * 进度与任务状态全在 [com.cycling.data.repository.DownloadRepositoryImpl] 里，服务不持有状态，
 * 每次 `startService` 只是「用新的文案再 `startForeground` 一次」。这样即使被系统重启，
 * 也不存在两套进度互相打脸。
 */
class DownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val indeterminate = intent?.getBooleanExtra(EXTRA_INDETERMINATE, false) == true
        try {
            startForeground(NOTIFICATION_ID, buildNotification(this, title, text, progress, indeterminate))
        } catch (e: Exception) {
            Timber.w(e, "DownloadService: startForeground 失败")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "sonic_download"
        private const val CHANNEL_NAME = "下载"
        private const val NOTIFICATION_ID = 0x50D1

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_PROGRESS = "progress"
        private const val EXTRA_INDETERMINATE = "indeterminate"

        fun start(context: Context, title: String, text: String, progress: Int, indeterminate: Boolean) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_PROGRESS, progress.coerceIn(0, 100))
                putExtra(EXTRA_INDETERMINATE, indeterminate)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Timber.w(it, "DownloadService: 启动失败") }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, DownloadService::class.java)) }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示歌曲下载进度"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        private fun buildNotification(
            context: Context,
            title: String,
            text: String,
            progress: Int,
            indeterminate: Boolean
        ): Notification {
            ensureChannel(context)
            val contentIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.let {
                    PendingIntent.getActivity(
                        context,
                        0,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(text)
                .setProgress(100, progress, indeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(contentIntent)
                .build()
        }
    }
}
