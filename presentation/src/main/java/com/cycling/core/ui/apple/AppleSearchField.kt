package com.cycling.core.ui.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName

/**
 * Apple 搜索框：`search_view_background_height` = 48dp 的圆角填充块。
 *
 * 圆角取 `corner_radius_l`(10dp)，底色取 `tertiary_fill_color`，文字用 Body 17sp，
 * 占位符压到 secondaryLabel。不用 Material 的 TextField —— 它的描边与浮动标签
 * 在 Apple 版式里没有对应物。
 */
@Composable
fun AppleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onSearch: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    val colors = AppleTheme.colors
    val shape = RoundedCornerShape(AppleDims.cornerL)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppleDims.searchFieldHeight)
            .clip(shape)
            .background(colors.fill)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3SymbolIcon(
            name = SymbolIconName.Search,
            contentDescription = null,
            filled = false,
            tint = colors.secondaryLabel,
            size = 18.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(AppleType.body).copy(color = colors.label),
                cursorBrush = SolidColor(colors.key),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = AppleType.body,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .applePress(onClear),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.Close,
                    contentDescription = null,
                    filled = true,
                    tint = colors.tertiaryLabel,
                    size = 16.dp
                )
            }
        }
    }
}
