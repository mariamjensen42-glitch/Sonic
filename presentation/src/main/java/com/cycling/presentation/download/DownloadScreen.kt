@file:OptIn(ExperimentalMaterial3Api::class)

package com.cycling.presentation.download

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.DownloadState
import com.cycling.domain.model.DownloadTask
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import java.util.Locale

/**
 * 下载管理：正在下载 / 排队 / 已暂停 / 失败 / 已完成 一屏可见。
 *
 * 进度条用静态 [LinearProgressIndicator]（不是波浪版）：这一屏会长时间停留，
 * 波浪的无限重绘没有必要，见 AGENTS 的「常驻动画必须自己收口」。
 */
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                DownloadEffect.NavigateBack -> onNavigateBack()
                is DownloadEffect.ShowToast -> Toast.makeText(
                    context,
                    context.getString(effect.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(DownloadIntent.NavigateBack) }) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            contentDescription = stringResource(R.string.download_back)
                        )
                    }
                },
                actions = {
                    if (uiState.hasCompleted) {
                        TextButton(
                            onClick = {
                                viewModel.handleIntent(DownloadIntent.RequestClearCompleted)
                            }
                        ) {
                            Text(stringResource(R.string.download_clear_completed))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + SonLayout.bottomBarClearance
        )
        when {
            uiState.isLoading -> SonLoadingState()
            uiState.tasks.isEmpty() -> SonEmptyState(
                icon = SymbolIconName.Folder,
                title = stringResource(R.string.download_empty_title),
                subtitle = stringResource(R.string.download_empty_subtitle)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                items(uiState.tasks, key = { it.id }) { task ->
                    DownloadRow(
                        task = task,
                        onPause = { viewModel.handleIntent(DownloadIntent.Pause(task.id)) },
                        onResume = { viewModel.handleIntent(DownloadIntent.Resume(task.id)) },
                        onCancel = { viewModel.handleIntent(DownloadIntent.Cancel(task.id)) },
                        onRemove = { viewModel.handleIntent(DownloadIntent.Remove(task.id)) }
                    )
                }
            }
        }
    }

    if (uiState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = {
                viewModel.handleIntent(DownloadIntent.DismissClearCompleted)
            },
            title = { Text(stringResource(R.string.download_clear_completed)) },
            text = { Text(stringResource(R.string.download_clear_completed_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(DownloadIntent.ConfirmClearCompleted) }
                ) {
                    Text(stringResource(R.string.download_clear_completed_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(DownloadIntent.DismissClearCompleted) }
                ) {
                    Text(stringResource(R.string.download_quality_cancel))
                }
            }
        )
    }
}

@Composable
private fun DownloadRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val stateText = when (task.state) {
        DownloadState.QUEUED -> stringResource(R.string.download_state_queued)
        DownloadState.RUNNING -> if (task.totalBytes > 0) {
            "${(task.progress * 100).toInt()}% · " +
                "${formatBytes(task.bytesDownloaded)} / ${formatBytes(task.totalBytes)}"
        } else {
            stringResource(R.string.download_state_resolving)
        }

        DownloadState.PAUSED -> stringResource(R.string.download_state_paused)
        DownloadState.COMPLETED -> stringResource(R.string.download_state_completed)
        DownloadState.FAILED -> task.errorMessage ?: stringResource(R.string.download_state_failed)
    }
    val supporting = "${task.artist} · $stateText · ${task.quality.displayName}"

    ListItem(
        leadingContent = {
            M3Artwork(
                artwork = task.albumArt,
                shape = MaterialTheme.shapes.small,
                contentDescription = null,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        },
        headlineContent = {
            Text(text = task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)) {
                Text(
                    text = supporting,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
                if (task.totalBytes > 0 &&
                    (task.state == DownloadState.RUNNING || task.state == DownloadState.PAUSED)
                ) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = SonSpacing.medium)
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (task.state) {
                    DownloadState.RUNNING -> IconButton(onClick = onPause) {
                        M3SymbolIcon(
                            name = SymbolIconName.Pause,
                            contentDescription = stringResource(R.string.download_action_pause)
                        )
                    }

                    DownloadState.QUEUED, DownloadState.PAUSED, DownloadState.FAILED -> IconButton(
                        onClick = onResume
                    ) {
                        M3SymbolIcon(
                            name = SymbolIconName.PlayArrow,
                            contentDescription = stringResource(R.string.download_action_resume)
                        )
                    }

                    DownloadState.COMPLETED -> IconButton(onClick = onRemove) {
                        M3SymbolIcon(
                            name = SymbolIconName.Close,
                            contentDescription = stringResource(R.string.download_action_remove)
                        )
                    }
                }
                if (task.state != DownloadState.COMPLETED) {
                    IconButton(onClick = onCancel) {
                        M3SymbolIcon(
                            name = SymbolIconName.Delete,
                            contentDescription = stringResource(R.string.download_action_cancel)
                        )
                    }
                }
            }
        }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1073741824.0)
    bytes >= 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0)
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
