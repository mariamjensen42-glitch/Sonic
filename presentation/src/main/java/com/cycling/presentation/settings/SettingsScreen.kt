package com.cycling.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.domain.model.AudioQuality
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.ScanResult
import com.cycling.core.ui.components.M3TopAppBar
import com.cycling.core.ui.theme.SpotifyColors
import com.cycling.presentation.components.PieChart
import com.cycling.presentation.components.PieChartData
import com.cycling.presentation.R

private val HRColor = Color(0xFFFFD700)
private val SQColor = Color(0xFF9C27B0)
private val HQColor = Color(0xFF2196F3)
private val OthersColor = Color(0xFF9E9E9E)

private data class QualityStatItemUi(
    val quality: AudioQuality,
    val count: Int,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

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

    LaunchedEffect(Unit) {
        val hasPermission = storagePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.checkAndRequestPermission(hasPermission)
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
            M3TopAppBar(
                title = when (uiState.currentDestination) {
                    SettingsDestination.MAIN -> stringResource(R.string.settings_title)
                    SettingsDestination.SCAN -> stringResource(R.string.settings_scan_menu)
                    SettingsDestination.EXCLUDE_FOLDERS -> stringResource(R.string.settings_exclude_menu)
                    SettingsDestination.API_KEY_CONFIG -> stringResource(R.string.settings_api_config_menu)
                    SettingsDestination.LIBRARY_STATS -> stringResource(R.string.settings_library_stats_menu)
                },
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = stringResource(R.string.settings_back_desc),
                onNavigationClick = { viewModel.handleIntent(SettingsIntent.NavigateBack) },
                actions = {
                    if (uiState.currentDestination == SettingsDestination.EXCLUDE_FOLDERS) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.settings_add_folder_desc),
                                tint = SpotifyColors.Accent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyColors.Background,
                    titleContentColor = SpotifyColors.TextPrimary,
                    navigationIconContentColor = SpotifyColors.TextPrimary
                )
            )
        },
        containerColor = SpotifyColors.Background
    ) { paddingValues ->
        when (uiState.currentDestination) {
            SettingsDestination.MAIN -> MainSettingsContent(
                hasApiKey = hasApiKey,
                onNavigateToScan = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) },
                onNavigateToExcludeFolders = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) },
                onNavigateToApiKeyConfig = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) },
                onNavigateToLibraryStats = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) },
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
// Spotify-style Components
// ════════════════════════════════════════════════════════════════

@Composable
private fun SpotifySectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = SpotifyColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SpotifyCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyColors.GridBackground)
    ) {
        content()
    }
}

@Composable
private fun SpotifyListItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = true,
    iconTint: Color = SpotifyColors.TextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpotifyColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SpotifyColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        } else if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SpotifyColors.TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SpotifyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(0.5.dp)
            .background(SpotifyColors.Divider)
    )
}

// ════════════════════════════════════════════════════════════════
// Main Settings Content
// ════════════════════════════════════════════════════════════════

@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SpotifyColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpotifyColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SpotifyColors.TextTertiary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MainSettingsContent(
    hasApiKey: Boolean,
    onNavigateToScan: () -> Unit,
    onNavigateToExcludeFolders: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit,
    onNavigateToLibraryStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // ── 账号/应用头部 ──
        SettingsHeader(
            title = stringResource(R.string.settings_app_name),
            subtitle = stringResource(R.string.settings_app_subtitle),
            onClick = {}
        )

        // ── 音乐库 ──
        SpotifySectionHeader(title = stringResource(R.string.settings_section_library))
        SpotifyCard {
            SpotifyListItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_scan_local),
                onClick = onNavigateToScan
            )
            SpotifyDivider()
            SpotifyListItem(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.settings_exclude_folders),
                onClick = onNavigateToExcludeFolders
            )
            SpotifyDivider()
            SpotifyListItem(
                icon = Icons.Default.BarChart,
                title = stringResource(R.string.settings_library_stats),
                onClick = onNavigateToLibraryStats
            )
        }

        // ── 播放 ──
        SpotifySectionHeader(title = stringResource(R.string.settings_section_playback))
        SpotifyCard {
            SpotifyListItem(
                icon = Icons.Default.Key,
                title = stringResource(R.string.settings_deepseek_config),
                subtitle = if (hasApiKey) stringResource(R.string.settings_api_configured) else stringResource(R.string.settings_api_not_configured),
                onClick = onNavigateToApiKeyConfig
            )
        }

        // ── 关于 ──
        SpotifySectionHeader(title = stringResource(R.string.settings_section_about))
        SpotifyCard {
            SpotifyListItem(
                icon = Icons.Default.LibraryMusic,
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_value),
                onClick = {},
                showChevron = false
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════════
// Scan Content
// ════════════════════════════════════════════════════════════════

@Composable
private fun ScanContent(
    uiState: SettingsUiState,
    onStartScan: () -> Unit,
    onResetScan: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        when {
            !uiState.hasStoragePermission && uiState.shouldRequestPermission -> {
                EmptyStateContent(
                    icon = Icons.Default.Error,
                    title = stringResource(R.string.settings_permission_title),
                    subtitle = stringResource(R.string.settings_permission_subtitle)
                ) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyColors.Accent
                        )
                    ) {
                        Text(stringResource(R.string.settings_grant_permission), color = Color.White)
                    }
                }
            }
            uiState.scanResult != null -> {
                ScanResultContent(uiState.scanResult)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onResetScan,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyColors.GridBackground
                    )
                ) {
                    Text(stringResource(R.string.settings_rescan), color = SpotifyColors.TextPrimary)
                }
            }
            uiState.isScanning -> {
                ScanningProgressContent(uiState)
            }
            uiState.scanErrorResId != null -> {
                EmptyStateContent(
                    icon = Icons.Default.Error,
                    title = stringResource(R.string.settings_scan_failed),
                    subtitle = stringResource(uiState.scanErrorResId!!)
                ) {
                    Button(
                        onClick = onStartScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyColors.Accent
                        )
                    ) {
                        Text(stringResource(R.string.settings_retry), color = Color.White)
                    }
                }
            }
            else -> {
                EmptyStateContent(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.settings_scan_empty_title),
                    subtitle = stringResource(R.string.settings_scan_empty_subtitle)
                ) {
                    Button(
                        onClick = onStartScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyColors.PlayGreen
                        )
                    ) {
                        Text(stringResource(R.string.settings_start_scan), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    action: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SpotifyColors.TextTertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = SpotifyColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = SpotifyColors.TextTertiary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        action()
    }
}

@Composable
private fun ScanResultContent(result: ScanResult) {
    SpotifyCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_scan_complete),
                color = SpotifyColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SpotifyColors.TextSecondary, fontSize = 14.sp)
        Text(text = value, color = SpotifyColors.TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun ScanningProgressContent(uiState: SettingsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SpotifyCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_scanning),
                    color = SpotifyColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { uiState.scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SpotifyColors.PlayGreen,
                    trackColor = SpotifyColors.SurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_scan_progress, uiState.songsProcessed, uiState.totalSongs),
                    color = SpotifyColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = when (uiState.scanStep) {
                        SettingsScanStep.QueryingMediaStore -> stringResource(R.string.settings_scan_querying)
                        SettingsScanStep.SavingToDatabase -> stringResource(R.string.settings_scan_saving)
                        else -> ""
                    },
                    color = SpotifyColors.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Exclude Folders Content
// ════════════════════════════════════════════════════════════════

@Composable
private fun ExcludeFoldersContent(
    folders: List<ExcludedFolder>,
    isLoading: Boolean,
    onRemoveFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotifyColors.Accent)
            }
        } else if (folders.isEmpty()) {
            EmptyStateContent(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.settings_empty_exclude_title),
                subtitle = stringResource(R.string.settings_empty_exclude_subtitle)
            )
        } else {
            LazyColumn {
                items(folders, key = { it.path }) { folder ->
                    ExcludedFolderItem(
                        folder = folder,
                        onDelete = { onRemoveFolder(folder.path) }
                    )
                }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyColors.GridBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = SpotifyColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getFolderDisplayName(folder.path),
                color = SpotifyColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = folder.path,
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.settings_delete_desc),
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// API Key Config Content
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
            .padding(16.dp)
            .imePadding()
    ) {
        if (hasExistingKey) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpotifyColors.Accent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SpotifyColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_api_configured),
                    color = SpotifyColors.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = stringResource(R.string.settings_api_key_title),
            color = SpotifyColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.settings_api_key_description),
            color = SpotifyColors.TextTertiary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        key(showApiKey) {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyInputChange,
                label = { Text(stringResource(R.string.settings_api_key_label), color = SpotifyColors.TextSecondary) },
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder), color = SpotifyColors.TextTertiary) },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SpotifyColors.TextPrimary,
                    unfocusedTextColor = SpotifyColors.TextPrimary,
                    focusedBorderColor = SpotifyColors.Accent,
                    unfocusedBorderColor = SpotifyColors.Divider,
                    cursorColor = SpotifyColors.Accent
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { showApiKey = !showApiKey }
        ) {
            Text(
                text = if (showApiKey) stringResource(R.string.settings_hide_api_key) else stringResource(R.string.settings_show_api_key),
                color = SpotifyColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveApiKey,
            enabled = apiKeyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyColors.PlayGreen,
                disabledContainerColor = SpotifyColors.SurfaceVariant
            )
        ) {
            Text(stringResource(R.string.settings_save), color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hasExistingKey) {
            androidx.compose.material3.OutlinedButton(
                onClick = onClearApiKey,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SpotifyColors.TextSecondary
                )
            ) {
                Text(stringResource(R.string.settings_clear_api_key), color = SpotifyColors.TextSecondary)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Library Stats Content
// ════════════════════════════════════════════════════════════════

@Composable
private fun LibraryStatsContent(
    stats: LibraryStats?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SpotifyColors.Accent)
        }
    } else if (stats != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stats.totalSongs > 0) {
                ChartSection(stats = stats)
                Spacer(modifier = Modifier.height(24.dp))
                StatsDetailSection(stats = stats)
            } else {
                EmptyStateContent(
                    icon = Icons.Default.LibraryMusic,
                    title = stringResource(R.string.settings_stats_empty_title),
                    subtitle = stringResource(R.string.settings_stats_empty_subtitle)
                )
            }
        }
    } else {
        EmptyStateContent(
            icon = Icons.Default.LibraryMusic,
            title = stringResource(R.string.settings_stats_empty_title),
            subtitle = stringResource(R.string.settings_stats_empty_subtitle)
        )
    }
}

@Composable
private fun ChartSection(stats: LibraryStats) {
    var selectedIndex by remember { mutableStateOf(-1) }

    val pieData = listOf(
        PieChartData(stats.hrCount.toFloat(), HRColor, "HR"),
        PieChartData(stats.sqCount.toFloat(), SQColor, "SQ"),
        PieChartData(stats.hqCount.toFloat(), HQColor, "HQ"),
        PieChartData(stats.othersCount.toFloat(), OthersColor, "Others")
    ).filter { it.value > 0 }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyColors.GridBackground)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_audio_quality_title),
            color = SpotifyColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        PieChart(
            data = pieData,
            selectedIndex = selectedIndex,
            onSliceClick = { selectedIndex = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_stats_total, stats.totalSongs),
            color = SpotifyColors.TextSecondary,
            fontSize = 13.sp
        )

        if (selectedIndex >= 0 && selectedIndex < pieData.size) {
            Spacer(modifier = Modifier.height(8.dp))
            val selected = pieData[selectedIndex]
            val percentage = (selected.value / stats.totalSongs.toFloat() * 100).toInt()
            Text(
                text = "${selected.label}: ${percentage}%",
                color = selected.color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsDetailSection(stats: LibraryStats) {
    val items = listOf(
        QualityStatItemUi(AudioQuality.HR, stats.hrCount, stats.hrPercentage, HRColor),
        QualityStatItemUi(AudioQuality.SQ, stats.sqCount, stats.sqPercentage, SQColor),
        QualityStatItemUi(AudioQuality.HQ, stats.hqCount, stats.hqPercentage, HQColor),
        QualityStatItemUi(AudioQuality.OTHERS, stats.othersCount, stats.othersPercentage, OthersColor)
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.settings_stats_detail),
            color = SpotifyColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        items.forEach { item ->
            StatItemRow(item = item)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatItemRow(item: QualityStatItemUi) {
    val (icon, title, description) = when (item.quality) {
        AudioQuality.HR -> Triple(
            Icons.Default.GraphicEq,
            stringResource(R.string.settings_quality_hr),
            stringResource(R.string.settings_quality_hr_desc)
        )
        AudioQuality.SQ -> Triple(
            Icons.Default.HighQuality,
            stringResource(R.string.settings_quality_sq),
            stringResource(R.string.settings_quality_sq_desc)
        )
        AudioQuality.HQ -> Triple(
            Icons.Default.BarChart,
            stringResource(R.string.settings_quality_hq),
            stringResource(R.string.settings_quality_hq_desc)
        )
        AudioQuality.OTHERS -> Triple(
            Icons.Default.LibraryMusic,
            stringResource(R.string.settings_quality_others),
            stringResource(R.string.settings_quality_others_desc)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyColors.GridBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpotifyColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = SpotifyColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.settings_quality_count, item.count),
                color = item.color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "%.1f%%".format(item.percentage),
                color = SpotifyColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Utilities
// ════════════════════════════════════════════════════════════════

private fun getFolderPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null
    val treeDocumentId = path.substringAfterLast(":")
    return "/storage/emulated/0/$treeDocumentId"
}

private fun getFolderDisplayName(path: String): String {
    return path.substringAfterLast("/")
}
