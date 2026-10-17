package com.cycling.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.cycling.presentation.R

/**
 * Material Symbols Rounded 图标字体族（ADR-0002，C 方案）。
 *
 * 符号是码位字体，不是 [androidx.compose.ui.graphics.vector.ImageVector]；
 * 用 [com.cycling.core.ui.components.M3SymbolIcon] 渲染，不要直接拿它拼文字。
 *
 * FILL 0/1 在两份静态实例里（`scripts/subset-fonts.py`），而不是运行时可变轴：
 * Compose 1.13 带自定义 FontVariation 的资源字体在 API 33 上会静默回退到系统字体，
 * 图标码位就变成缺字方块。静态实例也把体积压到 11–16 KB。
 */
object SonSymbolFontFamily {

    /** FILL 0：弱化/未选中态。 */
    val Outlined: FontFamily = FontFamily(Font(R.font.material_symbols_rounded))

    /** FILL 1：强调/选中态。 */
    val Filled: FontFamily = FontFamily(Font(R.font.material_symbols_rounded_filled))
}
