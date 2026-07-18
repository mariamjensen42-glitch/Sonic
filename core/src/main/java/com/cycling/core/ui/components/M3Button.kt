package com.cycling.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Theme

@Composable
fun M3FilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 10.dp
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(M3ComponentSize.buttonIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        content()
    }
}

@Composable
fun M3OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 10.dp
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(M3ComponentSize.buttonIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        content()
    }
}

@Composable
fun M3TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = M3Shapes.cornerMedium
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(M3ComponentSize.buttonIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        content()
    }
}

@Composable
fun M3ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )

    androidx.compose.material3.ElevatedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(M3ComponentSize.buttonIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        content()
    }
}

@Composable
fun M3TonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )

    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(M3ComponentSize.buttonIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        content()
    }
}

@Composable
fun M3ExtendedFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        text = {
            Text(text = text)
        },
        shape = M3Shapes.cornerLarge
    )
}

@Composable
fun M3FAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = M3Shapes.cornerLarge,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 6.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}

@Composable
fun M3SegmentedButtonRow(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
        space = 8.dp
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = M3Shapes.cornerExtraLarge
                ),
                onClick = { onOptionSelected(index) },
                selected = index == selectedIndex,
                label = { Text(text = label) }
            )
        }
    }
}

@Composable
fun M3IconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun M3ButtonPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3FilledButton(onClick = {}) {
                Text("Filled Button")
            }
            M3OutlinedButton(onClick = {}) {
                Text("Outlined Button")
            }
            M3TextButton(onClick = {}) {
                Text("Text Button")
            }
            M3ElevatedButton(onClick = {}) {
                Text("Elevated Button")
            }
            M3TonalButton(onClick = {}) {
                Text("Tonal Button")
            }
            M3ExtendedFAB(
                text = "Play All",
                icon = Icons.Default.PlayArrow,
                onClick = {}
            )
            M3SegmentedButtonRow(
                options = listOf("Songs", "Albums", "Artists"),
                selectedIndex = 0,
                onOptionSelected = {}
            )
        }
    }
}

enum class M3ButtonGroupStyle {
    Connected,
    Separated
}

data class M3ButtonGroupItem(
    val icon: ImageVector? = null,
    val label: String? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)

@Composable
fun M3HorizontalButtonGroup(
    buttons: List<M3ButtonGroupItem>,
    modifier: Modifier = Modifier,
    style: M3ButtonGroupStyle = M3ButtonGroupStyle.Connected,
    selectedIndex: Int? = null,
    onSelectionChange: ((Int) -> Unit)? = null
) {
    when (style) {
        M3ButtonGroupStyle.Connected -> {
            SingleChoiceSegmentedButtonRow(
                modifier = modifier,
                space = 0.dp
            ) {
                buttons.forEachIndexed { index, item ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = buttons.size,
                            baseShape = M3Shapes.cornerExtraLarge
                        ),
                        onClick = {
                            onSelectionChange?.invoke(index)
                            item.onClick?.invoke()
                        },
                        selected = selectedIndex == index,
                        enabled = item.enabled,
                        icon = {
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        label = item.label?.let { { Text(it) } } ?: {}
                    )
                }
            }
        }
        M3ButtonGroupStyle.Separated -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buttons.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        ),
                        label = "scale"
                    )

                    Surface(
                        modifier = Modifier
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = item.enabled
                            ) {
                                onSelectionChange?.invoke(index)
                                item.onClick?.invoke()
                            },
                        shape = M3Shapes.cornerMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) null else {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (item.label != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            item.label?.let { label ->
                                Text(
                                    text = label,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun M3VerticalButtonGroup(
    buttons: List<M3ButtonGroupItem>,
    modifier: Modifier = Modifier,
    style: M3ButtonGroupStyle = M3ButtonGroupStyle.Connected,
    selectedIndex: Int? = null,
    onSelectionChange: ((Int) -> Unit)? = null
) {
    when (style) {
        M3ButtonGroupStyle.Connected -> {
            Surface(
                modifier = modifier,
                shape = M3Shapes.cornerMedium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column {
                    buttons.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 400f
                            ),
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = item.enabled
                                ) {
                                    onSelectionChange?.invoke(index)
                                    item.onClick?.invoke()
                                }
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.icon != null) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (item.label != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                                item.label?.let { label ->
                                    Text(
                                        text = label,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                        if (index < buttons.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
        M3ButtonGroupStyle.Separated -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buttons.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        ),
                        label = "scale"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = item.enabled
                            ) {
                                onSelectionChange?.invoke(index)
                                item.onClick?.invoke()
                            },
                        shape = M3Shapes.cornerMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) null else {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (item.label != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            item.label?.let { label ->
                                Text(
                                    text = label,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class M3SplitButtonItem(
    val icon: ImageVector? = null,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun M3SplitButton(
    text: String,
    onClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    dropdownItems: List<M3SplitButtonItem>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = M3Shapes.cornerMedium
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            val mainButtonInteraction = remember { MutableInteractionSource() }
            val isMainPressed by mainButtonInteraction.collectIsPressedAsState()
            val mainScale by animateFloatAsState(
                targetValue = if (isMainPressed) M3Motion.buttonPressScale else 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "main_scale"
            )

            Row(
                modifier = Modifier
                    .scale(mainScale)
                    .clickable(
                        interactionSource = mainButtonInteraction,
                        indication = null,
                        enabled = enabled
                    ) { onClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
            )

            val dropInteraction = remember { MutableInteractionSource() }
            val isDropPressed by dropInteraction.collectIsPressedAsState()
            val dropScale by animateFloatAsState(
                targetValue = if (isDropPressed) M3Motion.buttonPressScale else 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "drop_scale"
            )

            Box(
                modifier = Modifier
                    .scale(dropScale)
                    .clickable(
                        interactionSource = dropInteraction,
                        indication = null,
                        enabled = enabled
                    ) { onExpandedChange(!expanded) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(if (expanded) -1f else 1f, 1f)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = M3Shapes.cornerMedium,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            dropdownItems.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        item.onClick()
                        onExpandedChange(false)
                    },
                    leadingIcon = item.icon?.let {
                        {
                            Icon(
                                imageVector = it,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}

enum class M3FABMenuDirection {
    Up,
    Left
}

data class M3FABMenuItem(
    val icon: ImageVector,
    val label: String? = null,
    val onClick: () -> Unit
)

@Composable
fun M3FABMenu(
    icon: ImageVector,
    items: List<M3FABMenuItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    direction: M3FABMenuDirection = M3FABMenuDirection.Up,
    showLabels: Boolean = true,
    fabSize: Dp = M3ComponentSize.fabStandardSize
) {
    val fabInteraction = remember { MutableInteractionSource() }
    val isFabPressed by fabInteraction.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        label = "fab_scale"
    )

    val fabRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        label = "fab_rotation"
    )

    Box(modifier = modifier) {
        if (direction == M3FABMenuDirection.Up) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = expanded,
                        enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f)) +
                                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
                        exit = scaleOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) +
                                fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showLabels && item.label != null) {
                                Surface(
                                    shape = M3Shapes.cornerSmall,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = item.label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    item.onClick()
                                    onExpandedChange(false)
                                },
                                shape = M3Shapes.cornerMedium,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.scale(fabScale),
                    shape = M3Shapes.cornerLarge,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    interactionSource = fabInteraction
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (expanded) -1f else 1f, 1f)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = expanded,
                        enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f)) +
                                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
                        exit = scaleOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) +
                                fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showLabels && item.label != null) {
                                Surface(
                                    shape = M3Shapes.cornerSmall,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = item.label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    item.onClick()
                                    onExpandedChange(false)
                                },
                                shape = M3Shapes.cornerMedium,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.scale(fabScale),
                    shape = M3Shapes.cornerLarge,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    interactionSource = fabInteraction
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = M3Shapes.cornerMedium,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scale"
    )

    Surface(
        modifier = modifier.scale(scale),
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 3.dp,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3ButtonGroupPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Connected Style", style = MaterialTheme.typography.titleMedium)
            M3HorizontalButtonGroup(
                buttons = listOf(
                    M3ButtonGroupItem(icon = Icons.Default.Check, label = "Day"),
                    M3ButtonGroupItem(icon = Icons.Default.Check, label = "Week"),
                    M3ButtonGroupItem(icon = Icons.Default.Check, label = "Month")
                ),
                style = M3ButtonGroupStyle.Connected,
                selectedIndex = 0,
                onSelectionChange = {}
            )

            Text("Separated Style", style = MaterialTheme.typography.titleMedium)
            M3HorizontalButtonGroup(
                buttons = listOf(
                    M3ButtonGroupItem(label = "Left"),
                    M3ButtonGroupItem(label = "Center"),
                    M3ButtonGroupItem(label = "Right")
                ),
                style = M3ButtonGroupStyle.Separated,
                selectedIndex = 1,
                onSelectionChange = {}
            )

            Text("Vertical Button Group", style = MaterialTheme.typography.titleMedium)
            M3VerticalButtonGroup(
                buttons = listOf(
                    M3ButtonGroupItem(label = "Option 1"),
                    M3ButtonGroupItem(label = "Option 2"),
                    M3ButtonGroupItem(label = "Option 3")
                ),
                style = M3ButtonGroupStyle.Connected,
                selectedIndex = 0,
                onSelectionChange = {},
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3SplitButtonPreview() {
    M3Theme {
        var expanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3SplitButton(
                text = "Save",
                icon = Icons.Default.Check,
                onClick = {},
                expanded = expanded,
                onExpandedChange = { expanded = it },
                dropdownItems = listOf(
                    M3SplitButtonItem(label = "Save as PDF") {},
                    M3SplitButtonItem(label = "Save as Image") {},
                    M3SplitButtonItem(label = "Save as Text") {}
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3FABMenuPreview() {
    M3Theme {
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            M3FABMenu(
                icon = Icons.Default.PlayArrow,
                items = listOf(
                    M3FABMenuItem(
                        icon = Icons.Default.Check,
                        label = "Play All"
                    ) {},
                    M3FABMenuItem(
                        icon = Icons.Default.Check,
                        label = "Shuffle"
                    ) {},
                    M3FABMenuItem(
                        icon = Icons.Default.Check,
                        label = "Add to Queue"
                    ) {}
                ),
                expanded = expanded,
                onExpandedChange = { expanded = it },
                direction = M3FABMenuDirection.Up,
                showLabels = true
            )
        }
    }
}
