package com.cycling.data.cache

import com.cycling.domain.model.CacheUsage
import com.cycling.domain.model.DiskCache
import com.cycling.domain.repository.CacheRepository
import com.cycling.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 上限的执行粒度是整目录，所以最要紧的一条是"够了就停"：
 * 600 MB 超 512 MB 时只该清掉最大的那个目录，不该把两个都清空。
 */
class CacheJanitorTest {

    private val cacheRepository = mockk<CacheRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private lateinit var janitor: CacheJanitor

    private val mb = 1024L * 1024

    @Before
    fun setup() {
        janitor = CacheJanitor(cacheRepository, settingsRepository)
    }

    @Test
    fun `clears only as much as needed`() = runTest {
        every { settingsRepository.autoCleanCacheEnabled } returns flowOf(true)
        every { settingsRepository.cacheLimitBytes } returns flowOf(512 * mb)
        coEvery { cacheRepository.usage() } returns CacheUsage(
            diskCaches = listOf(DiskCache("image_cache", 300 * mb), DiskCache("http_cache", 300 * mb))
        )
        coEvery { cacheRepository.clearDiskCache(any()) } returns 300 * mb

        val freed = janitor.cleanIfNeeded()

        assertEquals(300 * mb, freed)
        coVerify(exactly = 0) { cacheRepository.clearDiskCache("http_cache") }
    }

    @Test
    fun `leaves everything alone when under the limit`() = runTest {
        every { settingsRepository.autoCleanCacheEnabled } returns flowOf(true)
        every { settingsRepository.cacheLimitBytes } returns flowOf(512 * mb)
        coEvery { cacheRepository.usage() } returns CacheUsage(
            diskCaches = listOf(DiskCache("image_cache", 100 * mb))
        )

        assertEquals(0L, janitor.cleanIfNeeded())
        coVerify(exactly = 0) { cacheRepository.clearDiskCache(any()) }
    }

    @Test
    fun `does nothing when auto clean is off`() = runTest {
        every { settingsRepository.autoCleanCacheEnabled } returns flowOf(false)

        assertEquals(0L, janitor.cleanIfNeeded())
        coVerify(exactly = 0) { cacheRepository.clearDiskCache(any()) }
    }
}
