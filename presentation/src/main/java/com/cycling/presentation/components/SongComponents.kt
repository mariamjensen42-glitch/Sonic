@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.ApplePlayingBars
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.components.SonGlyphIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.domain.model.Song
import com.cycling.presentation.download.LocalDownloadRequester
import com.cycling.presentation.R

/**
 * 歌曲列表项，按 Apple Music 的 `collection_page_list_item` 还原：
 * 最小行高 48dp、标题 17sp（`SmallListATitleStyle`）、副标题 12sp（`SmallListASubTitleStyle`）、
 * 时长用 12sp 落在行尾、更多按钮 26dp。
 *
 * 与 Apple 原版的差异：原版行首是**曲目序号**（播放中换成跳动的音柱），这里保留 Sonic 的
 * 封面缩略图 —— 全应用的行都以封面为锚点，且行组件同时被歌手页、收藏页、文件夹页复用。
 *
 * 默认**不画分割线**（quiet surface）；需要表格感的地方显式传 `showDivider = true`。
 */
@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = false,
    showDuration: Boolean = true,
    showMoreButton: Boolean = false,
    isCurrentSong: Boolean = false,
    /**
     * 多选态。`null` 表示当前不在多选模式，此时不显示勾选框 —— 用可空而不是再配一个
     * `selectionMode` 布尔量，是为了让"没有多选"这件事只有一个表示方式。
     */
    selected: Boolean? = null,
    /** 曲目序号；传了就按 Apple 曲目表渲染（序号代替封面）。 */
    trackNumber: Int? = null,
    /** 该行是否为「正在播放」的曲目，为 true 时序号换成音柱。 */
    isPlaying: Boolean = false,
    moreMenuExpanded: Boolean = false,
    onMoreClick: () -> Unit = {},
    onMoreDismiss: () -> Unit = {},
    moreMenuItems: List<MenuItem> = emptyList(),
    onClick: (() -> Unit)? = null,
    /** 长按整行（菜单会从手指位置弹出）。不传则只有短按。 */
    onLongClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    /** 歌单排序用的拖拽把手；不传就没有把手，行内容与之前完全一致。 */
    dragHandle: (@Composable () -> Unit)? = null
) {
    val colors = AppleTheme.colors
    val selectedRow = selected == true

    // 长按要的是「整行都能触发菜单」，所以不能只给行尾那颗 ⋮ 挂手势。
    // 长按位置也要记下来：菜单从手指那里弹出，而不是固定贴着行尾。
    var pressPoint by remember { mutableStateOf<IntOffset?>(null) }
    // 手势协程只在首次组合时启动，回调必须走 rememberUpdatedState，否则闭包里是旧 lambda。
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val pressModifier = when {
        onLongClick != null -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { currentOnClick?.invoke() },
                onLongPress = { offset ->
                    pressPoint = offset.round()
                    currentOnLongClick?.invoke()
                }
            )
        }

        onClick != null -> Modifier.applePress(onClick)
        else -> Modifier
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selectedRow) colors.fill else Color.Transparent)
                .then(pressModifier)
                .heightIn(min = AppleDims.listItemHeight)
                .padding(start = AppleDims.padding, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null) {
                Checkbox(checked = selected, onCheckedChange = null)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (trackNumber != null) {
                // 专辑 / 歌单曲目表：行首是序号，播放中换成音柱，**不显示封面**（与 Apple 一致）
                Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        ApplePlayingBars()
                    } else {
                        Text(
                            text = trackNumber.toString(),
                            style = AppleType.subhead,
                            color = colors.tertiaryLabel
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                M3Artwork(
                    artwork = song.albumArt,
                    placeholderIcon = SymbolIconName.MusicNote,
                    shape = RoundedCornerShape(AppleDims.cornerS),
                    modifier = Modifier.size(AppleDims.listItemHeight)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    // SmallListATitleStyle：17sp，当前曲目换成品牌红
                    style = AppleType.body,
                    color = if (isCurrentSong) colors.systemRed else colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle ?: formatDuration(song.duration),
                    // SmallListASubTitleStyle：12sp
                    style = AppleType.caption1,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showDuration && subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(song.duration),
                    style = AppleType.caption1,
                    color = colors.secondaryLabel
                )
            }

            trailingContent?.invoke()

            if (showMoreButton) {
            Box(
                modifier = Modifier
                    .size(AppleDims.trailingIcon)
                    .clip(RoundedCornerShape(AppleDims.cornerM))
                    // 从 ⋮ 打开时菜单仍贴着行尾，不跟手指走。
                    .applePress {
                        pressPoint = null
                        onMoreClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                    M3SymbolIcon(
                        name = SymbolIconName.MoreVert,
                        filled = true,
                        contentDescription = stringResource(R.string.components_more_desc),
                        tint = colors.secondaryLabel,
                        size = 18.dp
                    )
                }
            }

            // 把手放行尾：常驻行首会把封面与标题整列推离屏幕边距，列表看起来像表格。
            if (dragHandle != null) {
                dragHandle()
            }
        }

        // 菜单挂在整行这一层，不再只属于行尾那颗 ⋮：长按整行也能弹。
        if (moreMenuItems.isNotEmpty()) {
            val point = pressPoint
            if (point != null) {
                // 长按：从手指位置弹出。DropdownMenuPopup 只收 M3 自己的定位器类型，
                // 这里用底层 Popup（收通用 PopupPositionProvider），里面的内容仍是同一个 MenuItemGroup。
                Popup(
                    popupPositionProvider = AtPointPopupPositionProvider(point),
                    onDismissRequest = onMoreDismiss,
                    properties = PopupProperties(focusable = true)
                ) {
                    // 底层 Popup 的内容拿到的是整窗约束，不自己收窄就会铺满一屏。
                    // 先按最长一项撑开（IntrinsicSize.Max），再压到菜单上限 280dp。
                    MenuItemGroup(
                        items = moreMenuItems,
                        onDismiss = onMoreDismiss,
                        modifier = Modifier
                            .widthIn(max = MenuMaxWidth)
                            .width(IntrinsicSize.Max)
                    )
                }
            } else {
                DropdownMenuPopup(
                    expanded = moreMenuExpanded,
                    onDismissRequest = onMoreDismiss,
                    // 贴着行尾弹出，横向收 8dp 让菜单右缘对齐行尾。
                    popupPositionProvider =
                        MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                            MenuAnchorPosition.Below,
                            offset = DpOffset(x = (-8).dp, y = 8.dp)
                        )
                ) {
                    MenuItemGroup(items = moreMenuItems, onDismiss = onMoreDismiss)
                }
            }
        }
    }

    if (showDivider) {
        // 缩进到标题起始位置：列表左右内边距 + 封面宽度 + 封面与文字间距
        HorizontalDivider(modifier = Modifier.padding(start = songDividerIndent))
    }
}

/** 歌曲行分隔线的起始缩进：行首留白 + 封面 + 封面与文字间距。 */
private val songDividerIndent: Dp = AppleDims.padding + AppleDims.listItemHeight + 12.dp

/** 长按菜单的宽度上限。M3 菜单的标准触达宽度，也是"不铺满屏"的关键。 */
private val MenuMaxWidth = 280.dp

/**
 * 一个菜单项。只描述"叫什么、什么图标、点了干什么"，长什么样由 [MenuItemGroup] 统一决定 ——
 * 菜单的圆角、分组底色、按压反馈全部交给 Material，页面不碰。
 *
 * [iconFilled] 只对有心形语义的项有意义（收藏 = 已收藏时给实心）。
 *
 * 图标有两条来源：[icon] 走 Material Symbols 字体（全 App 通用语义），[glyph] 走
 * [SonGlyph] 手绘集（字体子集里没有码位的图标，例如下载）。两者都给时以 [glyph] 为准。
 */
data class MenuItem(
    val name: String,
    val isDestructive: Boolean = false,
    val icon: SymbolIconName? = null,
    val glyph: SonGlyph? = null,
    val iconFilled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * 把 [MenuItem] 渲染成 M3 Expressive 的分组菜单。
 *
 * 一个 [DropdownMenuPopup] 里只放一个 [DropdownMenuGroup]：分组容器负责 surface 底色与大圆角，
 * 组内每项按位置取首/中/末形（[MenuDefaults.leadingItemShape] 等），于是菜单从"一列等宽文字"
 * 变成 Expressive 的分段形。文字字号、图标颜色、按压态全部走 Material 默认值，破坏性动作
 * 只把文字与图标换成 error 色。
 *
 * 所有下拉菜单都走这一个入口，形状与配色不会各自漂移。
 */
@Composable
fun MenuItemGroup(
    items: List<MenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    DropdownMenuGroup(
        shapes = MenuDefaults.groupShape(0, 1),
        modifier = modifier
    ) {
        val count = items.size
        items.forEachIndexed { index, item ->
            val glyph = item.glyph
            val symbol = item.icon
            DropdownMenuItem(
                onClick = {
                    item.onClick()
                    onDismiss()
                },
                text = { Text(text = item.name) },
                shape = menuItemShape(index = index, count = count),
                leadingIcon = when {
                    // 手绘字形优先：只有字体子集里缺码位的图标才会走这条。
                    glyph != null -> {
                        {
                            SonGlyphIcon(
                                glyph = glyph,
                                contentDescription = null,
                                size = MenuDefaults.LeadingIconSize
                            )
                        }
                    }

                    symbol != null -> {
                        {
                            M3SymbolIcon(
                                name = symbol,
                                contentDescription = null,
                                filled = item.iconFilled,
                                size = MenuDefaults.LeadingIconSize
                            )
                        }
                    }

                    else -> null
                },
                colors = if (item.isDestructive) {
                    MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    MenuDefaults.itemColors()
                }
            )
        }
    }
}

/** 组内每项的形状：首项上圆、末项下圆、中间方角；只有一项时四角全圆。 */
@Composable
private fun menuItemShape(index: Int, count: Int): Shape = when {
    count <= 1 -> MenuDefaults.standaloneItemShape
    index == 0 -> MenuDefaults.leadingItemShape
    index == count - 1 -> MenuDefaults.trailingItemShape
    else -> MenuDefaults.middleItemShape
}

/**
 * 由外壳（`AppNavGraph`）接线的歌曲动作：队列操作与「查看详情」。
 *
 * 抽出来是因为这三件事在被推入的页面里都必须有入口：队列操作只在「全部歌曲」里出现的话，
 * 用户在专辑、歌单、收藏里想"把这首插到下一首"就无路可走。
 */
data class SongQueueActions(
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onViewDetail: (Song) -> Unit
)

/**
 * 各列表统一的歌曲菜单：下一首播放 / 加入播放队列 / 收藏（或取消收藏）/ 查看详情。
 *
 * 页面特有的动作（例如歌单页的「移出歌单」）通过 [extra] 追加在末尾，顺序因此各处一致。
 * [queueActions] 为空时只产出 [extra] —— 没接线的页面宁可没有按钮，也不要出现点了没反应的菜单项。
 * [onToggleFavorite] 可空：行尾已经有心形的页面（歌手页、收藏页）不必再摆一遍。
 */
@Composable
fun songMenuItems(
    song: Song,
    queueActions: SongQueueActions?,
    onToggleFavorite: ((Song) -> Unit)? = null,
    extra: List<MenuItem> = emptyList()
): List<MenuItem> {
    if (queueActions == null) return extra

    // 标签先在组合作用域里取好：下面的 buildList 只是个普通 lambda，不该在里头读资源。
    val playNextLabel = stringResource(R.string.songs_play_next)
    val addToQueueLabel = stringResource(R.string.songs_add_to_queue)
    val favoriteLabel = stringResource(
        if (song.isFavorite) R.string.songs_batch_unfavorite else R.string.songs_batch_favorite
    )
    val viewDetailLabel = stringResource(R.string.songs_view_detail)
    // 下载入口由壳层提供：没有装载宿主（预览、测试）时不显示这一项。
    val downloadRequest = LocalDownloadRequester.current
    val downloadLabel = stringResource(R.string.download_menu_item)

    return buildList {
        add(
            MenuItem(
                name = playNextLabel,
                icon = SymbolIconName.SkipNext,
                onClick = { queueActions.onPlayNext(song) }
            )
        )
        add(
            MenuItem(
                name = addToQueueLabel,
                icon = SymbolIconName.QueueMusic,
                onClick = { queueActions.onAddToQueue(song) }
            )
        )
        if (downloadRequest != null && song.isOnline) {
            add(
                MenuItem(
                    name = downloadLabel,
                    glyph = SonGlyph.Download,
                    onClick = { downloadRequest(song) }
                )
            )
        }
        if (onToggleFavorite != null) {
            add(
                MenuItem(
                    name = favoriteLabel,
                    icon = SymbolIconName.Favorite,
                    // 已收藏显示实心心形，未收藏是描边 —— 图标本身就是当前状态。
                    iconFilled = song.isFavorite,
                    onClick = { onToggleFavorite(song) }
                )
            )
        }
        add(
            MenuItem(
                name = viewDetailLabel,
                icon = SymbolIconName.MusicNote,
                onClick = { queueActions.onViewDetail(song) }
            )
        )
        addAll(extra)
    }
}

fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

/**
 * 把菜单弹在**手指按下的位置**。
 *
 * [point] 是长按点相对锚点（整行容器）左上角的像素偏移，`anchorBounds` 是同一锚点在窗口里的矩形，
 * 两者相加即为窗口坐标。最终结果再收进窗口内：贴着屏幕边缘长按时，否则菜单会有一半落在窗口外。
 */
private class AtPointPopupPositionProvider(
    private val point: IntOffset
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(
            x = (anchorBounds.left + point.x).coerceIn(0, maxX),
            y = (anchorBounds.top + point.y).coerceIn(0, maxY)
        )
    }
}
