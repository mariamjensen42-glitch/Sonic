@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.onlinesources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
        contentPadding = PaddingValues(bottom = SonLayout.bottomBarClearance + bottomPadding)
    ) {
        item { PrioritySection(uiState.priority) { viewModel.handleIntent(OnlineSourcesIntent.SetPriority(it)) } }
        item { SortSection(uiState.sortMode) { viewModel.handleIntent(OnlineSourcesIntent.SetSortMode(it)) } }
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
            SectionTitle(
                stringResource(R.string.online_sources_installed, uiState.plugins.size)
            )
        }
        if (!uiState.hasPlugins) {
            item { EmptyState() }
        } else {
            items(uiState.plugins, key = { it.id }) { plugin ->
                PluginCard(
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
            }
        }
        item {
            Text(
                text = stringResource(R.string.online_sources_key_hint),
                modifier = Modifier.padding(
                    start = SonLayout.screenMargin,
                    end = SonLayout.screenMargin,
                    top = SonSpacing.medium,
                    bottom = SonSpacing.large
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = SonLayout.screenMargin,
            end = SonLayout.screenMargin,
            top = SonSpacing.large,
            bottom = SonSpacing.small
        ),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PrioritySection(
    priority: PluginPriority,
    onSelect: (PluginPriority) -> Unit
) {
    SectionTitle(stringResource(R.string.online_sources_priority))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = priority == PluginPriority.FALLBACK,
                onClick = { onSelect(PluginPriority.FALLBACK) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text(stringResource(R.string.online_sources_priority_fallback)) }
            )
            SegmentedButton(
                selected = priority == PluginPriority.PLUGIN_ONLY,
                onClick = { onSelect(PluginPriority.PLUGIN_ONLY) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text(stringResource(R.string.online_sources_priority_plugin_only)) }
            )
        }
        Text(
            text = if (priority == PluginPriority.FALLBACK) {
                stringResource(R.string.online_sources_priority_fallback_desc)
            } else {
                stringResource(R.string.online_sources_priority_plugin_only_desc)
            },
            modifier = Modifier.padding(top = SonSpacing.small),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SortSection(
    sortMode: PluginSortMode,
    onSelect: (PluginSortMode) -> Unit
) {
    SectionTitle(stringResource(R.string.online_sources_sort))
    val labels = mapOf(
        PluginSortMode.MANUAL to R.string.online_sources_sort_manual,
        PluginSortMode.PLATFORM_COUNT to R.string.online_sources_sort_platform_count,
        PluginSortMode.SPEED to R.string.online_sources_sort_speed,
        PluginSortMode.AVAILABLE to R.string.online_sources_sort_available
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        items(PluginSortMode.entries.toList()) { mode ->
            FilterChip(
                selected = mode == sortMode,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(labels.getValue(mode))) }
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
    SectionTitle(stringResource(R.string.settings_online_sources_menu))
    LazyRow(
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        item {
            FilledTonalButton(onClick = onPickFile, enabled = !importing) {
                M3SymbolIcon(
                    name = SymbolIconName.UploadFile,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.buttonIcon)
                )
                Spacer(modifier = Modifier.width(SonSpacing.small))
                Text(stringResource(R.string.online_sources_import_file))
            }
        }
        item {
            OutlinedButton(onClick = onPasteScript, enabled = !importing) {
                Text(stringResource(R.string.online_sources_import_text))
            }
        }
        item {
            OutlinedButton(onClick = onImportUrl, enabled = !importing) {
                Text(stringResource(R.string.online_sources_import_url))
            }
        }
    }
    if (importing) {
        LinearWavyProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small)
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.online_sources_empty),
        modifier = Modifier.padding(
            start = SonLayout.screenMargin,
            end = SonLayout.screenMargin,
            top = SonSpacing.medium,
            bottom = SonSpacing.large
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PluginCard(
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
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small)
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = plugin.enabled, onCheckedChange = onToggle)
            }

            if (plugin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(SonSpacing.small))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (plugin.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(SonSpacing.small))
                Text(
                    text = stringResource(
                        R.string.online_sources_sources,
                        plugin.sources.joinToString(" / ") { it.key.displayName }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (plugin.autoBlockedPlatforms.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.online_sources_blocked,
                        plugin.autoBlockedPlatforms.joinToString(" / ") { it.displayName }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (plugin.blockedPlatforms.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.online_sources_manual_blocked,
                        plugin.blockedPlatforms.joinToString(" / ") { it.displayName }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            val pluginError = plugin.error
            if (!pluginError.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.online_sources_error, pluginError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (plugin.opaque) {
                Text(
                    text = stringResource(R.string.online_sources_opaque_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(SonSpacing.small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.online_sources_auto_block),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(SonSpacing.small))
                Switch(
                    checked = plugin.autoBlockInvalid,
                    onCheckedChange = onAutoBlock
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sortMode == PluginSortMode.MANUAL) {
                    TextButton(onClick = onMoveUp) { Text(stringResource(R.string.online_sources_move_up)) }
                    TextButton(onClick = onMoveDown) { Text(stringResource(R.string.online_sources_move_down)) }
                }
                if (plugin.blockedPlatforms.isNotEmpty()) {
                    TextButton(onClick = onClearBlocked) {
                        Text(stringResource(R.string.online_sources_clear_blocked_short))
                    }
                }
                TextButton(onClick = onTest, enabled = !testing) {
                    Text(
                        if (testing) {
                            stringResource(R.string.online_sources_testing)
                        } else {
                            stringResource(R.string.online_sources_test)
                        }
                    )
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.online_sources_delete))
                }
            }
        }
    }
}

@Composable
private fun TextImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.online_sources_import_text_title)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                minLines = 6,
                maxLines = 12
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }, enabled = content.isNotBlank()) {
                Text(stringResource(R.string.online_sources_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.online_sources_cancel)) }
        }
    )
}

@Composable
private fun UrlImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.online_sources_import_url_title)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }, enabled = url.isNotBlank()) {
                Text(stringResource(R.string.online_sources_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.online_sources_cancel)) }
        }
    )
}
