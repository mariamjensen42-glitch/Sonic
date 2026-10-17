package com.cycling.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.Song
import com.cycling.presentation.R

/**
 * 歌曲列表项。
 *
 * 结构完全交给 [ListItem]：内边距、最小高度、按压形变、文字层级都由组件规范决定，
 * 这里只负责把领域模型的字段映射到插槽上。
 */
@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = true,
    showDuration: Boolean = true,
    showMoreButton: Boolean = false,
    isCurrentSong: Boolean = false,
    /**
     * 多选态。`null` 表示当前不在多选模式，此时不显示勾选框 —— 用可空而不是再配一个
     * `selectionMode` 布尔量，是为了让"没有多选"这件事只有一个表示方式。
     */
    selected: Boolean? = null,
    moreMenuExpanded: Boolean = false,
    onMoreClick: () -> Unit = {},
    onMoreDismiss: () -> Unit = {},
    moreMenuItems: List<MenuItem> = emptyList(),
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    /** 歌单排序用的拖拽把手；不传就没有把手，行内容与之前完全一致。 */
    dragHandle: (@Composable () -> Unit)? = null
) {
    val leadingContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected != null) {
                Checkbox(checked = selected, onCheckedChange = null)
                Spacer(modifier = Modifier.width(SonSpacing.small))
            }
            M3Artwork(
                artwork = song.albumArt,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        }
    }
    val headlineContent: @Composable () -> Unit = {
        Text(
            text = song.title,
            style = if (isCurrentSong) {
                MaterialTheme.typography.bodyLargeEmphasized
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (isCurrentSong) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val supportingContent: @Composable () -> Unit = {
        Text(
            text = subtitle ?: formatDuration(song.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    // ListItem 的 trailingContent 槽是 Box（子项互相叠放），时长与图标按钮必须自己排成一行，
    // 否则会压在一起。
    val trailing: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
        ) {
            if (showDuration && subtitle != null) {
                Text(
                    text = formatDuration(song.duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailingContent?.invoke()
            if (showMoreButton) {
                Box {
                    IconButton(onClick = onMoreClick) {
                        M3SymbolIcon(
                            name = SymbolIconName.MoreVert,
                            filled = true,
                            contentDescription = stringResource(R.string.components_more_desc)
                        )
                    }
                    if (moreMenuItems.isNotEmpty()) {
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = onMoreDismiss,
                            offset = DpOffset(x = (-8).dp, y = 8.dp)
                        ) {
                            moreMenuItems.forEach { menuItem ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = menuItem.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (menuItem.isDestructive) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        menuItem.onClick()
                                        onMoreDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // 把手放行尾：常驻行首会把封面与标题整列推离屏幕边距，列表看起来像表格。
            if (dragHandle != null) {
                dragHandle()
            }
        }
    }

    // onClick 可空：有值时用可点击重载，无值时用静态重载，避免给不可点击的行加涟漪。
    if (onClick != null) {
        ListItem(
            modifier = modifier,
            onClick = onClick,
            selected = selected == true,
            leadingContent = leadingContent,
            content = headlineContent,
            supportingContent = supportingContent,
            trailingContent = trailing
        )
    } else {
        ListItem(
            modifier = modifier,
            leadingContent = leadingContent,
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            trailingContent = trailing
        )
    }

    if (showDivider) {
        // 缩进到标题起始位置：列表左右内边距 + 封面宽度 + 封面与文字间距
        HorizontalDivider(modifier = Modifier.padding(start = songDividerIndent))
    }
}

/** 歌曲行分隔线的起始缩进。 */
private val songDividerIndent: Dp =
    SonSizes.listItemHorizontalPadding + SonSizes.thumbnail + SonSpacing.medium

data class MenuItem(
    val name: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}
