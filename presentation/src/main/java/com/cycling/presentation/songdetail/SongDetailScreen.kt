package com.cycling.presentation.songdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cycling.core.ui.components.M3FilledButton
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToTagEditor: (Long) -> Unit = {},
    onNavigateToApiKeyConfig: () -> Unit = {},
    viewModel: SongDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SongDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is SongDetailEffect.ShowAddToPlaylistDialog -> {}
                is SongDetailEffect.ShowCopiedMessage -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Song Path", context.getString(effect.message))
                    clipboard.setPrimaryClip(clip)
                }
                is SongDetailEffect.ShowError -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_LONG).show()
                }
                is SongDetailEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = { viewModel.handleIntent(SongDetailIntent.DiscardChanges) },
            onKeepEditing = { viewModel.handleIntent(SongDetailIntent.KeepEditing) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) stringResource(R.string.song_detail_title_edit) else stringResource(R.string.song_detail_title_view),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditMode) {
                            viewModel.handleIntent(SongDetailIntent.ExitEditMode)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.song_detail_back_desc)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(
                            onClick = { viewModel.handleIntent(SongDetailIntent.SaveChanges) },
                            enabled = uiState.hasChanges && !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.song_detail_save_desc),
                                    tint = if (uiState.hasChanges) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        uiState.song?.let {
                            IconButton(onClick = { viewModel.handleIntent(SongDetailIntent.EnterEditMode) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.song_detail_edit_desc)
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            uiState.song?.let { song ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.isEditMode) {
                        EditModeContent(
                            song = song,
                            uiState = uiState,
                            onTitleChange = { viewModel.handleIntent(SongDetailIntent.UpdateTitle(it)) },
                            onArtistChange = { viewModel.handleIntent(SongDetailIntent.UpdateArtist(it)) },
                            onAlbumChange = { viewModel.handleIntent(SongDetailIntent.UpdateAlbum(it)) },
                            onYearChange = { viewModel.handleIntent(SongDetailIntent.UpdateYear(it)) },
                            onGenreChange = { viewModel.handleIntent(SongDetailIntent.UpdateGenre(it)) },
                            onSave = { viewModel.handleIntent(SongDetailIntent.SaveChanges) },
                            onCancel = { viewModel.handleIntent(SongDetailIntent.ExitEditMode) }
                        )
                    } else {
                        ViewModeContent(
                            song = song,
                            uiState = uiState,
                            onPlay = { viewModel.handleIntent(SongDetailIntent.PlaySong) },
                            onFavorite = { viewModel.handleIntent(SongDetailIntent.ToggleFavorite) },
                            onAddToPlaylist = { viewModel.handleIntent(SongDetailIntent.AddToPlaylist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeContent(
    song: Song,
    uiState: SongDetailUiState,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    SongHeader(song = song)

    Spacer(modifier = Modifier.height(24.dp))

    ActionButtons(
        isFavorite = uiState.isFavorite,
        onPlay = onPlay,
        onFavorite = onFavorite,
        onAddToPlaylist = onAddToPlaylist
    )

    Spacer(modifier = Modifier.height(24.dp))

    SongInfoSection(song = song, audioMetadata = uiState.audioMetadata)
}

@Composable
private fun EditModeContent(
    song: Song,
    uiState: SongDetailUiState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFFF453A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            EditTextField(
                label = stringResource(R.string.song_detail_title_label),
                value = uiState.editedTitle,
                onValueChange = onTitleChange,
                placeholder = stringResource(R.string.song_detail_title_placeholder)
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditTextField(
                label = stringResource(R.string.song_detail_artist_label),
                value = uiState.editedArtist,
                onValueChange = onArtistChange,
                placeholder = stringResource(R.string.song_detail_artist_placeholder)
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditTextField(
                label = stringResource(R.string.song_detail_album_label),
                value = uiState.editedAlbum,
                onValueChange = onAlbumChange,
                placeholder = stringResource(R.string.song_detail_album_placeholder)
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditTextField(
                label = stringResource(R.string.song_detail_year_label),
                value = uiState.editedYear,
                onValueChange = onYearChange,
                placeholder = stringResource(R.string.song_detail_year_placeholder),
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditTextField(
                label = stringResource(R.string.song_detail_genre_label),
                value = uiState.editedGenre,
                onValueChange = onGenreChange,
                placeholder = stringResource(R.string.song_detail_genre_placeholder)
            )

            Spacer(modifier = Modifier.height(24.dp))

            M3FilledButton(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.hasChanges && !uiState.isSaving
            ) {
                Text(text = if (uiState.isSaving) stringResource(R.string.song_detail_saving) else stringResource(R.string.song_detail_save))
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.song_detail_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(text = stringResource(R.string.song_detail_discard_title)) },
        text = { Text(text = stringResource(R.string.song_detail_discard_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text = stringResource(R.string.song_detail_discard),
                    color = Color(0xFFFF453A)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(text = stringResource(R.string.song_detail_keep_editing))
            }
        }
    )
}

@Composable
private fun SongHeader(song: Song) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color(0xFFFF453A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = song.album,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtons(
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        M3FilledButton(
            onClick = onPlay,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(R.string.song_detail_play))
        }

        IconButton(
            onClick = onFavorite,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isFavorite) Color(0xFFFF453A).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) stringResource(R.string.player_unfavorite_desc) else stringResource(R.string.player_favorite_desc),
                tint = if (isFavorite) Color(0xFFFF453A) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(R.string.song_detail_add_to_playlist_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SongInfoSection(song: Song, audioMetadata: AudioMetadata?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.song_detail_section_basic),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoItem(label = stringResource(R.string.song_detail_duration), value = formatDuration(song.duration))
        InfoItem(label = stringResource(R.string.song_detail_format), value = getFormatFromMimeType(song.mimeType, stringResource(R.string.song_detail_unknown)))
        InfoItem(label = stringResource(R.string.song_detail_file_size), value = formatFileSize(song.size))

        audioMetadata?.let { metadata ->
            if (metadata.year != null) {
                InfoItem(label = stringResource(R.string.song_detail_year), value = metadata.year!!)
            }
            if (metadata.genre != null) {
                InfoItem(label = stringResource(R.string.song_detail_genre), value = metadata.genre!!)
            }
            if (metadata.composer != null) {
                InfoItem(label = stringResource(R.string.song_detail_composer), value = metadata.composer!!)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.song_detail_section_audio),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val bitrate = audioMetadata?.bitrate ?: song.bitrate
        InfoItem(label = stringResource(R.string.song_detail_bitrate), value = if (bitrate > 0) "$bitrate kbps" else stringResource(R.string.song_detail_unknown))
        
        audioMetadata?.let { metadata ->
            if (metadata.sampleRate > 0) {
                InfoItem(label = stringResource(R.string.song_detail_sample_rate), value = "${metadata.sampleRate} Hz")
            }
            if (metadata.channels > 0) {
                InfoItem(label = stringResource(R.string.song_detail_channels), value = if (metadata.channels == 2) stringResource(R.string.song_detail_stereo) else stringResource(R.string.song_detail_mono))
            }
            if (metadata.encodingType.isNotEmpty()) {
                InfoItem(label = stringResource(R.string.song_detail_encoding), value = metadata.encodingType)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.song_detail_section_file),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoItem(label = stringResource(R.string.song_detail_added_date), value = formatDate(song.dateAdded, stringResource(R.string.song_detail_unknown)))
        InfoItem(label = stringResource(R.string.song_detail_modified_date), value = formatDate(song.dateModified, stringResource(R.string.song_detail_unknown)))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.song_detail_section_path),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        PathItem(path = song.path)
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

@Composable
private fun PathItem(path: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        val context = LocalContext.current
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Song Path", path)
                clipboard.setPrimaryClip(clip)
            }
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.song_detail_copy_desc),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.song_detail_copy))
        }
    }
}

private fun getFormatFromMimeType(mimeType: String, unknownLabel: String = "未知"): String {
    return when {
        mimeType.contains("mp3", ignoreCase = true) -> "MP3"
        mimeType.contains("mpeg", ignoreCase = true) -> "MPEG"
        mimeType.contains("mp4", ignoreCase = true) -> "MP4"
        mimeType.contains("flac", ignoreCase = true) -> "FLAC"
        mimeType.contains("wav", ignoreCase = true) -> "WAV"
        mimeType.contains("aac", ignoreCase = true) -> "AAC"
        mimeType.contains("ogg", ignoreCase = true) -> "OGG"
        mimeType.contains("wma", ignoreCase = true) -> "WMA"
        mimeType.contains("m4a", ignoreCase = true) -> "M4A"
        mimeType.contains("3gpp", ignoreCase = true) -> "3GPP"
        else -> mimeType.ifEmpty { unknownLabel }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size >= 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f GB", size.toDouble() / (1024 * 1024 * 1024))
        size >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", size.toDouble() / (1024 * 1024))
        size >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", size.toDouble() / 1024)
        else -> "$size B"
    }
}

private fun formatDate(timestamp: Long, unknownLabel: String = "未知"): String {
    if (timestamp <= 0) return unknownLabel
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
