package com.cycling.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
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
    val waveformActive: Color,
    /**
     * 小金标的金色渐变（起 → 止）。
     *
     * 压在封面图上，两种主题下都必须是这一种金色 —— 它表达的是「这枚标志长什么样」，
     * 不是页面的语义层级，所以不参与主题推导。
     */
    val hiResBadgeGold: Color,
    val hiResBadgeGoldDeep: Color,
    /** 无线小金标的银色渐变（起 → 止）。 */
    val hiResBadgeSilver: Color,
    val hiResBadgeSilverDeep: Color,
    /** 小金标之上的文字色。 */
    val onHiResBadge: Color
)

internal val SonDarkExtendedColors = SonExtendedColors(
    artworkPlaceholder = Color(0xFF2B2930),
    onArtworkPlaceholder = Color(0xFF938F99),
    onArtwork = Color(0xFFFFFFFF),
    artworkScrim = Color(0xFF141218),
    lyricsActive = Color(0xFFFFFFFF),
    lyricsInactive = Color(0xFF938F99),
    waveform = Color(0xFF49454F),
    waveformActive = Color(0xFFD0BCFF),
    hiResBadgeGold = Color(0xFFFFD75E),
    hiResBadgeGoldDeep = Color(0xFFE3A008),
    hiResBadgeSilver = Color(0xFFF1F3F5),
    hiResBadgeSilverDeep = Color(0xFFB4BCC6),
    onHiResBadge = Color(0xFF241A00)
)

internal val SonLightExtendedColors = SonExtendedColors(
    artworkPlaceholder = Color(0xFFE6E0E9),
    onArtworkPlaceholder = Color(0xFF79747E),
    onArtwork = Color(0xFFFFFFFF),
    artworkScrim = Color(0xFF1D1B20).copy(alpha = 0.78f),
    lyricsActive = Color(0xFF1D1B20),
    lyricsInactive = Color(0xFF79747E),
    waveform = Color(0xFFCAC4D0),
    waveformActive = Color(0xFF6750A4),
    hiResBadgeGold = Color(0xFFFFD75E),
    hiResBadgeGoldDeep = Color(0xFFE3A008),
    hiResBadgeSilver = Color(0xFFF1F3F5),
    hiResBadgeSilverDeep = Color(0xFFB4BCC6),
    onHiResBadge = Color(0xFF241A00)
)

val LocalSonExtendedColors = staticCompositionLocalOf { SonDarkExtendedColors }

/**
 * 封面派生的动态色面颜色（1~3 个封面上真实存在的颜色）。
 *
 * 与 [SonExtendedColors] 的区别：那个是按明暗固定的语义色，这个是**每首歌**变的，
 * 由 `rememberArtworkBackdrop` 从封面位图取色、`SonicTheme` 注入。空表示当前没有封面
 * （或封面接近灰阶），动态色面回退到配色方案里的 container 角色。
 *
 * 它**只**影响播放页 / 歌词页的动态色面；应用主题的配色跟随系统壁纸，不从这里来。
 * 颜色只能从这个 local 或 `MaterialTheme.colorScheme` 来 —— 页面不自己取封面、不自己拼色。
 */
val LocalSonBackdropColors = compositionLocalOf { emptyList<Color>() }

/**
 * 封面的 4×4 采样（行优先，16 个颜色），给动态色面当控制点用。
 *
 * 与 [LocalSonBackdropColors] 的分工：那个给的是「封面上有哪几种颜色」，色面据此挑色相；
 * 这个给的是「封面每一格长什么样」，色面因此能带上封面的明暗与构图 —— 也就是把封面糊开
 * 的那种观感。空表示没有封面或采样失败，色面自动退回前者。
 */
val LocalSonBackdropArtwork = compositionLocalOf { emptyList<Color>() }
