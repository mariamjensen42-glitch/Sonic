package com.cycling.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3Alpha
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.M3Theme

@Composable
fun M3ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) M3Motion.buttonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && onClick != null) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .background(backgroundColor)
            .padding(horizontal = M3Spacing.medium, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.invoke()
        if (leadingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            overlineContent?.invoke()
            headlineContent()
            supportingContent?.invoke()
        }
        trailingContent?.invoke()
    }
}

@Composable
fun M3ListItemOneLine(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailingContent: @Composable (() -> Unit)? = null
) {
    M3ListItem(
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = modifier.height(M3ComponentSize.listItemOneLineHeight),
        leadingContent = leadingIcon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(M3ComponentSize.listItemAvatarSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(leadingIconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(M3ComponentSize.listItemIconSize)
                    )
                }
            }
        },
        trailingContent = trailingContent,
        onClick = onClick
    )
}

@Composable
fun M3ListItemTwoLine(
    headlineText: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailingContent: @Composable (() -> Unit)? = null
) {
    M3ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.height(M3ComponentSize.listItemTwoLineHeight),
        leadingContent = leadingIcon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(M3ComponentSize.listItemAvatarSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(leadingIconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(M3ComponentSize.listItemIconSize)
                    )
                }
            }
        },
        trailingContent = trailingContent,
        onClick = onClick
    )
}

@Composable
fun M3ListItemWithAvatar(
    headlineText: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarContent: @Composable () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    M3ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.height(M3ComponentSize.listItemTwoLineHeight),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(M3ComponentSize.listItemAvatarSize)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                avatarContent()
            }
        },
        trailingContent = trailingContent,
        onClick = onClick
    )
}

@Composable
fun M3ListItemWithSwitch(
    headlineText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    M3ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier,
        leadingContent = leadingIcon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(M3ComponentSize.listItemAvatarSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(leadingIconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(M3ComponentSize.listItemIconSize)
                    )
                }
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
fun M3ListItemWithCheckbox(
    headlineText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    M3ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier,
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Preview(showBackground = true)
@Composable
private fun M3ListItemPreview() {
    M3Theme {
        Column {
            M3ListItemOneLine(
                text = "Single Line Item",
                leadingIcon = Icons.Default.MusicNote,
                onClick = {}
            )
            M3ListItemTwoLine(
                headlineText = "Two Line Item",
                supportingText = "Supporting text",
                leadingIcon = Icons.Default.Album,
                onClick = {}
            )
            var switchChecked by remember { mutableStateOf(true) }
            M3ListItemWithSwitch(
                headlineText = "Switch Item",
                checked = switchChecked,
                onCheckedChange = { switchChecked = it },
                leadingIcon = Icons.Default.Person
            )
        }
    }
}

enum class M3TrailingContentType {
    None,
    Chevron,
    Icon,
    Text,
    Badge
}

@Composable
fun M3TrailingIcon(
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    if (onClick != null) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun M3TrailingChevron() {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun M3TrailingText(
    text: String,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun M3TrailingBadge(
    count: Int
) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun M3ListItemWithTrailing(
    headlineText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailingType: M3TrailingContentType = M3TrailingContentType.None,
    trailingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingBadgeCount: Int = 0
) {
    val trailingContent: @Composable (() -> Unit)? = when (trailingType) {
        M3TrailingContentType.None -> null
        M3TrailingContentType.Chevron -> { { M3TrailingChevron() } }
        M3TrailingContentType.Icon -> trailingIcon?.let { { M3TrailingIcon(it) } }
        M3TrailingContentType.Text -> trailingText?.let { { M3TrailingText(it) } }
        M3TrailingContentType.Badge -> { { M3TrailingBadge(trailingBadgeCount) } }
    }

    M3ListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.height(
            if (supportingText != null) M3ComponentSize.listItemTwoLineHeight
            else M3ComponentSize.listItemOneLineHeight
        ),
        leadingContent = leadingIcon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(M3ComponentSize.listItemAvatarSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(leadingIconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(M3ComponentSize.listItemIconSize)
                    )
                }
            }
        },
        trailingContent = trailingContent,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun M3ListItemWithTrailingPreview() {
    M3Theme {
        Column {
            M3ListItemWithTrailing(
                headlineText = "With Chevron",
                supportingText = "Supporting text",
                leadingIcon = Icons.Default.MusicNote,
                trailingType = M3TrailingContentType.Chevron,
                onClick = {}
            )
            M3ListItemWithTrailing(
                headlineText = "With Badge",
                leadingIcon = Icons.Default.Album,
                trailingType = M3TrailingContentType.Badge,
                trailingBadgeCount = 12,
                onClick = {}
            )
            M3ListItemWithTrailing(
                headlineText = "With Text",
                supportingText = "Supporting text",
                leadingIcon = Icons.Default.Person,
                trailingType = M3TrailingContentType.Text,
                trailingText = "3:45",
                onClick = {}
            )
        }
    }
}
