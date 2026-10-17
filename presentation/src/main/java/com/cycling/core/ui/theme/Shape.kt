package com.cycling.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive 形状标尺。
 *
 * 八档圆角，档位与官方 ShapeTokens 完全一致。这是**唯一**的形状来源：
 * 组件用到的形状全部由官方组件默认值从这个标尺取（如按钮取 CircleShape、
 * 卡片取 12dp、媒体卡取 largeIncreased 20dp），因此项目里不存在
 * "某个组件专属形状"的清单 —— 需要形状时用 `MaterialTheme.shapes.*`。
 */
val SonShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
)
