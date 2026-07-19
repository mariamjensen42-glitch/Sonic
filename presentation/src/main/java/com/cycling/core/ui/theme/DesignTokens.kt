package com.cycling.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object M3Spacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val extraExtraLarge = 48.dp
    val extraExtraExtraLarge = 64.dp
}

object M3Shapes {
    val cornerSmall = RoundedCornerShape(8.dp)
    val cornerMedium = RoundedCornerShape(12.dp)
    val cornerLarge = RoundedCornerShape(16.dp)
    val cornerExtraLarge = RoundedCornerShape(28.dp)
    val cornerFull = RoundedCornerShape(50)
}

object M3Elevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

object M3ComponentSize {
    val buttonMinHeight = 40.dp
    val buttonIconSize = 18.dp
    val fabStandardSize = 56.dp
    val fabSmallSize = 48.dp
    val fabLargeSize = 96.dp
    val fabIconSize = 24.dp
    
    val listItemOneLineHeight = 56.dp
    val listItemTwoLineHeight = 72.dp
    val listItemThreeLineHeight = 88.dp
    val listItemIconSize = 24.dp
    val listItemAvatarSize = 40.dp
    
    val topAppBarHeight = 64.dp
    val largeTopAppBarHeight = 152.dp
    val navigationBarHeight = 80.dp
    val navigationBarItemIconSize = 24.dp
    
    val miniPlayerHeight = 64.dp
    val miniPlayerArtworkSize = 48.dp
    val miniPlayerButtonSize = 40.dp
    
    val mediaCardWidth = 160.dp
    val mediaCardAspectRatio = 1f
    val artistCardSize = 100.dp
}

object M3Motion {
    val defaultSpringSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val expressiveSpringSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 400f
    )
    
    val bouncySpringSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 200f
    )
    
    val buttonPressScale = 0.95f
    val cardHoverScale = 1.02f
    
    const val durationShort = 150
    const val durationMedium = 300
    const val durationLong = 500
}

object M3Alpha {
    const val disabled = 0.38f
    const val mediumEmphasis = 0.6f
    const val highEmphasis = 0.87f
    const val pressed = 0.12f
    const val focus = 0.12f
    const val hover = 0.08f
    const val drag = 0.16f
}

object M3StateLayers {
    const val hover = 0.08f
    const val focus = 0.12f
    const val pressed = 0.12f
    const val drag = 0.16f
}

object M3Easing {
    const val standard = "cubic-bezier(0.2, 0.0, 0.0, 1.0)"
    const val emphasized = "cubic-bezier(0.2, 0.0, 0.0, 1.0)"
    const val emphasizedDecelerate = "cubic-bezier(0.05, 0.7, 0.1, 1.0)"
    const val emphasizedAccelerate = "cubic-bezier(0.3, 0.0, 0.8, 0.15)"
    const val standardDecelerate = "cubic-bezier(0.0, 0.0, 0.0, 1.0)"
    const val standardAccelerate = "cubic-bezier(0.3, 0.0, 1.0, 1.0)"
}

object M3Duration {
    const val short = 150
    const val medium = 300
    const val long = 500
    const val extraLong = 700
}


