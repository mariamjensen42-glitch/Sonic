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

/** 载入态：整屏居中一个 Expressive 的 [LoadingIndicator]，不另画骨架。 */
@Composable
fun SonLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}
