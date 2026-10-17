@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.settings

import android.net.Uri
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AudioQuality
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.ScanResult
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_AUDIO_FADE_DURATION_MS
import com.cycling.domain.repository.SettingsRepository.Companion.MAX_PLAYBACK_SPEED
import com.cycling.domain.repository.SettingsRepository.Companion.MIN_PLAYBACK_SPEED
import com.cycling.presentation.components.PieChart
import com.cycling.presentation.components.PieChartData
import com.cycling.presentation.permissions.MediaLibraryPermission
import com.cycling.presentation.R
import java.util.Locale

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
                sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                onSetSleepTimer = { viewModel.handleIntent(SettingsIntent.SetSleepTimer(it)) },
                onCancelSleepTimer = { viewModel.handleIntent(SettingsIntent.CancelSleepTimer) },
                onNavigateToScan = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) },
                onNavigateToExcludeFolders = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) },
                onNavigateToApiKeyConfig = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) },
                onNavigateToLibraryStats = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) },
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

/** 开关型设置项：点击整行或开关本身都能切换。 */
@Composable
private fun SettingsSwitchRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        onClick = { onCheckedChange(!checked) }
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToExcludeFolders: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit,
    onNavigateToLibraryStats: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportM3u: () -> Unit,
    isTransferring: Boolean,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showFadeDialog by remember { mutableStateOf(false) }
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
    LyricsProvider.NETEASE -> R.string.settings_lyrics_provider_netease
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
        isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
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
        isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
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

private fun getFolderPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null
    val treeDocumentId = path.substringAfterLast(":")
    return "/storage/emulated/0/$treeDocumentId"
}

private fun getFolderDisplayName(path: String): String {
    return path.substringAfterLast("/")
}
