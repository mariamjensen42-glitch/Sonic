package com.cycling.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class BottomNavTab(
    val screen: NavKey,
    val label: String,
    @DrawableRes val icon: Int
)

class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}

/**
 * Spotify-style bottom navigation bar.
 * The [modifier] (e.g. `Modifier.align(Alignment.BottomCenter)`)
 * is applied to the whole animated container so it pins to the
 * parent's bottom edge.
 */
@Composable
fun SpotifyBottomNavBar(
    currentTab: NavKey,
    onTabSelected: (NavKey) -> Unit,
    tabs: List<BottomNavTab>,
    visible: Boolean = true,
    miniPlayerVisible: Boolean = false,
    miniPlayerContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (miniPlayerVisible) 130.dp else 80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0B0B0F)),
                        startY = 0f
                    )
                )
        ) {
            Column {
                AnimatedVisibility(
                    visible = miniPlayerVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    miniPlayerContent()
                }

                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.screen == currentTab
                        NavigationBarItem(
                            selected = isSelected,
                            icon = {
                                Icon(
                                    painter = painterResource(id = tab.icon),
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = { onTabSelected(tab.screen) },
                            alwaysShowLabel = true,
                            interactionSource = remember { NoRippleInteractionSource() },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}
