package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.SymbolIconName

/**
 * 页面顶栏：状态栏间距 + 可选返回键 + 大标题。
 *
 * 直接塞进 `Scaffold(topBar = { ... })`：Scaffold 会用它的实际高度生成 `innerPadding`，
 * 因此页面内容一行都不用改，只换掉 Material 的 `MediumFlexibleTopAppBar` 即可。
 *
 * 与 Material 顶栏的区别：不做折叠（Apple 的 large title 是随内容滚走的，
 * 不是靠顶栏自身收缩），返回键与标题分两行。
 */
@Composable
fun AppleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    /** 返回键字形：多选态常换成关闭图标。 */
    backIcon: SymbolIconName = SymbolIconName.ArrowBack,
    actions: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppleTheme.colors.background)
            .statusBarsPadding()
    ) {
        if (onNavigateBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleIconButton(
                    icon = backIcon,
                    contentDescription = backContentDescription,
                    onClick = onNavigateBack,
                    tint = AppleTheme.colors.key
                )
            }
        }
        AppleLargeTitle(title = title, subtitle = subtitle, actions = actions)
    }
}

/**
 * 紧凑导航栏：44dp 高、返回键在左、标题居中 17sp、动作在右。
 *
 * 用于编辑类与二级页面（歌曲详情、歌词信息）—— 这些页面主体已经是内容本身，
 * 再放一个 34sp 的大标题就是重复。
 */
@Composable
fun AppleCompactBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    actions: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppleTheme.colors.background)
            .statusBarsPadding()
            .height(44.dp)
    ) {
        if (onNavigateBack != null) {
            AppleIconButton(
                icon = SymbolIconName.ArrowBack,
                contentDescription = backContentDescription,
                onClick = onNavigateBack,
                tint = AppleTheme.colors.key,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        Text(
            text = title,
            style = AppleType.headline,
            color = AppleTheme.colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }
    }
}
