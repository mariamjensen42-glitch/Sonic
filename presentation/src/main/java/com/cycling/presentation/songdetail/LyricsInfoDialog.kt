package com.cycling.presentation.songdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleSeparator
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTextAction
import com.cycling.core.ui.apple.AppleTextField
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.domain.lyrics.model.Attributes
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsSource
import com.cycling.presentation.R
import com.cycling.presentation.components.providerLabel
import com.cycling.presentation.components.qualityLabel

/**
 * 只认 LRC 头部标签的格式。
 *
 * TTML 是 XML、YRC 的每一行都是 `[start,duration](syllable)`，往这两种文本里插 `[ti:]` 不是
 * 元数据而是污染，所以元数据编辑只对 LRC 家族开放。
 */
private val LrcMetadataFormats = setOf("LRC", "WORD_BY_WORD_LRC", "ENHANCED_LRC", "LYRICIFY_SYLLABLE")

/**
 * 「歌词信息」面板。
 *
 * 只读时是一张信息卡：这份歌词从哪来、是什么格式、有多少行。编辑模式下同一张卡上多出 LRC 头部
 * 标签的输入框，确认后把标签写回编辑器里的歌词文本 —— 面板本身不写数据库，页面底部的保存按钮才
 * 是唯一落库的动作。来源与提供方无法从文本反推，只有页面加载时解析出的那一份。
 */
@Composable
internal fun LyricsInfoDialog(
    info: LyricsInfo,
    editable: Boolean,
    onApplyMetadata: (Attributes) -> Unit,
    onFillSongInfo: (Attributes) -> Unit,
    onDismiss: () -> Unit
) {
    val canEditMetadata = editable &&
        (info.format == null || info.format in LrcMetadataFormats)

    var title by remember(info.metadata) { mutableStateOf(info.metadata.title.orEmpty()) }
    var artist by remember(info.metadata) { mutableStateOf(info.metadata.artist.orEmpty()) }
    var album by remember(info.metadata) { mutableStateOf(info.metadata.album.orEmpty()) }
    var offset by remember(info.metadata) { mutableStateOf(info.metadata.offset?.toString().orEmpty()) }
    var byText by remember(info.metadata) { mutableStateOf(info.metadata.by.orEmpty()) }

    fun currentMetadata() = Attributes(
        title = title.trim().ifBlank { null },
        artist = artist.trim().ifBlank { null },
        album = album.trim().ifBlank { null },
        offset = offset.trim().toIntOrNull(),
        by = byText.trim().ifBlank { null },
        duration = info.metadata.duration
    )

    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(title = stringResource(R.string.song_detail_lyrics_info_title))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LyricsInfoRow(
                label = stringResource(R.string.song_detail_lyrics_info_source),
                value = sourceLabel(info.source, info.provider)
            )
            LyricsInfoRow(
                label = stringResource(R.string.song_detail_lyrics_info_timing),
                value = when {
                    info.isWordByWord -> qualityLabel(true)
                    info.hasTimings -> qualityLabel(false)
                    else -> stringResource(R.string.song_detail_lyrics_info_untimed)
                }
            )
            LyricsInfoRow(
                label = stringResource(R.string.song_detail_lyrics_info_lines),
                value = info.lineCount.toString()
            )
            LyricsInfoRow(
                label = stringResource(R.string.song_detail_lyrics_info_format),
                value = when {
                    info.format != null -> formatLabel(info.format)
                    info.lineCount > 0 -> stringResource(R.string.song_detail_lyrics_format_plain)
                    else -> stringResource(R.string.song_detail_unknown)
                }
            )
            LyricsInfoRow(
                label = stringResource(R.string.song_detail_lyrics_info_translation),
                value = stringResource(
                    if (info.hasTranslation) R.string.song_detail_lyrics_info_yes
                    else R.string.song_detail_lyrics_info_no
                )
            )

            if (!canEditMetadata) {
                if (editable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.song_detail_lyrics_metadata_unsupported),
                        style = AppleType.footnote,
                        color = AppleTheme.colors.secondaryLabel
                    )
                } else {
                    ReadOnlyMetadata(info.metadata)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                AppleSeparator(leadingInset = 0.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.song_detail_lyrics_metadata_section),
                    style = AppleType.sectionTitle,
                    color = AppleTheme.colors.label
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetadataField(
                        label = stringResource(R.string.song_detail_lyrics_meta_title),
                        value = title,
                        onValueChange = { title = it }
                    )
                    MetadataField(
                        label = stringResource(R.string.song_detail_lyrics_meta_artist),
                        value = artist,
                        onValueChange = { artist = it }
                    )
                    MetadataField(
                        label = stringResource(R.string.song_detail_lyrics_meta_album),
                        value = album,
                        onValueChange = { album = it }
                    )
                    MetadataField(
                        label = stringResource(R.string.song_detail_lyrics_meta_offset),
                        value = offset,
                        keyboardType = KeyboardType.Number,
                        supportingText = stringResource(R.string.song_detail_lyrics_meta_offset_hint),
                        onValueChange = { value ->
                            offset = value.filter { it.isDigit() || it == '-' }.take(7)
                        }
                    )
                    MetadataField(
                        label = stringResource(R.string.song_detail_lyrics_meta_by),
                        value = byText,
                        onValueChange = { byText = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                AppleTextAction(
                    label = stringResource(R.string.song_detail_lyrics_metadata_fill_song),
                    icon = SymbolIconName.MusicNote,
                    onClick = { onFillSongInfo(currentMetadata()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (canEditMetadata) {
                AppleActionButton(
                    label = stringResource(R.string.song_detail_lyrics_metadata_apply),
                    onClick = { onApplyMetadata(currentMetadata()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppleActionButton(
                    label = stringResource(R.string.song_detail_cancel),
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppleActionButton(
                    label = stringResource(R.string.song_detail_lyrics_metadata_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
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
            keyboardType = keyboardType
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = AppleType.caption1,
                color = AppleTheme.colors.secondaryLabel,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** 只读模式下的头部标签：只列文档里真有的，不拿一串「无」凑数。 */
@Composable
private fun ReadOnlyMetadata(metadata: Attributes) {
    val titleLabel = stringResource(R.string.song_detail_title_label)
    val artistLabel = stringResource(R.string.song_detail_artist_label)
    val albumLabel = stringResource(R.string.song_detail_album_label)
    val offsetLabel = stringResource(R.string.song_detail_lyrics_meta_offset_label)
    val byLabel = stringResource(R.string.song_detail_lyrics_meta_by_label)

    val rows = listOfNotNull(
        metadata.title?.let { titleLabel to it },
        metadata.artist?.let { artistLabel to it },
        metadata.album?.let { albumLabel to it },
        metadata.offset?.let { offsetLabel to it.toString() },
        metadata.by?.let { byLabel to it }
    )
    if (rows.isEmpty()) return

    Spacer(modifier = Modifier.height(12.dp))
    AppleSeparator(leadingInset = 0.dp)
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.song_detail_lyrics_metadata_section),
        style = AppleType.sectionTitle,
        color = AppleTheme.colors.label
    )
    rows.forEach { (label, value) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = AppleType.body, color = AppleTheme.colors.secondaryLabel)
            Text(text = value, style = AppleType.body, color = AppleTheme.colors.label)
        }
    }
}

@Composable
private fun LyricsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = AppleType.body, color = AppleTheme.colors.secondaryLabel)
        Text(text = value, style = AppleType.body, color = AppleTheme.colors.label)
    }
}

/** 手写 / 内嵌 / 旁车 / 在线，在线时再补上提供方 —— 单说「在线下载」等于没说。 */
@Composable
private fun sourceLabel(source: LyricsSource, provider: LyricsProvider?): String {
    val base = stringResource(
        when (source) {
            LyricsSource.USER -> R.string.song_detail_lyrics_source_user
            LyricsSource.EMBEDDED -> R.string.song_detail_lyrics_source_embedded
            LyricsSource.FILE -> R.string.song_detail_lyrics_source_file
            LyricsSource.ONLINE -> R.string.song_detail_lyrics_source_online
            LyricsSource.NONE -> R.string.song_detail_lyrics_source_none
        }
    )
    if (provider == null) return base
    return "$base · ${providerLabel(provider)}"
}

/** 解析器内部的格式名（`WORD_BY_WORD_LRC` 之类）不是给人看的，转成简短的展示名。 */
@Composable
private fun formatLabel(format: String): String = when (format) {
    "WORD_BY_WORD_LRC" -> stringResource(R.string.song_detail_lyrics_format_word_by_word_lrc)
    "ENHANCED_LRC" -> stringResource(R.string.song_detail_lyrics_format_enhanced_lrc)
    "LYRICIFY_SYLLABLE" -> stringResource(R.string.song_detail_lyrics_format_lyricify)
    else -> format
}
