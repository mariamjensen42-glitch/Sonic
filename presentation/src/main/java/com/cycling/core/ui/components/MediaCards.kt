package com.cycling.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing

/*
 * 本文件只保留 Material 3 未提供的组合：封面加载、带封面的媒体卡、歌手卡。
 * 其余一切（按钮、卡片、列表、应用栏、指示器、输入框、芯片）一律直接使用
 * androidx.compose.material3 的原生组件 —— 不要再在此包内新增转发壳。
 */

/**
 * 统一的封面渲染：有图按比例裁切，无图落到中性占位底色 + 中性图标。
 *
 * 这是 Compose / Material 都没有的原语（涉及 Coil 与占位语义），因此保留；
 * 形状由调用方用 `MaterialTheme.shapes.*` 传入。
 */
@Composable
fun M3Artwork(
    artwork: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    /** 有值时用 Apple 造型的矢量图标做占位（播放页 / 队列页），Material 那档只作兜底。 */
    placeholderGlyph: SonGlyph? = null,
    placeholderIcon: SymbolIconName = SymbolIconName.MusicNote
) {
    val extended = LocalSonExtendedColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(extended.artworkPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (placeholderGlyph != null) {
            SonGlyphIcon(
                glyph = placeholderGlyph,
                contentDescription = null,
                tint = extended.onArtworkPlaceholder,
                size = SonSizes.iconLarge,
                modifier = Modifier.size(SonSizes.iconLarge)
            )
        } else {
            M3SymbolIcon(
                name = placeholderIcon,
                filled = false,
                contentDescription = null,
                modifier = Modifier.size(SonSizes.iconLarge),
                tint = extended.onArtworkPlaceholder
            )
        }
    }
}

/**
 * 带封面的媒体卡（专辑 / 播放列表）。
 *
 * [width] 传 null 表示跟随父容器宽度（网格场景），横向轮播用固定宽度
 * （默认 [SonSizes.mediaCardWidth]）。形状取官方 `Shapes.largeIncreased`。
 */
@Composable
fun M3MediaCard(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = SonSizes.mediaCardWidth,
    placeholderIcon: SymbolIconName = SymbolIconName.MusicNote,
    shape: CornerBasedShape = MaterialTheme.shapes.largeIncreased
) {
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick)
    ) {
        M3Artwork(
            artwork = artwork,
            placeholderIcon = placeholderIcon,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 头像形状。 */
enum class M3AvatarShape {
    Circle,
    RoundedSquare
}

/**
 * 歌手卡。头像默认圆形，与矩形封面形成形状对比 —— 这是 M3 层级语言的一部分。
 *
 * [size] 传 null 表示跟随父容器宽度（网格场景）。
 */
@Composable
fun M3ArtistCard(
    name: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = SonSizes.artistCardSize,
    avatarShape: M3AvatarShape = M3AvatarShape.Circle
) {
    val shape: CornerBasedShape = when (avatarShape) {
        M3AvatarShape.RoundedSquare -> MaterialTheme.shapes.largeIncreased
        M3AvatarShape.Circle -> CircleShape
    }
    Column(
        modifier = modifier
            .then(if (size != null) Modifier.width(size) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        M3Artwork(
            artwork = artwork,
            placeholderIcon = SymbolIconName.Person,
            shape = shape,
            modifier = if (size != null) {
                Modifier.size(size)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            }
        )
        Spacer(modifier = Modifier.height(SonSpacing.small))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
