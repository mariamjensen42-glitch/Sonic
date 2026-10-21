@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import kotlinx.coroutines.delay

/**
 * 分区标题：Expressive 的“字阶跳级”落在这一行。
 *
 * 标题用 `titleLargeEmphasized`（22sp 的强调变体）而不是 body 放大，右侧的“查看全部”是真正的
 * 按钮（保留点击态层），副标题只在需要解释分组时出现。这是 Material 没有提供的组合
 * （标题 + 可选副标题 + 可选动作），所以留在 `presentation/components`，不是组件转发壳。
 */
@Composable
fun SonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SonLayout.screenMargin,
                end = SonLayout.screenMargin,
                top = SonLayout.cardGap,
                bottom = SonLayout.sectionTitleGap
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

/**
 * 空态模板：图标托在 `surfaceContainerHigh` 的圆形容器里（安静面里唯一的一点形），
 * 标题走 `headlineSmallEmphasized`，正文是 `bodyMedium`；动作最多一个主按钮。
 *
 * 列表族与内容族共用这一份，避免每屏各写一套字号与间距。
 */
@Composable
fun SonEmptyState(
    icon: SymbolIconName,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(SonSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(SonSizes.thumbnailLarge + SonSpacing.large)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = icon,
                    filled = false,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.iconLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            when {
                action != null -> {
                    Spacer(modifier = Modifier.height(SonSpacing.large))
                    action()
                }
                actionLabel != null && onAction != null -> {
                    Spacer(modifier = Modifier.height(SonSpacing.large))
                    Button(onClick = onAction) {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}

/**
 * 「载入态该不该现身」：真正等久了才返回 true。
 *
 * 本地数据（Room 的 `Flow`、DataStore、MediaStore 查询 + 内存缓存）几乎瞬间到达，从 ViewModel
 * 到这一帧最多差一次调度。不延迟的话，每进一次这类页面都会闪一帧转圈 —— 用户看到的是「卡了一下」
 * 而不是「在加载」。240ms 落在「察觉不到」与「需要反馈」之间的常规区间里。
 *
 * 给需要自己搭载入 UI 的页面用（转圈 + 一段说明文字、行内小转圈等）；
 * 只要整屏一个转圈时直接用 [SonLoadingState]。
 */
@Composable
fun rememberLoadingIndicatorVisible(delayMillis: Long = LoadingIndicatorDelayMillis): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    return visible
}

/**
 * 载入态：整屏居中一个 Expressive 的 [LoadingIndicator]，不另画骨架。
 *
 * 延迟 [delayMillis] 才现身，因为本地数据几乎没有等待时间：歌曲、歌单是 Room 的 `Flow`，
 * 专辑、歌手是 MediaStore 查询加一层内存缓存，数据从 ViewModel 到这一帧最多差一次调度。
 * 不延迟的话，每进一次这类页面都会闪一帧转圈——看着像卡顿，而不是像加载。
 *
 * 比阈值快的情况什么也不画（背景就是空的，和这一帧到下一帧的内容切换难以分辨）；
 * 真慢的情况（首次 MediaStore 全量查询、SAF 枚举）仍然按时出现。
 */
@Composable
fun SonLoadingState(
    modifier: Modifier = Modifier,
    delayMillis: Long = LoadingIndicatorDelayMillis
) {
    // 提前返回而不是画一个透明盒子：布局节点本就是空的，不必占位。
    if (!rememberLoadingIndicatorVisible(delayMillis)) return
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

/**
 * 转圈出现的阈值。低于它视为"瞬间完成"，不显示任何载入态。
 *
 * 200–300 ms 是这类阈值的常规区间：短于它用户察觉不到等待，长于它才需要反馈。
 */
private const val LoadingIndicatorDelayMillis = 240L
