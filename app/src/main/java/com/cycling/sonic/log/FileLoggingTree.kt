package com.cycling.sonic.log

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * 文件日志输出树：按级别开关过滤，按天切分日志文件，每条日志格式化为
 * `[yyyy-MM-dd HH:mm:ss.SSS] 级别/TAG: 消息`，异常追加堆栈。
 *
 * 各级别的输出开关由构造参数 [levels] 控制，装配处（SonicApplication）按环境传入：
 * - 开发/测试（DEBUG）：控制台 DebugTree 全级别 + 本树 INFO 及以上入文件
 * - 生产（release）：仅本树 WARNING/ERROR 入文件
 */
class FileLoggingTree(
    private val dir: File,
    private val levels: Set<Int> = setOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR)
) : Timber.Tree() {

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority in levels

    // ponytail: 每条日志开关一次文件流，量极大时换 FileChannel 缓冲；调试日志规模下够用
    @Synchronized
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return
        dir.mkdirs()
        val now = Date()
        val line = buildString {
            append('[')
            append(timestampFormat.format(now))
            append("] ")
            append(levelName(priority))
            append('/')
            append(tag ?: "-")
            append(": ")
            append(message)
            if (t != null) append('\n').append(t.stackTraceToString())
            append('\n')
        }
        File(dir, "sonic-log-${dateFormat.format(now)}.txt").appendText(line)
    }

    private fun levelName(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> priority.toString()
    }
}
