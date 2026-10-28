package com.cycling.presentation.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 收起几何的纯函数。这几条决定了「往下拖时页面到底动没动」，肉眼验收不可靠。
 */
class PlayerMorphTest {

    private val height = 2400f

    @Test
    fun `fully expanded has no transform`() {
        val shell = PlayerMorph.shell(1f, height)
        assertEquals(1f, shell.scale, 0f)
        assertEquals(0f, shell.translationY, 0f)
        assertEquals(1f, shell.alpha, 0f)
        assertEquals(0f, PlayerMorph.cornerFactor(1f), 0f)
    }

    @Test
    fun `fully collapsed is small sunk and gone`() {
        val shell = PlayerMorph.shell(0f, height)
        assertEquals(PlayerMorph.END_SCALE, shell.scale, 0.001f)
        assertTrue(shell.translationY > 0f)
        assertEquals(0f, shell.alpha, 0f)
        assertEquals(1f, PlayerMorph.cornerFactor(0f), 0f)
    }

    /**
     * 最重要的一条：**顶边位移和手指 1:1**。
     *
     * 手指走完 `DISMISS_DRAG_RANGE` 个屏高就收到底，而顶边的位移 = 缩放抬起的部分 + 下沉，
     * 两者之和必须正好等于手指走过的距离，否则会出现"手在走、页面慢慢跟"的脱节感。
     */
    @Test
    fun `top edge follows the finger one to one`() {
        val range = PlayerSceneSpec.DISMISS_DRAG_RANGE
        for (step in 1..10) {
            val t = step / 10f
            val shell = PlayerMorph.shell(1f - t, height)
            val top = height / 2f * (1f - shell.scale) + shell.translationY
            assertEquals(t * range * height, top, 0.5f)
        }
    }

    @Test
    fun `page stays opaque for most of the drag`() {
        // 只在最后 16% 才淡出：跟手的那一段必须是实心的，否则整段拖动只剩一个"变淡"。
        for (step in 17..100) {
            assertEquals(1f, PlayerMorph.shell(step / 100f, height).alpha, 0f)
        }
        assertTrue(PlayerMorph.shell(0.09f, height).alpha < 0.55f)
    }

    @Test
    fun `scale and alpha never go backwards`() {
        var lastScale = PlayerMorph.shell(0f, height).scale
        var lastAlpha = PlayerMorph.shell(0f, height).alpha
        for (step in 1..100) {
            val shell = PlayerMorph.shell(step / 100f, height)
            assertTrue(shell.scale >= lastScale)
            assertTrue(shell.alpha >= lastAlpha)
            lastScale = shell.scale
            lastAlpha = shell.alpha
        }
    }

    @Test
    fun `artwork docks into the mini player only at the end`() {
        assertEquals(1f, PlayerMorph.dive(1f), 0f)
        assertEquals(0f, PlayerMorph.dive(0f), 0f)
        assertTrue(PlayerMorph.dive(0.8f) > 0.95f)
        assertTrue(PlayerMorph.dive(0.3f) < 0.5f)
    }

    @Test
    fun `corner grows as soon as the page leaves the screen edge`() {
        assertEquals(0f, PlayerMorph.cornerFactor(1f), 0f)
        assertEquals(1f, PlayerMorph.cornerFactor(0.72f), 0f)
    }

    @Test
    fun `shell bounds agree with the transform`() {
        val width = 1080f
        val shell = PlayerMorph.shell(0.5f, height)
        val bounds = PlayerMorph.shellBounds(0.5f, width, height)
        assertEquals(width / 2f * (1f - shell.scale), bounds.left, 0.5f)
        assertEquals(height / 2f * (1f - shell.scale) + shell.translationY, bounds.top, 0.5f)
        assertEquals(bounds.width, width * shell.scale, 0.5f)
        assertEquals(bounds.height, height * shell.scale, 0.5f)
    }
}
