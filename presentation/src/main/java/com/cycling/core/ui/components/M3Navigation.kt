package com.cycling.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 统一的返回导航按钮，所有 TopAppBar 组件复用此实现。
 * 样式匹配参考项目：Icon + clickable（无 ripple），无 IconButton 的 48dp 强制触摸区域。
 */
@Composable
private fun M3TopAppBarNavigationIcon(
    navigationIcon: ImageVector?,
    navigationIconContentDescription: String?,
    onNavigationClick: (() -> Unit)?
) {
    if (navigationIcon != null && onNavigationClick != null) {
        val contentColor = LocalContentColor.current
        val interactionSource = remember { MutableInteractionSource() }
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onNavigationClick
            ),
            imageVector = navigationIcon,
            contentDescription = navigationIconContentDescription,
            tint = contentColor
        )
    }
}

/**
 * 标准 TopAppBar，适用于普通页面（如设置、文件夹列表）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            M3TopAppBarNavigationIcon(
                navigationIcon = navigationIcon,
                navigationIconContentDescription = navigationIconContentDescription,
                onNavigationClick = onNavigationClick
            )
        },
        actions = actions,
        colors = colors
    )
}

/**
 * 大标题 TopAppBar，带滚动折叠行为，适用于列表页（如歌手列表、专辑列表）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3LargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            M3TopAppBarNavigationIcon(
                navigationIcon = navigationIcon,
                navigationIconContentDescription = navigationIconContentDescription,
                onNavigationClick = onNavigationClick
            )
        },
        actions = actions,
        colors = colors
    )
}

/**
 * 居中标题 TopAppBar，适用于详情页（如歌手详情、专辑详情），
 * 常配合透明背景以显示内容渐变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DetailTopAppBar(
    title: String = "",
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            M3TopAppBarNavigationIcon(
                navigationIcon = navigationIcon,
                navigationIconContentDescription = navigationIconContentDescription,
                onNavigationClick = onNavigationClick
            )
        },
        actions = actions,
        colors = colors
    )
}
