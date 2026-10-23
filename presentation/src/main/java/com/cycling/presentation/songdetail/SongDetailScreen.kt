@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.songdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleAlert
import com.cycling.core.ui.apple.AppleCompactBar
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleIconButton
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
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
import com.cycling.presentation.components.LyricsCandidatesSheet
import com.cycling.presentation.components.PlaylistPickerDialog
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.formatDuration
import com.cycling.presentation.components.rememberLoadingIndicatorVisible
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

    // The photo picker returns a URI whose read grant only lives as long as this process; the
    // repository copies the bytes into app-private storage right away, so that is enough.
    val coverImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.handleIntent(SongDetailIntent.ApplyLocalCover(it.toString())) }
    }

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
        containerColor = AppleTheme.colors.background,
        topBar = {
            AppleCompactBar(
                title = if (uiState.isEditMode) {
                    stringResource(R.string.song_detail_title_edit)
                } else {
                    stringResource(R.string.song_detail_title_view)
                },
                onNavigateBack = {
                    if (uiState.isEditMode) {
                        viewModel.handleIntent(SongDetailIntent.ExitEditMode)
                    } else {
                        onNavigateBack()
                    }
                },
                backContentDescription = stringResource(R.string.song_detail_back_desc),
                actions = {
                    if (uiState.isEditMode) {
                        if (uiState.isSaving) {
                            LoadingIndicator(modifier = Modifier.size(SonSizes.iconMedium))
                        } else {
                            AppleIconButton(
                                icon = SymbolIconName.Check,
                                contentDescription = stringResource(R.string.song_detail_save_desc),
                                onClick = { viewModel.handleIntent(SongDetailIntent.SaveChanges) },
                                enabled = uiState.hasChanges
                            )
                        }
                    } else if (uiState.song != null) {
                        AppleIconButton(
                            icon = SymbolIconName.Edit,
                            contentDescription = stringResource(R.string.song_detail_edit_desc),
                            onClick = { viewModel.handleIntent(SongDetailIntent.EnterEditMode) }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            // 歌曲详情来自 Room，几乎瞬间就到：延迟现身，避免每进一次都闪一帧转圈。
            SonLoadingState(modifier = Modifier.padding(paddingValues))
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
                            onIntent = viewModel::handleIntent
                        )
                    } else {
                        ViewModeContent(
                            song = song,
                            uiState = uiState,
                            onPlay = { viewModel.handleIntent(SongDetailIntent.PlaySong) },
                            onFavorite = { viewModel.handleIntent(SongDetailIntent.ToggleFavorite) },
                            onAddToPlaylist = { viewModel.handleIntent(SongDetailIntent.AddToPlaylist) },
                            onEditLyrics = { viewModel.handleIntent(SongDetailIntent.EnterEditMode) },
                            onShowLyricsInfo = { viewModel.handleIntent(SongDetailIntent.OpenLyricsInfo) }
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

    if (uiState.showLyricsCandidates) {
        LyricsCandidatesSheet(
            candidates = uiState.lyricsCandidates,
            isLoading = uiState.isLoadingLyricsCandidates,
            onSelect = { viewModel.handleIntent(SongDetailIntent.SelectLyricsCandidate(it)) },
            onDismiss = { viewModel.handleIntent(SongDetailIntent.DismissLyricsCandidates) },
            hintRes = R.string.song_detail_lyrics_candidates_hint
        )
    }

    if (uiState.showLyricsInfoDialog) {
        LyricsInfoDialog(
            info = uiState.lyricsInfo,
            editable = uiState.isEditMode,
            onApplyMetadata = { viewModel.handleIntent(SongDetailIntent.ApplyLyricsMetadata(it)) },
            onFillSongInfo = { viewModel.handleIntent(SongDetailIntent.FillSongInfoFromLyricsMetadata(it)) },
            onDismiss = { viewModel.handleIntent(SongDetailIntent.DismissLyricsInfo) }
        )
    }

    if (uiState.showCoverPicker) {
        CoverPickerSheet(
            candidates = uiState.onlineCandidates,
            isLoading = uiState.isLoadingOnlineCandidates,
            isApplying = uiState.isApplyingCover,
            hasCustomCover = uiState.song?.hasCustomAlbumArt == true,
            onPickLocal = {
                coverImagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRetry = { viewModel.handleIntent(SongDetailIntent.RetryOnlineSearch) },
            onClear = { viewModel.handleIntent(SongDetailIntent.ClearCustomCover) },
            onSelectCandidate = { viewModel.handleIntent(SongDetailIntent.SelectCoverCandidate(it)) },
            onDismiss = { viewModel.handleIntent(SongDetailIntent.DismissCoverPicker) }
        )
    }

    if (uiState.showMetadataCandidates) {
        MetadataCandidatesSheet(
            candidates = uiState.onlineCandidates,
            isLoading = uiState.isLoadingOnlineCandidates,
            onSelect = { viewModel.handleIntent(SongDetailIntent.SelectMetadataCandidate(it)) },
            onRetry = { viewModel.handleIntent(SongDetailIntent.RetryOnlineSearch) },
            onDismiss = { viewModel.handleIntent(SongDetailIntent.DismissMetadataCandidates) }
        )
    }
}

@Composable
private fun ViewModeContent(
    song: Song,
    uiState: SongDetailUiState,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditLyrics: () -> Unit,
    onShowLyricsInfo: () -> Unit
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
        onEditLyrics = onEditLyrics,
        onShowLyricsInfo = onShowLyricsInfo
    )

    SongInfoSection(song = song, audioMetadata = uiState.audioMetadata)

    // 列表底部预留浮动导航栏空间。
    Spacer(modifier = Modifier.height(appleBottomBarClearance()))
}

/**
 * 歌词只读区。
 *
 * 折叠而不是全展开：一首歌的歌词往往比页面其余部分加起来还长，默认摊开会把音频信息、文件信息
 * 挤到很远的地方。阈值之外给一个展开按钮，两种需求都能满足。
 *
 * 标题右侧的「歌词信息」是整个歌词区块的入口：来源、格式、行数与 LRC 头部标签都在那个面板里，
 * 页面上不再摊开第二套信息行。
 */
@Composable
private fun LyricsSection(
    lyrics: List<LyricsDisplayLine>,
    isLoading: Boolean,
    onEditLyrics: () -> Unit,
    onShowLyricsInfo: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SonLayout.sectionTitleGap),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoSectionTitle(
                text = stringResource(R.string.song_detail_section_lyrics),
                modifier = Modifier
            )
            AppleTextAction(
                label = stringResource(R.string.song_detail_lyrics_info),
                icon = SymbolIconName.Checklist,
                onClick = onShowLyricsInfo
            )
        }

        when {
            // 歌词通常来自本地 sidecar / 内嵌标签 / 缓存，瞬间就到；真去网络查时才值得显示转圈。
            lyrics.isEmpty() && isLoading -> if (rememberLoadingIndicatorVisible()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SonSpacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(SonSizes.iconLarge))
                }
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
                    AppleTextAction(
                        label = stringResource(
                            if (expanded) R.string.song_detail_lyrics_collapse
                            else R.string.song_detail_lyrics_expand
                        ),
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(top = SonSpacing.small)
                    )
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
            style = AppleType.body,
            color = AppleTheme.colors.label
        )
        line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            Text(
                text = translation,
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
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
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))
        AppleTextAction(
            label = stringResource(R.string.song_detail_lyrics_add),
            icon = SymbolIconName.Edit,
            onClick = onEditLyrics
        )
    }
}

@Composable
private fun EditModeContent(    song: Song,
    uiState: SongDetailUiState,
    onIntent: (SongDetailIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin)
    ) {
        M3Artwork(
            artwork = song.albumArt,
            placeholderIcon = SymbolIconName.MusicNote,
            shape = RoundedCornerShape(AppleDims.cornerL),
            modifier = Modifier
                .size(SonSizes.headerArtwork)
                .align(Alignment.CenterHorizontally)
                .padding(top = SonSpacing.medium)
        )

        // 封面是立即生效的一次选择，没有草稿状态，所以入口放在封面上而不是表单末尾。
        AppleTextAction(
            label = stringResource(R.string.song_detail_cover_manage),
            icon = SymbolIconName.Edit,
            onClick = { onIntent(SongDetailIntent.OpenCoverPicker) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 在线信息与封面共用同一批目录搜索结果：这里只把文本字段填进下面的表单，封面仍由上面的
        // 「更换封面」负责，两个动作互不代劳。放在表单最前面，因为填完之后要往下逐项检查。
        AppleTextAction(
            label = stringResource(R.string.song_detail_metadata_online_manage),
            icon = SymbolIconName.AutoAwesome,
            onClick = { onIntent(SongDetailIntent.OpenMetadataCandidates) }
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        EditTextField(
            label = stringResource(R.string.song_detail_title_label),
            value = uiState.editedTitle,
            onValueChange = { onIntent(SongDetailIntent.UpdateTitle(it)) },
            placeholder = stringResource(R.string.song_detail_title_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_artist_label),
            value = uiState.editedArtist,
            onValueChange = { onIntent(SongDetailIntent.UpdateArtist(it)) },
            placeholder = stringResource(R.string.song_detail_artist_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_album_label),
            value = uiState.editedAlbum,
            onValueChange = { onIntent(SongDetailIntent.UpdateAlbum(it)) },
            placeholder = stringResource(R.string.song_detail_album_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_album_artist_label),
            value = uiState.editedAlbumArtist,
            onValueChange = { onIntent(SongDetailIntent.UpdateAlbumArtist(it)) },
            placeholder = stringResource(R.string.song_detail_album_artist_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_year_label),
            value = uiState.editedYear,
            onValueChange = { onIntent(SongDetailIntent.UpdateYear(it)) },
            placeholder = stringResource(R.string.song_detail_year_placeholder),
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_genre_label),
            value = uiState.editedGenre,
            onValueChange = { onIntent(SongDetailIntent.UpdateGenre(it)) },
            placeholder = stringResource(R.string.song_detail_genre_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_track_number_label),
            value = uiState.editedTrackNumber,
            onValueChange = { onIntent(SongDetailIntent.UpdateTrackNumber(it)) },
            placeholder = stringResource(R.string.song_detail_track_number_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_disc_number_label),
            value = uiState.editedDiscNumber,
            onValueChange = { onIntent(SongDetailIntent.UpdateDiscNumber(it)) },
            placeholder = stringResource(R.string.song_detail_disc_number_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_composer_label),
            value = uiState.editedComposer,
            onValueChange = { onIntent(SongDetailIntent.UpdateComposer(it)) },
            placeholder = stringResource(R.string.song_detail_composer_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_publisher_label),
            value = uiState.editedPublisher,
            onValueChange = { onIntent(SongDetailIntent.UpdatePublisher(it)) },
            placeholder = stringResource(R.string.song_detail_publisher_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_copyright_label),
            value = uiState.editedCopyright,
            onValueChange = { onIntent(SongDetailIntent.UpdateCopyright(it)) },
            placeholder = stringResource(R.string.song_detail_copyright_placeholder)
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        EditTextField(
            label = stringResource(R.string.song_detail_comment_label),
            value = uiState.editedComment,
            onValueChange = { onIntent(SongDetailIntent.UpdateComment(it)) },
            placeholder = stringResource(R.string.song_detail_comment_placeholder)
        )

        Text(
            text = stringResource(R.string.song_detail_metadata_hint),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel,
            modifier = Modifier.padding(top = SonSpacing.small, bottom = SonSpacing.medium)
        )

        // 歌词区块的两个入口：「获取歌词」拉在线版本填进下面的编辑器；「歌词信息」看来源/格式，
        // 并编辑 LRC 头部标签。两者都只改编辑器里的草稿，落库仍走页面底部的保存。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleTextAction(
                label = stringResource(R.string.song_detail_lyrics_fetch),
                icon = SymbolIconName.QueueMusic,
                onClick = { onIntent(SongDetailIntent.OpenLyricsCandidates) }
            )
            AppleTextAction(
                label = stringResource(R.string.song_detail_lyrics_info),
                icon = SymbolIconName.Checklist,
                onClick = { onIntent(SongDetailIntent.OpenLyricsInfo) }
            )
        }

        Text(
            text = stringResource(R.string.song_detail_lyrics_label),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel,
            modifier = Modifier.padding(top = SonSpacing.mediumSmall)
        )
        Spacer(modifier = Modifier.height(4.dp))
        AppleTextField(
            value = uiState.editedLyrics,
            onValueChange = { onIntent(SongDetailIntent.UpdateLyrics(it)) },
            placeholder = stringResource(R.string.song_detail_lyrics_placeholder),
            singleLine = false,
            minHeight = 160.dp
        )
        Text(
            text = stringResource(R.string.song_detail_lyrics_hint),
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        AppleActionButton(
            label = if (uiState.isSaving) {
                stringResource(R.string.song_detail_saving)
            } else {
                stringResource(R.string.song_detail_save)
            },
            onClick = { onIntent(SongDetailIntent.SaveChanges) },
            enabled = uiState.hasChanges && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))

        AppleActionButton(
            label = stringResource(R.string.song_detail_cancel),
            onClick = { onIntent(SongDetailIntent.ExitEditMode) },
            filled = false,
            modifier = Modifier.fillMaxWidth()
        )

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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel
        )
        Spacer(modifier = Modifier.height(4.dp))
        AppleTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            keyboardType = keyboardType
        )
    }
}

@Composable
private fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AppleAlert(
        title = stringResource(R.string.song_detail_discard_title),
        message = stringResource(R.string.song_detail_discard_message),
        confirmLabel = stringResource(R.string.song_detail_discard),
        onConfirm = onDiscard,
        onDismiss = onKeepEditing,
        dismissLabel = stringResource(R.string.song_detail_keep_editing),
        destructive = true
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
            shape = RoundedCornerShape(AppleDims.cornerL),
            modifier = Modifier.size(SonSizes.headerArtwork)
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        Text(
            text = song.title,
            style = AppleType.title2Emphasized,
            color = AppleTheme.colors.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))

        Text(
            text = song.artist,
            style = AppleType.body,
            color = AppleTheme.colors.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(SonSpacing.extraSmall))

        Text(
            text = song.album,
            style = AppleType.footnote,
            color = AppleTheme.colors.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(SonSpacing.large))

        // 主动作 + 次要动作并排一行。
        Row(
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleActionButton(
                label = stringResource(R.string.song_detail_play),
                onClick = onPlay,
                icon = SymbolIconName.PlayArrow
            )

            AppleCircleIconButton(
                icon = SymbolIconName.Favorite,
                filled = isFavorite,
                contentDescription = if (isFavorite) {
                    stringResource(R.string.player_unfavorite_desc)
                } else {
                    stringResource(R.string.player_favorite_desc)
                },
                onClick = onFavorite
            )

            AppleCircleIconButton(
                icon = SymbolIconName.PlaylistAdd,
                filled = true,
                contentDescription = stringResource(R.string.song_detail_add_to_playlist_desc),
                onClick = onAddToPlaylist
            )
        }
    }
}

/** 圆形填充图标按钮，对应 FilledTonalIconButton 的 iOS 形态。 */
@Composable
private fun AppleCircleIconButton(
    icon: SymbolIconName,
    filled: Boolean,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(AppleDims.actionHeight)
            .clip(CircleShape)
            .background(AppleTheme.colors.fill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = contentDescription,
            filled = filled,
            tint = AppleTheme.colors.label,
            size = 20.dp
        )
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
            if (metadata.albumArtist != null) {
                InfoItem(label = stringResource(R.string.song_detail_album_artist_label), value = metadata.albumArtist!!)
            }
            if (metadata.year != null) {
                InfoItem(label = stringResource(R.string.song_detail_year), value = metadata.year!!)
            }
            if (metadata.genre != null) {
                InfoItem(label = stringResource(R.string.song_detail_genre), value = metadata.genre!!)
            }
            if (metadata.trackNumber != null) {
                InfoItem(label = stringResource(R.string.song_detail_track_number_label), value = metadata.trackNumber!!)
            }
            if (metadata.discNumber != null) {
                InfoItem(label = stringResource(R.string.song_detail_disc_number_label), value = metadata.discNumber!!)
            }
            if (metadata.composer != null) {
                InfoItem(label = stringResource(R.string.song_detail_composer), value = metadata.composer!!)
            }
            if (metadata.publisher != null) {
                InfoItem(label = stringResource(R.string.song_detail_publisher_label), value = metadata.publisher!!)
            }
            if (metadata.copyright != null) {
                InfoItem(label = stringResource(R.string.song_detail_copyright_label), value = metadata.copyright!!)
            }
            if (metadata.comment != null) {
                InfoItem(label = stringResource(R.string.song_detail_comment_label), value = metadata.comment!!)
            }
        }

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        InfoSectionTitle(stringResource(R.string.song_detail_section_audio))

        val bitrate = audioMetadata?.bitrate?.takeIf { it > 0 } ?: song.bitrate
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
private fun InfoSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(bottom = SonLayout.sectionTitleGap)
) {
    Text(
        text = text,
        style = AppleType.sectionTitle,
        color = AppleTheme.colors.label,
        modifier = modifier
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
        Text(text = label, style = AppleType.body, color = AppleTheme.colors.secondaryLabel)
        Text(text = value, style = AppleType.body, color = AppleTheme.colors.label)
    }

    AppleSeparator()
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
            style = AppleType.footnote,
            color = AppleTheme.colors.label,
            modifier = Modifier.weight(1f)
        )

        val context = LocalContext.current
        AppleTextAction(
            label = stringResource(R.string.song_detail_copy),
            icon = SymbolIconName.ContentCopy,
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Song Path", path)
                clipboard.setPrimaryClip(clip)
            }
        )
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
