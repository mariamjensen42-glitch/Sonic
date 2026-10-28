package com.cycling.data.api.qqmusic

import okhttp3.OkHttpClient

/**
 * QQ 音乐 Web 端接口共用的请求特征。
 *
 * 这些端点按浏览器行为做风控：UA 必须是浏览器、Referer 必须是 `y.qq.com`。而全 App 共享的
 * [OkHttpClient] 会把 UA 统一改写成 Sonic 自己的（见 `NetworkModule`），所以凡是要走 QQ 音乐
 * Web 接口的客户端，都得在拦截器链的**后面**再覆盖一次。
 */
internal const val QQ_WEB_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/** 见 [QQ_WEB_USER_AGENT]：把 UA 换回浏览器标识。 */
internal fun OkHttpClient.Builder.withQqWebUserAgent(): OkHttpClient.Builder =
    addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder().header("User-Agent", QQ_WEB_USER_AGENT).build()
        )
    }
