package com.cycling.data.api.qqmusic

import com.cycling.data.api.qqmusic.dto.QqProfileEnvelope
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** 账号资料里界面真正要用的两项。 */
data class QQMusicProfile(
    val nickname: String,
    val avatarUrl: String?
)

/**
 * QQ 音乐的账号资料接口。
 *
 * 它的作用不止「显示昵称」：扫码换来的 Cookie 到底能不能用，只有带 Cookie 真发一次请求才知道。
 * 因此登录流程把「取资料成功」当作凭据有效的判据 —— 换票接口回 0 但 Cookie 已被风控作废的情况
 * 是存在的，不校验就会存下一份看起来登录成功、实际处处 401 的会话。
 *
 * `g_tk` 是必须的：缺了它服务端不会报错，只是把响应降级成游客视角。
 */
@Singleton
class QQMusicAccountApi @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .withQqWebUserAgent()
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun profile(cookie: String, uin: String, musicKey: String): QQMusicProfile =
        withContext(Dispatchers.IO) {
            val url = PROFILE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("g_tk", qqHash33(musicKey, 5381).toString())
                .addQueryParameter("format", "json")
                .addQueryParameter("inCharset", "utf-8")
                .addQueryParameter("outCharset", "utf-8")
                .addQueryParameter("notice", "0")
                .addQueryParameter("cid", "205360838")
                .addQueryParameter("needNewCode", "0")
                .addQueryParameter("loginUin", uin)
                .addQueryParameter("hostUin", "0")
                .addQueryParameter("userid", uin)
                .addQueryParameter("reqfrom", "1")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Referer", REFERER)
                .header("Cookie", cookie)
                .build()

            val text = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("QQ 音乐账号资料返回 ${response.code}")
                response.body?.string().orEmpty()
            }
            parseProfile(text, uin)
        }

    private fun parseProfile(body: String, uin: String): QQMusicProfile {
        // 少数边缘节点仍会包一层 callback(...)，把外层的括号剥掉再解码。
        val start = body.indexOf('{')
        val end = body.lastIndexOf('}')
        val payload = if (start >= 0 && end > start) body.substring(start, end + 1) else body
        val envelope = runCatching { json.decodeFromString<QqProfileEnvelope>(payload) }
            .getOrElse { throw IOException("QQ 音乐账号资料解析失败", it) }
        if (envelope.code != 0) {
            throw IOException(
                envelope.message.ifBlank { envelope.msg }
                    .ifBlank { "QQ 音乐登录状态无效（${envelope.code}）" }
            )
        }
        val data = envelope.data
        val nickname = (data?.people.orEmpty().asSequence()
            .flatMap { person -> sequenceOf(person.nick, person.nickname, person.name) } +
            sequenceOf(data?.nick.orEmpty(), data?.nickname.orEmpty(), data?.name.orEmpty()))
            .firstOrNull(String::isNotBlank)
            ?: "QQ 音乐用户 $uin"
        val avatar = (data?.people.orEmpty().asSequence()
            .flatMap { person -> sequenceOf(person.headpic, person.avatar, person.avatarUrl) } +
            sequenceOf(data?.headpic.orEmpty(), data?.avatar.orEmpty(), data?.avatarUrl.orEmpty()))
            .firstOrNull(String::isNotBlank)
            ?.let(::asHttpsUrlOrNull)
        return QQMusicProfile(nickname = nickname, avatarUrl = avatar)
    }

    private companion object {
        const val PROFILE_URL = "https://c6.y.qq.com/rsc/fcgi-bin/fcg_get_profile_homepage.fcg"
        const val REFERER = "https://y.qq.com/"
        const val CALL_TIMEOUT_SECONDS = 15L
    }
}

/**
 * 头像地址必须走 https：QQ 音乐至今仍有返回 `http://` 的情况，而 App 的明文流量默认关闭，
 * 这样的封面会直接加载失败。
 */
private fun asHttpsUrlOrNull(raw: String): String? = raw.trim()
    .takeIf(String::isNotBlank)
    ?.let { if (it.startsWith("http://")) "https://" + it.removePrefix("http://") else it }
