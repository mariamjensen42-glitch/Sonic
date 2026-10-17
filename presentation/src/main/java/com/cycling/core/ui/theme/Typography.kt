package com.cycling.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cycling.presentation.R

/**
 * 字体族（ADR-0002，C 方案）。
 *
 * Latin 统一使用 Roboto Flex 的静态实例（`scripts/subset-fonts.py`）：regular /
 * medium / bold 三个字重各一份子集，FILL 之外的轴已在构建期钉死。中文/日文/韩文
 * 没有 Flex 字形，由 Android 的系统 fallback 接管 —— 这是决策里明确接受的 CJK 混排
 * 方案，不要为中文伪造粗体。
 *
 * 为什么不用运行时可变轴：Compose 1.13 的 `Font(variationSettings = ...)` 在 API 33
 * 上会静默回退到系统字体（实测图标码位变成缺字方块），静态实例既绕开这条路径，也
 * 把三份字体子集压到 ~120 KB。
 */
object SonFontFamily {

    /** 展示位：display / headline。 */
    val Brand: FontFamily = robotoFlex()

    /** 正文位：title / body / label。 */
    val Plain: FontFamily = robotoFlex()

    private fun robotoFlex(): FontFamily = FontFamily(
        Font(R.font.roboto_flex_regular, FontWeight.Normal),
        Font(R.font.roboto_flex_medium, FontWeight.Medium),
        Font(R.font.roboto_flex_bold, FontWeight.Bold)
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