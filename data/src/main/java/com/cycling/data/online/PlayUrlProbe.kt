package com.cycling.data.online

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * 取链结果的可播性探测。
 *
 * 为什么需要它：音源脚本只是**拼一个 URL 字符串**就返回成功，脚本自己不会去访问它。
 * 后端挂了 / 路径改了 / 参数不对时，脚本照样回 200 的「成功」，于是宿主拿到一个
 * 404 的地址 —— 表现为「搜到歌、点了、然后没声音」，而且因为脚本「成功」了，
 * 换源逻辑根本不会继续试下一个插件。
 *
 * 实测案例：长青音源（`yinyue.haitangw.net`）的 PHP 路径全部 404，但脚本仍返回地址，
 * 排在它后面的幻音永远轮不到。
 *
 * 探测用 `GET` + `Range: bytes=0-1`：比 `HEAD` 稳（不少 CDN 对 HEAD 回 405），
 * 又不会把整首歌拉下来。2xx/206 视为可播，其余（403/404/5xx）视为不可播。
 */
@Singleton
class PlayUrlProbe @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun isPlayable(url: String): Boolean = withContext(Dispatchers.IO) {
        // 非 http(s) 的地址（file:// / content://）交给播放器自己判断。
        if (!url.startsWith("http://") && !url.startsWith("https://")) return@withContext true
        try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
                .build()
            client.newCall(request).execute().use { response ->
                val ok = response.isSuccessful
                if (!ok) Timber.w("PlayUrlProbe: %d %s", response.code, url.take(120))
                ok
            }
        } catch (e: Exception) {
            Timber.w(e, "PlayUrlProbe: 不可达 %s", url.take(120))
            false
        }
    }

    private companion object {
        const val PROBE_TIMEOUT_SECONDS = 6L
    }
}
