package com.cycling.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.cycling.core.ui.apple.AppleGlassCircleButton
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R

/**
 * 歌单详情页，照 `MeloX-Android` 的 `MeloXPlaylistDetailScreen` 复刻。
 *
 * 整页结构（MeloX 原实现）：
 * ```
 * 整屏背景：封面模糊铺满 + 明暗可读性层（MeloXFlowingLightBackdrop 的静态替身）
 * ├── 工具条 58dp：返回玻璃圆钮 / (动作玻璃圆钮 × N)          ← 浮在背景上，不占内容
 * ├── 英雄区（Column，上 26dp / 下 22dp）
 * │   ├── 封面 min(屏宽 × 0.68, 300dp)，圆角 12dp，投影 18dp
 * │   ├── 标题 22sp Bold / 行高 27 / 居中 / 至多两行（上 24dp，左右 24dp）
 * │   ├── 创作者 20sp / 行高 24 / 上 8dp
 * │   ├── 元数据 15sp Medium / 行高 19 / 上 7dp
 * │   ├── 动作行（上 17dp，间隔 14dp）
 * │   │   ├── 随机 玻璃圆钮 54dp
 * │   │   ├── 播放 胶囊 140×50，白 82% 底 + 黑字（深色前景时）
 * │   │   └── 收藏 玻璃圆钮 54dp
 * │   └── 简介 14sp / 行高 20 / 折叠 3 行（展开 12 行）/ 整段可点
 * └── 曲目列表
 * ```
 *
 * 与 Apple 那版头部（72dp 留白 + 顶栏大标题）的区别：这里封面按屏宽比例取大小、
 * 标题回到封面下方，动作区是「圆钮 + 胶囊 + 圆钮」而不是两颗等宽胶囊。
 */

/** 封面宽度占比与上限，取自 MeloX：`min(maxWidth * 0.68f, 300.dp)`。 */
private const val ArtworkWidthRatio = 0.68f
private val ArtworkMaxSize = 300.dp

/**
 * 背景模糊的两道保险。
 *
 * 1. **小图解码**：`Image` + `rememberAsyncImagePainter` 才不会被布局约束覆盖 `size`，
 *    这里按 [BackdropDecodeSize] 方图解码、再双线性拉满全屏。API 31 以下没有 RenderEffect，
 *    全靠这一道 —— 所以它必须小。
 * 2. **真高斯模糊**：`Modifier.blur` 在 API 31+ 生效，把小图拉满后残留的块状感磨平。
 *    以下版本它静默失效，退化成第 1 道的软拉伸。
 *
 * 用 `AsyncImage` 会拿约束把 `size` 改回全屏 ⇒ 两道全废，背景跟封面一样清楚。
 */
private const val BackdropDecodeSize = 8

/** 真实模糊半径；配合 8px 解码，任何封面都会化成色块。 */
private val BackdropBlurRadius = 32.dp

/** 工具条高度（MeloX `MeloXPlaylistToolbar`：58dp 行高、左右 18dp、上下 7dp）。 */
private val ToolbarHeight = 58.dp
private val ToolbarHorizontalPadding = 18.dp
private val ToolbarVerticalPadding = 7.dp

/** `MeloXGlassCircleButton` 的两种尺寸：工具条 44dp、英雄区 54dp。 */
private val ToolbarButtonSize = 44.dp
private val HeroButtonSize = 54.dp

/** 播放胶囊：MeloX 写死 140×50，圆角 25dp。 */
private val PlayPillWidth = 140.dp
private val PlayPillHeight = 50.dp
private val PlayPillRadius = 25.dp

/**
 * 工具条：[onBack] 在左，[actions] 一组玻璃圆钮在右。
 * 背景透明 —— 底下是整屏的封面模糊层，列表滚上来时从按钮下方经过。
 */
@Composable
fun MeloXPlaylistToolbar(
    onBack: () -> Unit,
    backContentDescription: String?,
    backIcon: SymbolIconName,
    contentColor: Color,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(ToolbarHeight)
            .padding(
                horizontal = ToolbarHorizontalPadding,
                vertical = ToolbarVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppleGlassCircleButton(
            icon = backIcon,
            contentDescription = backContentDescription,
            onClick = onBack,
            tint = contentColor,
            size = ToolbarButtonSize
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions()
        }
    }
}

/**
 * 整屏背景：封面模糊 + Apple 那层明暗可读性遮罩。
 *
 * MeloX 用的是 `MeloXFlowingLightBackdrop`（随时间流动的取色光斑）；Sonic 这里用
 * 同一张封面的低分辨率模糊代替 —— 色彩来源一致，且没有逐帧动画的开销。
 */
@Composable
fun MeloXPlaylistBackdrop(
    artwork: String?,
    modifier: Modifier = Modifier,
    placeholderIcon: SymbolIconName = SymbolIconName.MusicNote
) {
    val dark = isSystemInDarkTheme()
    val platformContext = LocalPlatformContext.current
    val backdrop = remember(artwork, platformContext) {
        artwork?.let { url ->
            ImageRequest.Builder(platformContext)
                .data(url)
                .size(Size(BackdropDecodeSize, BackdropDecodeSize))
                .build()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (backdrop != null) {
            Image(
                painter = rememberAsyncImagePainter(model = backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.95f
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
                    .blur(BackdropBlurRadius)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                M3Artwork(
                    artwork = null,
                    shape = RoundedCornerShape(0.dp),
                    placeholderIcon = placeholderIcon,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // MeloX 的可读性层：深色前景压 26% 黑，浅色前景压 46% 白。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (dark) {
                        Color.Black.copy(alpha = 0.26f)
                    } else {
                        Color.White.copy(alpha = 0.46f)
                    }
                )
        )
        // 下沿收进页面底色，列表滚到下半屏时不至于压着一团彩色噪点。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to AppleTheme.colors.background.copy(alpha = 0.92f)
                    )
                )
        )
    }
}

/**
 * 英雄区。几何全部取自 MeloX `MeloXStandardPlaylistHero`。
 *
 * @param meta 元数据行文案（「42 首歌曲 · 2 小时」），空串不占位。
 * @param footer 简介槽，由页面注入（MeloX 把简介也画在这个 Column 里）。
 */
@Composable
fun MeloXPlaylistHero(
    title: String,
    subtitle: String,
    meta: String,
    artwork: String?,
    placeholderIcon: SymbolIconName,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    hasSongs: Boolean = true,
    showSaveAction: Boolean = false,
    saveIcon: SymbolIconName = SymbolIconName.PlaylistAdd,
    saveContentDescription: String? = null,
    onSave: () -> Unit = {},
    onHeightChanged: (Int) -> Unit = {},
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    val dark = isSystemInDarkTheme()
    val foreground = colors.label
    val secondary = colors.secondaryLabel

    // MeloX：浅色前景（深色封面上）用白 82% 底 + 黑字，反之用黑 82% 底 + 白字。
    val pillSurface = if (!dark) Color.Black.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
    val pillContent = if (!dark) Color.White else Color.Black

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val artworkSize = minOf(maxWidth * ArtworkWidthRatio, ArtworkMaxSize)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp, bottom = 22.dp)
                .onSizeChanged { onHeightChanged(it.height) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 阴影留在裁剪之外（MeloX 踩过：写在 sharedElement 之后会让投影进 overlay）。
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    )
            ) {
                M3Artwork(
                    artwork = artwork,
                    shape = RoundedCornerShape(12.dp),
                    placeholderIcon = placeholderIcon,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = title,
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp),
                color = foreground,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 8.dp),
                    color = foreground,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    modifier = Modifier.padding(top = 7.dp),
                    color = secondary,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.padding(top = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppleGlassCircleButton(
                    icon = SymbolIconName.Shuffle,
                    contentDescription = stringResource(R.string.collection_detail_shuffle_desc),
                    onClick = onShuffle,
                    tint = foreground,
                    size = HeroButtonSize,
                    iconSize = 26.dp,
                    enabled = hasSongs
                )

                Box(
                    modifier = Modifier
                        .width(PlayPillWidth)
                        .height(PlayPillHeight)
                        .clip(RoundedCornerShape(PlayPillRadius))
                        .background(pillSurface)
                        .applePress(onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        M3SymbolIcon(
                            name = SymbolIconName.PlayArrow,
                            filled = true,
                            contentDescription = null,
                            tint = pillContent,
                            size = 19.dp
                        )
                        Text(
                            text = stringResource(R.string.collection_detail_play),
                            color = pillContent,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showSaveAction) {
                    AppleGlassCircleButton(
                        icon = saveIcon,
                        contentDescription = saveContentDescription,
                        onClick = onSave,
                        tint = foreground,
                        size = HeroButtonSize,
                        iconSize = 26.dp
                    )
                }
            }

            footer?.let { content ->
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    }
}

/**
 * 集合总时长文案：不足 1 小时只报分钟，否则「X 小时 Y 分钟」；空集合报 0 分钟。
 *
 * 歌单详情与「我喜欢」共用同一句：两页的元数据行是同一个位置，单位写法必须一致。
 */
@Composable
fun formatCollectionDuration(totalMillis: Long): String {
    val totalMinutes = totalMillis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 ->
            stringResource(R.string.listen_duration_hours_minutes, hours, minutes)

        hours > 0 -> stringResource(R.string.listen_duration_hours, hours)
        totalMinutes > 0 -> stringResource(R.string.listen_duration_minutes, totalMinutes)
        else -> stringResource(R.string.listen_duration_zero)
    }
}

/**
 * 英雄区里的简介块：14sp、secondary、折叠 3 行、展开 12 行，**整段可点**。
 * MeloX 没有单独一颗「更多」按钮，点击文字本身就是展开/收起。
 */
@Composable
fun MeloXPlaylistDescription(
    description: String,
    expanded: Boolean,
    isGenerating: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppleTheme.colors

    if (description.isBlank() && !isGenerating) return

    val bodyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
    ) {
        if (isGenerating) {
            Text(
                text = stringResource(R.string.playlist_detail_desc_loading),
                style = bodyStyle,
                color = colors.secondaryLabel
            )
            return@Box
        }

        Text(
            text = description,
            style = bodyStyle,
            color = colors.secondaryLabel,
            maxLines = if (expanded) 12 else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .applePress(onToggle)
        )
    }
}
