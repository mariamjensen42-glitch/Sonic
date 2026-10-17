package com.cycling.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sonic 品牌种子色。所有静态配色（无动态取色、无封面取色）都由它派生，
 * 因此主题里不存在"手写死"的调色板 —— 只有这一颗种子。
 */
val SonicSeed: Color = Color(0xFF6C4DFF)

/**
 * Material 3 色彩角色之外的补充语义色。
 * 只收那些官方角色确实无法表达、且业务反复需要的东西；
 * 其余一律走 [androidx.compose.material3.MaterialTheme.colorScheme]。
 */
@Immutable
data class SonExtendedColors(
    /** 封面缺失时的占位底色。 */
    val artworkPlaceholder: Color,
    /** 占位底色之上的图标/文字色。 */
    val onArtworkPlaceholder: Color,
    /** 封面之上的内容色（标题、控制按钮）。仅用于真正压在封面图上的内容。 */
    val onArtwork: Color,
    /**
     * 封面底部叠色。作用是在封面图上压出一条足够暗的底，保证 [onArtwork]（白字）
     * 在任意封面图上都可读 —— 因此**两种主题下都必须是深色**，不能跟随页面明暗。
     */
    val artworkScrim: Color,
    /** 歌词当前行高亮。 */
    val lyricsActive: Color,
    /** 歌词非当前行。 */
    val lyricsInactive: Color,
    /** 波形/振幅图形。 */
    val waveform: Color,
    /** 波形已播放部分。 */
    val waveformActive: Color
)

internal val SonDarkExtendedColors = SonExtendedColors(
    artworkPlaceholder = Color(0xFF2B2930),
    onArtworkPlaceholder = Color(0xFF938F99),
    onArtwork = Color(0xFFFFFFFF),
    artworkScrim = Color(0xFF141218),
    lyricsActive = Color(0xFFFFFFFF),
    lyricsInactive = Color(0xFF938F99),
    waveform = Color(0xFF49454F),
    waveformActive = Color(0xFFD0BCFF)
)

internal val SonLightExtendedColors = SonExtendedColors(
    artworkPlaceholder = Color(0xFFE6E0E9),
    onArtworkPlaceholder = Color(0xFF79747E),
    onArtwork = Color(0xFFFFFFFF),
    artworkScrim = Color(0xFF1D1B20).copy(alpha = 0.78f),
    lyricsActive = Color(0xFF1D1B20),
    lyricsInactive = Color(0xFF79747E),
    waveform = Color(0xFFCAC4D0),
    waveformActive = Color(0xFF6750A4)
)

val LocalSonExtendedColors = staticCompositionLocalOf { SonDarkExtendedColors }
