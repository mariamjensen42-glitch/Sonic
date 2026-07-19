package com.cycling.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Spacing

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
    val (interactionSource, scale, _) = rememberPressScale(enabled = onClick != null)

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
