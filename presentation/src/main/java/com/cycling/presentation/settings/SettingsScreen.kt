package com.cycling.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.domain.model.AudioQuality
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.model.LibraryStats
import com.cycling.domain.model.ScanResult
import com.cycling.domain.model.ThemeMode
import com.cycling.core.ui.components.M3FilledButton
import com.cycling.core.ui.components.M3ListItem
import com.cycling.core.ui.components.M3ListItemOneLine
import com.cycling.core.ui.components.M3ListItemTwoLine
import com.cycling.core.ui.components.M3OutlinedButton
import com.cycling.core.ui.components.M3TextButton
import com.cycling.core.ui.components.M3TopAppBar
import com.cycling.core.ui.theme.M3SemanticColors
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.presentation.components.PieChart
import com.cycling.presentation.components.PieChartData

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
    onNavigateToThemeSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
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
                onShowToast("无法获取文件夹路径")
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
                is SettingsEffect.ShowToast -> onShowToast(effect.message)
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
                    SettingsDestination.MAIN -> "设置"
                    SettingsDestination.SCAN -> "扫描本地音乐"
                    SettingsDestination.EXCLUDE_FOLDERS -> "排除文件夹"
                    SettingsDestination.API_KEY_CONFIG -> "DeepSeek API 配置"
                    SettingsDestination.LIBRARY_STATS -> "音乐库统计"
                },
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = { viewModel.handleIntent(SettingsIntent.NavigateBack) },
                actions = {
                    if (uiState.currentDestination == SettingsDestination.EXCLUDE_FOLDERS) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加文件夹",
                                tint = M3SemanticColors.Blue
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (uiState.currentDestination) {
            SettingsDestination.MAIN -> MainSettingsContent(
                themeMode = themeMode,
                hasApiKey = hasApiKey,
                onThemeModeChange = { viewModel.handleIntent(SettingsIntent.SetThemeMode(it)) },
                onNavigateToScan = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) },
                onNavigateToExcludeFolders = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) },
                onNavigateToApiKeyConfig = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) },
                onNavigateToLibraryStats = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) },
                onNavigateToThemeSettings = onNavigateToThemeSettings,
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

@Composable
private fun MainSettingsContent(
    themeMode: ThemeMode,
    hasApiKey: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToExcludeFolders: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit,
    onNavigateToLibraryStats: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(M3Spacing.small))

        SettingsSectionHeader(title = "音乐库")
        SettingsCardContainer {
            M3ListItemOneLine(
                text = "扫描本地音乐",
                leadingIcon = Icons.Default.Refresh,
                leadingIconBackgroundColor = M3SemanticColors.Blue,
                onClick = onNavigateToScan
            )
            M3ListItemOneLine(
                text = "排除文件夹",
                leadingIcon = Icons.Default.Folder,
                leadingIconBackgroundColor = M3SemanticColors.Orange,
                onClick = onNavigateToExcludeFolders
            )
            M3ListItemOneLine(
                text = "音乐库统计",
                leadingIcon = Icons.Default.BarChart,
                leadingIconBackgroundColor = M3SemanticColors.Green,
                onClick = onNavigateToLibraryStats
            )
        }

        Spacer(modifier = Modifier.height(M3Spacing.large))

        SettingsSectionHeader(title = "播放")
        SettingsCardContainer {
            M3ListItemTwoLine(
                headlineText = "DeepSeek API 配置",
                supportingText = if (hasApiKey) "已配置" else "未配置",
                leadingIcon = Icons.Default.Key,
                leadingIconBackgroundColor = M3SemanticColors.Purple,
                onClick = onNavigateToApiKeyConfig
            )
            M3ListItemTwoLine(
                headlineText = "主题",
                supportingText = getThemeModeText(themeMode),
                leadingIcon = Icons.Default.Palette,
                leadingIconBackgroundColor = M3SemanticColors.Indigo,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(M3Spacing.large))

        SettingsSectionHeader(title = "外观")
        SettingsCardContainer {
            ThemeOptionItem(
                text = "跟随系统",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
            )
            ThemeOptionItem(
                text = "浅色模式",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            ThemeOptionItem(
                text = "深色模式",
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
        }

        Spacer(modifier = Modifier.height(M3Spacing.large))

        SettingsSectionHeader(title = "关于")
        SettingsCardContainer {
            M3ListItemTwoLine(
                headlineText = "版本",
                supportingText = "1.0.0",
                leadingIcon = Icons.Default.Info,
                leadingIconBackgroundColor = M3SemanticColors.Teal,
                onClick = {},
                trailingContent = {
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(M3Spacing.large))
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = M3Spacing.medium, vertical = M3Spacing.small)
    )
}

@Composable
private fun SettingsCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = M3Spacing.medium)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        content = content
    )
}

@Composable
private fun ThemeOptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    M3ListItemOneLine(
        text = text,
        onClick = onClick,
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = M3SemanticColors.Green,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun getThemeModeText(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
    }
}

@Composable
private fun ScanContent(
    uiState: SettingsUiState,
    onStartScan: () -> Unit,
    onResetScan: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isScanning -> ScanningContent(uiState)
            uiState.scanResult != null -> CompletedContent(
                result = uiState.scanResult!!,
                onScanAgain = onResetScan
            )
            uiState.shouldRequestPermission && !uiState.hasStoragePermission -> PermissionRequiredContent(
                onRequestPermission = onRequestPermission
            )
            uiState.scanError != null -> ErrorContent(
                error = uiState.scanError!!,
                onRetry = {
                    if (!uiState.hasStoragePermission) {
                        onRequestPermission()
                    } else {
                        onStartScan()
                    }
                }
            )
            else -> IdleContent(
                onStartScan = onStartScan
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    CenteredContent(
        icon = Icons.Default.Folder,
        iconTint = MaterialTheme.colorScheme.primary,
        title = "需要存储权限",
        subtitle = "为了扫描您设备上的音乐文件，请授予存储访问权限"
    ) {
        M3FilledButton(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("授予权限")
        }
    }
}

@Composable
private fun IdleContent(
    onStartScan: () -> Unit
) {
    CenteredContent(
        icon = Icons.Default.Refresh,
        iconTint = MaterialTheme.colorScheme.primary,
        title = "扫描本地音乐",
        subtitle = "扫描设备上的音乐文件并添加到音乐库"
    ) {
        M3FilledButton(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("开始扫描")
        }
    }
}

@Composable
private fun ScanningContent(
    state: SettingsUiState
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when (state.scanStep) {
                SettingsScanStep.QueryingMediaStore -> "正在查询媒体库..."
                SettingsScanStep.SavingToDatabase -> "正在保存到数据库..."
                else -> "扫描中..."
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (state.totalSongs > 0) {
            Spacer(modifier = Modifier.height(24.dp))
            ScanProgressCard(
                modifier = Modifier.fillMaxWidth(0.7f),
                progress = state.scanProgress,
                songsProcessed = state.songsProcessed,
                totalSongs = state.totalSongs
            )
        }
    }
}

@Composable
private fun ScanProgressCard(
    progress: Float,
    songsProcessed: Int,
    totalSongs: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(M3Spacing.medium)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$songsProcessed / $totalSongs 首歌曲",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompletedContent(
    result: ScanResult,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(M3SemanticColors.Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "扫描完成",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        ScanResultCard(
            modifier = Modifier.fillMaxWidth(0.85f),
            result = result
        )
        Spacer(modifier = Modifier.height(32.dp))
        M3TextButton(onClick = onScanAgain) {
            Text("重新扫描")
        }
    }
}

@Composable
private fun ScanResultCard(
    result: ScanResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(M3Spacing.medium)
    ) {
        ResultRow(label = "歌曲", value = result.songsFound.toString())
        ResultRow(label = "专辑", value = result.albumsFound.toString())
        ResultRow(label = "歌手", value = result.artistsFound.toString())
        ResultRow(label = "耗时", value = "${result.duration}ms", isLast = true)
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isLast) Modifier.padding(bottom = M3Spacing.small) else Modifier),
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    CenteredContent(
        icon = Icons.Default.Error,
        iconTint = MaterialTheme.colorScheme.error,
        title = "扫描失败",
        subtitle = error,
        titleColor = MaterialTheme.colorScheme.error
    ) {
        M3FilledButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("重试")
        }
    }
}

@Composable
private fun CenteredContent(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    action: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(M3Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.height(M3Spacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(M3Spacing.small))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(M3Spacing.large))
        action()
    }
}

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
            ExcludeFoldersEmptyState(
                icon = Icons.Default.Folder,
                title = "加载中..."
            )
        } else if (folders.isEmpty()) {
            ExcludeFoldersEmptyState(
                icon = Icons.Default.Folder,
                title = "暂无排除的文件夹",
                subtitle = "点击右上角 + 添加要排除的文件夹"
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
private fun ExcludeFoldersEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExcludedFolderItem(
    folder: ExcludedFolder,
    onDelete: () -> Unit
) {
    M3ListItemTwoLine(
        headlineText = getFolderDisplayName(folder.path),
        supportingText = folder.path,
        leadingIcon = Icons.Default.Folder,
        leadingIconBackgroundColor = M3SemanticColors.Blue,
        onClick = {},
        trailingContent = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = M3SemanticColors.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

private fun getFolderPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null
    val treeDocumentId = path.substringAfterLast(":")
    return "/storage/emulated/0/$treeDocumentId"
}

private fun getFolderDisplayName(path: String): String {
    return path.substringAfterLast("/")
}

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
        Text(
            text = "请输入您的 DeepSeek API Key",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "API Key 用于获取歌曲、艺术家、专辑的 AI 解读信息。您可以在 DeepSeek 官网注册并获取 API Key。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onApiKeyInputChange,
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { showApiKey = !showApiKey }
        ) {
            Text(if (showApiKey) "隐藏 API Key" else "显示 API Key")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveApiKey,
            enabled = apiKeyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hasExistingKey) {
            androidx.compose.material3.OutlinedButton(
                onClick = onClearApiKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除已保存的 API Key")
            }
        }
    }
}

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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                LibraryStatsEmptyState()
            }
        }
    } else {
        LibraryStatsEmptyState()
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Text(
            text = "音频质量分布",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        PieChart(
            data = pieData,
            selectedIndex = selectedIndex,
            onSliceClick = { selectedIndex = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "共 ${stats.totalSongs} 首歌曲",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (selectedIndex >= 0 && selectedIndex < pieData.size) {
            Spacer(modifier = Modifier.height(8.dp))
            val selected = pieData[selectedIndex]
            val percentage = (selected.value / stats.totalSongs.toFloat() * 100).toInt()
            Text(
                text = "${selected.label}: ${percentage}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = selected.color
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
            text = "统计明细",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
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
            "HR (Hi-Res)",
            "高解析度音频 > 1411 kbps"
        )
        AudioQuality.SQ -> Triple(
            Icons.Default.HighQuality,
            "SQ (Studio Quality)",
            "CD质量/无损 = 1411 kbps"
        )
        AudioQuality.HQ -> Triple(
            Icons.Default.BarChart,
            "HQ (High Quality)",
            "高质量 320-1410 kbps"
        )
        AudioQuality.OTHERS -> Triple(
            Icons.Default.LibraryMusic,
            "Others",
            "其他 < 320 kbps 或未知"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.count} 首",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = item.color
            )
            Text(
                text = "%.1f%%".format(item.percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibraryStatsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无音乐",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请先扫描本地音乐",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
