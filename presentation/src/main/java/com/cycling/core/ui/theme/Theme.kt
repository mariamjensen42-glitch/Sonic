package com.cycling.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sonic 的唯一主题入口，完全基于 Material 3 Expressive。
 *
 * 配色来源按优先级解析：
 * 1. [colorScheme] —— 调用方显式给出的方案（如按封面种子色生成，见 `rememberArtworkTheme`）
 * 2. 系统壁纸动态取色（Android 12+，仅当 [dynamicColor] 为 true）
 * 3. [SonicSeed] 品牌种子色
 *
 * 三条路径最终都收敛到同一套色调映射（[SonTonalScheme]），因此任意来源下
 * 对比度、色阶关系与色相协调度都保持一致。
 *
 * 注意：由种子色生成方案要做上千次颜色空间迭代，调用方应放在后台线程完成
 * （`rememberArtworkTheme` 已经这么做了），不要把裸种子色直接丢进来。
 */
@Composable
fun SonicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resolved: ColorScheme = remember(darkTheme, dynamicColor, colorScheme) {
        colorScheme ?: resolveColorScheme(
            context = context,
            darkTheme = darkTheme,
            dynamicColor = dynamicColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSonExtendedColors provides if (darkTheme) SonDarkExtendedColors else SonLightExtendedColors
    ) {
        MaterialExpressiveTheme(
            colorScheme = resolved,
            motionScheme = MotionScheme.expressive(),
            shapes = SonShapes,
            typography = SonTypography,
            content = content
        )
    }
}

/**
 * 解析最终配色方案。动态取色仅对系统壁纸路径有版本要求（Android 12+），
 * 显式方案与品牌色路径在任何版本上行为一致。
 */
private fun resolveColorScheme(
    context: android.content.Context,
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    else -> sonColorSchemeOf(SonicSeed, darkTheme)
}

/** 由任意种子色直接得到一个配色方案。生成开销较大，请勿在主线程调用。 */
fun sonColorSchemeOf(seed: Color, darkTheme: Boolean): ColorScheme =
    SonTonalScheme.from(seed, SonSchemeStyle.Expressive)
        .let { if (darkTheme) it.toDarkColorScheme() else it.toLightColorScheme() }
