package com.cycling.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.M3Theme

enum class M3ContainerStyle {
    Elevated,
    Filled,
    Outlined,
    Tonal
}

@Composable
fun M3Container(
    modifier: Modifier = Modifier,
    style: M3ContainerStyle = M3ContainerStyle.Filled,
    shape: Shape = M3Shapes.cornerMedium,
    contentPadding: Dp = M3Spacing.medium,
    title: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    content: @Composable ColumnScope.() -> Unit
) {
    val (containerColor, contentColor, border) = when (style) {
        M3ContainerStyle.Elevated -> {
            Triple(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.onSurface,
                null
            )
        }
        M3ContainerStyle.Filled -> {
            Triple(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.onSurface,
                null
            )
        }
        M3ContainerStyle.Outlined -> {
            Triple(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.onSurface,
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            )
        }
        M3ContainerStyle.Tonal -> {
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                null
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        tonalElevation = if (style == M3ContainerStyle.Elevated) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(M3Spacing.small)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = contentColor
                )
            }
            content()
        }
    }
}

@Composable
fun M3GroupContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = M3Shapes.cornerMedium,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun M3InfoContainer(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    style: M3ContainerStyle = M3ContainerStyle.Tonal
) {
    M3Container(
        modifier = modifier.fillMaxWidth(),
        style = style,
        title = title
    ) {
        if (icon != null) {
            icon()
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = when (style) {
                M3ContainerStyle.Tonal -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun M3SectionContainer(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(M3Spacing.small)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            action?.invoke()
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun M3ContainerPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3Container(
                title = "Elevated Container",
                style = M3ContainerStyle.Elevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("This is an elevated container with shadow effect.")
            }

            M3Container(
                title = "Filled Container",
                style = M3ContainerStyle.Filled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("This is a filled container with surface color.")
            }

            M3Container(
                title = "Outlined Container",
                style = M3ContainerStyle.Outlined,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("This is an outlined container with border.")
            }

            M3Container(
                title = "Tonal Container",
                style = M3ContainerStyle.Tonal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("This is a tonal container with variant color.")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3SectionContainerPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3SectionContainer(
                title = "Recently Played",
                modifier = Modifier.fillMaxWidth(),
                action = {
                    androidx.compose.material3.TextButton(onClick = {}) {
                        Text("See All")
                    }
                }
            ) {
                Text("Your recently played tracks will appear here.")
            }
        }
    }
}
