package com.cycling.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.SonSizes

/**
 * 播放页的图标清单（Apple Music 图形语言）。
 *
 * 与 [SymbolIconName]（Material Symbols 字体）并存但用途不同：Material 那套是**全应用**的
 * 通用语义图标；这里只收播放页 / 歌词页 / 队列页用到的那二十个，造型按 Apple Music 走。
 *
 * 为什么是手写路径而不是字体：SF Symbols 是 Apple 授权、只允许在 Apple 平台使用，不能打进
 * 非 Apple 平台的安装包（本项目 AGENTS.md 亦明令禁止）。这里全部用 [ImageVector] 手绘，
 * 几何参考取自 Lucide（ISC）与 Material Symbols（Apache-2.0）的同形图标，再按 Apple Music
 * 的视觉重量重设描边与留白；单个图标 0.4~0.9 KB，比字体子集还省。
 */
enum class SonGlyph {
    Play,
    Pause,
    SkipBack,
    SkipForward,
    Heart,
    Ellipsis,
    Lyrics,
    Queue,
    Shuffle,
    Repeat,
    RepeatOne,
    Grip,
    Trash,
    Music,
    ChevronDown,
    AirPlay,
    VolumeLow,
    VolumeHigh,
    Download,
    Share
}

/**
 * 以 Apple Music 的造型渲染 [SonGlyph]。
 *
 * [filled] 只对 [SonGlyph.Heart] 有意义（空心/实心收藏）；其余图标本身就是实心或描边，
 * 传了也不改变形状 —— 这样调用方可以放心把 Material 那边的 `filled` 直接搬过来。
 */
@Composable
fun SonGlyphIcon(
    glyph: SonGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
    size: Dp = SonSizes.iconMedium
) {
    Icon(
        imageVector = glyph.vector(filled),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** 图标按 (glyph, filled) 缓存：ImageVector 构造要解析一次路径，不该每次组合都来一遍。 */
private val glyphCache = HashMap<Pair<SonGlyph, Boolean>, ImageVector>()

private fun SonGlyph.vector(filled: Boolean): ImageVector =
    glyphCache.getOrPut(this to filled) { build(this, filled) }

/** 统一的描边宽度。24 网格下 2.0 是 Apple Music 的视觉重量：1.5 偏细，2.5 发胖。 */
private const val StrokeWeight = 2f

private fun build(glyph: SonGlyph, filled: Boolean): ImageVector {
    val builder = ImageVector.Builder(
        name = glyph.name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    return when (glyph) {
        SonGlyph.Play -> builder
            .fillPath("M8.2 4.4v15.2c0 1 .9 1.7 1.9 1.2l11.2-7.6c.7-.5.7-1.6 0-2.1L10.1 3.2c-1-.5-1.9.2-1.9 1.2z")
            .build()

        SonGlyph.Pause -> builder
            .fillPath(roundedRect(6.9f, 4.4f, 3.6f, 15.2f, 1.4f) + roundedRect(13.5f, 4.4f, 3.6f, 15.2f, 1.4f))
            .build()

        // 竖条 + 三角：Apple Music 的上一首/下一首都是这个结构，条在三角的外侧。
        SonGlyph.SkipBack -> builder
            .fillPath(
                roundedRect(4.3f, 5.2f, 2.2f, 13.6f, 1.1f) +
                    "M19.4 6.2v11.6c0 1.1-.9 1.8-1.9 1.2l-9.6-5.8c-.9-.5-.9-1.8 0-2.3l9.6-5.8c1-.6 1.9.1 1.9 1.1z"
            )
            .build()

        SonGlyph.SkipForward -> builder
            .fillPath(
                "M4.6 6.2v11.6c0 1.1.9 1.8 1.9 1.2l9.6-5.8c.9-.5.9-1.8 0-2.3L6.5 5.1c-1-.6-1.9.1-1.9 1.1z" +
                    roundedRect(17.5f, 5.2f, 2.2f, 13.6f, 1.1f)
            )
            .build()

        SonGlyph.Heart -> builder
            .apply { if (filled) fillPath(HEART_PATH) else strokePath(HEART_PATH) }
            .build()

        SonGlyph.Ellipsis -> builder
            .fillPath(
                circle(5.4f, 12f, 1.65f) + circle(12f, 12f, 1.65f) + circle(18.6f, 12f, 1.65f)
            )
            .build()

        // 圆角气泡 + 内部两条引用竖线（Apple Music 的歌词按钮）。
        SonGlyph.Lyrics -> builder
            .strokePath(
                "M6.9 3.6h10.2a3.9 3.9 0 0 1 3.9 3.9v5.2a3.9 3.9 0 0 1-3.9 3.9h-5.6l-3.9 3.3c-.6.5-1.6.1-1.6-.7v-3" +
                    "a3.9 3.9 0 0 1-2.9-3.8V7.5a3.9 3.9 0 0 1 3.9-3.9z" +
                    "M9 8.1v2.9M13.2 8.1v2.9"
            )
            .build()

        // 三行文本 + 右下音符：Apple Music 的「播放队列」。
        SonGlyph.Queue -> builder
            .strokePath("M3.6 6.4h9.2M3.6 12h6.4M3.6 17.6h6.4")
            .strokePath(circle(16.9f, 17.1f, 2.7f))
            .strokePath("M19.6 17.1V6.4")
            .build()

        // 两条交叉路径，四个端点各一个 V 形箭头（Lucide shuffle 的几何，逐条独立以免相对命令串位）。
        SonGlyph.Shuffle -> builder
            .strokePath("M18 14l4 4-4 4")
            .strokePath("M18 2l4 4-4 4")
            .strokePath("M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22")
            .strokePath("M2 6h1.972a4 4 0 0 1 3.6 2.2")
            .strokePath("M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45")
            .build()

        // 闭合环路，两个对角各一个箭头。
        SonGlyph.Repeat -> builder
            .strokePath("M17 2l4 4-4 4")
            .strokePath("M3 11v-1a4 4 0 0 1 4-4h14")
            .strokePath("M7 22l-4-4 4-4")
            .strokePath("M21 13v1a4 4 0 0 1-4 4H3")
            .build()

        // 同样的环路，中间加一个带起笔的数字 1。
        SonGlyph.RepeatOne -> builder
            .strokePath("M17 2l4 4-4 4")
            .strokePath("M3 11v-1a4 4 0 0 1 4-4h14")
            .strokePath("M7 22l-4-4 4-4")
            .strokePath("M21 13v1a4 4 0 0 1-4 4H3")
            .strokePath("M10.6 11.2 12 10v4.2")
            .build()

        SonGlyph.Grip -> builder
            .strokePath("M6.6 7.8h10.8M6.6 12h10.8M6.6 16.2h10.8")
            .build()

        SonGlyph.Trash -> builder
            .strokePath("M10 11v6")
            .strokePath("M14 11v6")
            .strokePath("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6")
            .strokePath("M3 6h18")
            .strokePath("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
            .build()

        // 实心双联八分音符（无封面时的占位）：两个符头 + 两根符干 + 一条横杠。
        SonGlyph.Music -> builder
            .fillPath(
                circle(6.7f, 17.4f, 2.5f) +
                    circle(16.7f, 15.6f, 2.5f) +
                    roundedRect(8.5f, 6.3f, 1.7f, 11.3f, 0.85f) +
                    roundedRect(18.5f, 4.5f, 1.7f, 11.3f, 0.85f)
            )
            .strokePath("M9.35 6.3 19.35 4.5")
            .build()

        SonGlyph.ChevronDown -> builder
            .strokePath("M6.5 9.5 12 15l5.5-5.5")
            .build()

        // 矩形框底边中间开口，让出向上的三角；三角底边略低于框底，才有「从框里穿出来」的读法。
        SonGlyph.AirPlay -> builder
            .strokePath("M4 4.4h16a2.4 2.4 0 0 1 2.4 2.4v9.6a2.4 2.4 0 0 1-2.4 2.4h-3.2")
            .strokePath("M7 18.8H4a2.4 2.4 0 0 1-2.4-2.4V6.8A2.4 2.4 0 0 1 4 4.4")
            .strokePath("M12 12.4 17 19.9H7z")
            .build()

        SonGlyph.VolumeLow -> builder
            .strokePath("M3.6 9.4h2.7l4.2-3.8v12.8l-4.2-3.8H3.6z")
            .strokePath("M13.8 9.4a3 3 0 0 1 0 5.2")
            .build()

        SonGlyph.VolumeHigh -> builder
            .strokePath("M3.6 9.4h2.7l4.2-3.8v12.8l-4.2-3.8H3.6z")
            .strokePath("M13.8 9.4a3 3 0 0 1 0 5.2")
            .strokePath("M17.2 6.6a7 7 0 0 1 0 10.8")
            .build()

        SonGlyph.Download -> builder
            .strokePath("M12 3.6v10.6")
            .strokePath("M7.6 9.9 12 14.3l4.4-4.4")
            .strokePath("M6.8 16.9H4.8v2.3a2.4 2.4 0 0 0 2.4 2.4h9.6a2.4 2.4 0 0 0 2.4-2.4v-2.3h-2")
            .build()

        SonGlyph.Share -> builder
            .strokePath("M12 3.2v10.4")
            .strokePath("M8.5 6.7 12 3.2l3.5 3.5")
            .strokePath("M4.8 11.9v6.4a2.4 2.4 0 0 0 2.4 2.4h9.6a2.4 2.4 0 0 0 2.4-2.4v-6.4")
            .build()
    }
}

/** 心形轮廓：首尾闭合，描边与填充都用它。 */
private const val HEART_PATH =
    "M12 20.3c-.3 0-.6-.1-.8-.3C5.6 15.6 2 12.6 2 9.3 2 6.4 4.3 4 7.2 4c1.7 0 3.3 0.8 4.3 2.2L12 6.8" +
        "l0.5-0.6C13.5 4.8 15.1 4 16.8 4 19.7 4 22 6.4 22 9.3c0 3.3-3.6 6.3-9.2 10.7-.2.2-.5.3-.8.3z"

private fun ImageVector.Builder.fillPath(path: String): ImageVector.Builder =
    addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))

private fun ImageVector.Builder.strokePath(
    path: String,
    width: Float = StrokeWeight
): ImageVector.Builder =
    addPath(
        pathData = addPathNodes(path),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )

/** SVG 的圆角矩形路径。 */
private fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val radius = minOf(r, w / 2f, h / 2f)
    val innerW = w - 2 * radius
    val innerH = h - 2 * radius
    return buildString {
        append("M").append(x + radius).append(" ").append(y)
        append("h").append(innerW)
        append("a").append(radius).append(" ").append(radius).append(" 0 0 1 ").append(radius).append(" ").append(radius)
        append("v").append(innerH)
        append("a").append(radius).append(" ").append(radius).append(" 0 0 1 ").append(-radius).append(" ").append(radius)
        append("h").append(-innerW)
        append("a").append(radius).append(" ").append(radius).append(" 0 0 1 ").append(-radius).append(" ").append(-radius)
        append("v").append(-innerH)
        append("a").append(radius).append(" ").append(radius).append(" 0 0 1 ").append(radius).append(" ").append(-radius)
        append("z")
    }
}

/** SVG 的圆路径（两段半圆弧）。 */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0z"
