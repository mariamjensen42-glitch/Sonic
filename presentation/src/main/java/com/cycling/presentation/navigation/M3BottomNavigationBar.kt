@file:OptIn(ExperimentalMaterial3Api::class)

package com.cycling.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R

/**
 * 一个顶级导航目标。
 * [icon] / [selectedIcon] 保留两个槽位，供未来更换语义；当前两组用同一码位，
 * 选中态由 `M3SymbolIcon(filled = selected)` 切换 FILL 轴 ——
 * 填充/描边的切换本身就是选中状态的第一层视觉信号。
 */
data class BottomNavTab(
    val screen: NavKey,
    val label: String,
    val icon: SymbolIconName,
    val selectedIcon: SymbolIconName
)

/**
 * 两个顶级目标的标签与图标。
 *
 * 搜索不在这里：它是一个从首页顶栏进入的下钻页面，不是平级 tab。
 *
 * 定义放在 presentation：`:app` 只做装配，不引入图标库、也不挑选图标。
 */
@Composable
fun rememberBottomNavTabs(): List<BottomNavTab> {
    val home = stringResource(R.string.nav_home)
    val library = stringResource(R.string.nav_library)
    return remember(home, library) {
        listOf(
            BottomNavTab(Screen.Home, home, SymbolIconName.Home, SymbolIconName.Home),
            BottomNavTab(
                Screen.Library,
                library,
                SymbolIconName.LibraryMusic,
                SymbolIconName.LibraryMusic
            )
        )
    }
}

/**
 * 底部导航栏。
 *
 * 用标准的 [NavigationBar] + [NavigationBarItem]：各目标等宽、图标与文字**始终**可见，
 * 选中态由官方指示器胶囊 + 涟漪表达，容器色与系统栏内边距全部走 Material 默认值。
 *
 * 这里不用 `ShortNavigationBar`：在本版本里它只要 `label != null` 就常显标签，并不是
 * “选中项展开成图标+文字胶囊、其余只留图标”的形态；本应用只有首页/音乐库两个等价
 * 顶级目标，标准 NavigationBar 的等宽、标签常显与稳定位置更符合两级 IA。
 * 决策见 `docs/design/navigation-shell.md`。
 *
 * [modifier]（如 `Modifier.align(Alignment.BottomCenter)`）作用于整个动画容器，
 * 使导航栏固定在父布局底边。
 */
@Composable
fun M3BottomNavigationBar(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean = true,
    miniPlayerVisible: Boolean = false,
    miniPlayerContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(animationSpec = motionScheme.slowSpatialSpec()) { it },
        exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = miniPlayerVisible,
                enter = slideInVertically(animationSpec = motionScheme.defaultSpatialSpec()) { it } +
                    fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                exit = slideOutVertically(animationSpec = motionScheme.fastSpatialSpec()) { it } +
                    fadeOut(animationSpec = motionScheme.fastEffectsSpec())
            ) {
                miniPlayerContent()
            }

            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { tab ->
                    val selected = tab.screen == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(tab.screen) },
                        icon = {
                            M3SymbolIcon(
                                name = if (selected) tab.selectedIcon else tab.icon,
                                filled = selected,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(text = tab.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}
