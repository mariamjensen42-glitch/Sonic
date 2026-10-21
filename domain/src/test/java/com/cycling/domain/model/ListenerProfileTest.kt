package com.cycling.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 乐迷等级的档位换算。
 *
 * 等级是"我的乐迷档案"里唯一的成长线，门槛写死在 [ListenerProfile.LEVEL_THRESHOLDS_MS]，
 * 这里把起步、跨档、满级三种情况固定下来，改动门槛时能立刻看到影响。
 */
class ListenerProfileTest {

    @Test
    fun `no listening is level one`() {
        assertEquals(1, ListenerProfile(totalPlayMs = 0L).level)
    }

    @Test
    fun `just under an hour is still level one`() {
        val profile = ListenerProfile(totalPlayMs = HOUR_MS - 1)
        assertEquals(1, profile.level)
        assertEquals(HOUR_MS, profile.nextLevelPlayMs)
    }

    @Test
    fun `reaching the first threshold is level two`() {
        val profile = ListenerProfile(totalPlayMs = HOUR_MS)
        assertEquals(2, profile.level)
        assertEquals(5 * HOUR_MS, profile.nextLevelPlayMs)
    }

    @Test
    fun `level progress is the fraction inside the current tier`() {
        // 1h 到 5h 之间：2h 大约在 25% 的位置。
        val profile = ListenerProfile(totalPlayMs = 2 * HOUR_MS)
        assertTrue(profile.levelProgress > 0.2f && profile.levelProgress < 0.3f)
    }

    @Test
    fun `top tier has no next level`() {
        val threshold = ListenerProfile.LEVEL_THRESHOLDS_MS
        val profile = ListenerProfile(totalPlayMs = threshold.last())
        assertNull(profile.nextLevelPlayMs)
        assertEquals(threshold.size + 1, profile.level)
        assertEquals(1f, profile.levelProgress)
    }

    private companion object {
        const val HOUR_MS = 60L * 60 * 1000
    }
}
