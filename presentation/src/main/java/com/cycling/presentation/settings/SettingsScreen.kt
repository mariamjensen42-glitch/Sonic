@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.HiResBadgeOverlay
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AudioQuality
import com.cycling.domain.model.CacheUsage
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.ScanResult
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_AUDIO_FADE_DURATION_MS
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import com.cycling.presentation.components.PieChart
import com.cycling.presentation.components.PieChartData
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.onlinesources.OnlineSourcesContent
import com.cycling.presentation.permissions.MediaLibraryPermission
import com.cycling.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import com.cycling.presentation.equalizer.EqualizerContent

private data class QualityStatItemUi(
    val quality: AudioQuality,
    val count: Int,
    val percentage: Float,
    val color: Color
)

/** 带前置图标的列表项，分隔线缩进到文字起始处。 */
private val listDividerIndent: Dp =
    SonSizes.listItemHorizontalPadding + SonSizes.iconMedium + SonSpacing.medium

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onShowToast: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val storagePermissions = MediaLibraryPermission.names

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.handleIntent(SettingsIntent.PermissionResult(allGranted))
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = getFolderPathFromUri(uri)
            if (path != null) {
                viewModel.handleIntent(SettingsIntent.AddExcludedFolder(path))
            } else {
                onShowToast(context.getString(R.string.settings_cannot_get_path))
            }
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.handleIntent(SettingsIntent.ExportBackup(it.toString())) }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleIntent(SettingsIntent.ImportBackup(it.toString())) }
    }

    val m3uImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleIntent(SettingsIntent.ImportM3u(it.toString())) }
    }

    // 选中后先落盘再存路径：SAF 只给一次临时授权，直接存 URI 的话重启就只剩一串打不开的地址。
    val badgeScope = rememberCoroutineScope()
    val badgeImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            badgeScope.launch {
                val path = withContext(Dispatchers.IO) { importHiResBadgeImage(context, uri) }
                if (path != null) {
                    viewModel.handleIntent(SettingsIntent.SetHiResBadgeCustomUri(path))
                    // 选图之后立刻切到自定义档：只存图不切换的话，用户会觉得"点了没反应"。
                    viewModel.handleIntent(SettingsIntent.SetHiResBadge(HiResBadge.CUSTOM))
                } else {
                    onShowToast(context.getString(R.string.settings_hi_res_badge_pick_failed))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndRequestPermission(MediaLibraryPermission.isGranted(context))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowToast -> {
                    val msg = if (effect.messageArg != null) {
                        context.getString(effect.messageResId, effect.messageArg)
                    } else {
                        context.getString(effect.messageResId)
                    }
                    onShowToast(msg)
                }
                is SettingsEffect.RequestStoragePermission -> {
                    permissionLauncher.launch(storagePermissions)
                }
                is SettingsEffect.NavigateBackFromSettings -> onNavigateBack()
            }
        }
    }

    // 二级设置页（扫描 / 排除目录 / API Key / 统计）是这条路由内部的目的地，不是导航路由：
    // 系统返回不拦的话会穿透到 NavDisplay，把整条设置路由弹掉，直接落回首页。
    // 只在二级页接管；主设置页的返回仍交给导航（退出到首页）。
    BackHandler(enabled = uiState.currentDestination != SettingsDestination.MAIN) {
        viewModel.handleIntent(SettingsIntent.NavigateBack)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentDestination) {
                            SettingsDestination.MAIN -> stringResource(R.string.settings_title)
                            SettingsDestination.SCAN -> stringResource(R.string.settings_scan_menu)
                            SettingsDestination.EXCLUDE_FOLDERS -> stringResource(R.string.settings_exclude_menu)
                            SettingsDestination.API_KEY_CONFIG -> stringResource(R.string.settings_api_config_menu)
                            SettingsDestination.LIBRARY_STATS -> stringResource(R.string.settings_library_stats_menu)
                            SettingsDestination.EQUALIZER -> stringResource(R.string.settings_equalizer_menu)
                            SettingsDestination.ONLINE_SOURCES -> stringResource(R.string.settings_online_sources_menu)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(SettingsIntent.NavigateBack) }) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.settings_back_desc)
                        )
                    }
                },
                actions = {
                    if (uiState.currentDestination == SettingsDestination.EXCLUDE_FOLDERS) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            M3SymbolIcon(
                                name = SymbolIconName.Add,
                                filled = true,
                                contentDescription = stringResource(R.string.settings_add_folder_desc)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        when (uiState.currentDestination) {
            SettingsDestination.MAIN -> MainSettingsContent(
                hasApiKey = hasApiKey,
                onlineLyricsEnabled = uiState.onlineLyricsEnabled,
                lyricsProviderOrder = uiState.lyricsProviderOrder,
                playbackSpeed = uiState.playbackSpeed,
                skipSilence = uiState.skipSilence,
                replayGainEnabled = uiState.replayGainEnabled,
                audioFadeDurationMs = uiState.audioFadeDurationMs,
                rememberPlaybackPosition = uiState.rememberPlaybackPosition,
                openPlayerOnPlay = uiState.openPlayerOnPlay,
                autoContinueEnabled = uiState.autoContinueEnabled,
                hiResBadge = uiState.hiResBadge,
                hiResBadgeCustomUri = uiState.hiResBadgeCustomUri,
                onToggleOnlineLyrics = { viewModel.handleIntent(SettingsIntent.SetOnlineLyricsEnabled(it)) },
                onMoveLyricsProvider = { provider, offset ->
                    viewModel.handleIntent(SettingsIntent.MoveLyricsProvider(provider, offset))
                },
                onSetPlaybackSpeed = { viewModel.handleIntent(SettingsIntent.SetPlaybackSpeed(it)) },
                onSetSkipSilence = { viewModel.handleIntent(SettingsIntent.SetSkipSilence(it)) },
                onSetReplayGain = { viewModel.handleIntent(SettingsIntent.SetReplayGainEnabled(it)) },
                onSetAudioFadeDuration = { viewModel.handleIntent(SettingsIntent.SetAudioFadeDuration(it)) },
                onSetRememberPlaybackPosition = {
                    viewModel.handleIntent(SettingsIntent.SetRememberPlaybackPosition(it))
                },
                onSetOpenPlayerOnPlay = {
                    viewModel.handleIntent(SettingsIntent.SetOpenPlayerOnPlay(it))
                },
                onSetAutoContinue = {
                    viewModel.handleIntent(SettingsIntent.SetAutoContinueEnabled(it))
                },
                onSetHiResBadge = { viewModel.handleIntent(SettingsIntent.SetHiResBadge(it)) },
                onClearHiResBadgeCustomUri = {
                    viewModel.handleIntent(SettingsIntent.SetHiResBadgeCustomUri(null))
                    deleteHiResBadgeImage(context, uiState.hiResBadgeCustomUri)
                    // 清掉图片后还停在自定义档就只剩一块空白，退回不显示。
                    if (uiState.hiResBadge == HiResBadge.CUSTOM) {
                        viewModel.handleIntent(SettingsIntent.SetHiResBadge(HiResBadge.OFF))
                    }
                },
                onPickHiResBadgeImage = { badgeImageLauncher.launch(arrayOf("image/*")) },
                sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                onSetSleepTimer = { viewModel.handleIntent(SettingsIntent.SetSleepTimer(it)) },
                onCancelSleepTimer = { viewModel.handleIntent(SettingsIntent.CancelSleepTimer) },
                onNavigateToScan = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) },
                onNavigateToExcludeFolders = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) },
                onNavigateToApiKeyConfig = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) },
                onNavigateToLibraryStats = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) },
                onNavigateToEqualizer = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EQUALIZER)) },
                onNavigateToOnlineSources = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.ONLINE_SOURCES)) },
                onExportBackup = {
                    backupExportLauncher.launch("sonic-backup-${System.currentTimeMillis()}.json")
                },
                onImportBackup = {
                    backupImportLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                onImportM3u = {
                    m3uImportLauncher.launch(
                        arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain", "application/octet-stream")
                    )
                },
                isTransferring = uiState.isTransferring,
                cacheState = CachePanelState(
                    usage = uiState.cacheUsage,
                    limitBytes = uiState.cacheLimitBytes,
                    autoCleanEnabled = uiState.autoCleanCacheEnabled
                ),
                cacheActions = CachePanelActions(
                    onSetLimit = { viewModel.handleIntent(SettingsIntent.SetCacheLimit(it)) },
                    onSetAutoClean = { viewModel.handleIntent(SettingsIntent.SetAutoCleanCache(it)) },
                    onClearDisk = { viewModel.handleIntent(SettingsIntent.ClearDiskCache(it)) },
                    onClearLyrics = { viewModel.handleIntent(SettingsIntent.ClearLyricsCache) },
                    onClearAiInfo = { viewModel.handleIntent(SettingsIntent.ClearAiInfoCache) },
                    onClearAll = { viewModel.handleIntent(SettingsIntent.ClearAllCaches) }
                ),
                bottomPadding = bottomPadding,
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.SCAN -> ScanContent(
                uiState = uiState,
                onStartScan = { viewModel.handleIntent(SettingsIntent.StartScan) },
                onResetScan = { viewModel.handleIntent(SettingsIntent.ResetScan) },
                onRequestPermission = { viewModel.handleIntent(SettingsIntent.RequestPermission) },
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.EXCLUDE_FOLDERS -> ExcludeFoldersContent(
                folders = uiState.excludedFolders,
                isLoading = uiState.isLoadingFolders,
                onRemoveFolder = { viewModel.handleIntent(SettingsIntent.RemoveExcludedFolder(it)) },
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.API_KEY_CONFIG -> ApiKeyConfigContent(
                apiKeyInput = uiState.apiKeyInput,
                hasExistingKey = hasApiKey,
                onApiKeyInputChange = { viewModel.handleIntent(SettingsIntent.UpdateApiKeyInput(it)) },
                onSaveApiKey = { viewModel.handleIntent(SettingsIntent.SaveApiKey) },
                onClearApiKey = { viewModel.handleIntent(SettingsIntent.ClearApiKey) },
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.LIBRARY_STATS -> LibraryStatsContent(
                stats = uiState.libraryStats,
                isLoading = uiState.isLoadingStats,
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.EQUALIZER -> EqualizerContent(
                modifier = Modifier.padding(paddingValues)
            )
            SettingsDestination.ONLINE_SOURCES -> OnlineSourcesContent(
                onShowToast = onShowToast,
                bottomPadding = bottomPadding,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 通用列表组件
// ════════════════════════════════════════════════════════════════

/** 页面头部：圆形品牌图标 + 应用名 + 副标题。 */
@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(SonSizes.thumbnailLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            M3SymbolIcon(
                name = SymbolIconName.GraphicEq,
                filled = true,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(SonSizes.iconLarge)
            )
        }
        Spacer(modifier = Modifier.width(SonSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            M3SymbolIcon(
                name = SymbolIconName.KeyboardArrowRight,
                filled = true,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 可点击的导航型设置项：前置图标 + 标题（可带副标题）+ 尾部箭头。 */
@Composable
private fun SettingsNavigationRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit
) {
    SettingsRow(icon = icon, title = title, supporting = supporting, onClick = onClick) {
        M3SymbolIcon(
            name = SymbolIconName.KeyboardArrowRight,
            filled = true,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 开关型设置项：点击整行或开关本身都能切换；[enabled] 为 false 时整行只读。 */
@Composable
private fun SettingsSwitchRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        }
    ) {
        Switch(
            checked = checked && enabled,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}

/** 设置项骨架：把领域字段映射到 [ListItem] 的插槽上。 */
@Composable
private fun SettingsRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    val supportingSlot: (@Composable () -> Unit)? = supporting?.let { text ->
        {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    val leading: @Composable () -> Unit = {
        M3SymbolIcon(
            name = icon,
            filled = true,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    val headline: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    if (onClick != null) {
        ListItem(
            onClick = onClick,
            leadingContent = leading,
            content = headline,
            supportingContent = supportingSlot,
            trailingContent = trailingContent
        )
    } else {
        ListItem(
            leadingContent = leading,
            headlineContent = headline,
            supportingContent = supportingSlot,
            trailingContent = trailingContent
        )
    }
}

// ════════════════════════════════════════════════════════════════
// 主设置页
// ════════════════════════════════════════════════════════════════

@Composable
private fun MainSettingsContent(
    hasApiKey: Boolean,
    onlineLyricsEnabled: Boolean,
    lyricsProviderOrder: List<LyricsProvider>,
    playbackSpeed: Float,
    skipSilence: Boolean,
    replayGainEnabled: Boolean,
    audioFadeDurationMs: Long,
    rememberPlaybackPosition: Boolean,
    openPlayerOnPlay: Boolean,
    autoContinueEnabled: Boolean,
    hiResBadge: HiResBadge,
    hiResBadgeCustomUri: String?,
    sleepTimerRemainingMs: Long?,
    onToggleOnlineLyrics: (Boolean) -> Unit,
    onMoveLyricsProvider: (LyricsProvider, Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onSetReplayGain: (Boolean) -> Unit,
    onSetAudioFadeDuration: (Long) -> Unit,
    onSetRememberPlaybackPosition: (Boolean) -> Unit,
    onSetOpenPlayerOnPlay: (Boolean) -> Unit,
    onSetAutoContinue: (Boolean) -> Unit,
    onSetHiResBadge: (HiResBadge) -> Unit,
    onClearHiResBadgeCustomUri: () -> Unit,
    onPickHiResBadgeImage: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToExcludeFolders: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit,
    onNavigateToLibraryStats: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToOnlineSources: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportM3u: () -> Unit,
    isTransferring: Boolean,
    cacheState: CachePanelState,
    cacheActions: CachePanelActions,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showFadeDialog by remember { mutableStateOf(false) }
    var showCacheSheet by remember { mutableStateOf(false) }
    var showBadgeDialog by remember { mutableStateOf(false) }
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SettingsHeader(
            title = stringResource(R.string.settings_app_name),
            subtitle = stringResource(R.string.settings_app_subtitle)
        )

        // ── 音乐库 ──
        Text(
            text = stringResource(R.string.settings_section_library),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.Refresh,
                title = stringResource(R.string.settings_scan_local),
                onClick = onNavigateToScan
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.Folder,
                title = stringResource(R.string.settings_exclude_folders),
                onClick = onNavigateToExcludeFolders
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.BarChart,
                title = stringResource(R.string.settings_library_stats),
                onClick = onNavigateToLibraryStats
            )
        }

        // ── 播放 ──
        Text(
            text = stringResource(R.string.settings_section_playback),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.GraphicEq,
                title = stringResource(R.string.settings_equalizer_menu),
                supporting = stringResource(R.string.settings_equalizer_menu_subtitle),
                onClick = onNavigateToEqualizer
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.Speed,
                title = stringResource(R.string.settings_playback_speed),
                supporting = formatSpeed(playbackSpeed),
                onClick = { showSpeedDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.Bedtime,
                title = stringResource(R.string.settings_sleep_timer),
                supporting = sleepTimerRemainingMs?.let {
                    stringResource(R.string.settings_sleep_timer_remaining, formatRemaining(it))
                } ?: stringResource(R.string.settings_sleep_timer_off),
                onClick = { showSleepTimerDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsSwitchRow(
                icon = SymbolIconName.VolumeOff,
                title = stringResource(R.string.settings_skip_silence),
                supporting = stringResource(R.string.settings_skip_silence_subtitle),
                checked = skipSilence,
                onCheckedChange = onSetSkipSilence
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsSwitchRow(
                icon = SymbolIconName.VolumeUp,
                title = stringResource(R.string.settings_replay_gain),
                supporting = stringResource(R.string.settings_replay_gain_subtitle),
                checked = replayGainEnabled,
                onCheckedChange = onSetReplayGain
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.Waves,
                title = stringResource(R.string.settings_audio_fade),
                supporting = formatFadeDuration(audioFadeDurationMs),
                onClick = { showFadeDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsSwitchRow(
                icon = SymbolIconName.History,
                title = stringResource(R.string.settings_remember_position),
                supporting = stringResource(R.string.settings_remember_position_subtitle),
                checked = rememberPlaybackPosition,
                onCheckedChange = onSetRememberPlaybackPosition
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsSwitchRow(
                icon = SymbolIconName.PlayCircle,
                title = stringResource(R.string.settings_open_player_on_play),
                supporting = stringResource(R.string.settings_open_player_on_play_subtitle),
                checked = openPlayerOnPlay,
                onCheckedChange = onSetOpenPlayerOnPlay
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsSwitchRow(
                icon = SymbolIconName.Autorenew,
                title = stringResource(R.string.settings_auto_continue),
                supporting = stringResource(R.string.settings_auto_continue_subtitle),
                checked = autoContinueEnabled,
                onCheckedChange = onSetAutoContinue
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.HighQuality,
                title = stringResource(R.string.settings_hi_res_badge),
                supporting = hiResBadgeLabel(hiResBadge),
                onClick = { showBadgeDialog = true }
            )
        }

        // ── 在线音乐 ──
        Text(
            text = stringResource(R.string.settings_section_online),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.GraphicEq,
                title = stringResource(R.string.settings_online_sources_menu),
                supporting = stringResource(R.string.settings_online_sources_menu_subtitle),
                onClick = onNavigateToOnlineSources
            )
        }

        // ── 歌词 ──
        Text(
            text = stringResource(R.string.settings_section_lyrics),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsSwitchRow(
                icon = SymbolIconName.Lyrics,
                title = stringResource(R.string.settings_online_lyrics),
                supporting = stringResource(R.string.settings_online_lyrics_subtitle),
                checked = onlineLyricsEnabled,
                onCheckedChange = onToggleOnlineLyrics
            )
            if (onlineLyricsEnabled) {
                HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
                LyricsProviderOrder(
                    providers = lyricsProviderOrder,
                    onMoveProvider = onMoveLyricsProvider
                )
            }
        }

        // ── AI ──
        Text(
            text = stringResource(R.string.settings_section_ai),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.Key,
                title = stringResource(R.string.settings_deepseek_config),
                supporting = if (hasApiKey) {
                    stringResource(R.string.settings_api_configured)
                } else {
                    stringResource(R.string.settings_api_not_configured)
                },
                onClick = onNavigateToApiKeyConfig
            )
        }

        // ── 备份与恢复 ──
        Text(
            text = stringResource(R.string.settings_section_backup),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.Backup,
                title = stringResource(R.string.settings_export_backup),
                supporting = stringResource(R.string.settings_export_backup_subtitle),
                onClick = { if (!isTransferring) onExportBackup() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.Restore,
                title = stringResource(R.string.settings_import_backup),
                supporting = if (isTransferring) {
                    stringResource(R.string.settings_transfer_in_progress)
                } else {
                    stringResource(R.string.settings_import_backup_subtitle)
                },
                onClick = { if (!isTransferring) onImportBackup() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            SettingsNavigationRow(
                icon = SymbolIconName.UploadFile,
                title = stringResource(R.string.settings_import_m3u),
                supporting = stringResource(R.string.settings_import_m3u_subtitle),
                onClick = { if (!isTransferring) onImportM3u() }
            )
        }

        // ── 存储 ──
        Text(
            text = stringResource(R.string.settings_section_storage),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsNavigationRow(
                icon = SymbolIconName.Delete,
                title = stringResource(R.string.settings_cache_menu),
                supporting = cacheState.usage?.let {
                    stringResource(R.string.settings_cache_usage, formatCacheSize(it.diskBytes))
                } ?: stringResource(R.string.settings_cache_calculating),
                onClick = { showCacheSheet = true }
            )
        }

        // ── 关于 ──
        Text(
            text = stringResource(R.string.settings_section_about),
            modifier = Modifier.padding(
                start = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.large,
                bottom = SonSpacing.small
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            SettingsRow(
                icon = SymbolIconName.LibraryMusic,
                title = stringResource(R.string.settings_version_title),
                supporting = stringResource(R.string.settings_version_value),
                trailingContent = {}
            )
        }

        Spacer(modifier = Modifier.height(SonLayout.bottomBarClearance + bottomPadding))
    }

    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            speed = playbackSpeed,
            onSpeedChange = onSetPlaybackSpeed,
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            presets = SleepTimerPresetsMinutes,
            isTimerRunning = sleepTimerRemainingMs != null,
            onSelect = onSetSleepTimer,
            onCancelTimer = onCancelSleepTimer,
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showFadeDialog) {
        AudioFadeDialog(
            durationMs = audioFadeDurationMs,
            onDurationChange = onSetAudioFadeDuration,
            onDismiss = { showFadeDialog = false }
        )
    }

    if (showCacheSheet) {
        CacheManagerSheet(
            state = cacheState,
            actions = cacheActions,
            onDismiss = { showCacheSheet = false }
        )
    }

    if (showBadgeDialog) {
        HiResBadgeDialog(
            current = hiResBadge,
            customUri = hiResBadgeCustomUri,
            onSelect = onSetHiResBadge,
            onPickImage = onPickHiResBadgeImage,
            onClearImage = onClearHiResBadgeCustomUri,
            onDismiss = { showBadgeDialog = false }
        )
    }
}

// ════════════════════════════════════════════════════════════════
// 外观：封面小金标 / 歌词界面
// ════════════════════════════════════════════════════════════════

/** 预览列的固定宽度（两张缩略图）：让每一种标的左边缘对齐，标签文字不跟着标牌长短跳动。 */
private val BadgePreviewWidth = SonSizes.thumbnail * 2

@Composable
private fun hiResBadgeLabel(badge: HiResBadge): String = stringResource(
    when (badge) {
        HiResBadge.OFF -> R.string.settings_hi_res_badge_off
        HiResBadge.HI_RES -> R.string.settings_hi_res_badge_hi_res
        HiResBadge.HI_RES_WIRELESS -> R.string.settings_hi_res_badge_wireless
        HiResBadge.GOLD_SEAL -> R.string.settings_hi_res_badge_gold
        HiResBadge.CUSTOM -> R.string.settings_hi_res_badge_custom
    }
)

/**
 * 小金标选择器。
 *
 * 每一项左边就是它贴在封面上的真实样子 —— 这类纯装饰选项靠文字描述挑不出来。
 */
@Composable
private fun HiResBadgeDialog(
    current: HiResBadge,
    customUri: String?,
    onSelect: (HiResBadge) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_hi_res_badge_dialog_title)) },
        text = {
            Column {
                HiResBadgeOption(
                    badge = HiResBadge.OFF,
                    labelRes = R.string.settings_hi_res_badge_off,
                    selected = current == HiResBadge.OFF,
                    customUri = customUri,
                    onClick = { onSelect(HiResBadge.OFF) }
                )
                HiResBadgeOption(
                    badge = HiResBadge.HI_RES,
                    labelRes = R.string.settings_hi_res_badge_hi_res,
                    selected = current == HiResBadge.HI_RES,
                    customUri = customUri,
                    onClick = { onSelect(HiResBadge.HI_RES) }
                )
                HiResBadgeOption(
                    badge = HiResBadge.HI_RES_WIRELESS,
                    labelRes = R.string.settings_hi_res_badge_wireless,
                    selected = current == HiResBadge.HI_RES_WIRELESS,
                    customUri = customUri,
                    onClick = { onSelect(HiResBadge.HI_RES_WIRELESS) }
                )
                HiResBadgeOption(
                    badge = HiResBadge.GOLD_SEAL,
                    labelRes = R.string.settings_hi_res_badge_gold,
                    selected = current == HiResBadge.GOLD_SEAL,
                    customUri = customUri,
                    onClick = { onSelect(HiResBadge.GOLD_SEAL) }
                )
                HiResBadgeOption(
                    badge = HiResBadge.CUSTOM,
                    labelRes = R.string.settings_hi_res_badge_custom,
                    selected = current == HiResBadge.CUSTOM,
                    customUri = customUri,
                    onClick = { onSelect(HiResBadge.CUSTOM) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
                    TextButton(onClick = onPickImage) {
                        Text(stringResource(R.string.settings_hi_res_badge_pick))
                    }
                    if (customUri != null) {
                        TextButton(onClick = onClearImage) {
                            Text(stringResource(R.string.settings_hi_res_badge_clear))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_done))
            }
        }
    )
}

@Composable
private fun HiResBadgeOption(
    badge: HiResBadge,
    @StringRes labelRes: Int,
    selected: Boolean,
    customUri: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SonSpacing.mediumSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(BadgePreviewWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            if (badge == HiResBadge.OFF) {
                M3SymbolIcon(
                    name = SymbolIconName.Close,
                    filled = false,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HiResBadgeOverlay(badge = badge, customUri = customUri)
            }
        }
        Spacer(modifier = Modifier.width(SonSpacing.medium))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}


/** 缓存面板要看的值。 */
private data class CachePanelState(
    val usage: CacheUsage?,
    val limitBytes: Long,
    val autoCleanEnabled: Boolean
)

/** 缓存面板能触发的动作。 */
private data class CachePanelActions(
    val onSetLimit: (Long) -> Unit,
    val onSetAutoClean: (Boolean) -> Unit,
    val onClearDisk: (String) -> Unit,
    val onClearLyrics: () -> Unit,
    val onClearAiInfo: () -> Unit,
    val onClearAll: () -> Unit
)

/** 待确认的清理目标。清理是破坏性的，先问一句。 */
private sealed interface CacheClearTarget {
    data class Disk(val id: String, val label: String) : CacheClearTarget
    data object Lyrics : CacheClearTarget
    data object AiInfo : CacheClearTarget
    data object All : CacheClearTarget
}

/**
 * 缓存管理面板。上半是可调的上限与自动清理，下半是各类缓存的实际占用。
 *
 * 只列会重新长出来的东西。自定义封面、音源脚本属于用户数据，这里连用量都不显示 ——
 * 显示了就等于暗示"可以清"。
 */
@Composable
private fun CacheManagerSheet(
    state: CachePanelState,
    actions: CachePanelActions,
    onDismiss: () -> Unit
) {
    var pendingClear by remember { mutableStateOf<CacheClearTarget?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }
    val usage = state.usage

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SonSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_cache_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = usage?.let {
                            stringResource(R.string.settings_cache_usage, formatCacheSize(it.diskBytes))
                        } ?: stringResource(R.string.settings_cache_calculating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { pendingClear = CacheClearTarget.All },
                    enabled = usage != null
                ) {
                    Text(stringResource(R.string.settings_cache_clear_all))
                }
            }

            Spacer(modifier = Modifier.height(SonSpacing.small))

            CacheRow(
                label = stringResource(R.string.settings_cache_limit),
                supporting = if (state.autoCleanEnabled) {
                    formatCacheSize(state.limitBytes)
                } else {
                    stringResource(R.string.settings_cache_limit_off)
                },
                onClick = { showLimitDialog = true },
                trailing = {
                    M3SymbolIcon(
                        name = SymbolIconName.KeyboardArrowRight,
                        filled = true,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
            CacheRow(
                label = stringResource(R.string.settings_cache_auto_clean),
                supporting = stringResource(R.string.settings_cache_auto_clean_desc),
                onClick = { actions.onSetAutoClean(!state.autoCleanEnabled) },
                trailing = {
                    Switch(
                        checked = state.autoCleanEnabled,
                        onCheckedChange = actions.onSetAutoClean
                    )
                }
            )

            if (usage != null && (usage.diskCaches.isNotEmpty() || usage.lyricsEntries > 0 ||
                    usage.aiInfoEntries > 0)
            ) {
                HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
                usage.diskCaches.forEach { cache ->
                    val label = diskCacheLabel(cache.id)
                    CacheRow(
                        label = label,
                        supporting = formatCacheSize(cache.bytes),
                        onClick = null,
                        trailing = {
                            IconButton(onClick = {
                                pendingClear = CacheClearTarget.Disk(cache.id, label)
                            }) {
                                M3SymbolIcon(
                                    name = SymbolIconName.Delete,
                                    filled = true,
                                    contentDescription = stringResource(R.string.settings_cache_clear_desc)
                                )
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
                }
                CacheRow(
                    label = stringResource(R.string.settings_cache_lyrics),
                    supporting = stringResource(R.string.settings_cache_entries, usage.lyricsEntries),
                    onClick = null,
                    trailing = {
                        IconButton(onClick = { pendingClear = CacheClearTarget.Lyrics }) {
                            M3SymbolIcon(
                                name = SymbolIconName.Delete,
                                filled = true,
                                contentDescription = stringResource(R.string.settings_cache_clear_desc)
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
                CacheRow(
                    label = stringResource(R.string.settings_cache_ai),
                    supporting = stringResource(R.string.settings_cache_entries, usage.aiInfoEntries),
                    onClick = null,
                    trailing = {
                        IconButton(onClick = { pendingClear = CacheClearTarget.AiInfo }) {
                            M3SymbolIcon(
                                name = SymbolIconName.Delete,
                                filled = true,
                                contentDescription = stringResource(R.string.settings_cache_clear_desc)
                            )
                        }
                    }
                )
            }

            Text(
                text = stringResource(R.string.settings_cache_hint),
                modifier = Modifier.padding(top = SonSpacing.medium),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SonSpacing.medium))
        }
    }

    if (showLimitDialog) {
        CacheLimitDialog(
            currentBytes = state.limitBytes,
            onSelect = actions.onSetLimit,
            onDismiss = { showLimitDialog = false }
        )
    }

    pendingClear?.let { target ->
        CacheConfirmDialog(
            target = target,
            onConfirm = {
                when (target) {
                    is CacheClearTarget.Disk -> actions.onClearDisk(target.id)
                    CacheClearTarget.Lyrics -> actions.onClearLyrics()
                    CacheClearTarget.AiInfo -> actions.onClearAiInfo()
                    CacheClearTarget.All -> actions.onClearAll()
                }
            },
            onDismiss = { pendingClear = null }
        )
    }
}

/** 上限档位，MB。 */
private val CacheLimitPresetsMb = listOf(128, 256, 512, 1024, 2048)

@Composable
private fun CacheLimitDialog(currentBytes: Long, onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_limit)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_cache_limit_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SonSpacing.small))
                CacheLimitPresetsMb.forEach { mb ->
                    val bytes = mb.toLong() * 1024 * 1024
                    val selected = bytes == currentBytes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                onSelect(bytes)
                                onDismiss()
                            }
                            .padding(vertical = SonSpacing.mediumSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cache_limit_mb, mb),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selected) {
                            M3SymbolIcon(
                                name = SymbolIconName.Check,
                                filled = true,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cache_cancel))
            }
        }
    )
}

@Composable
private fun CacheConfirmDialog(
    target: CacheClearTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val description = when (target) {
        is CacheClearTarget.Disk ->
            stringResource(R.string.settings_cache_confirm_disk, target.label)
        CacheClearTarget.Lyrics -> stringResource(R.string.settings_cache_confirm_lyrics)
        CacheClearTarget.AiInfo -> stringResource(R.string.settings_cache_confirm_ai)
        CacheClearTarget.All -> stringResource(R.string.settings_cache_confirm_all)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_confirm_title)) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(stringResource(R.string.settings_cache_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cache_cancel))
            }
        }
    )
}

@Composable
private fun CacheRow(
    label: String,
    supporting: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = SonSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}

/** 目录名由各自的库决定（Coil / OkHttp），认识的给中文名，其余原样显示。 */
@Composable
private fun diskCacheLabel(id: String): String = when (id) {
    "image_cache" -> stringResource(R.string.settings_cache_images)
    "http_cache" -> stringResource(R.string.settings_cache_http)
    else -> id
}

private fun formatCacheSize(size: Long): String = when {
    size >= 1024 * 1024 * 1024 ->
        String.format(Locale.getDefault(), "%.2f GB", size.toDouble() / (1024 * 1024 * 1024))
    size >= 1024 * 1024 ->
        String.format(Locale.getDefault(), "%.2f MB", size.toDouble() / (1024 * 1024))
    size >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", size.toDouble() / 1024)
    else -> "$size B"
}

/** 睡眠定时器提供的时长，分钟。 */
private val SleepTimerPresetsMinutes = listOf(15, 30, 45, 60, 90)

@Composable
private fun SleepTimerDialog(
    presets: List<Int>,
    isTimerRunning: Boolean,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_sleep_timer)) },
        text = {
            Column {
                presets.forEach { minutes ->
                    Text(
                        text = stringResource(R.string.sleep_timer_minutes, minutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(minutes)
                                onDismiss()
                            }
                            .padding(vertical = SonSpacing.mediumSmall)
                    )
                }
                if (isTimerRunning) {
                    Text(
                        text = stringResource(R.string.sleep_timer_cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCancelTimer()
                                onDismiss()
                            }
                            .padding(vertical = SonSpacing.mediumSmall)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_done))
            }
        }
    )
}

/** 向上取整到秒：显示 0:00 却还在响会显得定时器坏了。 */
private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/**
 * 播放速度的选择器。
 *
 * 拖动过程中就写回仓库，而不是等确认 —— `PlayerManager` 直接读这个值，所以拖动能立刻听到快慢
 * 变化，这也正是用户拖动它的目的。
 */
@Composable
private fun PlaybackSpeedDialog(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_playback_speed)) },
        text = {
            Column {
                Text(
                    text = formatSpeed(speed),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
                    // 0.5..2.0 之间每 0.1 一档，端点之外还有 14 个中间值
                    steps = 13
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_done))
            }
        }
    )
}

private fun formatSpeed(speed: Float): String =
    String.format(Locale.getDefault(), "%.1fx", speed)

/** 淡入淡出时长。0 不是"零毫秒"，而是关掉 —— 界面上要说清楚，别让人以为是设坏了。 */
@Composable
private fun formatFadeDuration(durationMs: Long): String =
    if (durationMs <= 0L) {
        stringResource(R.string.settings_audio_fade_off)
    } else {
        stringResource(R.string.settings_audio_fade_value, durationMs)
    }

/**
 * 淡入淡出的时长选择器。
 *
 * 与倍速选择器同样在拖动时写回：下一次换曲就能听出差别，不必等确认。
 */
@Composable
private fun AudioFadeDialog(
    durationMs: Long,
    onDurationChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_audio_fade)) },
        text = {
            Column {
                Text(
                    text = formatFadeDuration(durationMs),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Slider(
                    value = durationMs.toFloat(),
                    onValueChange = { onDurationChange(it.toLong()) },
                    valueRange = 0f..MAX_AUDIO_FADE_DURATION_MS.toFloat(),
                    // 每 100ms 一档：0 / 100 / … / 2000，端点之外还有 19 个中间值
                    steps = (MAX_AUDIO_FADE_DURATION_MS / 100L).toInt() - 1
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_done))
            }
        }
    )
}

/**
 * The provider priority list, in the order the repository tries them.
 *
 * Reordering rather than picking one winner is the whole point: the list is a fallback chain, so a
 * NetEase outage degrades to LRCLIB instead of to no lyrics.
 */
@Composable
private fun LyricsProviderOrder(
    providers: List<LyricsProvider>,
    onMoveProvider: (LyricsProvider, Int) -> Unit
) {
    Text(
        text = stringResource(R.string.settings_lyrics_provider_order),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = SonSizes.listItemHorizontalPadding,
            top = SonSpacing.mediumSmall,
            bottom = SonSpacing.extraSmall
        )
    )
    providers.forEachIndexed { index, provider ->
        LyricsProviderRow(
            position = index + 1,
            provider = provider,
            canMoveUp = index > 0,
            canMoveDown = index < providers.lastIndex,
            onMove = { offset -> onMoveProvider(provider, offset) }
        )
    }
    Spacer(modifier = Modifier.height(SonSpacing.small))
}

@Composable
private fun LyricsProviderRow(
    position: Int,
    provider: LyricsProvider,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonSizes.listItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(SonSizes.iconLarge)
        )
        Text(
            text = stringResource(provider.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onMove(-1) },
            enabled = canMoveUp
        ) {
            M3SymbolIcon(
                name = SymbolIconName.KeyboardArrowUp,
                filled = true,
                contentDescription = stringResource(R.string.settings_lyrics_provider_move_up),
                modifier = Modifier.size(SonSizes.iconSmall)
            )
        }
        IconButton(
            onClick = { onMove(1) },
            enabled = canMoveDown
        ) {
            M3SymbolIcon(
                name = SymbolIconName.KeyboardArrowDown,
                filled = true,
                contentDescription = stringResource(R.string.settings_lyrics_provider_move_down),
                modifier = Modifier.size(SonSizes.iconSmall)
            )
        }
    }
}

@StringRes
private fun LyricsProvider.labelRes(): Int = when (this) {
    LyricsProvider.AMLL -> R.string.settings_lyrics_provider_amll
    LyricsProvider.NETEASE -> R.string.settings_lyrics_provider_netease
    LyricsProvider.QQ_MUSIC -> R.string.settings_lyrics_provider_qq_music
    LyricsProvider.KUGOU -> R.string.settings_lyrics_provider_kugou
    LyricsProvider.LRCLIB -> R.string.settings_lyrics_provider_lrclib
}

// ════════════════════════════════════════════════════════════════
// 扫描
// ════════════════════════════════════════════════════════════════

@Composable
private fun ScanContent(
    uiState: SettingsUiState,
    onStartScan: () -> Unit,
    onResetScan: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        !uiState.hasStoragePermission && uiState.shouldRequestPermission -> {
            EmptyStateContent(
                icon = SymbolIconName.Error,
                title = stringResource(R.string.settings_permission_title),
                subtitle = stringResource(R.string.settings_permission_subtitle),
                modifier = modifier
            ) {
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
        uiState.scanResult != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(SonSpacing.medium))
                ScanResultContent(uiState.scanResult)
                Spacer(modifier = Modifier.height(SonSpacing.medium))
                FilledTonalButton(
                    onClick = onResetScan,
                    modifier = Modifier
                        .padding(horizontal = SonLayout.screenMargin)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_rescan))
                }
                Spacer(modifier = Modifier.height(SonLayout.bottomBarClearance))
            }
        }
        uiState.isScanning -> {
            ScanningProgressContent(uiState, modifier)
        }
        uiState.scanErrorResId != null -> {
            EmptyStateContent(
                icon = SymbolIconName.Error,
                title = stringResource(R.string.scan_failed),
                subtitle = stringResource(uiState.scanErrorResId!!),
                modifier = modifier
            ) {
                Button(onClick = onStartScan) {
                    Text(stringResource(R.string.settings_retry))
                }
            }
        }
        else -> {
            EmptyStateContent(
                icon = SymbolIconName.Refresh,
                title = stringResource(R.string.settings_scan_empty_title),
                subtitle = stringResource(R.string.settings_scan_empty_subtitle),
                modifier = modifier
            ) {
                Button(onClick = onStartScan) {
                    Text(stringResource(R.string.scan_start))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: SymbolIconName,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SonSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        M3SymbolIcon(
            name = icon,
            filled = false,
            contentDescription = null,
            modifier = Modifier.size(SonSizes.iconExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SonSpacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(SonSpacing.large))
        action()
    }
}

@Composable
private fun ScanResultContent(result: ScanResult) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Text(
                text = stringResource(R.string.settings_scan_complete),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
            ScanStatRow(stringResource(R.string.settings_songs_found), stringResource(R.string.settings_songs_count, result.songsFound))
            ScanStatRow(stringResource(R.string.settings_albums_found), stringResource(R.string.settings_albums_count, result.albumsFound))
            ScanStatRow(stringResource(R.string.settings_artists_found), stringResource(R.string.settings_artists_count, result.artistsFound))
            ScanStatRow(stringResource(R.string.settings_time_elapsed), stringResource(R.string.settings_milliseconds, result.duration))
        }
    }
}

@Composable
private fun ScanStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ScanningProgressContent(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SonSpacing.medium)
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(SonSpacing.medium)) {
                Text(
                    text = stringResource(R.string.settings_scanning),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(SonSpacing.medium))
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { uiState.scanProgress }
                )
                Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
                Text(
                    text = stringResource(R.string.settings_scan_progress, uiState.songsProcessed, uiState.totalSongs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (uiState.scanStep) {
                        SettingsScanStep.QueryingMediaStore -> stringResource(R.string.settings_scan_querying)
                        SettingsScanStep.SavingToDatabase -> stringResource(R.string.settings_scan_saving)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 排除文件夹
// ════════════════════════════════════════════════════════════════

@Composable
private fun ExcludeFoldersContent(
    folders: List<ExcludedFolder>,
    isLoading: Boolean,
    onRemoveFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        // 排除列表存在 DataStore 里，读一次是毫秒级：延迟现身，别闪一帧转圈。
        isLoading -> SonLoadingState(modifier = modifier)
        folders.isEmpty() -> EmptyStateContent(
            icon = SymbolIconName.Folder,
            title = stringResource(R.string.settings_empty_exclude_title),
            subtitle = stringResource(R.string.settings_empty_exclude_subtitle),
            modifier = modifier
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = SonSpacing.small,
                bottom = SonLayout.bottomBarClearance
            )
        ) {
            items(folders, key = { it.path }) { folder ->
                ExcludedFolderItem(
                    folder = folder,
                    onDelete = { onRemoveFolder(folder.path) }
                )
            }
        }
    }
}

@Composable
private fun ExcludedFolderItem(
    folder: ExcludedFolder,
    onDelete: () -> Unit
) {
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(SonSizes.iconExtraLarge)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Folder,
                    filled = true,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            }
        },
        headlineContent = {
            Text(
                text = getFolderDisplayName(folder.path),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Delete,
                    filled = true,
                    contentDescription = stringResource(R.string.settings_delete_desc)
                )
            }
        }
    )
}

// ════════════════════════════════════════════════════════════════
// API Key 配置
// ════════════════════════════════════════════════════════════════

@Composable
private fun ApiKeyConfigContent(
    apiKeyInput: String,
    hasExistingKey: Boolean,
    onApiKeyInputChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SonSpacing.medium)
            .imePadding()
    ) {
        if (hasExistingKey) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(SonSpacing.mediumSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.CheckCircle,
                    filled = true,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
                Spacer(Modifier.width(SonSpacing.small))
                Text(
                    text = stringResource(R.string.settings_api_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(SonSpacing.medium))
        }

        Text(
            text = stringResource(R.string.settings_api_key_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(SonSpacing.small))

        Text(
            text = stringResource(R.string.settings_api_key_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        key(showApiKey) {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(SonSpacing.small))

        TextButton(onClick = { showApiKey = !showApiKey }) {
            Text(
                text = if (showApiKey) {
                    stringResource(R.string.settings_hide_api_key)
                } else {
                    stringResource(R.string.settings_show_api_key)
                }
            )
        }

        Spacer(modifier = Modifier.height(SonSpacing.large))

        Button(
            onClick = onSaveApiKey,
            enabled = apiKeyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_save))
        }

        if (hasExistingKey) {
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            OutlinedButton(
                onClick = onClearApiKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_clear_api_key))
            }
        }

        Spacer(modifier = Modifier.height(SonLayout.bottomBarClearance))
    }
}

// ════════════════════════════════════════════════════════════════
// 音乐库统计
// ════════════════════════════════════════════════════════════════

@Composable
private fun LibraryStatsContent(
    stats: LibraryStats?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        // 统计是本地 Room 聚合（毫秒级）：延迟现身，避免一闪而过。
        isLoading -> SonLoadingState(modifier = modifier)
        stats != null && stats.totalSongs > 0 -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SonSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChartSection(stats = stats)
            Spacer(modifier = Modifier.height(SonSpacing.large))
            StatsDetailSection(stats = stats)
            Spacer(modifier = Modifier.height(SonLayout.bottomBarClearance))
        }
        else -> EmptyStateContent(
            icon = SymbolIconName.LibraryMusic,
            title = stringResource(R.string.settings_stats_empty_title),
            subtitle = stringResource(R.string.settings_stats_empty_subtitle),
            modifier = modifier
        )
    }
}

/** 各音质档位对应的图表语义色，全部取自主题角色。 */
@Composable
private fun qualityColor(quality: AudioQuality): Color = when (quality) {
    AudioQuality.HR -> MaterialTheme.colorScheme.primary
    AudioQuality.SQ -> MaterialTheme.colorScheme.tertiary
    AudioQuality.HQ -> MaterialTheme.colorScheme.secondary
    AudioQuality.OTHERS -> MaterialTheme.colorScheme.outline
}

@Composable
private fun ChartSection(stats: LibraryStats) {
    var selectedIndex by remember { mutableStateOf(-1) }

    val pieData = listOf(
        PieChartData(stats.hrCount.toFloat(), qualityColor(AudioQuality.HR), "HR"),
        PieChartData(stats.sqCount.toFloat(), qualityColor(AudioQuality.SQ), "SQ"),
        PieChartData(stats.hqCount.toFloat(), qualityColor(AudioQuality.HQ), "HQ"),
        PieChartData(stats.othersCount.toFloat(), qualityColor(AudioQuality.OTHERS), "Others")
    ).filter { it.value > 0 }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SonSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.settings_audio_quality_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SonSpacing.medium))

            PieChart(
                data = pieData,
                selectedIndex = selectedIndex,
                onSliceClick = { selectedIndex = it }
            )

            Spacer(modifier = Modifier.height(SonSpacing.medium))

            Text(
                text = stringResource(R.string.settings_stats_total, stats.totalSongs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedIndex in pieData.indices) {
                Spacer(modifier = Modifier.height(SonSpacing.small))
                val selected = pieData[selectedIndex]
                val percentage = (selected.value / stats.totalSongs.toFloat() * 100).toInt()
                Text(
                    text = "${selected.label}: ${percentage}%",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = selected.color
                )
            }
        }
    }
}

@Composable
private fun StatsDetailSection(stats: LibraryStats) {
    val items = listOf(
        QualityStatItemUi(AudioQuality.HR, stats.hrCount, stats.hrPercentage, qualityColor(AudioQuality.HR)),
        QualityStatItemUi(AudioQuality.SQ, stats.sqCount, stats.sqPercentage, qualityColor(AudioQuality.SQ)),
        QualityStatItemUi(AudioQuality.HQ, stats.hqCount, stats.hqPercentage, qualityColor(AudioQuality.HQ)),
        QualityStatItemUi(AudioQuality.OTHERS, stats.othersCount, stats.othersPercentage, qualityColor(AudioQuality.OTHERS))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_stats_detail),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = SonSpacing.mediumSmall)
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                StatItemRow(item = item)
                if (index < items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = listDividerIndent))
                }
            }
        }
    }
}

@Composable
private fun StatItemRow(item: QualityStatItemUi) {
    val (icon, title, description) = when (item.quality) {
        AudioQuality.HR -> Triple(
            SymbolIconName.GraphicEq,
            stringResource(R.string.settings_quality_hr),
            stringResource(R.string.settings_quality_hr_desc)
        )
        AudioQuality.SQ -> Triple(
            SymbolIconName.HighQuality,
            stringResource(R.string.settings_quality_sq),
            stringResource(R.string.settings_quality_sq_desc)
        )
        AudioQuality.HQ -> Triple(
            SymbolIconName.BarChart,
            stringResource(R.string.settings_quality_hq),
            stringResource(R.string.settings_quality_hq_desc)
        )
        AudioQuality.OTHERS -> Triple(
            SymbolIconName.LibraryMusic,
            stringResource(R.string.settings_quality_others),
            stringResource(R.string.settings_quality_others_desc)
        )
    }

    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(SonSizes.iconExtraLarge)
                    .clip(MaterialTheme.shapes.small)
                    .background(item.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = icon,
                    filled = true,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(SonSizes.iconMedium)
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.settings_quality_count, item.count),
                    style = MaterialTheme.typography.labelLarge,
                    color = item.color
                )
                Text(
                    text = "%.1f%%".format(item.percentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

// ════════════════════════════════════════════════════════════════
// 工具函数
// ════════════════════════════════════════════════════════════════

/** 小金标图片的私有目录名。放在 files/ 下，随应用卸载一起清掉。 */
private const val HiResBadgeDir = "hi_res_badge"

/**
 * 把用户选的图片拷进应用私有目录，返回 `file://` URI（存的就是它）。
 *
 * 不存 SAF 的 content:// ：那是临时授权，系统随时可以收回，用户重启后只会看到一块空白。
 * 旧图一并删掉，否则换几次就在私有目录里堆几张几 MB 的图。
 */
private fun importHiResBadgeImage(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, HiResBadgeDir).apply { mkdirs() }
    val target = File(dir, "badge_${System.currentTimeMillis()}")
    val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
    input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }
    dir.listFiles()?.forEach { if (it != target) it.delete() }
    Uri.fromFile(target).toString()
}.getOrNull()

/** 删除小金标图片；只认自己目录下的路径，避免一条脏数据把别处的文件删掉。 */
private fun deleteHiResBadgeImage(context: Context, uriString: String?) {
    val path = uriString?.let { Uri.parse(it).path } ?: return
    if (!path.startsWith(File(context.filesDir, HiResBadgeDir).path)) return
    runCatching { File(path).delete() }
}

private fun getFolderPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null
    val treeDocumentId = path.substringAfterLast(":")
    return "/storage/emulated/0/$treeDocumentId"
}

private fun getFolderDisplayName(path: String): String {
    return path.substringAfterLast("/")
}
