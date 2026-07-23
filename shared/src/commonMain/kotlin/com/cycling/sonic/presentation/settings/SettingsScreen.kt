package com.cycling.sonic.presentation.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.cycling.sonic.core.ui.components.M3Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.sonic.core.ui.components.M3CircularProgressIndicator
import com.cycling.sonic.core.ui.components.M3TopAppBar
import com.cycling.sonic.core.ui.theme.SpotifyColors
import com.cycling.sonic.presentation.components.PieChart
import com.cycling.sonic.presentation.components.PieChartData

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowToast -> { }
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
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = { viewModel.handleIntent(SettingsIntent.NavigateBack) }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentDestination) {
                SettingsDestination.MAIN -> MainSettings(viewModel, hasApiKey)
                SettingsDestination.SCAN -> ScanSettings(viewModel, uiState)
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
            SettingsSectionHeader("音乐库")
            SettingsItem(Icons.Default.LibraryMusic, "扫描本地音乐") { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.SCAN)) }
            SettingsItem(Icons.Default.Folder, "排除文件夹") { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.EXCLUDE_FOLDERS)) }
            SettingsItem(Icons.Default.BarChart, "音乐库统计") { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.LIBRARY_STATS)) }
        }
        item {
            SettingsSectionHeader("播放")
            SettingsItem(Icons.Default.Key, "DeepSeek API 配置", trailing = if (hasApiKey) "已配置" else "未配置") { viewModel.handleIntent(SettingsIntent.NavigateTo(SettingsDestination.API_KEY_CONFIG)) }
        }
        item {
            SettingsSectionHeader("关于")
            SettingsItem(null, "Sonic", trailing = "1.0.0") { }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, color = SpotifyColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).then(Modifier.padding(top = 20.dp)))
}

@Composable
private fun SettingsItem(icon: ImageVector?, title: String, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = SpotifyColors.TextPrimary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)) }
        Text(title, color = SpotifyColors.TextPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) { Text(trailing, color = SpotifyColors.TextTertiary, fontSize = 13.sp); Spacer(Modifier.width(8.dp)) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SpotifyColors.TextTertiary)
    }
}

@Composable
private fun ScanSettings(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        if (uiState.isScanning) {
            LinearProgressIndicator(progress = { uiState.scanProgress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("正在扫描... ${uiState.songsProcessed}/${uiState.totalSongs}", color = SpotifyColors.TextSecondary)
        } else if (uiState.scanResult != null) {
            Icon(Icons.Default.Check, null, tint = SpotifyColors.PlayGreen, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("扫描完成", color = SpotifyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("发现 ${uiState.scanResult.songsFound} 首歌曲", color = SpotifyColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.handleIntent(SettingsIntent.StartScan) }) { Text("重新扫描") }
        } else if (uiState.scanError != null) {
            Text(uiState.scanError ?: "", color = SpotifyColors.Accent)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.handleIntent(SettingsIntent.StartScan) }) { Text("重试") }
        } else {
            Text("扫描您的设备上的音乐文件", color = SpotifyColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.handleIntent(SettingsIntent.StartScan) }) { Text("开始扫描") }
        }
    }
}

@Composable
private fun ExcludeFoldersSettings(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    if (uiState.isLoadingFolders) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { M3CircularProgressIndicator() }
    } else if (uiState.excludedFolders.isEmpty()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("暂无排除的文件夹", color = SpotifyColors.TextSecondary)
            Text("点击右上角 + 添加要排除的文件夹", color = SpotifyColors.TextTertiary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(uiState.excludedFolders, key = { it.path }) { folder ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = SpotifyColors.TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(folder.path, color = SpotifyColors.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Delete, null, tint = SpotifyColors.Accent, modifier = Modifier.size(20.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.handleIntent(SettingsIntent.RemoveExcludedFolder(folder.path)) })
                }
            }
        }
    }
}

@Composable
private fun ApiKeyConfigSettings(viewModel: SettingsViewModel, uiState: SettingsUiState, hasApiKey: Boolean) {
    var showKey by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("请输入您的 DeepSeek API Key", color = SpotifyColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("API Key 用于获取歌曲、艺术家、专辑的 AI 解读信息。", color = SpotifyColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.apiKeyInput,
            onValueChange = { viewModel.handleIntent(SettingsIntent.UpdateApiKeyInput(it)) },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏 API Key" else "显示 API Key") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.handleIntent(SettingsIntent.SaveApiKey) }, enabled = uiState.apiKeyInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("保存") }
        if (hasApiKey) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.handleIntent(SettingsIntent.ClearApiKey) }) { Text("清除已保存的 API Key", color = SpotifyColors.Accent) }
        }
    }
}

@Composable
private fun LibraryStatsSettings(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    if (uiState.isLoadingStats) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { M3CircularProgressIndicator() }
    } else if (uiState.libraryStats == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.HighQuality, null, modifier = Modifier.size(48.dp), tint = SpotifyColors.TextTertiary)
            Spacer(Modifier.height(12.dp))
            Text("暂无音乐", color = SpotifyColors.TextPrimary)
            Text("请先扫描本地音乐", color = SpotifyColors.TextSecondary, fontSize = 13.sp)
        }
    } else {
        val stats = uiState.libraryStats!!
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("音频质量分布", color = SpotifyColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PieChart(
                    data = listOf(
                        PieChartData(stats.hrCount.toFloat(), Color(0xFFFFD700), "HR"),
                        PieChartData(stats.sqCount.toFloat(), Color(0xFF9C27B0), "SQ"),
                        PieChartData(stats.hqCount.toFloat(), Color(0xFF2196F3), "HQ"),
                        PieChartData(stats.othersCount.toFloat(), Color(0xFF9E9E9E), "Others")
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("共 ${stats.totalSongs} 首歌曲", color = SpotifyColors.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            QualityStatItem("HR (Hi-Res)", "高解析度音频 > 1411 kbps", stats.hrCount, stats.hrPercentage, Color(0xFFFFD700))
            QualityStatItem("SQ (Studio Quality)", "CD质量/无损 = 1411 kbps", stats.sqCount, stats.sqPercentage, Color(0xFF9C27B0))
            QualityStatItem("HQ (High Quality)", "高质量 320-1410 kbps", stats.hqCount, stats.hqPercentage, Color(0xFF2196F3))
            QualityStatItem("Others", "其他 < 320 kbps 或未知", stats.othersCount, stats.othersPercentage, Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun QualityStatItem(label: String, desc: String, count: Int, percentage: Float, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, shape = androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = SpotifyColors.TextPrimary, fontWeight = FontWeight.Medium)
            Text(desc, color = SpotifyColors.TextTertiary, fontSize = 12.sp)
        }
        Text("${count} 首", color = SpotifyColors.TextSecondary)
    }
}


