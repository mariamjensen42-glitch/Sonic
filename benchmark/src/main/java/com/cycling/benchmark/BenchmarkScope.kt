package com.cycling.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** `:app` 的 applicationId。 */
const val PACKAGE_NAME: String = "com.cycling.sonic"

/**
 * 首页顶栏「设置」按钮的 contentDescription，值来自 `:presentation` 的 `home_settings_desc`。
 * UiAutomator 只能通过无障碍节点定位 Compose 组件，改了那句文案这里也要跟着改。
 */
private const val SETTINGS_DESCRIPTION: String = "设置"

/** 等待界面元素出现的上限。真机冷启动的首帧偶尔要几秒。 */
const val UI_TIMEOUT_MS: Long = 10_000

/** 启动基准的重复次数；基准库自己也建议至少 5 轮。 */
const val STARTUP_ITERATIONS: Int = 5

/** 滚动基准的重复次数：偶数轮往下、奇数轮往上，正好在列表顶/底之间来回。 */
const val SCROLL_ITERATIONS: Int = 6

/** 单轮测量里连甩几次。 */
const val SCROLL_FLINGS: Int = 3

/**
 * 授予读取本地音频的运行时权限。
 *
 * app 在第一帧之后立刻申请媒体权限（`MainActivity` 里的 `LaunchedEffect`），不授权的话每轮
 * 启动都会盖一层系统对话框：首页根本没机会出现，启动耗时和帧时间都被对话框的动画污染。
 * `pm grant` 以 shell 身份执行，绕开界面直接授权；Android 13（API 33）起是
 * `READ_MEDIA_AUDIO`，之前是 `READ_EXTERNAL_STORAGE`，两条都发一遍 —— 不适用于当前系统的
 * 那条只会返回一行错误文本，不会抛异常。
 */
fun MacrobenchmarkScope.grantMediaReadPermission() {
    device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.READ_MEDIA_AUDIO")
    device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.READ_EXTERNAL_STORAGE")
}

/**
 * 如果当前停在首页，就点顶栏的「设置」进设置页。
 *
 * 只在首页找得到那个按钮；已经在设置页（或首页还没渲染出来）时什么都不做。用非阻塞的
 * `findObject` 而不是 `wait`，是为了避免每轮都白等一个超时。
 */
fun MacrobenchmarkScope.enterSettingsIfOnHome() {
    val settingsEntry = device.findObject(By.desc(SETTINGS_DESCRIPTION)) ?: return
    settingsEntry.click()
    device.waitForIdle()
}

/**
 * 设置主页面最外层的滚动容器。
 *
 * 设置页是一个 `Column(Modifier.verticalScroll(...))`，滚动它的成本只跟 Compose 的布局、
 * 绘制和 Material 组件本身有关，不依赖设备上有没有音乐 —— 这是这个 app 里唯一一处
 * 不依赖用户媒体库的长列表，所以帧时间基准放在这里。
 */
fun MacrobenchmarkScope.settingsScrollable(): UiObject2 {
    if (!device.wait(Until.hasObject(By.scrollable(true)), UI_TIMEOUT_MS)) {
        error("设置页没有可滚动容器，界面结构可能已经变了")
    }
    return checkNotNull(device.findObject(By.scrollable(true))) {
        "设置页没有可滚动容器，界面结构可能已经变了"
    }
}
