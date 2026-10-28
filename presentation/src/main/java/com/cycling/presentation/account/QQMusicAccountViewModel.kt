package com.cycling.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import com.cycling.domain.repository.QQMusicAccountRepository
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * QQ 音乐账号页的 ViewModel。
 *
 * 扫码轮询放在这里而不是 Composable 的 `LaunchedEffect` 里：旋屏、进后台、页面重组都不该
 * 让一次已经扫过的授权作废，而 ViewModel 的生命周期正好覆盖这条路由。
 *
 * 手机号验证码是另一条通道：它没有轮询，只有「发码 → 提交」两次请求，所以单独一组字段，
 * 与扫码状态互不干扰 —— 用户在两条通道之间来回切，不该把对方的错误带到自己这边。
 *
 * 登录成功后由仓库落盘，[QQMusicAccountRepository.account] 发射新值，本类据此收掉轮询并切到
 * 已登录视图 —— 页面不再自己判断「登录成功了没有」。
 */
@HiltViewModel
class QQMusicAccountViewModel @Inject constructor(
    private val repository: QQMusicAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QQMusicAccountUiState())
    val uiState: StateFlow<QQMusicAccountUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<QQMusicAccountEffect>(extraBufferCapacity = 4)
    val uiEffect: SharedFlow<QQMusicAccountEffect> = _uiEffect.asSharedFlow()

    private var loginJob: Job? = null

    init {
        viewModelScope.launch {
            repository.account.collect { account ->
                _uiState.update { it.copy(account = account) }
                if (account != null) {
                    // 已经登上了，二维码与它的轮询都不再有意义。
                    loginJob?.cancel()
                    loginJob = null
                    _uiState.update {
                        it.copy(
                            ticket = null,
                            status = QQMusicQrStatus.Waiting,
                            error = null,
                            phoneCode = "",
                            phoneCodeSent = false,
                            phoneBusy = false,
                            phoneError = null
                        )
                    }
                } else if (loginJob == null && _uiState.value.method != QQMusicLoginMethod.PHONE) {
                    // 未登录（首次进入或刚退出）：直接给一张二维码。手机号通道不申请二维码。
                    startLogin()
                }
            }
        }
    }

    fun handleIntent(intent: QQMusicAccountIntent) {
        when (intent) {
            is QQMusicAccountIntent.SelectMethod -> selectMethod(intent.method)

            QQMusicAccountIntent.RefreshTicket -> {
                if (!_uiState.value.isSignedIn) startLogin()
            }

            is QQMusicAccountIntent.UpdatePhone -> _uiState.update {
                it.copy(phone = intent.value.filter(Char::isDigit).take(QQMusicAccountUiState.MAINLAND_PHONE_LENGTH))
            }

            is QQMusicAccountIntent.UpdatePhoneCode -> _uiState.update {
                it.copy(phoneCode = intent.value.filter(Char::isDigit).take(MAX_CODE_LENGTH))
            }

            QQMusicAccountIntent.SendPhoneCode -> sendPhoneCode()

            QQMusicAccountIntent.SubmitPhoneLogin -> submitPhoneLogin()

            QQMusicAccountIntent.Logout -> logout()
        }
    }

    /**
     * 切换登录通道。
     *
     * 从手机号切回扫码时要重新申请二维码（旧的那张早在切走时就被取消了）；切到手机号则
     * 反过来收掉轮询 —— 两个通道的请求都在跑，只会让状态互相打架。
     */
    private fun selectMethod(method: QQMusicLoginMethod) {
        if (_uiState.value.method == method) return
        _uiState.update { it.copy(method = method, error = null, phoneError = null) }
        if (_uiState.value.isSignedIn) return
        if (method == QQMusicLoginMethod.PHONE) {
            loginJob?.cancel()
            loginJob = null
            _uiState.update { it.copy(ticket = null, status = QQMusicQrStatus.Waiting) }
        } else {
            startLogin()
        }
    }

    /**
     * 开一轮扫码：申请二维码 → 轮询到「已授权 / 过期 / 被拒」。
     *
     * 过期不是终点 —— 二维码本来就会在两分钟内失效，这里直接换一张继续等，
     * 用户看到的只是二维码闪一下。
     */
    private fun startLogin() {
        loginJob?.cancel()
        val method = _uiState.value.method
        if (method == QQMusicLoginMethod.PHONE) return
        loginJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(ticket = null, status = QQMusicQrStatus.Waiting, error = null)
                }
                val ticket = try {
                    repository.createQrTicket(method)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    Timber.w(failure, "QQMusicAccount: create ticket failed")
                    _uiState.update { it.copy(error = failure.message ?: "获取二维码失败") }
                    return@launch
                }
                _uiState.update { it.copy(ticket = ticket, error = null) }

                // 给首帧一点时间把二维码画出来，避免刚拿到图就立刻打一次状态查询。
                delay(POLL_START_DELAY_MS)
                when (pollTicket(ticket.id)) {
                    PollOutcome.SignedIn -> {
                        _uiEffect.tryEmit(
                            QQMusicAccountEffect.ShowToast(R.string.qq_account_logged_in)
                        )
                        return@launch
                    }

                    PollOutcome.Rejected, PollOutcome.Failed -> return@launch

                    // 过期：回到外层换一张新的。
                    PollOutcome.Expired -> Unit
                }
            }
        }
    }

    /** 轮询一张二维码直到有结论；中途的「已扫码」会实时推到界面上。 */
    private suspend fun pollTicket(ticketId: String): PollOutcome {
        while (true) {
            val status = try {
                repository.checkQrTicket(ticketId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Timber.w(failure, "QQMusicAccount: poll failed")
                _uiState.update { it.copy(error = failure.message ?: "查询扫码状态失败") }
                return PollOutcome.Failed
            }
            _uiState.update { it.copy(status = status) }
            when (status) {
                QQMusicQrStatus.Authorized -> return PollOutcome.SignedIn
                QQMusicQrStatus.Expired -> return PollOutcome.Expired
                QQMusicQrStatus.Rejected -> return PollOutcome.Rejected
                QQMusicQrStatus.Waiting, QQMusicQrStatus.Scanned -> delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun sendPhoneCode() {
        val state = _uiState.value
        if (!state.canSendPhoneCode) return
        viewModelScope.launch {
            _uiState.update { it.copy(phoneBusy = true, phoneError = null) }
            try {
                repository.sendPhoneCode(state.phone)
                _uiState.update { it.copy(phoneBusy = false, phoneCodeSent = true) }
                _uiEffect.tryEmit(QQMusicAccountEffect.ShowToast(R.string.qq_account_phone_code_sent))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Timber.w(failure, "QQMusicAccount: send phone code failed")
                _uiState.update {
                    it.copy(phoneBusy = false, phoneError = failure.message ?: "验证码发送失败")
                }
            }
        }
    }

    /**
     * 提交验证码登录。
     *
     * 成功后不再改状态：仓库落盘 → `account` 流发射 → init 里的收集器负责收尾，
     * 这样「扫码成功」和「验证码成功」走的是同一条收尾路径。
     */
    private fun submitPhoneLogin() {
        val state = _uiState.value
        if (!state.canSubmitPhoneLogin) return
        viewModelScope.launch {
            _uiState.update { it.copy(phoneBusy = true, phoneError = null) }
            try {
                repository.loginWithPhone(state.phone, state.phoneCode)
                _uiEffect.tryEmit(QQMusicAccountEffect.ShowToast(R.string.qq_account_logged_in))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Timber.w(failure, "QQMusicAccount: phone login failed")
                _uiState.update {
                    it.copy(phoneBusy = false, phoneError = failure.message ?: "登录失败")
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                it.copy(
                    phone = "",
                    phoneCode = "",
                    phoneCodeSent = false,
                    phoneBusy = false,
                    phoneError = null
                )
            }
            _uiEffect.tryEmit(QQMusicAccountEffect.ShowToast(R.string.qq_account_logged_out))
        }
    }

    private enum class PollOutcome {
        SignedIn,
        Expired,
        Rejected,
        Failed
    }

    private companion object {
        /** 二维码拿到后先等一拍再开始查询。 */
        const val POLL_START_DELAY_MS = 500L

        /** 轮询间隔：两个协议的二维码都要扫一会儿，1 秒足够跟手又不会把服务端打疼。 */
        const val POLL_INTERVAL_MS = 1_100L

        /** QQ 音乐的短信验证码固定 6 位；输入框不限制更短的值，服务端会拒。 */
        const val MAX_CODE_LENGTH = 6
    }
}
