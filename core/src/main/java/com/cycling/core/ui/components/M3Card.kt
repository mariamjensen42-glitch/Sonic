package com.cycling.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cycling.core.ui.theme.M3ComponentSize
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.M3Theme

@Composable
fun M3ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        content()
    }
}

@Composable
fun M3FilledCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        content()
    }
}

@Composable
fun M3OutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = M3Shapes.cornerMedium,
        interactionSource = interactionSource
    ) {
        content()
    }
}

@Composable
fun M3MediaCard(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = M3ComponentSize.mediaCardWidth,
    placeholderIcon: ImageVector = Icons.Outlined.MusicNote,
    shape: Shape = M3Shapes.cornerMedium
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

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(M3ComponentSize.mediaCardAspectRatio)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(M3Spacing.small))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class M3AvatarShape {
    Circle,
    RoundedSquare,
    Pill
}

@Composable
fun M3ArtistCard(
    name: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = M3ComponentSize.artistCardSize,
    avatarShape: M3AvatarShape = M3AvatarShape.Circle
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

    val shapeModifier = when (avatarShape) {
        M3AvatarShape.Circle -> Modifier.clip(CircleShape)
        M3AvatarShape.RoundedSquare -> Modifier.clip(M3Shapes.cornerMedium)
        M3AvatarShape.Pill -> Modifier.clip(RoundedCornerShape(50))
    }

    Column(
        modifier = modifier
            .width(size)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(shapeModifier)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        Spacer(modifier = Modifier.height(M3Spacing.small))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun M3PlaylistCard(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = M3ComponentSize.mediaCardWidth
) {
    M3MediaCard(
        title = title,
        subtitle = subtitle,
        artwork = artwork,
        onClick = onClick,
        modifier = modifier,
        width = width,
        placeholderIcon = Icons.Outlined.QueueMusic
    )
}

@Preview(showBackground = true)
@Composable
private fun M3CardPreview() {
    M3Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3ElevatedCard(onClick = {}) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Elevated Card")
                }
            }
            M3FilledCard(onClick = {}) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Filled Card")
                }
            }
            M3OutlinedCard(onClick = {}) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Outlined Card")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3MediaCardPreview() {
    M3Theme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            M3MediaCard(
                title = "Album Title",
                subtitle = "Artist Name",
                artwork = null,
                onClick = {}
            )
            M3ArtistCard(
                name = "Artist",
                artwork = null,
                onClick = {}
            )
        }
    }
}
