package com.cycling.presentation.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.cycling.core.ui.theme.SonExtendedColors

/**
 * 歌词页的整套文字色。
 *
 * **规则只有一条：一页之上所有歌词文字都由 [textColor] 派生，不许直接取主题 token。**
 *
 * 曾经的做法是正文取 [SonExtendedColors.lyricsActive]、译文取 `lyricsActive/lyricsInactive`、
 * 非当前行取 `lyricsInactive` —— 这三个 token 会跟随系统主题，而歌词页压在一块**动态色面**
 * （播放页那页恒为深底）之上。于是浅色主题下同一页里出现近黑字与白字并存，
 * 用户感知成「跟这首歌的背景色有关」。
 *
 * 现在色阶只看一个数：`textColor` 的 alpha。
 *
 * | 位置 | alpha |
 * |---|---|
 * | 当前行正文 | 1.0 |
 * | 非当前行整行 | [inactiveLineAlpha]（由 `LyricsLineItem` 的图层统一施加） |
 * | 伴奏行整行 | [inactiveLineAlpha] × [accompanimentAlphaRatio] |
 * | 译文（本行未开始） | [TranslationInactiveAlpha] |
 * | 译文（本行已开始） | [TranslationActiveAlpha] |
 * | 「歌词不可用」占位 | [hintAlpha] |
 *
 * 注意「非当前行」不在颜色上压 alpha —— 整行已经由 `LyricsLineItem` 的图层压过一次，
 * 再压一次就是双重变暗。所以非当前行用的就是 [textColor] 本身，只是整行透明度更低。
 */
@Immutable
data class LyricsPalette(
    /** 当前行正文色。整页所有文字色都由它派生。 */
    val textColor: Color,
    /** 非当前行整行的图层不透明度。 */
    val inactiveLineAlpha: Float,
    /** 伴奏行在此基础上的倍数（字号另有比例，见 `AccompanimentSizeRatio`）。 */
    val accompanimentAlphaRatio: Float,
    val translationInactiveAlpha: Float,
    val translationActiveAlpha: Float,
    val hintAlpha: Float,
) {
    val hintColor: Color get() = textColor.copy(alpha = hintAlpha)

    companion object {
        /** 译文在本行未开始时的不透明度：比正文暗，但仍在深色底上可读。 */
        const val TranslationInactiveAlpha = 0.35f

        /** 译文在本行已开始后的不透明度。 */
        const val TranslationActiveAlpha = 0.85f

        /** 「歌词不可用」这类提示文字的不透明度。 */
        const val HintAlpha = 0.6f

        /** 伴奏行相对非当前行的倍数。 */
        const val AccompanimentAlphaRatio = 0.6f

        /** 深色底上非当前行的不透明度。压得比浅色底更低，当前行才真正跳出来。 */
        const val InactiveLineAlphaDark = 0.38f

        /** 浅色底上非当前行的不透明度。 */
        const val InactiveLineAlphaLight = 0.5f

        /**
         * 按页面底色解析出一整套颜色。
         *
         * [onDarkSurface] 为真表示这一页压在深色动态色面（或封面上）：所有文字必须是
         * [SonExtendedColors.onArtwork]（光照在深底上的白），不能跟随系统主题。
         */
        fun of(onDarkSurface: Boolean, extended: SonExtendedColors): LyricsPalette {
            return LyricsPalette(
                textColor = if (onDarkSurface) extended.onArtwork else extended.lyricsActive,
                inactiveLineAlpha = if (onDarkSurface) {
                    InactiveLineAlphaDark
                } else {
                    InactiveLineAlphaLight
                },
                accompanimentAlphaRatio = AccompanimentAlphaRatio,
                translationInactiveAlpha = TranslationInactiveAlpha,
                translationActiveAlpha = TranslationActiveAlpha,
                hintAlpha = HintAlpha,
            )
        }
    }
}

/**
 * 当前歌词页的调色板。
 *
 * 由 `LyricsPage` 在页面根部 provide，逐字歌词、行级歌词、译文、占位文案都从这里取色，
 * 组件自身不再回退到主题 token。默认值给深色底那一套（歌词页最常见的情形），
 * 预览或单测脱离 `LyricsPage` 单独用组件时也不会突然变黑字。
 */
val LocalLyricsPalette = staticCompositionLocalOf {
    LyricsPalette(
        textColor = Color.White,
        inactiveLineAlpha = LyricsPalette.InactiveLineAlphaDark,
        accompanimentAlphaRatio = LyricsPalette.AccompanimentAlphaRatio,
        translationInactiveAlpha = LyricsPalette.TranslationInactiveAlpha,
        translationActiveAlpha = LyricsPalette.TranslationActiveAlpha,
        hintAlpha = LyricsPalette.HintAlpha,
    )
}
