package com.cycling.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 设置页滚动时的帧时间（`frameDurationCpuMs` / `frameOverrunMs`）。
 *
 * 为什么滚设置页而不是「歌曲列表」：这个 app 的曲库来自 MediaStore，没有注入的样本数据，
 * 空库里没有可滚动的列表；设置页是唯一一条不依赖用户媒体库的长列表，而且它就是普通的
 * Material 组件树（`ListItem` + `Switch` + `Button`），改动后最容易在这里看到回归。
 *
 * 不在测量窗口里冷启动：设置入口第一次从首页点进去，之后 app 一直停在设置页，五轮以上
 * 的迭代只量滚动本身。列表在顶/底之间来回甩（偶数轮往下、奇数轮往上），既不会甩到底
 * 之后空转，也不需要额外花时间滚回顶部。
 */
@RunWith(AndroidJUnit4::class)
class SettingsScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollSettings() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = SCROLL_ITERATIONS,
        setupBlock = { grantMediaReadPermission() },
    ) {
        startActivityAndWait()
        enterSettingsIfOnHome()

        val list = settingsScrollable()
        // 甩动起点离屏幕边缘留出五分之一，避免手势被系统返回/侧滑吃掉。
        list.setGestureMargin(device.displayWidth / 5)

        val direction = if ((iteration ?: 0) % 2 == 0) Direction.DOWN else Direction.UP
        repeat(SCROLL_FLINGS) { list.fling(direction) }
        device.waitForIdle()
    }
}
