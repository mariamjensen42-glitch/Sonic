package com.cycling.sonic.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarDefaults
import com.cycling.sonic.core.ui.LocalM3ToastState
import com.cycling.sonic.core.ui.components.M3ListItemOneLine
import com.cycling.sonic.core.ui.components.M3Scaffold
import com.cycling.sonic.core.ui.components.M3TopAppBar
import com.cycling.sonic.core.ui.theme.M3Spacing
import com.cycling.sonic.core.ui.theme.SpotifyColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()

    val toastState = LocalM3ToastState.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowToast -> toastState?.show(effect.message)
                is SettingsEffect.NavigateBackFromSettings -> onNavigateBack()
            }
        }
    }

    val title = when (uiState.currentDestination) {
        SettingsDestination.MAIN -> "设置"
        SettingsDestination.SCAN -> "扫描本地音乐"
        SettingsDestination.EXCLUDE_FOLDERS -> "排除文件夹"
        SettingsDestination.API_KEY_CONFIG -> "DeepSeek API 配置"
        SettingsDestination.LIBRARY_STATS -> "音乐库统计"
    }

    M3Scaffold(
        topBar = {
            M3TopAppBar(
                title = title,
                navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = { viewModel.handleIntent(SettingsIntent.NavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyColors.Background,
                    titleContentColor = SpotifyColors.TextPrimary,
                    navigationIconContentColor = SpotifyColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentDestination) {
                SettingsDestination.MAIN -> MainSettings(viewModel, hasApiKey)
                SettingsDestination.SCAN -> ScanSettingsPage(viewModel, uiState)
                SettingsDestination.EXCLUDE_FOLDERS -> ExcludeFoldersSettings(viewModel, uiState)
                SettingsDestination.API_KEY_CONFIG -> ApiKeyConfigSettings(viewModel, uiState, hasApiKey)
                SettingsDestination.LIBRARY_STATS -> LibraryStatsSettings(viewModel, uiState)
            }
        }
    }
}

@Composable
private fun MainSettings(viewModel: SettingsViewModel, hasApiKey: Boolean) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SectionHeader("音乐库")
            M3ListItemOneLine(
                text = "扫描本地音乐",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) },
                leadingIcon = Icons.Outlined.LibraryMusic,
                leadingIconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            M3ListItemOneLine(
                text = "排除文件夹",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) },
                leadingIcon = Icons.Outlined.Folder,
                leadingIconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            M3ListItemOneLine(
                text = "音乐库统计",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) },
                leadingIcon = Icons.Outlined.BarChart,
                leadingIconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item {
            SectionHeader("播放")
            M3ListItemOneLine(
                text = "DeepSeek API 配置",
                onClick = { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) },
                leadingIcon = Icons.Outlined.Key,
                leadingIconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                trailingContent = {
                    Text(
                        text = if (hasApiKey) "已配置" else "未配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item {
            SectionHeader("关于")
            M3ListItemOneLine(
                text = "Sonic",
                onClick = {},
                trailingContent = {
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = M3Spacing.medium)
            .padding(top = M3Spacing.large, bottom = M3Spacing.small)
    )
}
