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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Sonic 的唯一主题入口，完全基于 Material 3 Expressive。
 *
 * 配色来源按优先级解析：
 * 1. [colorScheme] —— 调用方显式给出的方案（**特例**，正常页面不要传）
 * 2. 系统壁纸动态取色（Android 12+，[dynamicColor] 默认开启）
 * 3. [SonicSeed] 品牌种子色（低版本或动态取色不可用）
 *
 * 三条路径最终都收敛到同一套色调映射（[SonTonalScheme]），因此任意来源下
 * 对比度、色阶关系与色相协调度都保持一致。
 *
 * 封面**不参与**配色：它只通过 [backdropColors] 影响播放页 / 歌词页的动态色面。
 * 早先按封面种子色重建整套方案的做法已移除 —— 换歌时全应用重新着色既没有意义，
 * 也要为每次切歌白跑一遍上千次的 CAM16 迭代。
 *
 * 注意：由种子色生成方案要做上千次颜色空间迭代，调用方应放在后台线程完成
 * （[SonBrandSchemes] 已做进程级缓存与预热），不要把裸种子色直接丢进来。
 */
@Composable
fun SonicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorScheme: ColorScheme? = null,
    backdropColors: List<Color> = emptyList(),
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
        LocalSonExtendedColors provides if (darkTheme) SonDarkExtendedColors else SonLightExtendedColors,
        LocalSonBackdropColors provides backdropColors
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

    else -> if (darkTheme) SonBrandSchemes.dark() else SonBrandSchemes.light()
}

/**
 * 品牌种子色展开的日 / 夜两套方案，进程级缓存。
 *
 * 展开一次要做上千次 CAM16 迭代（见 [Hct]），所以 [sonColorSchemeOf] 的注释写明
 * 「请勿在主线程调用」。但冷启动首帧必然走到这条路径：Android 12 以下的机器、
 * 或系统没提供动态取色时，[SonicTheme] 只能回落到品牌色 —— 于是这条"别在主线程调用"
 * 的路径恰恰每次冷启动都在主线程上跑一遍。
 *
 * 这里把它拆成两步：
 * - [prewarm]：`Application.onCreate` 里在后台线程先把两套都算好（约在首帧之前几十毫秒）；
 * - [light] / [dark]：首帧取用。预热没赶上就当场算一次并缓存 —— 行为与改造前一致，
 *   不会更慢，只是不再每次冷启动都重复付这笔钱。
 */
object SonBrandSchemes {

    @Volatile
    private var cachedLight: ColorScheme? = null

    @Volatile
    private var cachedDark: ColorScheme? = null

    fun light(): ColorScheme =
        cachedLight ?: sonColorSchemeOf(SonicSeed, darkTheme = false).also { cachedLight = it }

    fun dark(): ColorScheme =
        cachedDark ?: sonColorSchemeOf(SonicSeed, darkTheme = true).also { cachedDark = it }

    /** 幂等：把两套方案算在后台线程上，供首帧直接命中。 */
    fun prewarm() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching {
                light()
                dark()
            }.onFailure { Timber.w(it, "SonBrandSchemes: prewarm failed") }
        }
    }
}

/** 由任意种子色直接得到一个配色方案。生成开销较大，请勿在主线程调用。 */
fun sonColorSchemeOf(seed: Color, darkTheme: Boolean): ColorScheme =
    SonTonalScheme.from(seed, SonSchemeStyle.Expressive)
        .let { if (darkTheme) it.toDarkColorScheme() else it.toLightColorScheme() }
