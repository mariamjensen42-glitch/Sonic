package com.cycling.sonic.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.cycling.sonic.core.ui.theme.SpotifyColors

data class BottomNavTab(
    val screen: NavKey,
    val label: String,
    val icon: @Composable () -> Unit
)

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
                            icon = tab.icon,
                            label = {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { onTabSelected(tab.screen) },
                            alwaysShowLabel = true,
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
