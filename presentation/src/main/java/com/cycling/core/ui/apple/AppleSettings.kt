package com.cycling.core.ui.apple

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.components.SonGlyphIcon
import com.cycling.core.ui.components.SymbolIconName

/*
 * 设置类行组件（设置页与它的二级页共用）。
 * 版式对齐 MeloX 的 iOS grouped list：行高 ≥56dp、左右 16dp、标题 16sp/副标题 14sp、
 * 图标与箭头用 Apple Music 红（systemRed）、分隔线两侧各缩进 16dp。
 */

/** 设置页的分组标题：13sp、次级色、左侧与卡片圆角对齐。 */
@Composable
fun AppleSettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        style = AppleType.footnoteEmphasized,
        color = AppleTheme.colors.secondaryLabel
    )
}

/**
 * 分组内的行分隔线：两侧各缩进 [inset]（MeloX 从 16dp 到 width-16dp）。
 * 比 [AppleSeparator] 细一档（0.5dp）且不贴边。
 */
@Composable
fun AppleSettingsSeparator(modifier: Modifier = Modifier, inset: Dp = 16.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset, end = inset)
            .height(0.5.dp)
            .background(AppleTheme.colors.separator)
    )
}

/** 设置项骨架。[trailingContent] 传 null 表示行尾什么都不放（纯信息行）。 */
@Composable
fun AppleSettingsRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    accent: Color = AppleTheme.colors.systemRed,
    trailingContent: (@Composable () -> Unit)? = null
) {
    AppleSettingsRowLayout(
        title = title,
        supporting = supporting,
        onClick = onClick,
        leading = {
            M3SymbolIcon(
                name = icon,
                contentDescription = null,
                filled = true,
                tint = accent,
                size = 22.dp
            )
        },
        trailingContent = trailingContent
    )
}

/**
 * 设置项骨架（手绘字形版）。
 *
 * Material Symbols 是**子集字体**，缺码位的图标（例如下载）走 [SonGlyph]；
 * 两种图标的字号与着色在这里对齐成同一套（22dp / systemRed）。
 */
@Composable
fun AppleSettingsRow(
    glyph: SonGlyph,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    accent: Color = AppleTheme.colors.systemRed,
    trailingContent: (@Composable () -> Unit)? = null
) {
    AppleSettingsRowLayout(
        title = title,
        supporting = supporting,
        onClick = onClick,
        leading = {
            SonGlyphIcon(
                glyph = glyph,
                contentDescription = null,
                tint = accent,
                size = 22.dp
            )
        },
        trailingContent = trailingContent
    )
}

/** 行的排版骨架：左图标槽 + 标题/副标题 + 行尾内容。 */
@Composable
private fun AppleSettingsRowLayout(
    title: String,
    supporting: String?,
    onClick: (() -> Unit)?,
    leading: @Composable () -> Unit,
    trailingContent: (@Composable () -> Unit)?
) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.applePress(onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { leading() }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = AppleType.settingsTitle,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = AppleType.settingsSubtitle,
                    color = colors.secondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/** 导航型：前置图标 + 标题 + 行尾红箭头。 */
@Composable
fun AppleSettingsNavRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit
) {
    AppleSettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        onClick = onClick,
        trailingContent = { AppleChevron(tint = AppleTheme.colors.systemRed) }
    )
}

/** 导航型（手绘字形版）。 */
@Composable
fun AppleSettingsNavRow(
    glyph: SonGlyph,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit
) {
    AppleSettingsRow(
        glyph = glyph,
        title = title,
        supporting = supporting,
        onClick = onClick,
        trailingContent = { AppleChevron(tint = AppleTheme.colors.systemRed) }
    )
}

/** 开关型：整行可点，[enabled] 为 false 时只读。 */
@Composable
fun AppleSettingsSwitchRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    AppleSettingsRow(
        icon = icon,
        title = title,
        supporting = supporting,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailingContent = {
            AppleSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        }
    )
}

/**
 * iOS 开关：51×31dp 胶囊、白色 27dp 圆头、合上时苹果绿。
 *
 * 不用 Material 的 `Switch` —— 它的轨道形状与拇指尺寸都是 Material 语义，
 * 放进 iOS grouped list 里一眼能看出是外来的。
 */
@Composable
fun AppleSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val accent = if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    val trackOff = if (dark) Color(0xFF787880).copy(alpha = 0.36f) else Color(0xFF787878).copy(alpha = 0.16f)
    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
        label = "appleSwitchThumb"
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(lerp(trackOff, accent, fraction))
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.toggleable(
                        value = checked,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onCheckedChange
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .offset(x = 20.dp * fraction)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.7f))
        )
    }
}

/**
 * iOS 的 inset grouped 分组容器：14dp 圆角 + 相对页面抬 6% 的实色底，
 * 行之间的分隔线由调用方插。
 *
 * 替代 Material 的 `OutlinedCard` —— 后者有描边和阴影，iOS 分组没有。
 *
 * 底色**不写死**成 `secondary_background_color`：设置页的页面底色来自壁纸动态取色，
 * 挂一块固定中性灰就会出现「卡片偏冷、页面偏紫」这种脏对比。改成把页面底色抬 6% 的
 * 前景色（亮色下是黑 6% ≈ #F0F0F0，暗色下是白 6%），任何壁纸下都成立 ——
 * 这也是 MeloX `MeloXIosGroupedList` 默认那层 `onBackground.copy(alpha = .045f)` 的做法。
 * 不用 `glass`：85% alpha 压在纯黑上会被抬成 #212123，再加白描边就成了描边卡片。
 */
@Composable
fun AppleGroupedSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppleDims.cornerXl))
            .background(AppleTheme.colors.label.copy(alpha = 0.06f)),
        content = content
    )
}
