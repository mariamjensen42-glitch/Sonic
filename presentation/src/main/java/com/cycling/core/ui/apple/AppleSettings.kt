package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName

/*
 * 设置类行组件（设置页与它的二级页共用）。
 * 规格来自 `item_preference_title_description` / `item_preference_switch`：
 * 上下 12dp、左右 16dp、标题 16sp、副标题 14sp、可点击态铺 fill 色。
 */

/** 设置项骨架。 */
@Composable
fun AppleSettingsRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.applePress(onClick) else Modifier)
            .padding(horizontal = AppleDims.padding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = icon,
            contentDescription = null,
            filled = true,
            tint = colors.secondaryLabel,
            size = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppleType.settingsTitle, color = colors.label)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = AppleType.settingsSubtitle,
                    color = colors.secondaryLabel
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailingContent()
    }
}

/** 导航型：前置图标 + 标题 + 行尾箭头。 */
@Composable
fun AppleSettingsNavRow(
    icon: SymbolIconName,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit
) {
    AppleSettingsRow(icon = icon, title = title, supporting = supporting, onClick = onClick) {
        AppleChevron()
    }
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
        }
    ) {
        Switch(
            checked = checked && enabled,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}

/**
 * iOS 的 inset grouped 分组容器：二级背景色 + 14dp 圆角，行之间的分隔线由调用方插。
 *
 * 替代 Material 的 `OutlinedCard` —— 后者有描边和阴影，iOS 分组没有。
 */
@Composable
fun AppleGroupedSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppleDims.cornerXl))
            .background(AppleTheme.colors.secondaryBackground),
        content = content
    )
}
