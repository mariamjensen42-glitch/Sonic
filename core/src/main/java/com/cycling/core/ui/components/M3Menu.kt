package com.cycling.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Theme

enum class M3MenuStyle {
    Standard,
    Vibrant
}

data class M3MenuItem(
    val icon: ImageVector? = null,
    val label: String,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val hasSubmenu: Boolean = false,
    val onClick: () -> Unit = {}
)

@Composable
fun M3VerticalMenu(
    items: List<M3MenuItem>,
    modifier: Modifier = Modifier,
    style: M3MenuStyle = M3MenuStyle.Standard,
    withGap: Boolean = false,
    gapIndices: List<Int> = emptyList(),
    expanded: Boolean = true
) {
    val containerColor = when (style) {
        M3MenuStyle.Standard -> MaterialTheme.colorScheme.surfaceContainer
        M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.tertiaryContainer
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) +
                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
        exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) +
                fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
    ) {
        Surface(
            modifier = modifier,
            shape = M3Shapes.cornerLarge,
            color = containerColor,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                items.forEachIndexed { index, item ->
                    if (withGap && gapIndices.contains(index) && index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    M3MenuItemContent(
                        item = item,
                        style = style,
                        showDivider = withGap && gapIndices.contains(index) && index < items.lastIndex
                    )

                    if (!withGap && index < items.lastIndex && !gapIndices.contains(index + 1)) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun M3MenuItemContent(
    item: M3MenuItem,
    style: M3MenuStyle,
    showDivider: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scale"
    )

    val contentColor = when {
        !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        style == M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val selectedBackgroundColor = when (style) {
        M3MenuStyle.Standard -> MaterialTheme.colorScheme.secondaryContainer
        M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (item.selected) {
                    Modifier.background(selectedBackgroundColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.enabled
            ) { item.onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        if (item.selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(20.dp),
                tint = when (style) {
                    M3MenuStyle.Standard -> MaterialTheme.colorScheme.primary
                    M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }

        if (item.hasSubmenu) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Submenu",
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
        }
    }

    if (showDivider) {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun M3DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<M3MenuItem>,
    modifier: Modifier = Modifier,
    style: M3MenuStyle = M3MenuStyle.Standard
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = M3Shapes.cornerLarge,
        containerColor = when (style) {
            M3MenuStyle.Standard -> MaterialTheme.colorScheme.surfaceContainer
            M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.tertiaryContainer
        }
    ) {
        items.forEach { item ->
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        text = item.label,
                        color = if (item.enabled) {
                            when (style) {
                                M3MenuStyle.Standard -> MaterialTheme.colorScheme.onSurface
                                M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                },
                onClick = {
                    item.onClick()
                    onDismissRequest()
                },
                enabled = item.enabled,
                leadingIcon = item.icon?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (item.enabled) {
                                when (style) {
                                    M3MenuStyle.Standard -> MaterialTheme.colorScheme.onSurfaceVariant
                                    M3MenuStyle.Vibrant -> MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                },
                trailingIcon = if (item.selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun M3ContextMenu(
    items: List<M3MenuItem>,
    modifier: Modifier = Modifier,
    style: M3MenuStyle = M3MenuStyle.Standard,
    withGap: Boolean = false,
    gapIndices: List<Int> = emptyList()
) {
    M3VerticalMenu(
        items = items,
        modifier = modifier,
        style = style,
        withGap = withGap,
        gapIndices = gapIndices
    )
}

@Preview(showBackground = true)
@Composable
private fun M3MenuPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Standard Menu", style = MaterialTheme.typography.titleMedium)
            M3VerticalMenu(
                items = listOf(
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Edit",
                        onClick = {}
                    ),
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Duplicate",
                        onClick = {}
                    ),
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Delete",
                        enabled = false,
                        onClick = {}
                    )
                ),
                style = M3MenuStyle.Standard,
                modifier = Modifier.width(200.dp)
            )

            Text("Vibrant Menu", style = MaterialTheme.typography.titleMedium)
            M3VerticalMenu(
                items = listOf(
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Share",
                        selected = true,
                        onClick = {}
                    ),
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Export",
                        onClick = {}
                    ),
                    M3MenuItem(
                        icon = Icons.Default.Check,
                        label = "Archive",
                        onClick = {}
                    )
                ),
                style = M3MenuStyle.Vibrant,
                modifier = Modifier.width(200.dp)
            )

            Text("Menu with Gap", style = MaterialTheme.typography.titleMedium)
            M3VerticalMenu(
                items = listOf(
                    M3MenuItem(label = "Copy", onClick = {}),
                    M3MenuItem(label = "Cut", onClick = {}),
                    M3MenuItem(label = "Paste", onClick = {}),
                    M3MenuItem(label = "Select All", onClick = {}),
                    M3MenuItem(label = "Delete", onClick = {})
                ),
                style = M3MenuStyle.Standard,
                withGap = true,
                gapIndices = listOf(3),
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3DropdownMenuPreview() {
    var expanded by remember { mutableStateOf(true) }
    M3Theme {
        Box(modifier = Modifier.padding(16.dp)) {
            M3DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                items = listOf(
                    M3MenuItem(icon = Icons.Default.Check, label = "Edit", onClick = {}),
                    M3MenuItem(icon = Icons.Default.Check, label = "Delete", onClick = {}),
                    M3MenuItem(icon = Icons.Default.Check, label = "Share", selected = true, onClick = {})
                ),
                style = M3MenuStyle.Standard
            )
        }
    }
}
