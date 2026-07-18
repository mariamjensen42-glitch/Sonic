package com.cycling.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.theme.M3Motion
import com.cycling.core.ui.theme.M3Shapes
import com.cycling.core.ui.theme.M3Spacing
import com.cycling.core.ui.theme.M3Theme

data class M3CarouselItem(
    val id: Any,
    val content: @Composable () -> Unit
)

@Composable
fun <T> M3Carousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 280.dp,
    itemSpacing: Dp = 16.dp,
    showIndicator: Boolean = true,
    itemContent: @Composable (T) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentPage = listState.firstVisibleItemIndex
    }

    Column(modifier = modifier) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.size) { index ->
                val item = items[index]
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) M3Motion.buttonPressScale else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .scale(scale)
                        .clip(M3Shapes.cornerLarge)
                ) {
                    itemContent(item)
                }
            }
        }

        if (showIndicator && items.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            M3CarouselIndicator(
                itemCount = items.size,
                currentPage = currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun M3CarouselIndicator(
    itemCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 8.dp,
    indicatorSpacing: Dp = 8.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(indicatorSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(itemCount) { index ->
            val isActive = index == currentPage
            val size by animateFloatAsState(
                targetValue = if (isActive) 1.5f else 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "indicator_size"
            )

            Box(
                modifier = Modifier
                    .size(indicatorSize * size)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun M3CarouselCard(
    title: String,
    subtitle: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier,
    shape: Shape = M3Shapes.cornerLarge,
    content: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(M3Spacing.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun M3CarouselPreview() {
    M3Theme {
        M3Carousel(
            items = listOf(
                M3CarouselItem(1) {
                    M3CarouselCard(
                        title = "Recently Played",
                        subtitle = "Your recent tracks",
                        modifier = Modifier.height(180.dp)
                    )
                },
                M3CarouselItem(2) {
                    M3CarouselCard(
                        title = "Favorites",
                        subtitle = "Your favorite songs",
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(180.dp)
                    )
                },
                M3CarouselItem(3) {
                    M3CarouselCard(
                        title = "Playlists",
                        subtitle = "Your playlists",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.height(180.dp)
                    )
                }
            ),
            itemContent = { it.content() },
            modifier = Modifier.padding(16.dp)
        )
    }
}
