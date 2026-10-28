package com.cycling.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.SonSizes

/**
 * Flamingo 图标的统一入口，资源是 `res/drawable/ic_flamingo_*.xml`。
 *
 * 这套图来自 Flamingo 1.0.0（2608310056）的播放页与歌词页：`pathData` 原样搬运，只把
 * viewport 收到图形的实际包围盒（源文件里 Repeat 只占画布 46%、Play 满格，不裁的话同一 dp
 * 下会一大一小），所以每个图标在自己的尺寸里都铺满，视觉重量一致。
 *
 * 颜色一律交给 [tint]：XML 里的 `fillColor` 只是占位，`#FF000000` 与源文件的白色没有区别 ——
 * `Icon` 用 `SrcIn` 混合，任何不透明源色都会被替换成 tint。
 *
 * 与 [SonGlyph]（Apple Music 造型）并存：两套互不影响，页面按需挑一套，别在一屏里混搭。
 */
@Composable
fun FlamingoIcon(
    @DrawableRes res: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = SonSizes.iconMedium
) {
    Icon(
        painter = painterResource(res),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}
