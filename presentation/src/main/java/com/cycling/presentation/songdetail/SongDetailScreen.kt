@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonColorFieldBackground
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.PlaylistPickerDialog
import com.cycling.presentation.components.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 沉浸式头部的封面尺寸：无对应 token，这里集中一处命名，避免散落魔法值。 */

/** 歌词折叠时显示的行数：大约一屏，再多就会把下面的信息区推得太远。 */
private const val CollapsedLyricsLines = 12

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) {
                            stringResource(R.string.song_detail_title_edit)
                        } else {
                            stringResource(R.string.song_detail_title_view)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isEditMode) {
                                viewModel.handleIntent(SongDetailIntent.ExitEditMode)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.song_detail_back_desc)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        if (uiState.isSaving) {
                            LoadingIndicator(modifier = Modifier.size(SonSizes.iconMedium))
                        } else {
                            IconButton(
                                onClick = { viewModel.handleIntent(SongDetailIntent.SaveChanges) },
                                enabled = uiState.hasChanges
                            ) {
                                M3SymbolIcon(
                                    name = SymbolIconName.Check,
                                    filled = true,
                                    contentDescription = stringResource(R.string.song_detail_save_desc)
                                )
                            }
                        }
                    } else {
                        uiState.song?.let {
                            IconButton(
                                onClick = { viewModel.handleIntent(SongDetailIntent.EnterEditMode) }
                            ) {
                                M3SymbolIcon(
                                    name = SymbolIconName.Edit,
                                    filled = true,
                                    contentDescription = stringResource(R.string.song_detail_edit_desc)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
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
                            onLyricsChange = { viewModel.handleIntent(SongDetailIntent.UpdateLyrics(it)) },
                            onSave = { viewModel.handleIntent(SongDetailIntent.SaveChanges) },
                            onCancel = { viewModel.handleIntent(SongDetailIntent.ExitEditMode) }
                        )
                    } else {
                        ViewModeContent(
                            song = song,
                            uiState = uiState,
                            onPlay = { viewModel.handleIntent(SongDetailIntent.PlaySong) },
                            onFavorite = { viewModel.handleIntent(SongDetailIntent.ToggleFavorite) },
                            onAddToPlaylist = { viewModel.handleIntent(SongDetailIntent.AddToPlaylist) },
                            onEditLyrics = { viewModel.handleIntent(SongDetailIntent.EnterEditMode) }
                        )
                    }
                }
            }
        }
    }

    PlaylistPickerDialog(
        visible = uiState.showAddToPlaylistDialog,
        playlists = uiState.playlists,
        onDismiss = { viewModel.handleIntent(SongDetailIntent.DismissAddToPlaylistDialog) },
        onPlaylistSelected = { playlistId ->
            viewModel.handleIntent(SongDetailIntent.AddToPlaylistTo(playlistId))
        },
        onCreateNewPlaylist = { viewModel.handleIntent(SongDetailIntent.CreatePlaylistAndAddSong(it)) }
    )
}

@Composable
private fun ViewModeContent(
    song: Song,
    uiState: SongDetailUiState,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditLyrics: () -> Unit
) {
    // 头部色面：封面背后一层 primaryContainer 光晕，到封面下沿收干净；
    // 标题以下（歌词、文件属性）全部落在 surface 上，信息密度不受影响。
    Box(modifier = Modifier.fillMaxWidth()) {
        SonColorFieldBackground(
            heroBottom = SonSizes.headerArtwork + SonSpacing.extraLarge + SonSpacing.medium,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        SongHeader(
            song = song,
            isFavorite = uiState.isFavorite,
            onPlay = onPlay,
            onFavorite = onFavorite,
            onAddToPlaylist = onAddToPlaylist
        )
    }

    Spacer(modifier = Modifier.height(SonSpacing.large))

    // 歌词排在信息之前：它是这首歌的内容，下面的时长、比特率只是文件属性。
    LyricsSection(
        lyrics = uiState.lyrics,
        isLoading = uiState.isLoadingLyrics,
        onEditLyrics = onEditLyrics
    )

    SongInfoSection(song = song, audioMetadata = uiState.audioMetadata)

    // 列表底部预留浮动导航栏空间。
    Spacer(modifier = Modifier.height(SonLayout.bottomBarClearance))
}

/**
 * 歌词只读区。
 *
 * 折叠而不是全展开：一首歌的歌词往往比页面其余部分加起来还长，默认摊开会把音频信息、文件信息
 * 挤到很远的地方。阈值之外给一个展开按钮，两种需求都能满足。
 */
@Composable
private fun LyricsSection(
    lyrics: List<LyricsDisplayLine>,
    isLoading: Boolean,
    onEditLyrics: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        InfoSectionTitle(stringResource(R.string.song_detail_section_lyrics))

        when {
            lyrics.isEmpty() && isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SonSpacing.large),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.size(SonSizes.iconLarge))
            }

            lyrics.isEmpty() -> EmptyLyrics(onEditLyrics = onEditLyrics)

            else -> {
                val visible = if (expanded) lyrics else lyrics.take(CollapsedLyricsLines)
                Column(verticalArrangement = Arrangement.spacedBy(SonSpacing.mediumSmall)) {
                    visible.forEach { line ->
                        LyricsLine(line = line)
                    }
                }

                if (lyrics.size > CollapsedLyricsLines) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(top = SonSpacing.small)
                    ) {
                        Text(
                            text = stringResource(
                                if (expanded) R.string.song_detail_lyrics_collapse
                                else R.string.song_detail_lyrics_expand
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SonSpacing.medium))
    }
}

@Composable
private fun LyricsLine(line: LyricsDisplayLine) {
    Column(verticalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 空态不是一句「暂无歌词」就完事：这里正是用户想补一份歌词时会看到的地方。 */
@Composable
private fun EmptyLyrics(onEditLyrics: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.song_detail_lyrics_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onEditLyrics,
            modifier = Modifier.padding(top = SonSpacing.extraSmall)
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Edit,
                filled = true,
                contentDescription = null,
                modifier = Modifier.size(SonSizes.buttonIcon)
            )
            Spacer(modifier = Modifier.width(SonSpacing.small))
            Text(text = stringResource(R.string.song_detail_lyrics_add))
        }
    }
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
    onLyricsChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        M3Artwork(
            artwork = song.albumArt,
            placeholderIcon = SymbolIconName.MusicNote,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .size(SonSizes.headerArtwork)
                .align(Alignment.CenterHorizontally)
                .padding(top = SonSpacing.medium)
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        EditTextField(
            label = stringResource(R.string.song_detail_title_label),
            value = uiState.editedTitle,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.song_detail_title_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_artist_label),
            value = uiState.editedArtist,
            onValueChange = onArtistChange,
            placeholder = stringResource(R.string.song_detail_artist_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_album_label),
            value = uiState.editedAlbum,
            onValueChange = onAlbumChange,
            placeholder = stringResource(R.string.song_detail_album_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_year_label),
            value = uiState.editedYear,
            onValueChange = onYearChange,
            placeholder = stringResource(R.string.song_detail_year_placeholder),
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_genre_label),
            value = uiState.editedGenre,
            onValueChange = onGenreChange,
            placeholder = stringResource(R.string.song_detail_genre_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        OutlinedTextField(
            value = uiState.editedLyrics,
            onValueChange = onLyricsChange,
            label = { Text(text = stringResource(R.string.song_detail_lyrics_label)) },
            placeholder = { Text(text = stringResource(R.string.song_detail_lyrics_placeholder)) },
            supportingText = { Text(text = stringResource(R.string.song_detail_lyrics_hint)) },
            minLines = 6,
            maxLines = 14,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.hasChanges && !uiState.isSaving
        ) {
            Text(text = if (uiState.isSaving) stringResource(R.string.song_detail_saving) else stringResource(R.string.song_detail_save))
        }

        Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.song_detail_cancel))
        }

        Spacer(modifier = Modifier.height(SonSpacing.extraLarge))
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
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
                    color = MaterialTheme.colorScheme.error
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
private fun SongHeader(
    song: Song,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        M3Artwork(
            artwork = song.albumArt,
            placeholderIcon = SymbolIconName.MusicNote,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(SonSizes.headerArtwork)
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))

        Text(
            text = song.album,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        // 主动作 + 次要动作并排一行，取代原先"居中 FAB + 下一行图标按钮"的堆叠。
        Row(
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onPlay) {
                M3SymbolIcon(
                    name = SymbolIconName.PlayArrow,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.buttonIcon)
                )
                Spacer(modifier = Modifier.width(SonSpacing.small))
                Text(text = stringResource(R.string.song_detail_play))
            }

            FilledTonalIconButton(onClick = onFavorite) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = isFavorite,
                    contentDescription = if (isFavorite) stringResource(R.string.player_unfavorite_desc) else stringResource(R.string.player_favorite_desc)
                )
            }

            FilledTonalIconButton(onClick = onAddToPlaylist) {
                M3SymbolIcon(
                    name = SymbolIconName.PlaylistAdd,
                    filled = true,
                    contentDescription = stringResource(R.string.song_detail_add_to_playlist_desc)
                )
            }
        }
    }
}

@Composable
private fun SongInfoSection(song: Song, audioMetadata: AudioMetadata?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        InfoSectionTitle(stringResource(R.string.song_detail_section_basic))

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

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        InfoSectionTitle(stringResource(R.string.song_detail_section_audio))

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

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        InfoSectionTitle(stringResource(R.string.song_detail_section_file))

        InfoItem(label = stringResource(R.string.song_detail_added_date), value = formatDate(song.dateAdded, stringResource(R.string.song_detail_unknown)))
        InfoItem(label = stringResource(R.string.song_detail_modified_date), value = formatDate(song.dateModified, stringResource(R.string.song_detail_unknown)))

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        InfoSectionTitle(stringResource(R.string.song_detail_section_path))

        PathItem(path = song.path)
    }
}

@Composable
private fun InfoSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = SonLayout.sectionTitleGap)
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.small),
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
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun PathItem(path: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SonSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(SonSpacing.small))

        val context = LocalContext.current
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Song Path", path)
                clipboard.setPrimaryClip(clip)
            }
        ) {
            M3SymbolIcon(
                name = SymbolIconName.ContentCopy,
                filled = true,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(SonSpacing.small))
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
