package com.cycling.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 两个主题层 morph 的纯插值：播放键按压 0/1/松手，hero 折叠 0/0.5/1。
 * 页面只消费结果，不自己写形状或弹簧。
 */
class SonShapeMorphTest {

    @Test
    fun `press starts at the extra large shape`() {
        assertEquals(SonShapes.extraLarge, SonShapeMorph.pressShape(0f))
        assertEquals(28f, SonShapeMorph.pressCorner(0f).value, 0f)
    }

    @Test
    fun `press ends as a circle`() {
        assertEquals(CircleShape, SonShapeMorph.pressShape(1f))
        assertEquals(48f, SonShapeMorph.pressCorner(1f).value, 0f)
    }

    @Test
    fun `release returns to the extra large shape`() {
        // 松手/取消时进度回到 0，形状回到 idle。
        assertEquals(SonShapes.extraLarge, SonShapeMorph.pressShape(0f))
        assertEquals(SonShapeMorph.pressShape(0f), SonShapeMorph.pressShape(0f))
    }

    @Test
    fun `press interpolates the corner at half`() {
        assertEquals(38f, SonShapeMorph.pressCorner(0.5f).value, 0.001f)
        assertEquals(RoundedCornerShape(38.dp), SonShapeMorph.pressShape(0.5f))
    }

    @Test
    fun `hero fold interpolates size corner and position`() {
        val start = SonShapeMorph.heroFold(
            progress = 0f,
            startSize = 360.dp,
            endSize = 32.dp,
            endX = 64.dp,
            endY = 16.dp
        )
        assertEquals(360f, start.size.value, 0f)
        assertEquals(0f, start.corner.value, 0f)
        assertEquals(0f, start.x.value, 0f)
        assertEquals(0f, start.y.value, 0f)

        val middle = SonShapeMorph.heroFold(
            progress = 0.5f,
            startSize = 360.dp,
            endSize = 32.dp,
            endX = 64.dp,
            endY = 16.dp
        )
        assertEquals(196f, middle.size.value, 0.001f)
        assertEquals(4f, middle.corner.value, 0.001f)
        assertEquals(32f, middle.x.value, 0.001f)
        assertEquals(8f, middle.y.value, 0.001f)

        val end = SonShapeMorph.heroFold(
            progress = 1f,
            startSize = 360.dp,
            endSize = 32.dp,
            endX = 64.dp,
            endY = 16.dp
        )
        assertEquals(32f, end.size.value, 0f)
        assertEquals(8f, end.corner.value, 0f)
        assertEquals(64f, end.x.value, 0f)
        assertEquals(16f, end.y.value, 0f)
    }

    @Test
    fun `hero fold shape goes from rectangle to small`() {
        assertEquals(RectangleShape, SonShapeMorph.heroFoldShape(0f))
        assertEquals(RoundedCornerShape(4.dp), SonShapeMorph.heroFoldShape(0.5f))
        assertEquals(SonShapes.small, SonShapeMorph.heroFoldShape(1f))
    }
}
