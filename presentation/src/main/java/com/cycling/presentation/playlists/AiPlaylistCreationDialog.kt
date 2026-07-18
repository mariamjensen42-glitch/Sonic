package com.cycling.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.cycling.domain.model.Song
import com.cycling.core.ui.components.M3FilledButton
import com.cycling.core.ui.components.M3SegmentedButtonRow
import com.cycling.core.ui.theme.M3SemanticColors

enum class AiPlaylistCreationStep {
    SELECT_MODE,
    INPUT_DETAILS,
    PREVIEW
}

enum class AiPlaylistMode {
    RANDOM,
    THEME
}

data class AiPlaylistCreationState(
    val step: AiPlaylistCreationStep = AiPlaylistCreationStep.SELECT_MODE,
    val mode: AiPlaylistMode = AiPlaylistMode.RANDOM,
    val songCount: Int = 10,
    val theme: String = "",
    val playlistName: String = "",
    val generatedSongs: List<Song> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null
)

@Composable
fun AiPlaylistCreationDialog(
    uiState: PlaylistsUiState,
    onIntent: (PlaylistsIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var creationState by remember { mutableStateOf(AiPlaylistCreationState()) }
    var sliderValue by remember { mutableFloatStateOf(uiState.selectedSongCount.toFloat()) }

    LaunchedEffect(uiState.aiGeneratedSongs, uiState.isAiGenerating, uiState.aiCreateError) {
        creationState = creationState.copy(
            generatedSongs = uiState.aiGeneratedSongs,
            isGenerating = uiState.isAiGenerating,
            error = uiState.aiCreateError
        )
    }

    when (creationState.step) {
        AiPlaylistCreationStep.SELECT_MODE -> {
            SelectModeDialog(
                selectedMode = creationState.mode,
                onModeSelected = { mode ->
                    creationState = creationState.copy(mode = mode)
                    onIntent(PlaylistsIntent.SetAiCreateMode(mode.toAiCreateMode()))
                },
                onNext = {
                    creationState = creationState.copy(step = AiPlaylistCreationStep.INPUT_DETAILS)
                },
                onDismiss = onDismiss
            )
        }

        AiPlaylistCreationStep.INPUT_DETAILS -> {
            InputDetailsDialog(
                mode = creationState.mode,
                songCount = sliderValue.toInt(),
                theme = uiState.themeInput,
                onSongCountChange = { 
                    sliderValue = it.toFloat()
                    onIntent(PlaylistsIntent.SetSelectedSongCount(it))
                },
                onThemeChange = { 
                    onIntent(PlaylistsIntent.SetThemeInput(it))
                },
                onGenerate = {
                    creationState = creationState.copy(
                        songCount = sliderValue.toInt(),
                        step = AiPlaylistCreationStep.PREVIEW
                    )
                    onIntent(PlaylistsIntent.GenerateAiPlaylist)
                },
                onBack = {
                    creationState = creationState.copy(step = AiPlaylistCreationStep.SELECT_MODE)
                },
                onDismiss = onDismiss
            )
        }

        AiPlaylistCreationStep.PREVIEW -> {
            PreviewDialog(
                playlistName = uiState.aiGeneratedPlaylistName.ifEmpty { 
                    if (creationState.mode == AiPlaylistMode.RANDOM) "随机歌单" else uiState.themeInput
                },
                songs = uiState.aiGeneratedSongs,
                isLoading = uiState.isAiGenerating,
                error = uiState.aiCreateError,
                onCreate = {
                    onIntent(PlaylistsIntent.ConfirmAiPlaylist)
                    onDismiss()
                },
                onRegenerate = {
                    onIntent(PlaylistsIntent.GenerateAiPlaylist)
                },
                onDismiss = onDismiss
            )
        }
    }
}

private fun AiPlaylistMode.toAiCreateMode(): AiCreateMode {
    return when (this) {
        AiPlaylistMode.RANDOM -> AiCreateMode.RANDOM
        AiPlaylistMode.THEME -> AiCreateMode.THEME
    }
}

@Composable
private fun SelectModeDialog(
    selectedMode: AiPlaylistMode,
    onModeSelected: (AiPlaylistMode) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AI 创建歌单",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "选择创建方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                M3SegmentedButtonRow(
                    options = listOf("随机创建", "主题创建"),
                    selectedIndex = if (selectedMode == AiPlaylistMode.RANDOM) 0 else 1,
                    onOptionSelected = { index ->
                        onModeSelected(if (index == 0) AiPlaylistMode.RANDOM else AiPlaylistMode.THEME)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (selectedMode == AiPlaylistMode.RANDOM) {
                        "随机从您的音乐库中选择歌曲"
                    } else {
                        "根据主题描述智能推荐歌曲"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            M3FilledButton(onClick = onNext) {
                Text("下一步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun InputDetailsDialog(
    mode: AiPlaylistMode,
    songCount: Int,
    theme: String,
    onSongCountChange: (Int) -> Unit,
    onThemeChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val minCount = if (mode == AiPlaylistMode.RANDOM) 5 else 5
    val maxCount = if (mode == AiPlaylistMode.RANDOM) 50 else 30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == AiPlaylistMode.RANDOM) "随机创建" else "主题创建",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (mode == AiPlaylistMode.THEME) {
                    OutlinedTextField(
                        value = theme,
                        onValueChange = onThemeChange,
                        placeholder = { 
                            Text("例如: 适合跑步的音乐、安静的夜晚") 
                        },
                        label = { Text("主题描述") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "歌曲数量",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$songCount 首",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = songCount.toFloat(),
                        onValueChange = { onSongCountChange(it.toInt()) },
                        valueRange = minCount.toFloat()..maxCount.toFloat(),
                        steps = maxCount - minCount - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$minCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$maxCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            M3FilledButton(
                onClick = onGenerate,
                enabled = !(mode == AiPlaylistMode.THEME && theme.isBlank())
            ) {
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    )
}

@Composable
private fun PreviewDialog(
    playlistName: String,
    songs: List<Song>,
    isLoading: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = playlistName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "正在生成歌单...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "生成失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                M3FilledButton(
                                    onClick = onRegenerate
                                ) {
                                    Text("重试")
                                }
                            }
                        }
                    }

                    songs.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无歌曲",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                PreviewSongItem(song = song)
                            }
                        }
                    }
                }

                if (!isLoading && error == null && songs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "共 ${songs.size} 首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!isLoading && error == null && songs.isNotEmpty()) {
                M3FilledButton(onClick = onCreate) {
                    Text("创建")
                }
            }
        },
        dismissButton = {
            Row {
                if (!isLoading && error == null && songs.isNotEmpty()) {
                    TextButton(onClick = onRegenerate) {
                        Text("重新生成")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = false
        )
    )
}

@Composable
private fun PreviewSongItem(
    song: Song,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = M3SemanticColors.Red
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
