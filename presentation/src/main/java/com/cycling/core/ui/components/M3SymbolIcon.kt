package com.cycling.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSymbolFontFamily

/**
 * 全 App 的图标语义清单（Material Symbols Rounded 码位）。
 *
 * 用码位而不是连字字符串：连字拼错不会报错，只会静默渲染成字面文字；
 * 码位缺失会在 `scripts/subset-fonts.py` 的清单校验里暴露。
 * 新增图标时：把码位加进枚举，并同步 `scripts/subset-fonts.py` 的 SYMBOL_CODEPOINTS。
 */
enum class SymbolIconName(val codepoint: Int) {
    // ── 播放控制 ────────────────────────────────────────────────
    PlayArrow(0xE037),
    Pause(0xE034),
    SkipPrevious(0xE045),
    SkipNext(0xE044),
    Shuffle(0xE043),
    Repeat(0xE040),
    PlayCircle(0xE1C4),
    VolumeUp(0xE050),
    VolumeOff(0xE04F),
    Speed(0xE9E4),
    GraphicEq(0xE1B8),
    Waves(0xE176),
    Mic(0xE029),

    // ── 导航与外壳 ──────────────────────────────────────────────
    Home(0xE9B2),
    LibraryMusic(0xE030),
    ArrowBack(0xE5C4),
    KeyboardArrowDown(0xE313),
    KeyboardArrowRight(0xE315),
    KeyboardArrowUp(0xE316),
    ExpandMore(0xE5CF),
    ExpandLess(0xE5CE),
    MoreVert(0xE5D4),

    // ── 音乐内容 ────────────────────────────────────────────────
    MusicNote(0xE405),
    Album(0xE019),
    Person(0xF0D3),
    Folder(0xE2C7),
    QueueMusic(0xE03D),
    PlaylistAdd(0xE03B),
    PlaylistRemove(0xEB80),
    Lyrics(0xEC0B),

    // ── 操作与状态 ──────────────────────────────────────────────
    Add(0xE145),
    Favorite(0xE87E),
    Search(0xEF7A),
    Close(0xE5CD),
    Check(0xE668),
    CheckCircle(0xF0BE),
    Checklist(0xE6B1),
    SelectAll(0xE162),
    Sort(0xE164),
    Edit(0xF097),
    Delete(0xE92E),
    Refresh(0xE5D5),
    AutoAwesome(0xE65F),
    Settings(0xE8B8),
    Tune(0xE429),
    SwapVert(0xE8D5),
    DragHandle(0xE25D),
    DragIndicator(0xE945),
    ContentCopy(0xE14D),
    IosShare(0xE6B8),
    History(0xE8B3),
    Restore(0xE8B3),
    Autorenew(0xE863),
    Backup(0xE864),
    UploadFile(0xE9FC),
    Key(0xE73C),
    Bedtime(0xF159),
    Error(0xF8B6),
    BarChart(0xE26B),
    HighQuality(0xE024);

    /** 字体里的字符；按码位构造，避免依赖连字表。 */
    val glyph: String get() = String(Character.toChars(codepoint))
}

/**
 * Material Symbols 的自然行高 / 字号比（ascent 1056 + |descent 96| / upem 960）。
 * 用它做 lineHeight 才能让字形在行盒里垂直居中。
 */
private const val SymbolLineHeightRatio = 1.2f

/**
 * Material Symbols Rounded 图标。
 *
 * 这是 Material 3 没有提供、而这个 App 必须有的原语（图标来自字体而不是
 * [androidx.compose.ui.graphics.vector.ImageVector]），因此放在
 * `core/ui/components/`。参数与 [androidx.compose.material3.Icon] 对齐：
 * [contentDescription] 为 null 时按装饰性图标处理，不进入无障碍树。
 *
 * 字号跟随**实际盒子边长**（[BoxWithConstraints]），而不是只认 [size] 参数：
 * 调用方常写 `Modifier.size(SonSizes.iconExtraLarge)`，如果字号仍按默认 24sp，
 * 字形会缩在大盒子中间，看起来又小又空（“图标没显示全/间距不对”）。现在
 * 40dp 的槽位就渲染 40sp 的字形。
 *
 * @param filled FILL 1（强调/选中）或 FILL 0（弱化/未选中）
 * @param size 没有外部尺寸约束时的兜底边长
 */
@Composable
fun M3SymbolIcon(
    name: SymbolIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
    size: Dp = SonSizes.iconMedium
) {
    BoxWithConstraints(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        // 外部 Modifier.size(...) 先于兜底 size 生效，所以这里以真实约束为准。
        val glyphSize = if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            minOf(maxWidth, maxHeight)
        } else {
            size
        }
        val fontSize = with(LocalDensity.current) { glyphSize.toSp() }
        Text(
            text = name.glyph,
            color = tint,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            // 字形行盒比图标盒子高（1.2em）；先无界测量再居中，避免被盒子高度
            // 截断后基线整体下移。
            modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true),
            fontFamily = if (filled) SonSymbolFontFamily.Filled else SonSymbolFontFamily.Outlined,
            style = TextStyle(
                fontSize = fontSize,
                // Material Symbols 的 ascent/descent 是 1056/-96（upem 960），自然行高
                // 1.2em 且字形中心正好落在自然行盒中心（480/960）。若把 lineHeight
                // 压成 1em，基线仍按 ascent 放置，字形就会整体下沉约 0.1em —— 图标
                // 看起来"偏下"。这里保留自然行高，再由外层 Box 居中。
                lineHeight = fontSize * SymbolLineHeightRatio,
                letterSpacing = 0.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
