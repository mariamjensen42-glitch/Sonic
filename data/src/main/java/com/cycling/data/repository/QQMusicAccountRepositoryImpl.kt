package com.cycling.data.repository

import com.cycling.data.api.qqmusic.QQMusicAccountApi
import com.cycling.data.api.qqmusic.QQMusicPhoneAuthClient
import com.cycling.data.api.qqmusic.QQMusicQrLoginClient
import com.cycling.data.api.qqmusic.QQMusicQrSession
import com.cycling.data.api.qqmusic.parseSessionCookie
import com.cycling.data.store.QQMusicSessionStore
import com.cycling.domain.model.QQMusicAccount
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import com.cycling.domain.model.QQMusicQrTicket
import com.cycling.domain.repository.QQMusicAccountRepository
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * QQ 音乐账号仓库。
 *
 * 把「扫码 / 验证码 → 换票 → 校验 → 落盘」这两条链都收在这里：界面只看到二维码、验证码、
 * 状态和最终的账号，协议细节不外泄。
 *
 * 同时只保留一张有效二维码（[tickets]）：界面本来也只展示一张，留着旧会话会让
 * 已经过期的 `qrsig` 参与轮询，返回的状态码就无法解释了。
 */
@Singleton
class QQMusicAccountRepositoryImpl @Inject constructor(
    private val store: QQMusicSessionStore,
    private val loginClient: QQMusicQrLoginClient,
    private val phoneAuthClient: QQMusicPhoneAuthClient,
    private val accountApi: QQMusicAccountApi
) : QQMusicAccountRepository {

    private val tickets = ConcurrentHashMap<String, QQMusicQrSession>()

    override val account: Flow<QQMusicAccount?> = store.session.map { session ->
        session?.let {
            QQMusicAccount(
                uin = it.uin,
                nickname = it.nickname.ifBlank { "QQ 音乐用户 ${it.uin}" },
                avatarUrl = it.avatarUrl
            )
        }
    }

    override suspend fun createQrTicket(method: QQMusicLoginMethod): QQMusicQrTicket {
        // 手机号登录没有二维码；挡在这里而不是让协议层去处理，界面那边就不必再判一次。
        require(method != QQMusicLoginMethod.PHONE) { "手机号登录不需要二维码" }
        val session = loginClient.createSession(method)
        tickets.clear()
        val id = UUID.randomUUID().toString()
        tickets[id] = session
        Timber.d("QQMusicAccount: created ${method.name} ticket")
        return QQMusicQrTicket(
            id = id,
            method = method,
            imageBytes = session.imageBytes,
            imageMimeType = session.imageMimeType,
            expiresAtMillis = session.expiresAtMillis
        )
    }

    override suspend fun checkQrTicket(ticketId: String): QQMusicQrStatus {
        val session = tickets[ticketId] ?: return QQMusicQrStatus.Expired
        val check = loginClient.checkSession(session)
        return when (check.status) {
            QQMusicQrStatus.Authorized -> {
                val cookie = check.cookie
                    ?: throw IOException("QQ 音乐登录成功但没有返回登录凭据")
                val identity = parseSessionCookie(cookie)
                    ?: throw IOException("QQ 音乐登录凭据不完整，请重新扫码")
                // 换票接口回 0 不等于凭据可用：带 Cookie 真取一次资料才算验证通过，
                // 否则会存下一份「看起来登录成功、实际处处被拒」的会话。
                val profile = accountApi.profile(cookie, identity.uin, identity.musicKey)
                store.save(identity.copy(nickname = profile.nickname, avatarUrl = profile.avatarUrl))
                tickets.remove(ticketId)
                Timber.d("QQMusicAccount: login succeeded for uin=${identity.uin}")
                QQMusicQrStatus.Authorized
            }

            QQMusicQrStatus.Expired, QQMusicQrStatus.Rejected -> {
                tickets.remove(ticketId)
                check.status
            }

            else -> check.status
        }
    }

    override suspend fun sendPhoneCode(phone: String) {
        phoneAuthClient.sendCode(phone)
    }

    /**
     * 手机号登录。
     *
     * 与扫码共用「落盘前先校验」这条规矩：验证码换来的 Cookie 也可能是被风控作废的，
     * 不真取一次账号资料就发现不了。校验失败直接抛，[store] 里不会留下任何东西。
     */
    override suspend fun loginWithPhone(phone: String, code: String) {
        val session = phoneAuthClient.login(phone, code)
        val profile = accountApi.profile(session.cookie, session.uin, session.musicKey)
        store.save(session.copy(nickname = profile.nickname, avatarUrl = profile.avatarUrl))
        // 扫码留下的票据在验证码登录成功后已经没有意义，清掉免得旧轮询继续跑。
        tickets.clear()
        Timber.d("QQMusicAccount: phone login succeeded for uin=${session.uin}")
    }

    override suspend fun logout() {
        store.clear()
        tickets.clear()
        phoneAuthClient.reset()
        Timber.d("QQMusicAccount: logged out")
    }
}
