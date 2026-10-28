@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleGroupedSection
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleSearchField
import com.cycling.core.ui.apple.AppleSettingsSectionTitle
import com.cycling.core.ui.apple.AppleSettingsSeparator
import com.cycling.core.ui.apple.AppleSettingsNavRow as SettingsNavigationRow
import com.cycling.core.ui.apple.AppleSettingsRow as SettingsRow
import com.cycling.core.ui.apple.AppleSettingsSwitchRow as SettingsSwitchRow
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleTopBar
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SonGlyph
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
import com.cycling.presentation.account.QQMusicAccountContent
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
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToMetadataScrape: () -> Unit = {},
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

    val destination = uiState.currentDestination

    Scaffold(
        topBar = {
            AppleTopBar(
                title = when (destination) {
                    SettingsDestination.MAIN -> stringResource(R.string.settings_title)
                    SettingsDestination.SCAN -> stringResource(R.string.settings_scan_menu)
                    SettingsDestination.EXCLUDE_FOLDERS -> stringResource(R.string.settings_exclude_menu)
                    SettingsDestination.API_KEY_CONFIG -> stringResource(R.string.settings_api_config_menu)
                    SettingsDestination.LIBRARY_STATS -> stringResource(R.string.settings_library_stats_menu)
                    SettingsDestination.EQUALIZER -> stringResource(R.string.settings_equalizer_menu)
                    SettingsDestination.ONLINE_SOURCES -> stringResource(R.string.settings_online_sources_menu)
                    SettingsDestination.QQ_MUSIC_ACCOUNT -> stringResource(R.string.settings_qq_account_menu)
                },
                onNavigateBack = { viewModel.handleIntent(SettingsIntent.NavigateBack) },
                backContentDescription = stringResource(R.string.settings_back_desc),
                actions = {
                    if (destination == SettingsDestination.EXCLUDE_FOLDERS) {
                        AppleIconButton(
                            icon = SymbolIconName.Add,
                            contentDescription = stringResource(R.string.settings_add_folder_desc),
                            onClick = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (destination) {
            SettingsDestination.MAIN -> MainSettingsContent(
                hasApiKey = hasApiKey,
                qqAccountNickname = uiState.qqMusicAccount?.nickname,
                excludeRecordings = uiState.excludeRecordings,
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
                onSetExcludeRecordings = { viewModel.handleIntent(SettingsIntent.SetExcludeRecordings(it)) },
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
                onNavigateToQqAccount = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.QQ_MUSIC_ACCOUNT)) },
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToMetadataScrape = onNavigateToMetadataScrape,
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
            SettingsDestination.QQ_MUSIC_ACCOUNT -> QQMusicAccountContent(
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

// 导航型 / 开关型 / 骨架行已上提到 core/ui/apple/AppleSettings.kt，
// 与设置页的二级页（均衡器、在线源）共用一份实现。

/**
 * 主设置页的水平页边距。
 *
 * 取 16dp 是为了和 `AppleTopBar` 里大标题的左边缘（`AppleDims.padding` = 16dp）对齐 ——
 * MeloX 那边能用 20dp，是因为它的顶栏自身没有内边距。
 */
private val SettingsPageMargin = 16.dp

/** 主设置页的分区标题到卡片的间距。 */
private val SettingsSectionGap = 20.dp

/**
 * 主设置页的一行。
 *
 * 同一份模型既给渲染用，也给搜索过滤提供匹配文本 —— 行是内联声明的话，
 * 想让搜索框「找得到但点不进去」几乎不可能，所以这里把行抽成数据。
 */
private class SettingsEntry(
    val title: String,
    val supporting: String? = null,
    /** 标题里没有、但用户可能输入的检索词（EQ / API / M3U 之类）。 */
    val keywords: String = "",
    val icon: SymbolIconName? = null,
    val glyph: SonGlyph? = null,
    val checked: Boolean? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    /** 不是标准行时（歌词源排序）整块自定义内容。 */
    val custom: (@Composable () -> Unit)? = null
) {
    fun matches(query: String): Boolean =
        query.isBlank() || "$title ${supporting.orEmpty()} $keywords".contains(query, ignoreCase = true)
}

private class SettingsSectionSpec(val title: String, val entries: List<SettingsEntry>)

/** 把 [SettingsEntry] 渲染成对应的行组件。 */
@Composable
private fun SettingsEntryRow(entry: SettingsEntry) {
    val custom = entry.custom
    if (custom != null) {
        custom()
        return
    }
    val onClick = entry.onClick
    val checked = entry.checked
    val onChange = entry.onCheckedChange
    when {
        checked != null && onChange != null -> {
            val icon = entry.icon ?: return
            SettingsSwitchRow(
                icon = icon,
                title = entry.title,
                supporting = entry.supporting,
                checked = checked,
                enabled = entry.enabled,
                onCheckedChange = onChange
            )
        }

        onClick != null -> when {
            entry.icon != null -> SettingsNavigationRow(
                icon = entry.icon,
                title = entry.title,
                supporting = entry.supporting,
                onClick = onClick
            )

            entry.glyph != null -> SettingsNavigationRow(
                glyph = entry.glyph,
                title = entry.title,
                supporting = entry.supporting,
                onClick = onClick
            )

            else -> Unit
        }

        // 纯信息行：没有动作，行尾也不放箭头。
        entry.icon != null -> SettingsRow(
            icon = entry.icon,
            title = entry.title,
            supporting = entry.supporting
        )

        entry.glyph != null -> SettingsRow(
            glyph = entry.glyph,
            title = entry.title,
            supporting = entry.supporting
        )
    }
}

/**
 * 一个分区：[title] + 玻璃分组卡。行分隔线由这里统一插入，
 * 因此「过滤后只剩一行」的情况不会留下一条孤零零的线。
 */
@Composable
private fun SettingsSectionBlock(section: SettingsSectionSpec, visible: List<SettingsEntry>) {
    if (visible.isEmpty()) return
    AppleSettingsSectionTitle(section.title)
    AppleGroupedSection(modifier = Modifier.fillMaxWidth()) {
        visible.forEachIndexed { index, entry ->
            if (index > 0) AppleSettingsSeparator()
            key(entry.title) { SettingsEntryRow(entry) }
        }
    }
    Spacer(modifier = Modifier.height(SettingsSectionGap))
}

// ════════════════════════════════════════════════════════════════
// 主设置页
// ════════════════════════════════════════════════════════════════

@Composable
private fun MainSettingsContent(
    hasApiKey: Boolean,
    qqAccountNickname: String?,
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
    excludeRecordings: Boolean,
    onSetExcludeRecordings: (Boolean) -> Unit,
    onNavigateToApiKeyConfig: () -> Unit,
    onNavigateToLibraryStats: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToOnlineSources: () -> Unit,
    onNavigateToQqAccount: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToMetadataScrape: () -> Unit,
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
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.trim()

    val sections = listOf(
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_library),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_scan_local),
                    keywords = "scan 扫描",
                    icon = SymbolIconName.Refresh,
                    onClick = onNavigateToScan
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_exclude_recordings),
                    supporting = stringResource(R.string.settings_exclude_recordings_subtitle),
                    keywords = "录音 mic",
                    icon = SymbolIconName.Mic,
                    checked = excludeRecordings,
                    onCheckedChange = onSetExcludeRecordings
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_exclude_folders),
                    keywords = "目录 folder",
                    icon = SymbolIconName.Folder,
                    onClick = onNavigateToExcludeFolders
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_library_stats),
                    keywords = "统计 stats",
                    icon = SymbolIconName.BarChart,
                    onClick = onNavigateToLibraryStats
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_playback),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_equalizer_menu),
                    supporting = stringResource(R.string.settings_equalizer_menu_subtitle),
                    keywords = "均衡器 eq",
                    icon = SymbolIconName.GraphicEq,
                    onClick = onNavigateToEqualizer
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_playback_speed),
                    supporting = formatSpeed(playbackSpeed),
                    keywords = "倍速 speed",
                    icon = SymbolIconName.Speed,
                    onClick = { showSpeedDialog = true }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_sleep_timer),
                    supporting = sleepTimerRemainingMs?.let {
                        stringResource(R.string.settings_sleep_timer_remaining, formatRemaining(it))
                    } ?: stringResource(R.string.settings_sleep_timer_off),
                    keywords = "定时 sleep",
                    icon = SymbolIconName.Bedtime,
                    onClick = { showSleepTimerDialog = true }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_skip_silence),
                    supporting = stringResource(R.string.settings_skip_silence_subtitle),
                    keywords = "静音 silence",
                    icon = SymbolIconName.VolumeOff,
                    checked = skipSilence,
                    onCheckedChange = onSetSkipSilence
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_replay_gain),
                    supporting = stringResource(R.string.settings_replay_gain_subtitle),
                    keywords = "响度 gain",
                    icon = SymbolIconName.VolumeUp,
                    checked = replayGainEnabled,
                    onCheckedChange = onSetReplayGain
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_audio_fade),
                    supporting = formatFadeDuration(audioFadeDurationMs),
                    keywords = "淡入淡出 fade",
                    icon = SymbolIconName.Waves,
                    onClick = { showFadeDialog = true }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_remember_position),
                    supporting = stringResource(R.string.settings_remember_position_subtitle),
                    keywords = "记忆 进度",
                    icon = SymbolIconName.History,
                    checked = rememberPlaybackPosition,
                    onCheckedChange = onSetRememberPlaybackPosition
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_open_player_on_play),
                    supporting = stringResource(R.string.settings_open_player_on_play_subtitle),
                    keywords = "播放页 player",
                    icon = SymbolIconName.PlayCircle,
                    checked = openPlayerOnPlay,
                    onCheckedChange = onSetOpenPlayerOnPlay
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_auto_continue),
                    supporting = stringResource(R.string.settings_auto_continue_subtitle),
                    keywords = "连播 continue",
                    icon = SymbolIconName.Autorenew,
                    checked = autoContinueEnabled,
                    onCheckedChange = onSetAutoContinue
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_hi_res_badge),
                    supporting = hiResBadgeLabel(hiResBadge),
                    keywords = "小金标 hires",
                    icon = SymbolIconName.HighQuality,
                    onClick = { showBadgeDialog = true }
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_online),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_online_sources_menu),
                    supporting = stringResource(R.string.settings_online_sources_menu_subtitle),
                    keywords = "音源 脚本 lx",
                    icon = SymbolIconName.GraphicEq,
                    onClick = onNavigateToOnlineSources
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_qq_account_menu),
                    // 登录后直接显示昵称：这一行是账号是否真的生效的唯一常驻反馈。
                    supporting = qqAccountNickname?.let {
                        stringResource(R.string.settings_qq_account_signed_in, it)
                    } ?: stringResource(R.string.settings_qq_account_menu_subtitle),
                    keywords = "qq 账号 登录 login ${qqAccountNickname.orEmpty()}",
                    icon = SymbolIconName.Person,
                    onClick = onNavigateToQqAccount
                ),
                SettingsEntry(
                    // 字体子集里没有下载码位，走手绘字形；Folder 在这里会被读成「文件管理」。
                    title = stringResource(R.string.settings_downloads_menu),
                    supporting = stringResource(R.string.settings_downloads_menu_subtitle),
                    keywords = "下载 download",
                    glyph = SonGlyph.Download,
                    onClick = onNavigateToDownloads
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_metadata_scrape),
                    supporting = stringResource(R.string.settings_metadata_scrape_subtitle),
                    keywords = "刮削 标签 scrape",
                    icon = SymbolIconName.Tune,
                    onClick = onNavigateToMetadataScrape
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_lyrics),
            entries = listOfNotNull(
                SettingsEntry(
                    title = stringResource(R.string.settings_online_lyrics),
                    supporting = stringResource(R.string.settings_online_lyrics_subtitle),
                    keywords = "歌词 lyrics",
                    icon = SymbolIconName.Lyrics,
                    checked = onlineLyricsEnabled,
                    onCheckedChange = onToggleOnlineLyrics
                ),
                if (onlineLyricsEnabled) {
                    SettingsEntry(
                        title = stringResource(R.string.settings_lyrics_provider_order),
                        keywords = "优先级 排序 provider",
                        custom = {
                            LyricsProviderOrder(
                                providers = lyricsProviderOrder,
                                onMoveProvider = onMoveLyricsProvider
                            )
                        }
                    )
                } else {
                    null
                }
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_ai),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_deepseek_config),
                    supporting = if (hasApiKey) {
                        stringResource(R.string.settings_api_configured)
                    } else {
                        stringResource(R.string.settings_api_not_configured)
                    },
                    keywords = "api key deepseek",
                    icon = SymbolIconName.Key,
                    onClick = onNavigateToApiKeyConfig
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_backup),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_export_backup),
                    supporting = stringResource(R.string.settings_export_backup_subtitle),
                    keywords = "导出 backup",
                    icon = SymbolIconName.Backup,
                    enabled = !isTransferring,
                    onClick = { if (!isTransferring) onExportBackup() }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_import_backup),
                    supporting = if (isTransferring) {
                        stringResource(R.string.settings_transfer_in_progress)
                    } else {
                        stringResource(R.string.settings_import_backup_subtitle)
                    },
                    keywords = "导入 restore",
                    icon = SymbolIconName.Restore,
                    enabled = !isTransferring,
                    onClick = { if (!isTransferring) onImportBackup() }
                ),
                SettingsEntry(
                    title = stringResource(R.string.settings_import_m3u),
                    supporting = stringResource(R.string.settings_import_m3u_subtitle),
                    keywords = "播放列表 m3u",
                    icon = SymbolIconName.UploadFile,
                    enabled = !isTransferring,
                    onClick = { if (!isTransferring) onImportM3u() }
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_storage),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_cache_menu),
                    supporting = cacheState.usage?.let {
                        stringResource(R.string.settings_cache_usage, formatCacheSize(it.diskBytes))
                    } ?: stringResource(R.string.settings_cache_calculating),
                    keywords = "缓存 cache 清理",
                    icon = SymbolIconName.Delete,
                    onClick = { showCacheSheet = true }
                )
            )
        ),
        SettingsSectionSpec(
            title = stringResource(R.string.settings_section_about),
            entries = listOf(
                SettingsEntry(
                    title = stringResource(R.string.settings_version_title),
                    supporting = stringResource(R.string.settings_version_value),
                    keywords = "版本 version",
                    icon = SymbolIconName.LibraryMusic
                )
            )
        )
    )

    val filtered = sections.mapNotNull { section ->
        section.entries
            .filter { it.matches(query) || section.title.contains(query, ignoreCase = true) }
            .takeIf { it.isNotEmpty() }
            ?.let { section to it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsPageMargin)
            .padding(top = 8.dp, bottom = appleBottomBarClearance() + bottomPadding)
    ) {
        AppleSearchField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.settings_search_placeholder),
            onClear = { search = "" }
        )
        Spacer(modifier = Modifier.height(SettingsSectionGap))

        // 应用标识块 —— 对应 MeloX 设置页顶部的账号卡，位置与分量一致。
        AppleGroupedSection(modifier = Modifier.fillMaxWidth()) {
            SettingsRow(
                icon = SymbolIconName.GraphicEq,
                title = stringResource(R.string.settings_app_name),
                supporting = stringResource(R.string.settings_app_subtitle)
            )
        }
        Spacer(modifier = Modifier.height(SettingsSectionGap))

        filtered.forEach { (section, visible) ->
            SettingsSectionBlock(section = section, visible = visible)
        }

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_empty),
                modifier = Modifier.padding(vertical = 24.dp),
                style = AppleType.subhead,
                color = AppleTheme.colors.secondaryLabel
            )
        }
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
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(title = stringResource(R.string.settings_hi_res_badge_dialog_title))
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

        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppleTextAction(
                label = stringResource(R.string.settings_hi_res_badge_pick),
                onClick = onPickImage
            )
            if (customUri != null) {
                AppleTextAction(
                    label = stringResource(R.string.settings_hi_res_badge_clear),
                    onClick = onClearImage,
                    tint = AppleTheme.colors.systemRed
                )
            }
        }
    }
}

@Composable
private fun HiResBadgeOption(
    badge: HiResBadge,
    @StringRes labelRes: Int,
    selected: Boolean,
    customUri: String?,
    onClick: () -> Unit
) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    tint = colors.tertiaryLabel
                )
            } else {
                HiResBadgeOverlay(badge = badge, customUri = customUri)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(labelRes),
            style = AppleType.body,
            color = colors.label,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            M3SymbolIcon(
                name = SymbolIconName.Check,
                filled = true,
                contentDescription = null,
                tint = colors.key,
                modifier = Modifier.size(20.dp)
            )
        }
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

    AppleSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_cache_title),
                        style = AppleType.title2Emphasized,
                        color = AppleTheme.colors.label
                    )
                    Text(
                        text = usage?.let {
                            stringResource(R.string.settings_cache_usage, formatCacheSize(it.diskBytes))
                        } ?: stringResource(R.string.settings_cache_calculating),
                        style = AppleType.footnote,
                        color = AppleTheme.colors.secondaryLabel
                    )
                }
                if (usage != null) {
                    AppleTextAction(
                        label = stringResource(R.string.settings_cache_clear_all),
                        onClick = { pendingClear = CacheClearTarget.All },
                        tint = AppleTheme.colors.systemRed
                    )
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
                        tint = AppleTheme.colors.secondaryLabel
                    )
                }
            )
            AppleSeparator(leadingInset = listDividerIndent)
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
                AppleSeparator(leadingInset = listDividerIndent)
                usage.diskCaches.forEach { cache ->
                    val label = diskCacheLabel(cache.id)
                    CacheRow(
                        label = label,
                        supporting = formatCacheSize(cache.bytes),
                        onClick = null,
                        trailing = {
                            AppleIconButton(
                                icon = SymbolIconName.Delete,
                                contentDescription = stringResource(R.string.settings_cache_clear_desc),
                                onClick = { pendingClear = CacheClearTarget.Disk(cache.id, label) },
                                tint = AppleTheme.colors.systemRed,
                                size = 20.dp
                            )
                        }
                    )
                    AppleSeparator(leadingInset = listDividerIndent)
                }
                CacheRow(
                    label = stringResource(R.string.settings_cache_lyrics),
                    supporting = stringResource(R.string.settings_cache_entries, usage.lyricsEntries),
                    onClick = null,
                    trailing = {
                        AppleIconButton(
                            icon = SymbolIconName.Delete,
                            contentDescription = stringResource(R.string.settings_cache_clear_desc),
                            onClick = { pendingClear = CacheClearTarget.Lyrics },
                            tint = AppleTheme.colors.systemRed,
                            size = 20.dp
                        )
                    }
                )
                AppleSeparator(leadingInset = listDividerIndent)
                CacheRow(
                    label = stringResource(R.string.settings_cache_ai),
                    supporting = stringResource(R.string.settings_cache_entries, usage.aiInfoEntries),
                    onClick = null,
                    trailing = {
                        AppleIconButton(
                            icon = SymbolIconName.Delete,
                            contentDescription = stringResource(R.string.settings_cache_clear_desc),
                            onClick = { pendingClear = CacheClearTarget.AiInfo },
                            tint = AppleTheme.colors.systemRed,
                            size = 20.dp
                        )
                    }
                )
            }

            Text(
                text = stringResource(R.string.settings_cache_hint),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = SonSpacing.medium),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
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
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.settings_cache_limit),
            subtitle = stringResource(R.string.settings_cache_limit_desc)
        )
        CacheLimitPresetsMb.forEach { mb ->
            val bytes = mb.toLong() * 1024 * 1024
            AppleSheetRow(
                title = stringResource(R.string.settings_cache_limit_mb, mb),
                onClick = {
                    onSelect(bytes)
                    onDismiss()
                },
                trailing = {
                    if (bytes == currentBytes) {
                        M3SymbolIcon(
                            name = SymbolIconName.Check,
                            filled = true,
                            contentDescription = null,
                            tint = AppleTheme.colors.key,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppleActionButton(
            label = stringResource(R.string.settings_cache_cancel),
            onClick = onDismiss,
            filled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
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
    AppleAlert(
        title = stringResource(R.string.settings_cache_confirm_title),
        message = description,
        confirmLabel = stringResource(R.string.settings_cache_confirm_action),
        onConfirm = {
            onConfirm()
            onDismiss()
        },
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.settings_cache_cancel),
        destructive = true
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = AppleType.body, color = AppleTheme.colors.label)
            Text(
                text = supporting,
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
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
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(title = stringResource(R.string.settings_sleep_timer))
        presets.forEach { minutes ->
            AppleSheetRow(
                title = stringResource(R.string.sleep_timer_minutes, minutes),
                onClick = {
                    onSelect(minutes)
                    onDismiss()
                }
            )
        }
        if (isTimerRunning) {
            AppleSheetRow(
                title = stringResource(R.string.sleep_timer_cancel),
                onClick = {
                    onCancelTimer()
                    onDismiss()
                }
            )
        }
    }
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
    AppleAlert(
        title = stringResource(R.string.settings_playback_speed),
        confirmLabel = stringResource(R.string.settings_dialog_done),
        onConfirm = onDismiss,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatSpeed(speed),
                style = AppleType.title2Emphasized,
                color = AppleTheme.colors.label,
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
    }
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
    AppleAlert(
        title = stringResource(R.string.settings_audio_fade),
        confirmLabel = stringResource(R.string.settings_dialog_done),
        onConfirm = onDismiss,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatFadeDuration(durationMs),
                style = AppleType.title2Emphasized,
                color = AppleTheme.colors.label,
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
    }
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
        style = AppleType.caption1Emphasized,
        color = AppleTheme.colors.secondaryLabel,
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
            style = AppleType.caption1Emphasized,
            color = AppleTheme.colors.secondaryLabel,
            modifier = Modifier.width(SonSizes.iconLarge)
        )
        Text(
            text = stringResource(provider.labelRes()),
            style = AppleType.body,
            color = AppleTheme.colors.label,
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
    LyricsProvider.LX -> R.string.settings_lyrics_provider_lx
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
                Spacer(modifier = Modifier.height(appleBottomBarClearance()))
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
            tint = AppleTheme.colors.secondaryLabel
        )
        Spacer(modifier = Modifier.height(SonSpacing.medium))
        Text(
            text = title,
            style = AppleType.headline,
            color = AppleTheme.colors.label
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = subtitle,
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )
        }
        Spacer(modifier = Modifier.height(SonSpacing.large))
        action()
    }
}

@Composable
private fun ScanResultContent(result: ScanResult) {
    AppleGroupedSection(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Text(
                text = stringResource(R.string.settings_scan_complete),
                style = AppleType.headline,
                color = AppleTheme.colors.label
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
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
        Text(
            text = value,
            style = AppleType.footnote,
            color = AppleTheme.colors.label
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
        AppleGroupedSection(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(SonSpacing.medium)) {
                Text(
                    text = stringResource(R.string.settings_scanning),
                    style = AppleType.headline,
                    color = AppleTheme.colors.label
                )
                Spacer(modifier = Modifier.height(SonSpacing.medium))
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { uiState.scanProgress }
                )
                Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
                Text(
                    text = stringResource(R.string.settings_scan_progress, uiState.songsProcessed, uiState.totalSongs),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
                Text(
                    text = when (uiState.scanStep) {
                        SettingsScanStep.QueryingMediaStore -> stringResource(R.string.settings_scan_querying)
                        SettingsScanStep.SavingToDatabase -> stringResource(R.string.settings_scan_saving)
                        else -> ""
                    },
                    style = AppleType.caption1,
                    color = AppleTheme.colors.secondaryLabel
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
                bottom = appleBottomBarClearance()
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = SymbolIconName.Folder,
            filled = true,
            contentDescription = null,
            tint = AppleTheme.colors.secondaryLabel,
            size = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getFolderDisplayName(folder.path),
                style = AppleType.settingsTitle,
                color = AppleTheme.colors.label
            )
            Text(
                text = folder.path,
                style = AppleType.settingsSubtitle,
                color = AppleTheme.colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AppleIconButton(
            icon = SymbolIconName.Delete,
            contentDescription = stringResource(R.string.settings_delete_desc),
            onClick = onDelete,
            tint = AppleTheme.colors.systemRed,
            size = 20.dp
        )
    }
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
                    .clip(RoundedCornerShape(AppleDims.cornerS))
                    .background(AppleTheme.colors.fill)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.CheckCircle,
                    filled = true,
                    contentDescription = null,
                    tint = AppleTheme.colors.key,
                    size = 18.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_api_configured),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.label
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = stringResource(R.string.settings_api_key_title),
            style = AppleType.sectionTitle,
            color = AppleTheme.colors.label
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.settings_api_key_description),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        key(showApiKey) {
            AppleTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyInputChange,
                placeholder = stringResource(R.string.settings_api_key_placeholder),
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppleTextAction(
            label = if (showApiKey) {
                stringResource(R.string.settings_hide_api_key)
            } else {
                stringResource(R.string.settings_show_api_key)
            },
            onClick = { showApiKey = !showApiKey }
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        AppleActionButton(
            label = stringResource(R.string.settings_save),
            onClick = onSaveApiKey,
            enabled = apiKeyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        if (hasExistingKey) {
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            AppleActionButton(
                label = stringResource(R.string.settings_clear_api_key),
                onClick = onClearApiKey,
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(appleBottomBarClearance()))
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
            Spacer(modifier = Modifier.height(appleBottomBarClearance()))
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
    AudioQuality.HR -> AppleTheme.colors.systemRed
    AudioQuality.SQ -> AppleTheme.colors.key
    AudioQuality.HQ -> AppleTheme.colors.secondaryLabel
    AudioQuality.OTHERS -> AppleTheme.colors.tertiaryLabel
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

    AppleGroupedSection(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SonSpacing.large)
        ) {
            Text(
                text = stringResource(R.string.settings_audio_quality_title),
                style = AppleType.headline,
                color = AppleTheme.colors.label
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
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )

            if (selectedIndex in pieData.indices) {
                Spacer(modifier = Modifier.height(SonSpacing.small))
                val selected = pieData[selectedIndex]
                val percentage = (selected.value / stats.totalSongs.toFloat() * 100).toInt()
                Text(
                    text = "${selected.label}: ${percentage}%",
                    style = AppleType.footnoteEmphasized,
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
            style = AppleType.headline,
            color = AppleTheme.colors.label,
            modifier = Modifier.padding(bottom = SonSpacing.mediumSmall)
        )

        AppleGroupedSection(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                StatItemRow(item = item)
                if (index < items.lastIndex) {
                    AppleSeparator(leadingInset = listDividerIndent)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(AppleDims.cornerS))
                .background(item.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            M3SymbolIcon(
                name = icon,
                filled = true,
                contentDescription = null,
                tint = item.color,
                size = 18.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppleType.settingsTitle, color = AppleTheme.colors.label)
            Text(
                text = description,
                style = AppleType.settingsSubtitle,
                color = AppleTheme.colors.secondaryLabel
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.settings_quality_count, item.count),
                style = AppleType.footnoteEmphasized,
                color = item.color
            )
            Text(
                text = "%.1f%%".format(item.percentage),
                style = AppleType.caption1,
                color = AppleTheme.colors.secondaryLabel
            )
        }
    }
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
