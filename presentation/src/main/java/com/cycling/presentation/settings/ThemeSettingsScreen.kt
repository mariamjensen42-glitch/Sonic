package com.cycling.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.theme.M3ContrastLevel
import com.cycling.core.ui.theme.M3ThemeManager
import com.cycling.core.ui.theme.M3ThemePreset
import com.cycling.core.ui.theme.M3ThemePresets
import com.cycling.core.ui.theme.M3ThemeState
import com.cycling.core.ui.theme.M3ThemeViewModel
import com.cycling.core.ui.theme.rememberM3ThemeState
import com.cycling.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: M3ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val themeState by rememberM3ThemeState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ThemeModeSection(
                    currentMode = themeState.darkMode,
                    onModeChange = { viewModel.setDarkMode(it) }
                )
            }
            
            item {
                DynamicColorSection(
                    enabled = themeState.dynamicColorEnabled,
                    onToggle = { viewModel.setDynamicColorEnabled(it) }
                )
            }
            
            item {
                if (!themeState.dynamicColorEnabled) {
                    ThemePresetSection(
                        currentSeedColor = themeState.seedColor,
                        presets = M3ThemePresets.All,
                        onPresetSelect = { viewModel.applyPreset(it) }
                    )
                }
            }
            
            item {
                ContrastSection(
                    currentLevel = themeState.contrastLevel,
                    onLevelChange = { viewModel.setContrastLevel(it) }
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSection(
    currentMode: Boolean?,
    onModeChange: (Boolean?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "外观模式",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = 0,
                    count = 3,
                    baseShape = RoundedCornerShape(16.dp)
                ),
                onClick = { onModeChange(false) },
                selected = currentMode == false,
                icon = {
                    if (currentMode == false) {
                        Icon(Icons.Default.LightMode, contentDescription = null)
                    }
                }
            ) {
                Text("浅色")
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = 1,
                    count = 3,
                    baseShape = RoundedCornerShape(16.dp)
                ),
                onClick = { onModeChange(null) },
                selected = currentMode == null,
                icon = {
                    if (currentMode == null) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            ) {
                Text("跟随系统")
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = 2,
                    count = 3,
                    baseShape = RoundedCornerShape(16.dp)
                ),
                onClick = { onModeChange(true) },
                selected = currentMode == true,
                icon = {
                    if (currentMode == true) {
                        Icon(Icons.Default.DarkMode, contentDescription = null)
                    }
                }
            ) {
                Text("深色")
            }
        }
    }
}

@Composable
private fun DynamicColorSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "动态颜色",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "根据壁纸自动调整应用颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                thumbContent = if (enabled) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePresetSection(
    currentSeedColor: Color,
    presets: List<M3ThemePreset>,
    onPresetSelect: (M3ThemePreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "主题颜色",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presets) { preset ->
                ThemePresetItem(
                    preset = preset,
                    isSelected = currentSeedColor == preset.seedColor,
                    onClick = { onPresetSelect(preset) }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetItem(
    preset: M3ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(preset.seedColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ContrastSection(
    currentLevel: M3ContrastLevel,
    onLevelChange: (M3ContrastLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "对比度",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "调整界面元素的对比度，提高可读性",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            M3ContrastLevel.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = M3ContrastLevel.entries.size,
                        baseShape = RoundedCornerShape(16.dp)
                    ),
                    onClick = { onLevelChange(level) },
                    selected = currentLevel == level
                ) {
                    Text(
                        when (level) {
                            M3ContrastLevel.Default -> "标准"
                            M3ContrastLevel.Medium -> "中等"
                            M3ContrastLevel.High -> "高"
                        }
                    )
                }
            }
        }
    }
}
