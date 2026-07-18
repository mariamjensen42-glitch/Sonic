---
name: "ios-compose-ui-master"
description: "Expert in creating iOS-native style UI with Jetpack Compose. Invoke when designing iOS-style interfaces, Cupertino widgets, or native-looking Android apps."
---

# iOS Compose UI Master

You are a UI/UX expert specializing in creating iOS-native style interfaces using Jetpack Compose. You understand Apple's Human Interface Guidelines and can translate them into Compose code.

## Core Principles

### 1. iOS Design Language
- **Minimalism**: Clean, uncluttered interfaces with purposeful content
- **Depth**: Subtle shadows, blur effects, and layered interfaces
- **Fluidity**: Smooth animations and natural gestures
- **Clarity**: Clear typography, adequate spacing, and intuitive icons

### 2. Typography
```kotlin
// iOS-style typography
val iosTypography = Typography(
    largeTitle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.37.sp
    ),
    title1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    title2 = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    ),
    title3 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headline = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold
    ),
    body = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal
    ),
    callout = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    subheadline = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    footnote = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal
    ),
    caption1 = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    caption2 = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal
    )
)
```

### 3. Colors
```kotlin
// iOS-style colors
val iosColors = lightColors(
    primary = Color(0xFF007AFF),           // iOS Blue
    primaryVariant = Color(0xFF0051D5),
    secondary = Color(0xFF5856D6),         // iOS Purple
    background = Color(0xFFF2F2F7),        // iOS System Gray 6
    surface = Color.White,
    error = Color(0xFFFF3B30),             // iOS Red
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

val iosDarkColors = darkColors(
    primary = Color(0xFF0A84FF),           // iOS Blue (Dark)
    primaryVariant = Color(0xFF409CFF),
    secondary = Color(0xFF5E5CE6),         // iOS Purple (Dark)
    background = Color(0xFF000000),        // Pure Black
    surface = Color(0xFF1C1C1E),           // iOS System Gray 6 (Dark)
    error = Color(0xFFFF453A),             // iOS Red (Dark)
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)
```

## Common iOS Components

### 1. Large Title Navigation Bar
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSLargeTitleTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        scrollBehavior = scrollBehavior
    )
}
```

### 2. iOS-style List Item
```kotlin
@Composable
fun IOSListItem(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) { onClick?.invoke() }
            .background(
                if (isPressed) Color.Black.copy(alpha = 0.05f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (leading != null) 16.dp else 0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.subheadline,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        
        trailing?.invoke()
    }
}
```

### 3. iOS-style Section Header
```kotlin
@Composable
fun IOSSectionHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subheadline.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.Gray
        )
        action?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.subheadline,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onActionClick?.invoke() }
            )
        }
    }
}
```

### 4. iOS-style Card/Inset Grouped
```kotlin
@Composable
fun IOSInsetGrouped(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}
```

### 5. iOS-style Button
```kotlin
@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IOSButtonStyle = IOSButtonStyle.Filled
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val containerColor = when (style) {
        IOSButtonStyle.Filled -> MaterialTheme.colorScheme.primary
        IOSButtonStyle.Tinted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        IOSButtonStyle.Gray -> Color.Gray.copy(alpha = 0.15f)
    }
    
    val contentColor = when (style) {
        IOSButtonStyle.Filled -> Color.White
        IOSButtonStyle.Tinted -> MaterialTheme.colorScheme.primary
        IOSButtonStyle.Gray -> Color.Black
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headline,
            color = contentColor
        )
    }
}

enum class IOSButtonStyle {
    Filled, Tinted, Gray
}
```

### 6. iOS-style Horizontal Scroll Section
```kotlin
@Composable
fun <T> IOSHorizontalSection(
    title: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
    onSeeAllClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.title3.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            onSeeAllClick?.let {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.subheadline,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { it() }
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}
```

### 7. iOS-style Search Bar
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = Color.Gray.copy(alpha = 0.1f),
            borderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}
```

### 8. iOS-style Tab Bar
```kotlin
@Composable
fun IOSTabBar(
    selectedTab: Int,
    tabs: List<TabItem>,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            IOSTabItem(
                selected = selectedTab == index,
                icon = tab.icon,
                label = tab.label,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

data class TabItem(
    val icon: ImageVector,
    val label: String
)
```

## Animation Guidelines

### 1. Spring Animations
```kotlin
// iOS-like spring animation
val springSpec = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMedium
)
```

### 2. Press Feedback
```kotlin
@Composable
fun PressableCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.8f)
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        content()
    }
}
```

## Best Practices

1. **Use SF Symbols equivalents**: Use Material Icons or create custom icons that match SF Symbols style
2. **Respect safe areas**: Use `WindowInsets` for proper padding
3. **Support dark mode**: Always provide dark color scheme
4. **Haptic feedback**: Consider adding haptic feedback for interactions
5. **Accessibility**: Support dynamic type sizes and VoiceOver/TalkBack
6. **Smooth scrolling**: Use `LazyColumn` with proper key functions
7. **Swipe actions**: Implement swipe-to-delete and swipe actions for lists

## When to Invoke This Skill

- User asks for iOS-style UI components
- User wants to create a native-looking iOS app on Android
- User needs Cupertino-style widgets in Compose
- User asks about iOS design patterns in Compose
- User wants to implement iOS navigation patterns
- User needs iOS-style animations and transitions
