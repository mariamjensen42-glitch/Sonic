package com.cycling.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cycling.presentation.R

/**
 * 字体族（ADR-0002，C 方案；2026-10-22 换字）。
 *
 * 正文与标题统一使用 **MiSans**（小米，官方免费商用），三档静态实例 regular / medium / bold，
 * 已按 GB2312 全量 + Latin-1 + 常用标点做过子集（每档约 1.8 MB）。选它的原因：参考项目
 * （MeloX-Android）的观感来自 MiLanPro / MiSans 这一路的中性黑体，而 Roboto 的字面更窄、
 * 中文全落回设备系统字体，跨设备观感不一致。
 *
 * 子集之外的生僻字（歌名里偶尔会出现）由系统的 CJK fallback 接管，不会缺字。
 *
 * 历史：v1 用 Roboto Flex 静态子集（~120 KB）+ 系统 CJK 回退；换字后这两档不再打包。
 */
object SonFontFamily {

    /** 展示位：display / headline。 */
    val Brand: FontFamily = miSans()

    /** 正文位：title / body / label。 */
    val Plain: FontFamily = miSans()

    private fun miSans(): FontFamily = FontFamily(
        Font(R.font.misans_regular, FontWeight.Normal),
        Font(R.font.misans_medium, FontWeight.Medium),
        Font(R.font.misans_bold, FontWeight.Bold)
    )
}

/**
 * Material 3 Expressive 字阶。
 *
 * 每档都提供标准与"强调"两个变体：强调变体抬高字重（Regular→Medium、Medium→Bold）
 * 并微调字距，用于需要在层级中跳出来的标题、按钮标签与数据强调。
 */
val SonTypography: Typography = Typography(
    // ── Display（品牌字体，最大号） ──────────────────────────────────
    displayLarge = displayToken(57, 64, FontWeight.Normal, -0.2f),
    displayMedium = displayToken(45, 52, FontWeight.Normal, 0.0f),
    displaySmall = displayToken(36, 44, FontWeight.Normal, 0.0f),

    // ── Headline（品牌字体） ────────────────────────────────────────
    headlineLarge = displayToken(32, 40, FontWeight.Normal, 0.0f),
    headlineMedium = displayToken(28, 36, FontWeight.Normal, 0.0f),
    headlineSmall = displayToken(24, 32, FontWeight.Normal, 0.0f),

    // ── Title（Plain 字体） ────────────────────────────────────────
    titleLarge = plainToken(22, 28, FontWeight.Normal, 0.0f),
    titleMedium = plainToken(16, 24, FontWeight.Medium, 0.2f),
    titleSmall = plainToken(14, 20, FontWeight.Medium, 0.1f),

    // ── Body ───────────────────────────────────────────────────────
    bodyLarge = plainToken(16, 24, FontWeight.Normal, 0.5f),
    bodyMedium = plainToken(14, 20, FontWeight.Normal, 0.2f),
    bodySmall = plainToken(12, 16, FontWeight.Normal, 0.4f),

    // ── Label ──────────────────────────────────────────────────────
    labelLarge = plainToken(14, 20, FontWeight.Medium, 0.1f),
    labelMedium = plainToken(12, 16, FontWeight.Medium, 0.5f),
    labelSmall = plainToken(11, 16, FontWeight.Medium, 0.5f),

    // ── Emphasized 变体 ────────────────────────────────────────────
    displayLargeEmphasized = displayToken(57, 64, FontWeight.Medium, 0.0f),
    displayMediumEmphasized = displayToken(45, 52, FontWeight.Medium, 0.0f),
    displaySmallEmphasized = displayToken(36, 44, FontWeight.Medium, 0.0f),

    headlineLargeEmphasized = displayToken(32, 40, FontWeight.Medium, 0.0f),
    headlineMediumEmphasized = displayToken(28, 36, FontWeight.Medium, 0.0f),
    headlineSmallEmphasized = displayToken(24, 32, FontWeight.Medium, 0.0f),

    titleLargeEmphasized = plainToken(22, 28, FontWeight.Medium, 0.0f),
    titleMediumEmphasized = plainToken(16, 24, FontWeight.Bold, 0.15f),
    titleSmallEmphasized = plainToken(14, 20, FontWeight.Bold, 0.1f),

    bodyLargeEmphasized = plainToken(16, 24, FontWeight.Medium, 0.15f),
    bodyMediumEmphasized = plainToken(14, 20, FontWeight.Medium, 0.25f),
    bodySmallEmphasized = plainToken(12, 16, FontWeight.Medium, 0.4f),

    labelLargeEmphasized = plainToken(14, 20, FontWeight.Bold, 0.1f),
    labelMediumEmphasized = plainToken(12, 16, FontWeight.Bold, 0.5f),
    labelSmallEmphasized = plainToken(11, 16, FontWeight.Bold, 0.5f)
)

private fun displayToken(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float
): TextStyle = TextStyle(
    fontFamily = SonFontFamily.Brand,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

private fun plainToken(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Float
): TextStyle = TextStyle(
    fontFamily = SonFontFamily.Plain,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)