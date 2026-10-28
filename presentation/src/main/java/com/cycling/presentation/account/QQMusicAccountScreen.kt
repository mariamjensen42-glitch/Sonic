package com.cycling.presentation.account

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleGroupedSection
import com.cycling.core.ui.apple.AppleSectionHeader
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.model.QQMusicAccount
import com.cycling.domain.model.QQMusicLoginMethod
import com.cycling.domain.model.QQMusicQrStatus
import com.cycling.domain.model.QQMusicQrTicket
import com.cycling.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置 → 在线音乐 → QQ 音乐账号。
 *
 * 未登录时是「扫码登录」页：QQ / 微信两种通道、二维码、实时状态；已登录时是账号卡与退出登录。
 * 两种视图共用同一条路由，靠 [QQMusicAccountUiState.account] 切换 —— 登录成功不需要额外导航，
 * 页面自己会变。
 */
@Composable
fun QQMusicAccountContent(
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    viewModel: QQMusicAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is QQMusicAccountEffect.ShowToast -> onShowToast(context.getString(effect.messageRes))
            }
        }
    }

    val clearance = appleBottomBarClearance() + bottomPadding

    val saveTicket: () -> Unit = {
        val ticket = uiState.ticket
        if (ticket != null && !saving) {
            saving = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { saveQrToGallery(context, ticket) }
                }
                saving = false
                onShowToast(
                    result.fold(
                        onSuccess = { location ->
                            context.getString(R.string.qq_account_saved, location)
                        },
                        onFailure = { failure ->
                            context.getString(
                                R.string.qq_account_save_failed,
                                failure.message
                                    ?: context.getString(R.string.qq_account_error_generic)
                            )
                        }
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = clearance)
    ) {
        val account = uiState.account
        if (account != null) {
            item { SignedInSection(account) }
            item {
                AppleActionButton(
                    label = stringResource(R.string.qq_account_logout),
                    onClick = { confirmLogout = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    filled = false
                )
            }
            item {
                Text(
                    text = stringResource(R.string.qq_account_benefit),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
            }
            item {
                Text(
                    text = stringResource(R.string.qq_account_local_only),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
            }
        } else {
            item {
                AppleSectionHeader(title = stringResource(R.string.qq_account_section_login))
            }
            item {
                MethodSelector(
                    selected = uiState.method,
                    onSelect = { viewModel.handleIntent(QQMusicAccountIntent.SelectMethod(it)) }
                )
            }
            if (uiState.method == QQMusicLoginMethod.PHONE) {
                item {
                    PhoneLoginPanel(
                        state = uiState,
                        onPhoneChange = {
                            viewModel.handleIntent(QQMusicAccountIntent.UpdatePhone(it))
                        },
                        onCodeChange = {
                            viewModel.handleIntent(QQMusicAccountIntent.UpdatePhoneCode(it))
                        },
                        onSendCode = {
                            viewModel.handleIntent(QQMusicAccountIntent.SendPhoneCode)
                        },
                        onSubmit = {
                            viewModel.handleIntent(QQMusicAccountIntent.SubmitPhoneLogin)
                        }
                    )
                }
            } else {
                item {
                    QrPanel(
                        ticket = uiState.ticket,
                        method = uiState.method,
                        saving = saving,
                        onSave = saveTicket,
                        onRefresh = { viewModel.handleIntent(QQMusicAccountIntent.RefreshTicket) }
                    )
                }
                item {
                    StatusBlock(
                        state = uiState,
                        onRefresh = { viewModel.handleIntent(QQMusicAccountIntent.RefreshTicket) }
                    )
                }
            }
        }
    }

    if (confirmLogout) {
        AppleAlert(
            title = stringResource(R.string.qq_account_logout_confirm_title),
            message = stringResource(R.string.qq_account_logout_confirm_message),
            confirmLabel = stringResource(R.string.qq_account_logout),
            onConfirm = {
                confirmLogout = false
                viewModel.handleIntent(QQMusicAccountIntent.Logout)
            },
            onDismiss = { confirmLogout = false },
            dismissLabel = stringResource(R.string.qq_account_logout_cancel),
            destructive = true
        )
    }
}

@Composable
private fun SignedInSection(account: QQMusicAccount) {
    AppleSectionHeader(title = stringResource(R.string.qq_account_section_signed_in))
    AppleGroupedSection(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3Artwork(
                artwork = account.avatarUrl,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                placeholderIcon = SymbolIconName.Person
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = account.nickname,
                    style = AppleType.headline,
                    color = AppleTheme.colors.label
                )
                Text(
                    text = stringResource(R.string.qq_account_uin, account.uin),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
            }
        }
    }
}

/** 通道选择：QQ / 微信 / 手机号。切换即重新申请二维码，旧的那张由仓库顶掉。 */
@Composable
private fun MethodSelector(
    selected: QQMusicLoginMethod,
    onSelect: (QQMusicLoginMethod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QQMusicLoginMethod.entries.forEach { method ->
            AppleChip(
                label = stringResource(method.labelRes()),
                selected = method == selected,
                onClick = { onSelect(method) }
            )
        }
    }
}

/**
 * 手机号验证码登录。
 *
 * 两个输入框都只收数字：手机号限制 11 位、验证码限制 6 位，在输入层就挡住明显不合法的值，
 * 免得把一次必然失败的请求发出去。验证码框在发码成功前是禁用的 —— 让用户先填验证码再点
 * 「获取验证码」，顺序反了会以为卡住了。
 */
@Composable
private fun PhoneLoginPanel(
    state: QQMusicAccountUiState,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.qq_account_phone_label)) },
            placeholder = { Text(stringResource(R.string.qq_account_phone_placeholder)) },
            singleLine = true,
            enabled = !state.phoneBusy,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = state.phoneCode,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.qq_account_phone_code_label)) },
            singleLine = true,
            enabled = !state.phoneBusy && state.phoneCodeSent,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppleActionButton(
                label = stringResource(R.string.qq_account_phone_send_code),
                onClick = onSendCode,
                filled = false,
                enabled = state.canSendPhoneCode,
                modifier = Modifier.weight(1f)
            )
            AppleActionButton(
                label = stringResource(R.string.qq_account_phone_login),
                onClick = onSubmit,
                enabled = state.canSubmitPhoneLogin,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(R.string.qq_account_phone_instruction),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
        state.phoneError?.let { message ->
            Text(
                text = message,
                style = AppleType.footnote,
                color = AppleTheme.colors.systemRed
            )
        }
    }
}

/**
 * 二维码面板与它的两个动作。
 *
 * 底板用中性填充色而不是白：二维码图片自带白底与静区，外面再垫一层白只会在暗色主题下
 * 变成一块突兀的亮方块。
 */
@Composable
private fun QrPanel(
    ticket: QQMusicQrTicket?,
    method: QQMusicLoginMethod,
    saving: Boolean,
    onSave: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(MaterialTheme.shapes.large)
                .background(AppleTheme.colors.fill),
            contentAlignment = Alignment.Center
        ) {
            if (ticket == null) {
                // 真等网络，这里直接显示进度条是合适的（只有本地数据才需要延迟现身）。
                CircularProgressIndicator()
            } else {
                val bitmap = remember(ticket) {
                    BitmapFactory.decodeByteArray(ticket.imageBytes, 0, ticket.imageBytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(
                            R.string.qq_account_qr_content_desc,
                            stringResource(method.labelRes())
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppleActionButton(
            label = stringResource(R.string.qq_account_refresh),
            onClick = onRefresh,
            icon = SymbolIconName.Refresh,
            enabled = ticket != null,
            modifier = Modifier.weight(1f)
        )
        AppleActionButton(
            label = stringResource(R.string.qq_account_save),
            onClick = onSave,
            filled = false,
            enabled = ticket != null && !saving,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBlock(
    state: QQMusicAccountUiState,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(statusTextRes(state)),
            style = AppleType.subheadEmphasized,
            color = AppleTheme.colors.label
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(instructionRes(state.method)),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
        state.error?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = AppleType.footnote,
                color = AppleTheme.colors.systemRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppleActionButton(
                label = stringResource(R.string.qq_account_retry),
                onClick = onRefresh,
                filled = false,
                icon = SymbolIconName.Refresh
            )
        }
    }
}

private fun QQMusicLoginMethod.labelRes(): Int = when (this) {
    QQMusicLoginMethod.QQ -> R.string.qq_account_method_qq
    QQMusicLoginMethod.WECHAT -> R.string.qq_account_method_wechat
    QQMusicLoginMethod.PHONE -> R.string.qq_account_method_phone
}

private fun instructionRes(method: QQMusicLoginMethod): Int = when (method) {
    QQMusicLoginMethod.QQ -> R.string.qq_account_instruction_qq
    QQMusicLoginMethod.WECHAT -> R.string.qq_account_instruction_wechat
    QQMusicLoginMethod.PHONE -> R.string.qq_account_phone_instruction
}

private fun statusTextRes(state: QQMusicAccountUiState): Int = when {
    state.error != null -> R.string.qq_account_status_failed
    state.ticket == null -> R.string.qq_account_status_creating
    state.status == QQMusicQrStatus.Scanned -> R.string.qq_account_status_scanned
    state.status == QQMusicQrStatus.Rejected -> R.string.qq_account_status_rejected
    state.status == QQMusicQrStatus.Expired -> R.string.qq_account_status_expired
    state.method == QQMusicLoginMethod.QQ -> R.string.qq_account_status_waiting_qq
    else -> R.string.qq_account_status_waiting_wechat
}

/**
 * 把二维码存进相册。
 *
 * 走 MediaStore 的 `IS_PENDING` 流程（minSdk 30 起步，不需要任何存储权限）：先占位、写完再发布，
 * 中途失败就删掉占位，相册里不会留下半张图。
 *
 * @return 保存位置，供提示文案使用。
 */
private fun saveQrToGallery(context: Context, ticket: QQMusicQrTicket): String {
    val extension = if (ticket.imageMimeType == "image/jpeg") "jpg" else "png"
    val fileName = "Sonic-QQMusic-${System.currentTimeMillis()}.$extension"
    val resolver = context.contentResolver
    val target = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, ticket.imageMimeType)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Sonic"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    ) ?: error("无法在相册中创建文件")

    runCatching {
        resolver.openOutputStream(target)?.use { output -> output.write(ticket.imageBytes) }
            ?: error("无法写入图片")
        resolver.update(
            target,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }.getOrElse { failure ->
        resolver.delete(target, null, null)
        throw failure
    }
    return "Pictures/Sonic"
}
