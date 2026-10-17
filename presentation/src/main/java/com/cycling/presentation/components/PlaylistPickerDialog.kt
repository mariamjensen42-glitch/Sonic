package com.cycling.presentation.components

import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerDialog(
    visible: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long) -> Unit,
    onCreateNewPlaylist: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateDialog by remember { mutableStateOf(false) }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = SonSpacing.large)
            ) {
                Text(
                    text = stringResource(R.string.playlist_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = SonSpacing.large, vertical = SonSpacing.small)
                )

                ListItem(
                    content = {
                        Text(text = stringResource(R.string.playlist_picker_new))
                    },
                    leadingContent = {
                        M3SymbolIcon(
                            name = SymbolIconName.Add,
                            filled = true,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(SonSizes.iconMedium)
                        )
                    },
                    onClick = { showCreateDialog = true }
                )

                if (playlists.isEmpty()) {
                    EmptyPlaylistsContent()
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistPickerItem(
                                playlist = playlist,
                                onClick = { onPlaylistSelected(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistInputDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateNewPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistPickerItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    ListItem(
        content = {
            Text(text = playlist.name)
        },
        leadingContent = {
            M3SymbolIcon(
                name = SymbolIconName.QueueMusic,
                filled = true,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SonSizes.iconMedium)
            )
        },
        trailingContent = {
            Text(
                text = stringResource(R.string.playlist_picker_song_count, playlist.numberOfSongs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = onClick
    )
}

@Composable
private fun EmptyPlaylistsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        M3SymbolIcon(
            name = SymbolIconName.LibraryMusic,
            filled = true,
            contentDescription = null,
            modifier = Modifier.size(SonSizes.iconExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
        Text(
            text = stringResource(R.string.playlist_picker_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))
        Text(
            text = stringResource(R.string.playlist_picker_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreatePlaylistInputDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_picker_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(text = stringResource(R.string.playlist_picker_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.playlist_picker_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_picker_cancel))
            }
        }
    )
}
