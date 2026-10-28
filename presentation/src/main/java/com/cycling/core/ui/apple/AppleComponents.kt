package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName

/*
 * Apple Music 组件集。与 Material 组件不通用：这里的间距、圆角、字阶、按压反馈
 * 全部走 AppleTheme 令牌，不要混用 MaterialTheme 的取值。
 */

/** Apple 式按压：无涟漪，按下时铺一层 fill 色。 */
@Composable
internal fun Modifier.applePress(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this
        .background(if (pressed) AppleTheme.colors.fill else Color.Transparent)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** 行尾尖括号，list_action_icon_width = 26dp。 */
@Composable
fun AppleChevron(tint: Color = AppleTheme.colors.tertiaryLabel) {
    M3SymbolIcon(
        name = SymbolIconName.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        size = AppleDims.trailingIcon
    )
}

/**
 * 玻璃圆钮：44dp 圆底 + 22dp 图标，无涟漪，按下只加深一层底色。
 *
 * 版式取自 MeloX 歌单详情页顶栏的 `MeloXGlassCircleButton`：托底不是实色 scrim，而是
 * 「跟着图标明暗走的半透明玻璃」—— 图标浅（压在深封面上）用黑 22%，图标深用白 64%。
 * 这样同一颗按钮在任意封面上都有对比度，又不会像 60% 黑 scrim 那样压出一块死色。
 *
 * [glassAlpha] 给「顶栏随滚动淡出」的页面用（详情页折叠时托底要退干净）。
 */
@Composable
fun AppleGlassCircleButton(
    icon: SymbolIconName,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppleTheme.colors.label,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    glassAlpha: Float = 1f,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = glassSurfaceFor(tint)
    val alpha = (base.alpha * (if (pressed) 1.35f else 1f) * glassAlpha).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(base.copy(alpha = alpha))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = contentDescription,
            filled = true,
            tint = if (enabled) tint else AppleTheme.colors.tertiaryLabel,
            size = iconSize
        )
    }
}

/**
 * 玻璃托底色：前景（图标色）偏亮时垫黑 22%，偏暗时垫白 64%。
 *
 * MeloX 那边是「前景 == 纯白 ? 黑 22% : 白 64%」的硬判断；这边顶栏的图标色是按折叠进度
 * 插值出来的中间色，所以改成按相对亮度分档，插值到一半也不会突然跳色。
 */
private fun glassSurfaceFor(tint: Color): Color =
    if (tint.luminance() > 0.5f) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.64f)

/** 标题栏上的图标按钮，44dp 触达区，无涟漪。 */
@Composable
fun AppleIconButton(
    icon: SymbolIconName,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppleTheme.colors.key,
    size: Dp = 24.dp,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.applePress(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = contentDescription,
            filled = true,
            tint = if (enabled) tint else AppleTheme.colors.tertiaryLabel,
            size = size
        )
    }
}

/**
 * 大标题栏。34sp Bold，右侧动作图标，与 iOS 的 large title 一致。
 */
@Composable
fun AppleLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppleDims.padding, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppleType.largeTitle,
                    color = AppleTheme.colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppleType.footnote,
                        color = AppleTheme.colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            actions()
        }
    }
}

/**
 * 分区标题。20sp Bold + 可选副标题 + 行尾尖括号，
 * 上下留白取自 header_section_a（paddingTop 13 / paddingBottom 10）。
 */
@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.applePress(onClick) else Modifier)
            .padding(
                start = AppleDims.padding,
                end = AppleDims.padding,
                top = AppleDims.sectionTitleTop,
                bottom = AppleDims.sectionTitleBottom
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleType.sectionTitle,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppleType.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null) AppleChevron()
    }
}

/** 横滑容器：左右留白 16dp，行尾露出 36dp（dynamic_grid_peek）。 */
@Composable
fun AppleCarousel(
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = AppleDims.padding, end = AppleDims.gridPeek),
        horizontalArrangement = Arrangement.spacedBy(AppleDims.gridGutter),
        content = content
    )
}

/**
 * 方形封面卡（专辑 / 歌单）。封面圆角 corner_radius_m = 7dp，
 * 标题 15sp、副标题 13sp，与 swiping grid 卡一致。
 */
@Composable
fun AppleCard(
    artwork: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 148.dp,
    circular: Boolean = false
) {
    val colors = AppleTheme.colors
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(AppleDims.cornerM))
            .applePress(onClick)
            .padding(bottom = 2.dp)
    ) {
        M3Artwork(
            artwork = artwork,
            shape = if (circular) CircleShape else RoundedCornerShape(AppleDims.cornerM),
            modifier = Modifier
                .fillMaxWidth()
                .height(width)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = AppleType.subhead,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppleType.footnote,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * 大列表行（资料库的「最近添加 / 播放列表」）。封面与行同高 88dp，圆角 7dp。
 */
@Composable
fun AppleLargeListRow(
    artwork: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    imageSize: Dp = AppleDims.largeListImage,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .applePress(onClick)
            .padding(horizontal = AppleDims.padding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = artwork,
            shape = if (circular) CircleShape else RoundedCornerShape(AppleDims.cornerM),
            modifier = Modifier.size(imageSize)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleType.body,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppleType.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * 菜单行（播放列表 / 艺人 / 专辑 / 歌曲…）。
 * 上下留白 13dp、末端 20dp、图标与文字间距 16dp，取自 large_list_a_item。
 */
@Composable
fun AppleMenuRow(
    icon: SymbolIconName,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppleTheme.colors.systemRed,
    showChevron: Boolean = true
) {
    val colors = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .applePress(onClick)
            .padding(
                start = AppleDims.padding,
                end = AppleDims.menuRowEnd,
                top = AppleDims.menuRowVertical,
                bottom = AppleDims.menuRowVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = null,
            filled = true,
            tint = tint,
            size = AppleDims.trailingIcon
        )
        Spacer(modifier = Modifier.width(AppleDims.menuRowGap))
        Text(
            text = label,
            style = AppleType.sectionTitle,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) AppleChevron()
    }
}

/** 歌曲行：48dp 封面 + 两行文本，行高由内容决定（≈ 64dp）。 */
@Composable
fun AppleSongRow(
    artwork: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .applePress(onClick)
            .padding(horizontal = AppleDims.padding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = artwork,
            shape = RoundedCornerShape(AppleDims.cornerS),
            modifier = Modifier.size(AppleDims.listItemHeight)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleType.body,
                color = if (isPlaying) colors.systemRed else colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppleType.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * 分隔线。iOS 的分隔线不与屏幕同宽，而是从内容起始处起笔，
 * 由 [leadingInset] 控制（菜单行 58dp / 封面行 116dp）。
 */
@Composable
fun AppleSeparator(
    modifier: Modifier = Modifier,
    leadingInset: Dp = AppleDims.padding
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = leadingInset)
            .height(0.5.dp)
            .background(AppleTheme.colors.separator)
    )
}

/**
 * 「最近添加」四宫格拼图：2×2 封面，圆角 corner_radius_m。
 * 是资料库页最有辨识度的结构。
 */
@Composable
fun AppleArtworkMosaic(
    artworks: List<String?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AppleDims.largeListImage
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(AppleDims.cornerM))
            .applePress(onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            artworks.chunked(2).take(2).forEach { rowItems ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(2) { index ->
                        val artwork = rowItems.getOrNull(index)
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            if (artwork != null) {
                                M3Artwork(
                                    artwork = artwork,
                                    shape = RoundedCornerShape(0.dp),
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AppleTheme.colors.secondaryBackground)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
