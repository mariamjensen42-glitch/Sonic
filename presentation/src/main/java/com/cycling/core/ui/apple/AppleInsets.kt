package com.cycling.core.ui.apple

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 悬浮胶囊的高度（与 AppleBottomNavigationBar 的 CapsuleHeight 一致）。 */
private val CapsuleHeight = 64.dp

/** 胶囊上下的浮动留白。 */
private val CapsuleGap = 8.dp

/**
 * 列表底部要让出的高度。
 *
 * 底部导航改成**悬浮胶囊**之后不再占布局高度，页面必须自己把这段留白补进 contentPadding：
 * 胶囊本体 + 上下浮动 + 系统导航栏 inset。少了最后一项，三键导航的机器上最后一行会被压住。
 *
 * 用法：`contentPadding = PaddingValues(bottom = appleBottomBarClearance() + extra)`
 */
@Composable
fun appleBottomBarClearance(extra: Dp = 0.dp): Dp =
    CapsuleHeight + CapsuleGap * 2 +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        extra

/**
 * 只有系统栏、没有底栏的页面（全屏播放、歌词）用这个：只要导航栏 inset。
 */
@Composable
fun appleNavigationBarInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
