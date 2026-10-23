package com.cycling.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.model.Playlist
import com.cycling.presentation.R

@Composable
fun PlaylistPickerDialog(
    visible: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (playlistId: Long) -> Unit,
    onCreateNewPlaylist: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (visible) {
        AppleSheet(onDismiss = onDismiss, modifier = modifier) {
            AppleSheetTitle(title = stringResource(R.string.playlist_picker_title))

            AppleSheetRow(
                title = stringResource(R.string.playlist_picker_new),
                onClick = { showCreateDialog = true },
                trailing = {
                    M3SymbolIcon(
                        name = SymbolIconName.Add,
                        contentDescription = null,
                        filled = true,
                        tint = AppleTheme.colors.key,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            if (playlists.isEmpty()) {
                EmptyPlaylistsContent()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        AppleSheetRow(
                            title = playlist.name,
                            subtitle = stringResource(
                                R.string.playlist_picker_song_count,
                                playlist.numberOfSongs
                            ),
                            onClick = { onPlaylistSelected(playlist.id) }
                        )
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
private fun EmptyPlaylistsContent() {
    val colors = AppleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        M3SymbolIcon(
            name = SymbolIconName.LibraryMusic,
            contentDescription = null,
            filled = true,
            tint = colors.tertiaryLabel,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.playlist_picker_empty_title),
            style = AppleType.headline,
            color = colors.label
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.playlist_picker_empty_subtitle),
            style = AppleType.footnote,
            color = colors.secondaryLabel,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreatePlaylistInputDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AppleAlert(
        title = stringResource(R.string.playlist_picker_dialog_title),
        confirmLabel = stringResource(R.string.playlist_picker_create),
        onConfirm = { if (name.isNotBlank()) onCreate(name.trim()) },
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.playlist_picker_cancel),
        confirmEnabled = name.isNotBlank()
    ) {
        AppleTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.playlist_picker_name_placeholder)
        )
    }
}
