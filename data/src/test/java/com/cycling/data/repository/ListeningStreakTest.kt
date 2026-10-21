package com.cycling.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连续听歌天数的边界。
 *
 * 这段逻辑决定"我的乐迷档案"里那个连击数字，用户对它非常敏感（断一天就归零），
 * 所以把容易出错的几种情况固定下来：今天还没听、中断一天、跨月、空数据。
 */
class ListeningStreakTest {

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, listeningStreakDays(emptyList(), todayEpoch = 20_000))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val days = listOf(20_000L, 19_999L, 19_998L, 19_997L)
        assertEquals(4, listeningStreakDays(days, todayEpoch = 20_000))
    }

    @Test
    fun `today may still be missing`() {
        // 昨天听过、今天还没听：连击不该在早上就归零。
        val days = listOf(19_999L, 19_998L, 19_997L)
        assertEquals(3, listeningStreakDays(days, todayEpoch = 20_000))
    }

    @Test
    fun `a gap older than yesterday resets the streak`() {
        val days = listOf(19_998L, 19_997L, 19_996L)
        assertEquals(0, listeningStreakDays(days, todayEpoch = 20_000))
    }

    @Test
    fun `a hole inside the history stops counting`() {
        // 20_000、19_999 连续，但 19_998 缺了，再往前的天数不算。
        val days = listOf(20_000L, 19_999L, 19_997L, 19_996L)
        assertEquals(2, listeningStreakDays(days, todayEpoch = 20_000))
    }

    @Test
    fun `single day today is a one day streak`() {
        assertEquals(1, listeningStreakDays(listOf(20_000L), todayEpoch = 20_000))
    }

    @Test
    fun `single day yesterday is a one day streak`() {
        assertEquals(1, listeningStreakDays(listOf(19_999L), todayEpoch = 20_000))
    }
}
