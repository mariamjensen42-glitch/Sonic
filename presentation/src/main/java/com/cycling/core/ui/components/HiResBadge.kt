package com.cycling.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.HiResBadge

/** 小金标的高度基准：与图标档位同源，避免各处自己写 dp。 */
private val BadgeHeight = SonSizes.iconMedium

/** 自定义图片的占位宽高比。固定框 + Fit，任何比例的图都不会被拉伸。 */
private const val CustomBadgeAspect = 2.4f

/**
 * 封面左下角的小金标。
 *
 * 只是一个贴在封面上的静态标记，不参与布局计算，也不拦截点击 —— 调用方把它放进
 * 封面的 Box 里对齐到 `BottomStart` 即可。[HiResBadge.OFF] 时什么都不画。
 */
@Composable
fun HiResBadgeOverlay(
    badge: HiResBadge,
    customUri: String?,
    modifier: Modifier = Modifier
) {
    if (badge == HiResBadge.OFF) return

    val extended = LocalSonExtendedColors.current
    val plateShape = MaterialTheme.shapes.extraSmall

    when (badge) {
        HiResBadge.OFF -> Unit
        HiResBadge.HI_RES -> BadgePlate(
            title = "Hi-Res",
            caption = "AUDIO",
            brush = Brush.linearGradient(
                listOf(extended.hiResBadgeGold, extended.hiResBadgeGoldDeep)
            ),
            contentColor = extended.onHiResBadge,
            shape = plateShape,
            modifier = modifier
        )
        HiResBadge.HI_RES_WIRELESS -> BadgePlate(
            title = "Hi-Res",
            caption = "WIRELESS",
            brush = Brush.linearGradient(
                listOf(extended.hiResBadgeSilver, extended.hiResBadgeSilverDeep)
            ),
            contentColor = extended.onHiResBadge,
            shape = plateShape,
            modifier = modifier
        )
        HiResBadge.GOLD_SEAL -> Box(
            modifier = modifier
                .size(BadgeHeight)
                .background(
                    brush = Brush.linearGradient(
                        listOf(extended.hiResBadgeGold, extended.hiResBadgeGoldDeep)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HR",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = extended.onHiResBadge
            )
        }
        HiResBadge.CUSTOM -> if (customUri != null) {
            AsyncImage(
                model = customUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .height(BadgeHeight)
                    .width(BadgeHeight * CustomBadgeAspect)
            )
        }
    }
}

/** 带渐变底与两段文字的标牌：主标（Hi-Res）+ 副标（AUDIO / WIRELESS）。 */
@Composable
private fun BadgePlate(
    title: String,
    caption: String,
    brush: Brush,
    contentColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(BadgeHeight)
            .background(brush = brush, shape = shape)
            .padding(horizontal = SonSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = contentColor
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
