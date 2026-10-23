@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.presentation.R

/**
 * 一个顶级导航目标。
 */
data class BottomNavTab(
    val screen: NavKey,
    val label: String,
    val icon: SymbolIconName,
    val selectedIcon: SymbolIconName
)

/**
 * 顶级目标的标签与图标。
 *
 * 搜索不在这里：它是右侧那颗独立的圆形按钮，不是胶囊里的分栏。
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

/** 胶囊高度，也是右侧圆形搜索钮的直径。 */
private val CapsuleHeight = 64.dp

/** 悬浮留白：左右各 16dp，与胶囊和圆形钮之间 8dp。 */
private val CapsuleSide = 16.dp
private val CapsuleGap = 8.dp

/**
 * 底部导航：iOS 26 Liquid Glass 的悬浮胶囊。
 *
 * 结构是「一条胶囊 + 一颗独立圆形按钮」，整体浮在内容之上，不占满整行：
 * ```
 *        ┌──────────────────────────┐  ┌───┐
 *        │  ⌂ 首页    ♪ 音乐库       │  │ 🔍 │
 *        └──────────────────────────┘  └───┘
 * ```
 * 胶囊与圆钮都是 64dp 高、全圆角、玻璃底 + 0.5dp 描边；选中项用品牌红，
 * 其余压到三级标签色。迷你播放器在胶囊上方，同样浮着。
 */
@Composable
fun AppleBottomNavigationBar(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean = true,
    miniPlayerVisible: Boolean = false,
    miniPlayerContent: @Composable () -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val motionScheme = MaterialTheme.motionScheme
    val colors = AppleTheme.colors
    val capsuleShape = RoundedCornerShape(percent = 50)

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
                Column {
                    miniPlayerContent()
                    Spacer(modifier = Modifier.height(CapsuleGap))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = CapsuleSide, vertical = CapsuleGap),
                horizontalArrangement = Arrangement.spacedBy(CapsuleGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(CapsuleHeight)
                        .clip(capsuleShape)
                        .background(colors.glass)
                        .border(AppleDims.hairline, colors.materialStroke, capsuleShape),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = tab.screen == currentTab
                        val tint = if (selected) colors.systemRed else colors.secondaryLabel
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(CapsuleHeight)
                                .applePress { onTabSelected(tab.screen) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            M3SymbolIcon(
                                name = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label,
                                filled = selected,
                                tint = tint,
                                size = 22.dp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = AppleType.caption2,
                                color = tint
                            )
                        }
                    }
                }

                if (onSearchClick != null) {
                    Box(
                        modifier = Modifier
                            .size(CapsuleHeight)
                            .clip(CircleShape)
                            .background(colors.glass)
                            .border(AppleDims.hairline, colors.materialStroke, CircleShape)
                            .applePress(onSearchClick),
                        contentAlignment = Alignment.Center
                    ) {
                        M3SymbolIcon(
                            name = SymbolIconName.Search,
                            contentDescription = stringResource(R.string.search_icon_desc),
                            filled = false,
                            tint = colors.label,
                            size = 24.dp
                        )
                    }
                }
            }
        }
    }
}
