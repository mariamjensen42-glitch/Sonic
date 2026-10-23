package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName

/*
 * Apple 面板 / 弹窗 / 胶囊控件。替代 Material 的 ModalBottomSheet、AlertDialog、
 * FilterChip、Button 与 OutlinedTextField —— 这几类组件的几何（圆角、内边距、
 * 描边、浮动标签）在 iOS 版式里没有对应物。
 */

/** 品牌红上的文字恒为白色，不随日夜主题变化。 */
private val OnBrandLabel = Color(0xFFFFFFFF)

/** iOS 面板圆角，取 corner_radius_xl。 */
private val SheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

/** 面板顶部的抓手：36×5dp 的次级标签色横条。 */
@Composable
private fun AppleSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 5.dp)
                .background(AppleTheme.colors.tertiaryLabel, CircleShape)
        )
    }
}

/**
 * Apple 式底部面板：底色取 `background_color`，圆角 14dp，抓手用次级标签色。
 *
 * [content] 直接就是列内容，底部已处理系统导航栏 inset 与 16dp 余量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppleTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        contentColor = colors.label,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = SheetShape,
        dragHandle = { AppleSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            content = content
        )
    }
}

/** 面板标题：22sp 粗体 + 13sp 副标题。 */
@Composable
fun AppleSheetTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colors = AppleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Text(text = title, style = AppleType.title2Emphasized, color = colors.label)
        if (subtitle != null) {
            Text(text = subtitle, style = AppleType.footnote, color = colors.secondaryLabel)
        }
    }
}

/**
 * 面板里的可点行：17sp 标题 + 13sp 副标题 + 可选行尾。
 *
 * 与 [AppleMenuRow] 的区别是没有图标、行高更矮 —— 面板是"选一项"的场景，
 * 不是资料库的入口列表。
 */
@Composable
fun AppleSheetRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleType.body,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppleType.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/** 胶囊筛选块。选中态用 label 反底，未选中是三级填充。 */
@Composable
fun AppleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppleTheme.colors
    val background = when {
        !enabled -> colors.fill.copy(alpha = 0.5f)
        selected -> colors.label
        else -> colors.fill
    }
    val foreground = when {
        !enabled -> colors.tertiaryLabel
        selected -> colors.background
        else -> colors.label
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppleType.subhead,
            color = foreground,
            maxLines = 1
        )
    }
}

/** 胶囊动作按钮：实心为品牌红，否则为三级填充。高度取详情页动作条的 40dp。 */@Composable
fun AppleActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    icon: SymbolIconName? = null
) {
    val colors = AppleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val foreground = if (filled) OnBrandLabel else colors.label
    Row(
        modifier = modifier
            .height(AppleDims.actionHeight)
            .clip(CircleShape)
            .background(if (filled) colors.systemRed else colors.fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            M3SymbolIcon(
                name = icon,
                contentDescription = null,
                filled = true,
                tint = if (enabled) foreground else colors.tertiaryLabel,
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = AppleType.subheadEmphasized,
            color = if (enabled) foreground else colors.tertiaryLabel,
            maxLines = 1
        )
    }
}

/** 圆角填充输入框，替代 OutlinedTextField（iOS 没有描边 + 浮动标签这套）。 */
@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 36.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = AppleTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(AppleDims.cornerS))
            .background(colors.fill)
            .padding(horizontal = 10.dp, vertical = if (singleLine) 0.dp else 8.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = LocalTextStyle.current.merge(AppleType.body).copy(color = colors.label),
            cursorBrush = SolidColor(colors.key),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = AppleType.body,
                color = colors.tertiaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 行内文字动作：17sp + 可选前置图标，默认 key 色（破坏性动作传 systemRed）。 */
@Composable
fun AppleTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: SymbolIconName? = null,
    tint: Color = AppleTheme.colors.key
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            M3SymbolIcon(
                name = icon,
                contentDescription = null,
                filled = true,
                tint = tint,
                size = 18.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = label, style = AppleType.body, color = tint, maxLines = 1)
    }
}

/**
 * iOS 式居中弹窗：270dp 宽、14dp 圆角、按钮之间用发丝线分隔。
 *
 * 与 AlertDialog 的区别就在这几处 —— Material 的版本是宽幅、圆角 28dp、按钮右对齐。
 */
@Composable
fun AppleAlert(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .width(270.dp)
                .clip(RoundedCornerShape(AppleDims.cornerXl))
                .background(colors.tertiaryBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppleType.headline,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)
            )
            if (message != null) {
                Text(
                    text = message,
                    style = AppleType.footnote,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
                )
            }
            if (content != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    content()
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
            AppleSeparator(leadingInset = 0.dp)
            Row(modifier = Modifier.fillMaxWidth()) {
                if (dismissLabel != null) {
                    AppleAlertButton(
                        label = dismissLabel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(colors.separator)
                    )
                }
                AppleAlertButton(
                    label = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    destructive = destructive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppleAlertButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
    destructive: Boolean = false
) {
    val colors = AppleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(44.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (emphasized) AppleType.headline else AppleType.body,
            color = when {
                !enabled -> colors.tertiaryLabel
                destructive -> colors.systemRed
                else -> colors.key
            },
            maxLines = 1
        )
    }
}
