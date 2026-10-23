@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.onlinesources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleSectionHeader
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.PluginPriority
import com.cycling.domain.online.model.PluginSortMode
import com.cycling.presentation.R

/**
 * 设置 → 在线音乐 → 在线音源。
 *
 * 洛雪（LX）式脚本插件的管理面：导入、启停、排序、换源优先级、自动屏蔽、探测。
 * 脚本执行（QuickJS）在 `:data` 里，这里只消费仓库状态。
 */
@Composable
fun OnlineSourcesContent(
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    viewModel: OnlineSourcesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTextDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.handleIntent(OnlineSourcesIntent.ImportFile(it.toString())) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OnlineSourcesEffect.ShowToast -> {
                    val message = effect.arg?.let { context.getString(effect.messageRes, it) }
                        ?: context.getString(effect.messageRes)
                    onShowToast(message)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = appleBottomBarClearance() + bottomPadding)
    ) {
        item {
            PrioritySection(uiState.priority) {
                viewModel.handleIntent(OnlineSourcesIntent.SetPriority(it))
            }
        }
        item {
            SortSection(uiState.sortMode) {
                viewModel.handleIntent(OnlineSourcesIntent.SetSortMode(it))
            }
        }
        item {
            ImportSection(
                importing = uiState.importing,
                onPickFile = {
                    filePicker.launch(
                        arrayOf(
                            "text/javascript",
                            "application/javascript",
                            "text/plain",
                            "application/octet-stream"
                        )
                    )
                },
                onPasteScript = { showTextDialog = true },
                onImportUrl = { showUrlDialog = true }
            )
        }
        item {
            AppleSectionHeader(
                title = stringResource(R.string.online_sources_installed, uiState.plugins.size)
            )
        }
        if (!uiState.hasPlugins) {
            item { EmptyState() }
        } else {
            items(uiState.plugins, key = { it.id }) { plugin ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    PluginRow(
                        plugin = plugin,
                        sortMode = uiState.sortMode,
                        testing = uiState.testingPluginId == plugin.id,
                        onToggle = { enabled ->
                            viewModel.handleIntent(OnlineSourcesIntent.ToggleEnabled(plugin.id, enabled))
                        },
                        onTest = { viewModel.handleIntent(OnlineSourcesIntent.TestPlugin(plugin.id)) },
                        onMoveUp = { viewModel.handleIntent(OnlineSourcesIntent.MovePlugin(plugin.id, -1)) },
                        onMoveDown = { viewModel.handleIntent(OnlineSourcesIntent.MovePlugin(plugin.id, 1)) },
                        onDelete = { viewModel.handleIntent(OnlineSourcesIntent.DeletePlugin(plugin.id)) },
                        onAutoBlock = { enabled ->
                            viewModel.handleIntent(OnlineSourcesIntent.SetAutoBlock(plugin.id, enabled))
                        },
                        onClearBlocked = {
                            viewModel.handleIntent(OnlineSourcesIntent.ClearBlockedPlatforms(plugin.id))
                        }
                    )
                    AppleSeparator()
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.online_sources_key_hint),
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )
        }
    }

    if (showTextDialog) {
        TextImportDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { content ->
                showTextDialog = false
                viewModel.handleIntent(OnlineSourcesIntent.ImportText(content))
            }
        )
    }
    if (showUrlDialog) {
        UrlImportDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url ->
                showUrlDialog = false
                viewModel.handleIntent(OnlineSourcesIntent.ImportUrl(url))
            }
        )
    }
}

@Composable
private fun PrioritySection(
    priority: PluginPriority,
    onSelect: (PluginPriority) -> Unit
) {
    AppleSectionHeader(title = stringResource(R.string.online_sources_priority))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppleChip(
            label = stringResource(R.string.online_sources_priority_fallback),
            selected = priority == PluginPriority.FALLBACK,
            onClick = { onSelect(PluginPriority.FALLBACK) }
        )
        AppleChip(
            label = stringResource(R.string.online_sources_priority_plugin_only),
            selected = priority == PluginPriority.PLUGIN_ONLY,
            onClick = { onSelect(PluginPriority.PLUGIN_ONLY) }
        )
    }
    Text(
        text = if (priority == PluginPriority.FALLBACK) {
            stringResource(R.string.online_sources_priority_fallback_desc)
        } else {
            stringResource(R.string.online_sources_priority_plugin_only_desc)
        },
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        style = AppleType.footnote,
        color = AppleTheme.colors.secondaryLabel
    )
}

@Composable
private fun SortSection(
    sortMode: PluginSortMode,
    onSelect: (PluginSortMode) -> Unit
) {
    AppleSectionHeader(title = stringResource(R.string.online_sources_sort))
    val labels = mapOf(
        PluginSortMode.MANUAL to R.string.online_sources_sort_manual,
        PluginSortMode.PLATFORM_COUNT to R.string.online_sources_sort_platform_count,
        PluginSortMode.SPEED to R.string.online_sources_sort_speed,
        PluginSortMode.AVAILABLE to R.string.online_sources_sort_available
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PluginSortMode.entries.toList()) { mode ->
            AppleChip(
                label = stringResource(labels.getValue(mode)),
                selected = mode == sortMode,
                onClick = { onSelect(mode) }
            )
        }
    }
}

@Composable
private fun ImportSection(
    importing: Boolean,
    onPickFile: () -> Unit,
    onPasteScript: () -> Unit,
    onImportUrl: () -> Unit
) {
    AppleSectionHeader(title = stringResource(R.string.settings_online_sources_menu))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AppleActionButton(
                label = stringResource(R.string.online_sources_import_file),
                onClick = onPickFile,
                enabled = !importing,
                icon = SymbolIconName.UploadFile
            )
        }
        item {
            AppleActionButton(
                label = stringResource(R.string.online_sources_import_text),
                onClick = onPasteScript,
                filled = false,
                enabled = !importing
            )
        }
        item {
            AppleActionButton(
                label = stringResource(R.string.online_sources_import_url),
                onClick = onImportUrl,
                filled = false,
                enabled = !importing
            )
        }
    }
    if (importing) {
        LinearWavyProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.online_sources_empty),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        style = AppleType.footnote,
        color = AppleTheme.colors.secondaryLabel
    )
}

/**
 * 插件行。
 *
 * 不再用卡片：iOS 的列表是"分组 + 发丝线"，卡片会给每一行套一层边框，视觉噪音太大。
 */
@Composable
private fun PluginRow(
    plugin: OnlinePlugin,
    sortMode: PluginSortMode,
    testing: Boolean,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onAutoBlock: (Boolean) -> Unit,
    onClearBlocked: () -> Unit
) {
    val colors = AppleTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = AppleType.headline,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val platformText = if (plugin.musicPlatformCount > 0) {
                    stringResource(
                        R.string.online_sources_platform_count,
                        plugin.musicPlatformCount
                    )
                } else {
                    ""
                }
                Text(
                    text = buildString {
                        if (plugin.version.isNotBlank()) append("v").append(plugin.version)
                        if (platformText.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(platformText)
                        }
                    },
                    style = AppleType.footnote,
                    color = colors.secondaryLabel
                )
            }
            Switch(checked = plugin.enabled, onCheckedChange = onToggle)
        }

        if (plugin.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = plugin.description,
                style = AppleType.footnote,
                color = colors.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (plugin.sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.online_sources_sources,
                    plugin.sources.joinToString(" / ") { it.key.displayName }
                ),
                style = AppleType.footnote,
                color = colors.secondaryLabel
            )
        }
        if (plugin.autoBlockedPlatforms.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.online_sources_blocked,
                    plugin.autoBlockedPlatforms.joinToString(" / ") { it.displayName }
                ),
                style = AppleType.footnote,
                color = colors.systemRed
            )
        }
        if (plugin.blockedPlatforms.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.online_sources_manual_blocked,
                    plugin.blockedPlatforms.joinToString(" / ") { it.displayName }
                ),
                style = AppleType.footnote,
                color = colors.systemRed
            )
        }
        val pluginError = plugin.error
        if (!pluginError.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.online_sources_error, pluginError),
                style = AppleType.footnote,
                color = colors.systemRed,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (plugin.opaque) {
            Text(
                text = stringResource(R.string.online_sources_opaque_hint),
                style = AppleType.footnote,
                color = colors.secondaryLabel
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.online_sources_auto_block),
                style = AppleType.footnote,
                color = colors.secondaryLabel
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = plugin.autoBlockInvalid, onCheckedChange = onAutoBlock)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sortMode == PluginSortMode.MANUAL) {
                PluginAction(stringResource(R.string.online_sources_move_up), onMoveUp)
                PluginAction(stringResource(R.string.online_sources_move_down), onMoveDown)
            }
            if (plugin.blockedPlatforms.isNotEmpty()) {
                PluginAction(
                    stringResource(R.string.online_sources_clear_blocked_short),
                    onClearBlocked
                )
            }
            PluginAction(
                label = if (testing) {
                    stringResource(R.string.online_sources_testing)
                } else {
                    stringResource(R.string.online_sources_test)
                },
                onClick = onTest,
                enabled = !testing
            )
            PluginAction(
                label = stringResource(R.string.online_sources_delete),
                onClick = onDelete,
                destructive = true
            )
        }
    }
}

/** 行内文字动作，颜色取 key（删除类取品牌红）。 */
@Composable
private fun PluginAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val colors = AppleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = AppleType.footnoteEmphasized,
        color = when {
            !enabled -> colors.tertiaryLabel
            destructive -> colors.systemRed
            else -> colors.key
        },
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun TextImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    AppleAlert(
        title = stringResource(R.string.online_sources_import_text_title),
        confirmLabel = stringResource(R.string.online_sources_import_confirm),
        onConfirm = { onConfirm(content) },
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.online_sources_cancel),
        confirmEnabled = content.isNotBlank()
    ) {
        AppleTextField(
            value = content,
            onValueChange = { content = it },
            singleLine = false,
            minHeight = 140.dp,
            placeholder = stringResource(R.string.online_sources_import_text_title)
        )
    }
}

@Composable
private fun UrlImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AppleAlert(
        title = stringResource(R.string.online_sources_import_url_title),
        confirmLabel = stringResource(R.string.online_sources_import_confirm),
        onConfirm = { onConfirm(url) },
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.online_sources_cancel),
        confirmEnabled = url.isNotBlank()
    ) {
        AppleTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = "https://"
        )
    }
}
