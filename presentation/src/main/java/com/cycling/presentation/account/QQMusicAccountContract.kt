package com.cycling.presentation.account

import androidx.annotation.StringRes
import com.cycling.domain.model.QQMusicAccount
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import com.cycling.domain.model.QQMusicQrTicket

/**
 * QQ 音乐账号页的状态。
 *
 * 三条登录通道共用一份状态：扫码相关的字段（[ticket] / [status]）只在 [QQMusicLoginMethod.QQ]
 * 与 [QQMusicLoginMethod.WECHAT] 下有意义，手机号相关的字段（[phone] / [phoneCode]）只在
 * [QQMusicLoginMethod.PHONE] 下有意义。有 [account] 时整页切到已登录视图，其余字段一律不再展示。
 */
data class QQMusicAccountUiState(
    val account: QQMusicAccount? = null,
    val method: QQMusicLoginMethod = QQMusicLoginMethod.QQ,
    val ticket: QQMusicQrTicket? = null,
    val status: QQMusicQrStatus = QQMusicQrStatus.Waiting,
    val error: String? = null,
    val phone: String = "",
    val phoneCode: String = "",
    /** 验证码是否已经发出去；只有发过码才允许提交登录。 */
    val phoneCodeSent: Boolean = false,
    /** 发码 / 登录请求进行中；界面据此禁用按钮，避免重复提交。 */
    val phoneBusy: Boolean = false,
    /** 手机号通道自己的错误，和扫码的 [error] 分开，切通道时不互相污染。 */
    val phoneError: String? = null
) {
    val isSignedIn: Boolean get() = account != null

    /** 手机号是否已经够 11 位，用来决定「获取验证码」按钮能不能点。 */
    val canSendPhoneCode: Boolean
        get() = !phoneBusy && phone.length == MAINLAND_PHONE_LENGTH && phone.all(Char::isDigit)

    /** 验证码是否够 4 位以上；QQ 音乐的验证码固定 6 位，这里放宽一点，长度不对服务端会拒。 */
    val canSubmitPhoneLogin: Boolean
        get() = !phoneBusy && phoneCodeSent && phoneCode.length >= MIN_CODE_LENGTH

    companion object {
        const val MAINLAND_PHONE_LENGTH = 11
        const val MIN_CODE_LENGTH = 4
    }
}

sealed interface QQMusicAccountIntent {
    data class SelectMethod(val method: QQMusicLoginMethod) : QQMusicAccountIntent

    /** 主动换一张二维码（手动刷新，或二维码过期后的重试）。 */
    data object RefreshTicket : QQMusicAccountIntent

    data class UpdatePhone(val value: String) : QQMusicAccountIntent

    data class UpdatePhoneCode(val value: String) : QQMusicAccountIntent

    /** 请求发送验证码。 */
    data object SendPhoneCode : QQMusicAccountIntent

    /** 提交手机号 + 验证码登录。 */
    data object SubmitPhoneLogin : QQMusicAccountIntent

    data object Logout : QQMusicAccountIntent
}

sealed interface QQMusicAccountEffect {
    data class ShowToast(@StringRes val messageRes: Int) : QQMusicAccountEffect
}
