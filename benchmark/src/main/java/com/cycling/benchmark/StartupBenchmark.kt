package com.cycling.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷启动耗时（`STARTUP` / `timeToInitialDisplay` 一类系统侧指标）。
 *
 * 两条测试量的是同一条启动路径的两种编译状态，结果要放在一起看：
 * - [coldStartup] 用基准库默认的 `CompilationMode`（先预热再落 speed-profile），
 *   接近「装上后用了一阵子」的真实情况；
 * - [coldStartupFullyCompiled] 用 `CompilationMode.Full`，量的是全部 AOT 之后的上限。
 *   两条差得越远，说明启动慢越是因为「没编译到」，而不是代码路径本身长。
 *
 * `setupBlock` 在杀进程**之前**执行，所以在那里授权是安全的；权限本身跟着包走，
 * 五轮迭代只需要授权一次。
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = STARTUP_ITERATIONS,
        startupMode = StartupMode.COLD,
        setupBlock = { grantMediaReadPermission() },
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun coldStartupFullyCompiled() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Full(),
        iterations = STARTUP_ITERATIONS,
        startupMode = StartupMode.COLD,
        setupBlock = { grantMediaReadPermission() },
    ) {
        pressHome()
        startActivityAndWait()
    }
}
