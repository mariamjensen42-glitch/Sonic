# material3 1.5.0-alpha29 Expressive 组件与 API 盘点

> 关联票：[Sonic#2 盘点 material3 1.5.0-alpha29 的 Expressive 组件与 API](https://github.com/mariamjensen42-glitch/Sonic/issues/2)
>
> **一句话结论**：这个 artifact 里 Expressive 组件**几乎全部已转正**（`ButtonGroup`、`SplitButtonLayout`、
> `FloatingActionButtonMenu`、浮动工具栏、Flexible top/bottom app bar、`ShortNavigationBar`、
> Wavy progress、Carousel、`MotionScheme.expressive()`、`MaterialExpressiveTheme`、Emphasized 字号
> 都不再需要 opt-in）。真正仍被 `@ExperimentalMaterial3ExpressiveApi` 挡住的只有
> **LoadingIndicator 家族、MaterialShapes 形状库、两个 DropdownMenuItem 重载、Slider Thumb、
> PullToRefresh 的 loading 配色**。项目里 13 个文件上的 `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`
> 大部分只是因为 `LoadingIndicator` 才需要的，其中 2 个已经多余。

## 0. 元信息

| 项 | 值 |
| --- | --- |
| 盘点日期 | 2026-10-17 |
| 盘点对象 | `androidx.compose.material3:material3:1.5.0-alpha29`（artifact `material3-android`） |
| Compose BOM | `2026.09.00`（`gradle/libs.versions.toml:10`） |
| material3 显式钉版 | `1.5.0-alpha29`（`gradle/libs.versions.toml:14`，刻意覆盖 BOM 的 1.4.0） |
| 被拖动的 Compose 版本 | `ui` / `ui-text` / `foundation` / `foundation-layout` / `animation-core` 均为 `1.13.0-alpha01` |
| 编译要求 | `minCompileSdk=37`、`minCompileMinorSdk=0`、`minAndroidGradlePluginVersion=9.1.0`（AAR metadata） |
| 方法 | ① 解包本地 Gradle 缓存里的真实 AAR + sources JAR；② `javap -v` 逐类确认 opt-in 注解；③ 与 Google Maven 元数据、androidx-main 的 `current.txt` 交叉核对；④ 官方文档/M3 规范对照 |
| 未做 | 未改任何源码/构建文件，未跑 Gradle，未动 git |

### 置信度图例

| 标记 | 含义 |
| --- | --- |
| ✅ **本地已验证** | 直接来自本地 AAR 的 `classes.jar` / sources JAR，给出类名或源码行号 |
| 🔁 **交叉验证** | 本地证据 + androidx-main 的 `current.txt`（公开 API 清单）两边一致 |
| 📄 **仅文档** | 只有官方文档/规范说法，本地 artifact 无法证实 |
| ⚠️ **不确定** | 证据不足或来源冲突，已写明冲突点 |

> **重要前提**：`1.5.0-alpha29` 是 `1.5.0-beta01` 之前的最后一个 alpha。Google Maven 的
> [`maven-metadata.xml`](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/maven-metadata.xml)
> 显示 `1.5.0-alpha29` 已发布、`latest/release` 已经是 `1.5.0-beta01`（`lastUpdated=20261007170356`）。
> 也就是说这个依赖是**真实存在的已发布 artifact**，不是本地魔改版。

---

## 1. 摘要表

| 组件 / API | 可用? | opt-in | 关键默认值 | 来源 | 备注 |
| --- | --- | --- | --- | --- | --- |
| `FloatingActionButtonMenu` | ✅ 有 | **无（stable）** | item 容器宽 = `FabMenuBaselineTokens.ListItemContainerHeight`；关闭按钮 = `CloseButtonContainerHeight`，圆角 = 尺寸/2；水平 padding 16dp | `FloatingActionButtonMenuKt` / `FloatingActionButtonMenu.kt:114` | 另有 `ToggleFloatingActionButton` + `ToggleFloatingActionButtonDefaults` |
| `ButtonGroup` | ✅ 有 | **无（stable）** | `ExpandedRatio=0.15f`；连通间距 2dp；普通间距 12dp；高度 40dp | `ButtonGroupKt` / `ButtonGroup.kt:129` | ⚠️ 只有**一个**重载，`overflowIndicator` 是**必填首参** |
| `SplitButton` | ⚠️ **没有这个 composable** | 无 | — | `SplitButtonKt` 只有 `SplitButtonLayout` | 用 `SplitButtonLayout` + `SplitButtonDefaults.LeadingButton/TrailingButton` 拼 |
| `ToggleButtonGroup` | ❌ 不存在 | — | — | 全仓 grep 无 `ToggleButtonGroup` | 用 `ToggleButton` 或 `ButtonGroup.toggleableItem` |
| `ToggleButton` | ✅ 有 | 无（stable） | 尺寸档 `ToggleButtonSize`，形状集 `ToggleButtonShapes` | `ToggleButton.kt:157` | 单控件，非 group |
| `HorizontalFloatingToolbar` | ✅ 有（2 个重载） | **无（stable）** | 高 64dp；形状 `CornerFull`；屏边距 16dp；内容 padding 8/8 | `FloatingToolbarKt` / `FloatingToolbar.kt:173,274` | 重载 A 用 `leading/trailingContent`，重载 B 用 `floatingActionButton` + FAB position |
| `VerticalFloatingToolbar` | ✅ 有（2 个重载） | **无（stable）** | 同上，宽 64dp | `FloatingToolbar.kt:345,439` | |
| `MediumFlexibleTopAppBar` | ✅ 有 | **无（stable）** | 无 subtitle 展开 112dp；有 subtitle 展开 136dp；收起 64dp | `AppBarKt` / `AppBar.kt:614` | 标题字体 `HeadlineMedium`，subtitle `LabelLarge` |
| `LargeFlexibleTopAppBar` | ✅ 有 | **无（stable）** | 无 subtitle 展开 **120dp**；有 subtitle 展开 152dp；收起 64dp | `AppBar.kt:808` | 标题 `DisplaySmall`，subtitle `TitleMedium` |
| `TopAppBar(title, subtitle, …)` | ✅ 有（新重载） | 无（stable） | 高 64dp | `AppBar.kt:439` | 比旧重载多一个**必填** `subtitle` 形参 |
| `FlexibleBottomAppBar` | ✅ 有 | **无（stable）** | 展开高 64dp（= `DockedToolbarTokens.ContainerHeight`）；`Arrangement.SpaceBetween` | `AppBarKt` / `AppBar.kt:1276` | 与 `BottomAppBar`(80dp) 是两个组件 |
| `BottomAppBar` 滚动/展开重载 | ✅ 有（4 个重载） | 无（stable） | 高 80dp | `AppBar.kt:1005,1069,1134,1186` | 新增 `expandedHeight` / `horizontalArrangement` / `scrollBehavior` |
| `ShortNavigationBar` | ✅ 有 | **无（stable）** | 最小高 64dp（`NavigationBarTokens.ContainerHeight`）；`arrangement=EqualWeight` | `ShortNavigationBarKt` / `ShortNavigationBar.kt:93` | `ShortNavigationBarArrangement` 有 `EqualWeight` / `Centered` |
| `LoadingIndicator` | ✅ 有（2 个重载） | ⚠️ **需要** `ExperimentalMaterial3ExpressiveApi` | 容器 48dp × 48dp；`CornerFull`；活动形 38dp | `LoadingIndicatorKt` / `LoadingIndicator.kt:98,136` | 形状 morph 序列见 §2.7 |
| `ContainedLoadingIndicator` | ✅ 有（2 个重载） | ⚠️ **需要** | 容器 48dp；`PrimaryContainer` 底 | `LoadingIndicator.kt:184,227` | |
| `LinearWavyProgressIndicator` | ✅ 有（2 个重载） | **无（stable）** | 轨道厚 4dp；振幅 3dp；波长 40dp；容器 240dp × 10dp | `WavyProgressIndicatorKt` | 🎯 项目已在 4 处使用 |
| `CircularWavyProgressIndicator` | ✅ 有（2 个重载） | **无（stable）** | 容器 **48dp**；振幅 1.6dp；波长 15dp | 同上 | 项目未使用；⚠️ 不是老的 40dp |
| `HorizontalMultiBrowseCarousel` | ✅ 有 | **无（stable）** | `MinSmallItemSize=40dp`，`MaxSmallItemSize=56dp` | `carousel/CarouselKt` | ⚠️ 位于子包 `androidx.compose.material3.carousel` |
| `HorizontalUncontainedCarousel` | ✅ 有 | 无（stable） | 同上 | `carousel/Carousel.kt:179` | |
| `HorizontalCenteredHeroCarousel` | ✅ 有 | 无（stable） | 同上 | `carousel/Carousel.kt:240` | |
| 竖向 Carousel | ❌ **不存在** | — | — | grep 无 `VerticalCarousel` | 官方规范有 vertical，Compose 未实现 |
| Expressive 卡片变体 | ❌ **无专属 API** | — | `CornerMedium` | `Card.kt:367` | 仍是 `Card`/`ElevatedCard`/`OutlinedCard` 三件套 |
| `MaterialShapes` | ✅ 有 | ⚠️ **需要** | 35 个预定义 `RoundedPolygon` | `MaterialShapesKt` / `MaterialShapes.kt:140` | `sealed class` + `Companion` |
| `MotionScheme.expressive()` | ✅ 有 | **无（stable）** | default spatial 0.8/380，fast 0.6/800，slow 0.8/200 | `MotionSchemeKt` / `MotionScheme.kt:132` | 🎯 项目 `Theme.kt:67` 已注入 |
| `Modifier.animateShape` | ❌ **不存在** | — | — | 全 artifact 无此符号 | 只有 `internal` 的 `animateToShape` |
| `MaterialExpressiveTheme` | ✅ 有 | **无（stable）** | 透传 `colorScheme/motionScheme/shapes/typography` | `MaterialThemeKt` | 🎯 项目 `Theme.kt:65` 已在用 |
| Emphasized 字号（15 个） | ✅ 全部有 | **无（stable）** | 见 §4 | `Typography.kt:128-141` | 🎯 项目已用 |
| `expressiveLightColorScheme()` | ✅ 有 | **无（stable）** | — | `ColorScheme.kt:1172` | 无 `expressiveDarkColorScheme()` |

---

## 2. 组件

### 2.1 FAB menu

✅ **本地已验证**：`FloatingActionButtonMenuKt.class`、`FloatingActionButtonMenuScope.class`、
`ToggleFloatingActionButtonDefaults.class`、`ToggleFloatingActionButtonScope.class` 均在
`classes.jar` 中。

```kotlin
// FloatingActionButtonMenu.kt:114
@Composable
public fun FloatingActionButtonMenu(
    expanded: Boolean,
    button: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable FloatingActionButtonMenuScope.() -> Unit,
)
```

配套 API：

| 符号 | 位置 | 说明 |
| --- | --- | --- |
| `FloatingActionButtonMenuScope.FloatingActionButtonMenuItem(onClick, text, icon, modifier, containerColor, contentColor)` | `:329` | 菜单项**必须**同时给 `text` 和 `icon`（都无默认值） |
| `ToggleFloatingActionButton(checked, onCheckedChange, modifier, containerColor, contentAlignment, containerSize, containerCornerRadius, content)` | `:441` | 展开态是"关闭"按钮；`containerSize`/`containerCornerRadius` 是 `(Float) -> Dp` 的函数式，随展开进度形变 |
| `ToggleFloatingActionButtonDefaults.containerSize()` / `containerSizeMedium()` / `containerSizeLarge()` | `:582-591` | 三档初始尺寸 |
| `ToggleFloatingActionButtonDefaults.containerCornerRadius()` / `...Medium/Large` | `:593-604` | 初始圆角：16dp / 20dp / 28dp |
| `ToggleFloatingActionButtonDefaults.iconSize()` / `...Medium/Large` | `:613-622` | 末态图标 = `FabMenuBaselineTokens.CloseButtonIconSize` |
| `Modifier.animateIcon(...)` | `:633` | `ToggleFloatingActionButtonScope` 上的图标动画 |
| `ToggleFloatingActionButtonScope.checkedProgress: Float` | `:657` | 0→1 展开进度 |

**与规范的差异**：官方 M3 Expressive 的 FAB menu 是"FAB 原地变成关闭按钮 + 菜单项向上弹出"。
这里的实现完全一致（`FabInitialCornerRadius` 16dp → `FabFinalCornerRadius = FabFinalSize/2`，
即 16dp 圆角方 → 全圆）。**没有** 官方规范里的 `FloatingActionButtonMenu` 无障碍焦点陷阱的公开配置项。

### 2.2 ButtonGroup

✅ **本地已验证**。`ButtonGroupKt`、`ButtonGroupDefaults`、`ButtonGroupScope`、`ButtonGroupMenuState`、
`ButtonGroupMeasurePolicy`、`ButtonGroupItemProvider` 等 15 个类。

```kotlin
// ButtonGroup.kt:129 —— 全 artifact 只有这一个 ButtonGroup 重载
@Composable
public fun ButtonGroup(
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit,   // ⚠️ 必填、无默认值
    modifier: Modifier = Modifier,
    @FloatRange(0.0) expandedRatio: Float = ButtonGroupDefaults.ExpandedRatio,
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: ButtonGroupScope.() -> Unit,
)
```

**关键默认值（全部源码实证）**：

| 项 | 值 | 来源 |
| --- | --- | --- |
| `ButtonGroupDefaults.ExpandedRatio` | `0.15f`（按下时按钮最多横向膨胀 15%） | `ButtonGroup.kt:186` |
| `HorizontalArrangement` | `Arrangement.spacedBy(12.dp)` | `:189`，`ButtonGroupSmallTokens.BetweenSpace=12.0.dp` |
| `ConnectedSpaceBetween` | `2.dp` | `:193`，`ConnectedButtonGroupSmallTokens.BetweenSpace=2.0.dp` |
| 连通组容器高 | `40.dp` | `ConnectedButtonGroupSmallTokens.ContainerHeight` |
| 连通组形状 | `CornerFull` | `ConnectedButtonGroupSmallTokens.ContainerShape` |

**`ButtonGroupScope` 公开成员**（`ButtonGroup.kt:826-908`）：
`Modifier.weight(Float)`、`Modifier.animateWidth(interactionSource)`（另有带 `compressionLimit: Dp` 的重载）、
`clickableItem(onClick, label, icon, weight, enabled)`、`toggleableItem(checked, label, onCheckedChange, icon, weight, enabled)`、
`customItem(buttonGroupContent, menuContent)`。

**与规范的差异**：
- ⚠️ **`overflowIndicator` 是必填参数**。官方文档示例里常写成 `ButtonGroup(modifier) { … }`，
  在这个版本编译不过。不想要溢出行为时传 `{}`。
- 连通组（connected）需要用 `ButtonGroupDefaults.connectedLeadingButtonShapes()` /
  `connectedMiddleButtonShapes()` / `connectedTrailingButtonShapes()` 显式给每个按钮配形状，
  不是自动推导。

### 2.3 SplitButton

⚠️ **本地已验证：不存在名为 `SplitButton` 的 composable**。`SplitButtonKt.class` 里唯一的公开函数是：

```kotlin
// SplitButton.kt:120
@Composable
public fun SplitButtonLayout(
    leadingButton: @Composable () -> Unit,
    trailingButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = SplitButtonDefaults.Spacing,   // 2.dp
)
```

按钮本身来自 `SplitButtonDefaults`（`object`，`SplitButton.kt:204` 起）：

| 按钮族 | 签名位置 |
| --- | --- |
| `LeadingButton` | `:681` |
| `TrailingButton`（普通） | `:761` |
| `TrailingButton`（可 toggle/checked） | `:872` |
| `TonalLeadingButton` / `TonalTrailingButton` | `:993` / `:1053` |
| `OutlinedLeadingButton` / `OutlinedTrailingButton` | `:1110` / `:1170` |
| `ElevatedLeadingButton` / `ElevatedTrailingButton` | `:1227` / `:1287` |

**关键默认值**：

| 项 | 值 |
| --- | --- |
| 容器高 | `40.dp`（`SplitButtonSmallTokens.ContainerHeight`） |
| 两键间距 | `2.dp`（`BetweenSpace`） |
| 外角 | `ShapeDefaults.CornerFull` |
| 内角 | `CornerValueExtraSmall`（未按下）/ `CornerValueMedium`（按下、hover） |
| trailing 图标 | `22.dp` |
| 形状集 | `SplitButtonDefaults.leadingButtonShapes()` / `trailingButtonShapes()`，以及 `leadingButtonShapesFor(buttonHeight)` / `trailingButtonShapesFor(buttonHeight)` |

**与规范的差异**：官方规范把 SplitButton 当作一个整体控件（"主键 + 分离的箭头键，箭头键按下时
内角变方"）。Compose 这里把形状计算（`SplitButtonShapes(shape, pressedShape, checkedShape)`）
暴露出来但**不替你调**——你必须自己调 `leadingButtonShapesFor(按钮高度)` 才能拿到跟高度档位匹配的形状。

### 2.4 浮动工具栏

✅ **本地已验证**，四个重载 + 完整 defaults。

```kotlin
// FloatingToolbar.kt:173 —— 不带 FAB
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevation,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevation,
    content: @Composable RowScope.() -> Unit,
)

// FloatingToolbar.kt:274 —— 带 FAB
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    floatingActionButtonPosition: FloatingToolbarHorizontalFabPosition = FloatingToolbarHorizontalFabPosition.End,
    animationSpec: FiniteAnimationSpec<Float> = FloatingToolbarDefaults.animationSpec(),
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevationWithFab,
    content: @Composable RowScope.() -> Unit,
)
```

`VerticalFloatingToolbar` 在 `:345`（无 FAB）与 `:439`（有 FAB）两个同构重载。

| 默认值 | 值 | 来源 |
| --- | --- | --- |
| `ContainerSize` | `64.dp` | `FloatingToolbarTokens.ContainerHeight = 64.0.dp` |
| `ContainerShape` | `ShapeKeyTokens.CornerFull` | 同上 |
| `ScreenOffset` | `16.dp` | `FloatingToolbarTokens.ContainerExternalPadding = 16.0.dp` |
| 展开/收起阴影 | `ElevationTokens.Level0`（源码里有 `// TODO read from token`） | `FloatingToolbar.kt:632-638` ⚠️ |
| 滚动阈值 | `ScrollDistanceThreshold = 40.dp` | `:1140` |

配套：`FloatingToolbarDefaults.exitAlwaysScrollBehavior(...)`、`horizontalEnterTransition(expandFrom)` /
`verticalEnterTransition` / `horizontalExitTransition` / `verticalExitTransition`、
`rememberFloatingToolbarState(...)`、`FloatingToolbarDefaults.VibrantFloatingActionButton`、
`Modifier.floatingToolbarVerticalNestedScroll(...)`、`FloatingToolbarColors`（`standard` / `vibrant`）。

**与规范的差异**：⚠️ 源码里展开/收起阴影硬编码为 `ElevationTokens.Level0` 并留了 TODO —— 也就是说
官方规范里"浮动工具栏有轻微 elevation"在这个 alpha 里**没生效**。要视觉分层得自己传
`expandedShadowElevation`。

### 2.5 Flexible top app bars

✅ **本地已验证**，且**已转正**（`AppBarKt` 的 `javap -v` 输出里 `ExperimentalMaterial3ExpressiveApi`
出现 0 次）。

| 函数 | 行号 | 无 subtitle 展开高 | 有 subtitle 展开高 | 收起高 |
| --- | --- | --- | --- | --- |
| `TopAppBar(title, subtitle, …)` | `AppBar.kt:439` | 64dp（`AppBarSmallTokens.ContainerHeight`） | — | — |
| `MediumFlexibleTopAppBar` | `:614` | `112.dp` | `136.dp` | `64.dp` |
| `LargeFlexibleTopAppBar` | `:808` | `120.dp` | `152.dp` | `64.dp` |
| `MediumTopAppBar` | `:516` | `112.dp` | — | `64.dp` |
| `LargeTopAppBar` | `:710` | `152.dp` | — | `64.dp` |

字号 token：Medium Flexible 标题 `HeadlineMedium` + 副标题 `LabelLarge`；Large Flexible 标题
`DisplaySmall` + 副标题 `TitleMedium`；收起态一律降到 `AppBarSmallTokens` 的 `TitleFont`。

滚动行为：`TopAppBarScrollBehavior` / `ExitUntilCollapsedScrollBehavior` /
`ExitAlwaysFloatingToolbarScrollBehavior`，由 `rememberTopAppBarState()` 承载。

**与规范的差异**：
- ⚠️ **`LargeFlexibleTopAppBar` 无副标题时是 120dp，比老 `LargeTopAppBar` 的 152dp 更矮**。
  从 `LargeTopAppBar` 直接换成 `LargeFlexibleTopAppBar` 会**变矮**，不是变大。这跟"Expressive 更大更醒目"
  的直觉相反，容易误判为回归。
- 新 `TopAppBar(title, subtitle, …)` 重载的 `subtitle` 是**必填**形参（无默认值）；想要无副标题的小顶栏
  必须走旧的 `TopAppBar(title, modifier, …)` 重载。

### 2.6 ShortNavigationBar / FlexibleBottomAppBar / NavigationBar

✅ **本地已验证**：

```kotlin
// ShortNavigationBar.kt:93
@Composable
public fun ShortNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
    arrangement: ShortNavigationBarArrangement = ShortNavigationBarDefaults.arrangement,
    content: @Composable () -> Unit,
)

// ShortNavigationBar.kt:188
@Composable
public fun ShortNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,          // ⚠️ 可空、无默认值
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPosition: NavigationItemIconPosition = NavigationItemIconPosition.Top,
    colors: NavigationItemColors = ShortNavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
)
```

- `ShortNavigationBarArrangement`：`EqualWeight`（默认）/ `Centered`（`ShortNavigationBar.kt:126-142`）。
- 最小高 = `NavigationBarTokens.ContainerHeight = 64.dp`（`:105`）。
- `ShortNavigationBarDefaults.containerColor` 取 `SurfaceContainer`，`containerColor` 在
  `ShortNavigationBarDefaults`（`:238`）。

```kotlin
// AppBar.kt:1276
@Composable
public fun FlexibleBottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: PaddingValues = BottomAppBarDefaults.FlexibleContentPadding,
    horizontalArrangement: Arrangement.Horizontal = BottomAppBarDefaults.FlexibleHorizontalArrangement,
    expandedHeight: Dp = BottomAppBarDefaults.FlexibleBottomAppBarHeight,   // 64.dp
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    scrollBehavior: BottomAppBarScrollBehavior? = null,
    content: @Composable RowScope.() -> Unit,
)
```

对比：`NavigationBar` 高 64dp、`TallContainerHeight` 80dp、`NavShape = CornerNone`；
`BottomAppBar` 高 80dp、`CornerNone`、`SurfaceContainer`、elevation Level2；
`FlexibleBottomAppBar` 高 **64dp**、`FlexibleHorizontalArrangement = Arrangement.SpaceBetween`。

**与规范的差异**：`FlexibleBottomAppBar` 的 `expandedHeight` 在传 `null` scrollBehavior 时就是
**固定高**（KDoc 明说："In case the scrollBehavior is null, this value will simply be the fixed
height of the bottom bar"）。官方规范的 flexible bottom bar 是可随滚动增高的；不传 scrollBehavior
就退化成一条 64dp 的普通底栏。

> 项目现状提示：`M3BottomNavigationBar.kt:73` 的注释解释了**为什么不用** `ShortNavigationBar`
> （它会隐藏未选中项的文字）。这个取舍在 alpha29 依然成立 —— `ShortNavigationBarItem` 的
> `label` 是可空参数，由组件自行决定是否展示。

### 2.7 LoadingIndicator

⚠️ **这是仅剩的少数仍需要 opt-in 的组件之一**。`javap -v -p androidx.compose.material3.LoadingIndicatorKt`
里 `ExperimentalMaterial3ExpressiveApi` 出现 **5 次**。

| 符号 | 行号 | 签名要点 |
| --- | --- | --- |
| `LoadingIndicator(progress: () -> Float, modifier, color, polygons)` | `:98` | 确定性（determinate） |
| `LoadingIndicator(modifier, color, polygons)` | `:136` | 不确定性（indeterminate） |
| `ContainedLoadingIndicator(progress, modifier, containerColor, indicatorColor, containerShape, polygons)` | `:184` | 确定性 + 有底容器 |
| `ContainedLoadingIndicator(modifier, containerColor, indicatorColor, containerShape, polygons)` | `:227` | 不确定性 + 有底容器 |
| `LoadingIndicatorDefaults` | `:505` | `ContainerWidth=48dp`、`ContainerHeight=48dp`、`IndicatorSize=38dp`、`containerShape=CornerFull`、`indicatorColor=Primary`、`containedContainerColor=PrimaryContainer`、`containedIndicatorColor=OnPrimaryContainer` |

**形变序列（这才是 Expressive 的"signature moment"）**——`LoadingIndicatorDefaults.IndeterminateIndicatorPolygons`
（`LoadingIndicator.kt:545`）默认在 **7 个形状之间 morph**：

```
SoftBurst → Cookie9Sided → Pentagon → Pill → Sunny → Cookie4Sided → Oval
```

`DeterminateIndicatorPolygons` 则随 `progress` 0→1 在这些形状间插值。

**与规范的差异**：规范里的 loading indicator 是 7 形状循环 + 可自定义形状序列，这里一致且
`polygons: List<RoundedPolygon>` 完全开放。差别是**默认尺寸 48dp 是"含容器"尺寸，无容器版直接用 48dp 画布**
——比老 `CircularProgressIndicator` 的 40dp 视觉上更大，替换时要注意占位对齐。

### 2.8 Wavy progress indicators

✅ **本地已验证，且已转正**（`WavyProgressIndicatorKt` 的 `javap -v` 里 expressive 注解 0 次）。

| 符号 | 行号 |
| --- | --- |
| `LinearWavyProgressIndicator(progress, modifier, color, trackColor, stroke, trackStroke, gapSize, stopSize, amplitude, wavelength, waveSpeed)` | `WavyProgressIndicator.kt:96` |
| `LinearWavyProgressIndicator(...)`（indeterminate 重载） | `:166` |
| `CircularWavyProgressIndicator(...)`（determinate） | `:275` |
| `CircularWavyProgressIndicator(...)`（indeterminate） | `:345` |
| `WavyProgressIndicatorDefaults` | `:379` |

| 默认值 | Linear | Circular |
| --- | --- | --- |
| 轨道厚 `TrackThickness` | `4.0.dp` | `4.0.dp` |
| 活动波振幅 `ActiveWaveAmplitude` | `3.0.dp` | `1.6.dp` |
| 波长 `ActiveWaveWavelength` | `40.0.dp` | `15.0.dp` |
| indeterminate 波长 | `20.0.dp` | — |
| 容器尺寸 | 高 `10.0.dp`（`WaveHeight`）× 宽 **`240.dp`**（源码硬编码） | **`48.0.dp`**（`WaveSize`） |
| 活动/轨道间隙 `TrackActiveSpace` | `4.0.dp` | `4.0.dp` |
| 停止点尺寸 `StopSize` | `4.0.dp` | — |
| 描边端点 | `StrokeCap.Round` | `StrokeCap.Round` |

> ⚠️ **易错**：波浪版圆形进度的默认尺寸是 **48dp**（`CircularProgressIndicatorTokens.WaveSize`），
> 不是老 `CircularProgressIndicator` 的 40dp（那是 `CircularProgressIndicatorTokens.Size`）。
> 两者并排会出现 8dp 的视觉错位。Linear 的默认宽度 240dp 是写死在
> `WavyProgressIndicatorDefaults.LinearContainerWidth`（`:458`）里的常量，**没有 token 支撑**；
> 实际使用时几乎总要 `Modifier.fillMaxWidth()` 覆盖。

**振幅是 progress 的函数**，不是常量（`WavyProgressIndicator.kt:482-489`）：

```kotlin
public val indicatorAmplitude: (progress: Float) -> Float = { progress ->
    if (progress <= 0.1f || progress >= 0.95f) 0f else 1f
}
```

即：**进度 ≤10% 或 ≥95% 时波形振幅归零，只剩直线**，中间段才是完整波浪。这是官方规范里
"接近完成时波动平息"的落地方式。

**关键差异**：`waveSpeed` 的默认值**等于 `wavelength`**，源码注释写明意图是"1 秒 1 个波长"
（`WavyProgressIndicator.kt:107`）。

### 2.9 Carousel

✅ **本地已验证**，但**在子包**里：`androidx.compose.material3.carousel`。

| 符号 | 行号 | 必需参数 |
| --- | --- | --- |
| `HorizontalMultiBrowseCarousel(state, preferredItemWidth, …)` | `carousel/Carousel.kt:108` | `state: CarouselState`、`preferredItemWidth: Dp` |
| `HorizontalUncontainedCarousel(state, itemWidth, …)` | `:179` | 同上 |
| `HorizontalCenteredHeroCarousel(state, itemWidth, …)` | `:240` | 同上 |
| `CarouselDefaults` | `:705` | — |

- `CarouselDefaults.MinSmallItemSize = 40.dp`、`MaxSmallItemSize = 56.dp`（`:795`,`:798`）。
- fling 行为：`singleAdvanceFlingBehavior(state, snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow))`、
  `multiBrowseFlingBehavior(...)`、`noSnapFlingBehavior()`。
- ⚠️ **没有竖向 Carousel**：`grep VerticalCarousel` 结果为空，androidx-main 的 `current.txt` 里也是 0 次。

**与规范的差异**：官方规范有 vertical carousel；Compose 只有 horizontal 三种。另外
`preferredItemWidth` 是必填（不像规范说的"自适应"），必须自己算。

### 2.10 Expressive 卡片

❌ **没有专属 API**。`Card.kt` 里只有 `Card` / `ElevatedCard` / `OutlinedCard`（各 2 个重载，共 6 个，
`:80,136,191,241,291,343`）和 `CardDefaults`（`:367`）。

`CardDefaults.shape` / `elevatedShape` / `outlinedShape` 均取 `ShapeKeyTokens.CornerMedium`（12dp）。
Expressive 的"卡片形状升级"体现在 **`ShapeDefaults` 的 `largeIncreased` / `extraLargeIncreased` /
`extraExtraLarge` 三档**（`Shapes.kt:89-91`），由**主题**生效而不是卡片 API 本身。项目 `SonShapes`
已经用了这三档。

### 2.11 其它 Expressive-only API

以下是在这个 artifact 里**仅剩的**、直接被 `@ExperimentalMaterial3ExpressiveApi` 标注的公开声明
（全 artifact 穷举，`javap` 与源码一致）：

| API | 位置 | 用途 |
| --- | --- | --- |
| `DropdownMenuItem(checked, onCheckedChange, …)` | `Menu.kt:553`（`@JvmName("DropdownMenuItemChecked")`） | 带勾选态的菜单项 |
| `DropdownMenuItem(selected, onClick, …)` | `Menu.kt:795`（`@JvmName("DropdownMenuItemSelected")`） | 带选中态的菜单项 |
| `SliderDefaults.Thumb(...)` | `Slider.kt:1535` | Expressive 滑块拇指 |
| `PullToRefreshDefaults.loadingIndicatorContainerColor` | `pulltorefresh/PullToRefresh.kt:472` | 下拉刷新用 loading 配色 |
| `PullToRefreshDefaults.loadingIndicatorColor` | `:486` | 同上 |
| `PullToRefreshDefaults.LoadingIndicator(...)` | `:635` | 下拉刷新的 loading 指示器 |

**不再需要 opt-in 的（已转正）**：`ButtonGroup`、`SplitButtonLayout` + `SplitButtonDefaults`、
`FloatingActionButtonMenu`、`FloatingToolbar`（横竖）、`LargeFlexibleTopAppBar` /
`MediumFlexibleTopAppBar` / `FlexibleBottomAppBar`、`ShortNavigationBar`、
`LinearWavyProgressIndicator` / `CircularWavyProgressIndicator`、三种 Carousel、
`MotionScheme.expressive()`、`MaterialExpressiveTheme`、`expressiveLightColorScheme()`、
`ToggleButton`、Emphasized 字号、`ShapeDefaults` 三档新形状、
以及 `ButtonShapes` / `ToggleButtonShapes` / `ListItemShapes` / `IconButtonShapes` / `ChipShapes` /
`MenuGroupShapes` / `MenuItemShapes` / `SplitButtonShapes` / `DragHandleShapes` / `TimePickerShapes`
这些形状集类型。

---

## 3. Shape 与 Motion

### 3.1 MaterialShapes —— 需要 opt-in

✅ 本地已验证：`MaterialShapes.class`、`MaterialShapes$Companion.class`、
`MaterialShapes$Companion$PointNRound.class`。

⚠️ `javap -v androidx.compose.material3.MaterialShapes` 里 expressive 注解出现 **2 次**；
`MaterialShapes.kt:139-140` 标注在 `sealed class` 上。**所有 `Companion` 形状都是 `RoundedPolygon`，
归一化过（`normalized()`）**。

35 个预定义形状（`MaterialShapes.kt:192-…`）：

```
Circle, Square, Slanted, Arch, Fan, Arrow, SemiCircle, Oval, Pill, Triangle,
Diamond, ClamShell, Pentagon, Gem, VerySunny, Sunny,
Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided,
Ghostish, Clover4Leaf, Clover8Leaf, Burst, SoftBurst, Boom, SoftBoom,
Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart
```

公开的转换函数（**都需要 opt-in**）：

| 函数 | 行号 | 说明 |
| --- | --- | --- |
| `Morph.toPath(progress: Float, path: Path = Path(), startAngle: Int = 0): Path` | `:56` | 形状插值核心 |
| `RoundedPolygon.toPath(startAngle: Int = 0): Path` | `:72` | `@Composable`，remember 缓存 |
| `RoundedPolygon.toShape(startAngle: Int = 0): Shape` | `:91` | `@Composable`，返回可给 `Modifier.clip` 的 `Shape` |

### 3.2 MotionScheme —— 已转正

✅ 本地已验证：`MotionScheme`、`MotionScheme$ExpressiveMotionSchemeImpl`、
`MotionScheme$StandardMotionSchemeImpl`、`MotionSchemeKt`。`MotionScheme.kt:132` 的
`expressive()` **无 opt-in 注解**，`javap` 确认 0 次。

`MotionScheme` 接口 6 个方法（`MotionScheme.kt:45-113`），全部返回泛型 `FiniteAnimationSpec<T>`：
`defaultSpatialSpec()`、`fastSpatialSpec()`、`slowSpatialSpec()`、`defaultEffectsSpec()`、
`fastEffectsSpec()`、`slowEffectsSpec()`。

**Expressive 与 Standard 的弹簧参数（源码实证，`tokens/*MotionTokens.kt`）**：

| token | Expressive | Standard |
| --- | --- | --- |
| `SpringDefaultSpatialDamping` / `Stiffness` | `0.8f` / `380.0f` | `0.9f` / `700.0f` |
| `SpringFastSpatialDamping` / `Stiffness` | `0.6f` / `800.0f` | `0.9f` / `1400.0f` |
| `SpringSlowSpatialDamping` / `Stiffness` | `0.8f` / `200.0f` | `0.9f` / `300.0f` |
| `SpringDefaultEffectsDamping` / `Stiffness` | `1.0f` / `1600.0f` | `1.0f` / `1600.0f` |
| `SpringFastEffectsDamping` / `Stiffness` | `1.0f` / `3800.0f` | `1.0f` / `3800.0f` |
| `SpringSlowEffectsDamping` / `Stiffness` | `1.0f` / `800.0f` | `1.0f` / `800.0f` |

**读法**：Expressive 的差异**只在 spatial**——阻尼更低（0.6/0.8 vs 0.9，即允许过冲）、
刚度按档位不同；**effects 两套完全相同**（阻尼恒 1.0，不过冲）。这正好印证项目的
`SonMotion`（`Motion.kt:21-32`）抄的是 Expressive 那一列，并且抄对了。

### 3.3 `Modifier.animateShape` —— 不存在

❌ **本地已验证**：整个 artifact 的 sources 里 grep `animateShape` **0 命中**；
`androidx-main` 的 `current.txt` 里也是 **0 命中**。

存在的是 **`internal`** 的 `androidx/compose/material3/internal/AnimatedShape.kt`：

```kotlin
// internal/AnimatedShape.kt:81
internal suspend fun animateToShape(newTarget: CornerBasedShape)
// internal/AnimatedShape.kt:180
LaunchedEffect(currentShape, state) { state.animateToShape(currentShape) }
```

**所以想自己写 shape morph 动画，公开路径只有两条**：

1. **形状集 + 组件内部形变**：给 Material 组件传 `ButtonShapes` / `ToggleButtonShapes` /
   `ListItemShapes` / `SplitButtonShapes` 这类 `(shape, pressedShape, checkedShape)` 三元组，
   组件内部用 `shapeByInteraction(shapes, pressed, checked, animationSpec)` 做形变
   （见 `SplitButton.kt:780`）。
2. **`RoundedPolygon` 手搓**：`MaterialShapes.*` + `Morph(…)` + `Morph.toPath(progress)` 或
   `RoundedPolygon.toShape()`，配 `Modifier.clip(shape)`，自己驱动 `progress`。
   **两个函数都要 `ExperimentalMaterial3ExpressiveApi`。**

> `AGENTS.md` 里"Expressive 的按压反馈是形状变形，Material 组件内部已经实现，页面不要再手写形变"
> 这句话在 alpha29 **仍然成立**，而且现在有了更硬的理由：**公开 API 根本没有 `animateShape` 可用**。

### 3.4 形状标尺

✅ `Shapes` 是 `@Immutable` 的 stable 类（`Shapes.kt:81-92`），8 档：

`extraSmall` / `small` / `medium` / `large` / **`largeIncreased`** / `extraLarge` /
**`extraLargeIncreased`** / **`extraExtraLarge`**（后三档是 Expressive 新增，`ShapeDefaults` 里
`LargeIncreased` / `ExtraLargeIncreased` / `ExtraExtraLarge` 均为 stable）。

项目 `SonShapes`（`core/ui/theme/Shape.kt:15-24`）给了 4/8/12/16/20/28/32/48 dp，**档位与官方一致**。

---

## 4. Typography

✅ **本地已验证，全部 15 个 Emphasized 样式都在，且都是 stable**（`Typography.kt` 未出现在
`ExperimentalMaterial3ExpressiveApi` 的使用文件列表里；`javap -v androidx.compose.material3.Typography`
里 expressive 注解 **0 次**）。

`Typography()` 的**主构造器**就把 Emphasized 作为具名参数并默认取 token 值
（`Typography.kt:128-141`）：

```kotlin
displayLargeEmphasized:    TextStyle = typographyTokens.DisplayLargeEmphasized,
displayMediumEmphasized:   TextStyle = typographyTokens.DisplayMediumEmphasized,
displaySmallEmphasized:    TextStyle = typographyTokens.DisplaySmallEmphasized,
headlineLargeEmphasized:   TextStyle = typographyTokens.HeadlineLargeEmphasized,
headlineMediumEmphasized:  TextStyle = typographyTokens.HeadlineMediumEmphasized,
headlineSmallEmphasized:   TextStyle = typographyTokens.HeadlineSmallEmphasized,
titleLargeEmphasized:      TextStyle = typographyTokens.TitleLargeEmphasized,
titleMediumEmphasized:     TextStyle = typographyTokens.TitleMediumEmphasized,
titleSmallEmphasized:      TextStyle = typographyTokens.TitleSmallEmphasized,
bodyLargeEmphasized:       TextStyle = typographyTokens.BodyLargeEmphasized,
bodyMediumEmphasized:      TextStyle = typographyTokens.BodyMediumEmphasized,
bodySmallEmphasized:       TextStyle = typographyTokens.BodySmallEmphasized,
labelLargeEmphasized:      TextStyle = typographyTokens.LabelLargeEmphasized,
labelMediumEmphasized:     TextStyle = typographyTokens.LabelMediumEmphasized,
labelSmallEmphasized:      TextStyle = typographyTokens.LabelSmallEmphasized,
```

即：**`Typography()` 不用传任何参数就已经带 Emphasized 变体**，不需要 opt-in。

**token 实测值 vs 项目手写值**（`tokens/TypeScaleTokens.kt`）：

| 样式 | M3 token | 项目 `SonTypography` | 是否一致 |
| --- | --- | --- | --- |
| `displayLargeEmphasized` | 57sp / 64 / Medium / 0 | `displayToken(57, 64, Medium, 0.0f)` | ✅ 一致 |
| `headlineSmallEmphasized` | 24sp / 32 / Medium / 0 | `displayToken(24, 32, Medium, 0.0f)` | ✅ 一致 |
| `titleMediumEmphasized` | 16sp / 24 / Bold / 0.15 | `plainToken(16, 24, Bold, 0.15f)` | ✅ 一致 |
| `labelLargeEmphasized` | 14sp / 20 / Bold / 0.1 | `plainToken(14, 20, Bold, 0.1f)` | ✅ 一致 |

结论：项目 `Typography.kt:53-71` 那段手写的 Emphasized 块**与官方 token 完全同值**，不是"自定义风格"，
而是**忠实复刻**。字体族上 M3 分 `TypefaceTokens.Brand`（display/headline）与 `TypefaceTokens.Plain`
（title/body/label），项目用 `SonFontFamily.Brand` / `.Plain` 对应，两者都指向 `FontFamily.Default`。

> 也就是说：**这 15 行是可以删的**——`Typography()` 的默认值已经一样，除非以后要把 Brand 族换成
> Roboto Flex 而 Plain 族保持系统字体（那时才需要显式覆盖）。这属于可安全简化的重复，不是缺陷。

---

## 5. 字体与图标

### 5.1 Roboto Flex 作为内置可变字体

| 问题 | 结论 | 来源 |
| --- | --- | --- |
| 许可 | **SIL Open Font License 1.1**（可以随 App 打包、可以商用、可以子集化；要求保留版权与许可声明，且字体本身不得单独售卖） | [`ofl/robotoflex/METADATA.pb`](https://raw.githubusercontent.com/google/fonts/main/ofl/robotoflex/METADATA.pb) 的 `license: "OFL"` |
| 可变轴 | **13 条**：`wght` 100–1000、`wdth` 25–151、`opsz` 8–144、`slnt` -10–0、`GRAD` -200–150、`XOPQ` 27–175、`XTRA` 323–603、`YOPQ` 25–135、`YTAS` 649–854、`YTDE` -305–-98、`YTFI` 560–788、`YTLC` 416–570、`YTUC` 528–760 | 同上 `axes{}` 块 |
| 文件体积 | **1.70 MB**（单个可变 TTF，全轴全字符集） | 对 `RobotoFlex[...].ttf` 发 HTTP HEAD 得到的 `Content-Length` |
| Compose 支持 | `androidx.compose.ui.text.font.FontVariation` 及其 `Settings` / `SettingFloat` / `SettingInt` / `SettingTextUnit` 在 resolved 的 `ui-text` **1.13.0-alpha01** 里全部存在；`FontVariation` 需要 API 26+，本项目 `minSdk=30`，✅ 可用 | 本地 `ui-text-android/1.13.0-alpha01/ui-text.aar` → `classes.jar` 条目 |
| 当前项目状态 | **没有任何内置字体**：`res/font/` 目录不存在；`SonFontFamily.Brand` 与 `.Plain` 都是 `FontFamily.Default` | `core/ui/theme/Typography.kt:16-17`；`app/src/main/res` 只有 `drawable`/`mipmap-anydpi-v26`/`values`/`values-night`/`xml` |

**⚠️ 中文（CJK）兜底影响 —— 这是最关键的一条**：

Roboto Flex 的 `subsets` 字段只有：`cyrillic`、`cyrillic-ext`、`greek`、`latin`、`latin-ext`、
`menu`、`vietnamese`（同 `METADATA.pb`）。**没有 `chinese-simplified`，也完全没有 CJK 字形。**

本项目 UI 是中文（`AGENTS.md`、`docs/design/*` 全中文，界面文案中文）。如果把
`SonFontFamily.Brand`/`.Plain` 换成只含 Roboto Flex 的 `FontFamily`：

- 所有中文会**掉进系统 fallback**（Android 上通常是 Noto Sans CJK / 厂商字体），
  中英混排时**字面大小、基线、字重、字间距会不一致**，Emphasized 的 Bold 变体在中文上很可能
  被合成加粗（faux bold），观感与英文部分割裂。
- 正确做法是**组一个 `FontFamily` 列表**：`FontFamily(Font(R.font.roboto_flex, variationSettings=…), 系统CJK)`，
  或者干脆保留 `FontFamily.Default` 只把 Roboto Flex 用在**纯拉丁的展示位**
  （数字、英文歌名、`displayLargeEmphasized` 这类 hero 标题）。
- **建议**：不要全局替换字体族。先把 Roboto Flex 试在 hero 标题/大数字上，
  中文正文继续走系统字体。

### 5.2 Material Symbols

| 问题 | 结论 | 来源 |
| --- | --- | --- |
| 许可 | **Apache License 2.0**（可商用、可修改、需保留 NOTICE/许可） | [`google/material-design-icons/LICENSE`](https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE) |
| 可变字体体积 | `MaterialSymbolsOutlined[FILL,GRAD,opsz,wght].ttf` = **10.23 MB**；`MaterialSymbolsRounded[...].ttf` = **14.51 MB** | HTTP HEAD 的 `Content-Length` |
| 可调轴 | `FILL`、`GRAD`、`opsz`、`wght`（四个，文件名即轴列表） | 同上 |
| 能不能在 Compose 里用 | **能，但不是 `Icon`** —— Material Symbols 是**连字（ligature）字体**，不是 `ImageVector`。`Icon(imageVector=…)` 只吃 `ImageVector`/`Painter`。用法是 `res/font/` + `FontFamily` + `Text("settings", fontFamily = symbolsFont)`，或自己包一个 `@Composable SymbolIcon(name: String)` | 由 Compose `Icon` 的签名决定；`FontVariation` 在 `ui-text` 存在（见 5.1） |

**关键区分（任务里点名的那条）**：
`androidx.compose.material.icons` 里 ship 的是 **Material Icons**，**不是 Material Symbols**。本地证据：

- `material-icons-extended-android/1.7.8` 的 `classes.jar` 下有且只有这几个包：
  `automirrored`、`filled`、`outlined`、`rounded`、`sharp`、`twotone`
  —— 这是 Material Icons 的**5 种 theme + automirrored**，没有 `symbols` 包。
- 每个图标是**编译进代码的 `ImageVector`**（Kotlin 源生成），不是字体字形。
- AAR 体积 **34.07 MB**（`material-icons-extended-release.aar`）；`material-icons-core` 只有 0.79 MB。

**从 `material-icons-extended` 迁到 Material Symbols 的成本**：

| 维度 | Material Icons（现状） | Material Symbols | 迁移成本 |
| --- | --- | --- | --- |
| 交付形态 | `ImageVector`（Kotlin 代码） | 字体（`.ttf`）+ 连字 | 需要新增 `res/font/`，改所有调用点 |
| 调用方式 | `Icon(Icons.Rounded.PlayArrow, …)` | `Text("play_arrow", fontFamily=…)` 包一层 | **每一个图标调用点都要改** |
| 体积 | AAR 34 MB（R8 后只留用到的） | 10–14 MB 全量字体；子集化可大幅缩小 | 全量字体比现在的 AAR 更糟；必须子集化 |
| 类型安全 | ✅ 编译期检查，IDE 补全 | ❌ 字符串写错就静默渲染成豆腐块 | 变差 |
| 轴可调 | ❌ 固定字形 | ✅ FILL/GRAD/opsz/wght 连续可调 | 想要 expressive 的"填充态"才值得 |
| 项目规模 | 全项目图标**全部**来自 `Icons.Rounded.*` / `Icons.Outlined.*`（`CLAUDE.md` 已锁死这条规则） | — | 改动面 = 整个 `:presentation` 模块 |

**建议**：**不要迁移**。理由是唯一的真实收益（`FILL` 轴可做"选中填充动画"）可以用现有
`Icons.Rounded.Favorite` / `Icons.Outlined.FavoriteBorder` 两态切换近似，
而成本是改遍全模块 + 丢掉编译期检查 + 需要自建字体子集化流水线。这是一次**收益/成本严重倒挂**的迁移。

---

## 6. 与官方 M3 Expressive 规范的差距

按"规范有 / 这个 artifact 没有"列：

| # | 官方规范 | alpha29 现状 | 影响 |
| --- | --- | --- | --- |
| 1 | **竖向 Carousel** | ❌ 只有 `Horizontal*` 三种 | 竖向浏览场景只能回退 `LazyRow`/`HorizontalPager` |
| 2 | **`SplitButton` 作为单控件** | ⚠️ 只有 `SplitButtonLayout` + 自己拼 `LeadingButton`/`TrailingButton`，形状还要自己调 `leadingButtonShapesFor(height)` | 想完全符合规范的按下内角变方，必须读源码抄参数 |
| 3 | **`ToggleButtonGroup`** | ❌ 不存在 | 用 `ButtonGroup` + `toggleableItem` 或 `ButtonGroupDefaults.connectedXxxButtonShapes()` 手工组 |
| 4 | **公开的 shape-morph modifier** | ❌ 无 `Modifier.animateShape`，只有 `internal` 实现 | 自定义 morph 只能走 `MaterialShapes` + 手动驱动 `progress` |
| 5 | **ButtonGroup 无溢出也能用** | ⚠️ `overflowIndicator` 必填 | 需要 `ButtonGroup(overflowIndicator = {})` 这种噪音写法 |
| 6 | **浮动工具栏有 elevation 分层** | ⚠️ 源码硬编码 `ElevationTokens.Level0` + `// TODO read from token` | 视觉上是纯平面，得手动传 elevation |
| 7 | **LoadingIndicator 是"标准组件"** | ⚠️ 仍需 `@ExperimentalMaterial3ExpressiveApi` | 它是 Expressive 的招牌组件，却仍是实验性 API |
| 8 | **卡片有 expressive 形状档** | ⚠️ `Card` 本身不变，靠主题的 `largeIncreased`/`extraLargeIncreased`/`extraExtraLarge` 生效 | 想只给某些卡片更大圆角要显式传 `shape`（但 `AGENTS.md` 禁止给卡片传 `shape` → 两者冲突，见 §7） |
| 9 | **Motorola/官方示例的 `MaterialShapes` 直接当图标用** | ⚠️ 需 opt-in 且要自己 `toShape()`/`toPath()` | 可用但入门有摩擦 |
| 10 | **Expressive 的中文字体** | ❌ Roboto Flex 无 CJK 子集 | 中文 UI 无法整体换字体（见 §5.1） |

另外两条"规范说 A、代码是 B"的实测差异，容易误判成回归：

- **`LargeFlexibleTopAppBar` 无副标题 = 120dp < `LargeTopAppBar` = 152dp**。换组件会变矮。
- **`FlexibleBottomAppBar` 不传 `scrollBehavior` 时是固定 64dp**，不会随滚动增高。

---

## 7. 风险与建议

### 风险

1. **alpha 会漂移**。`1.5.0-alpha29` 之后已有 `1.5.0-beta01`。实测两个例子：
   - `DropdownMenuItemChecked` / `DropdownMenuItemSelected` 在 androidx-main 里**已被标 `@Deprecated`**
     并仍带 opt-in（`current.txt` 第 2510/2511 行）——也就是说 alpha29 里的这两个 Expressive 重载
     **已经进入废弃通道**。谁现在用它们，升级时就要改。
   - `SliderDefaults.Thumb` 在 androidx-main 里同样有一份 `@Deprecated @ExperimentalMaterial3ExpressiveApi`
     重载（第 3668 行）。
   → **结论：不要把 alpha29 的 `@ExperimentalMaterial3ExpressiveApi` API 写进核心路径。**
   项目目前只用了 `LoadingIndicator`（11 个文件）+ `LinearWavyProgressIndicator`（稳定）+ Emphasized 字号（稳定），
   风险面已经很小，**不要扩大**。

2. **`minCompileSdk=37` 是硬门槛**。AAR metadata 明写 `minCompileSdk=37` +
   `minAndroidGradlePluginVersion=9.1.0`。项目已经 `compileSdk=37`、`compileSdkMinor=1`、AGP 9.3.0，
   满足。但这条会挡住任何想降 `compileSdk` 的尝试。

3. **图标字体化的体积陷阱**。Material Symbols 全量 10–14 MB，比 `material-icons-extended` 的
   R8 后产物更大。若真要做，必须自建子集化。

4. **Roboto Flex 会打断中文排版**。见 §5.1。

### 建议（按性价比排序）

1. **清掉 2 个多余的 opt-in**。`PlayerScreen.kt` 与 `MiniPlayer.kt` 的
   `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` 已经没有必要——这两个文件只用了
   `LinearWavyProgressIndicator`（stable）和 Emphasized 字号（stable），没有 `LoadingIndicator`、
   没有 `MaterialShapes`。删掉可以验证"转正"这个结论，也减少以后 alpha 漂移时的噪音。
   （其余 11 个文件确实在用 `LoadingIndicator`，opt-in 必须保留。）

2. **优先落地这些"零 opt-in 成本"的 Expressive 组件**（全部 stable，可直接用）：
   `MediumFlexibleTopAppBar` / `LargeFlexibleTopAppBar`（带 subtitle 的 136/152dp 是真正的
   Expressive moment）、`FloatingActionButtonMenu`（播放页的动作簇）、
   `HorizontalFloatingToolbar`（播放页运输控件）、`ButtonGroup`（详情页动作簇）、
   `ShortNavigationBar`（如果愿意接受隐藏未选中文字）、三种 Carousel（首页/专辑横滑）。

3. **`ButtonGroup` + `SplitButtonLayout` 是详情页"动作簇"的最佳抓手**，因为它们正好是
   `docs/design/expressive-audit.md` 六条验收标记里第 3 条（按下形变）和第 5 条（Expressive 组件）的交集，
   而且**完全不需要 opt-in**。落地时记得：
   - `ButtonGroup(overflowIndicator = {})`（必填参数给空实现）
   - 连通组形状要手调 `ButtonGroupDefaults.connectedLeadingButtonShapes()` 等
   - SplitButton 形状要手调 `SplitButtonDefaults.leadingButtonShapesFor(height)`

4. **不要为了"Expressive"去全局换字体**。`SonFontFamily.Brand/Plain = FontFamily.Default` 加上
   已有的 15 个 Emphasized 样式，已经拿到了 Expressive 排版的全部收益；Roboto Flex 只可能用在
   纯拉丁 hero 位。真要试，用 `res/font/roboto_flex.ttf`（1.7 MB）+ `FontVariation.Settings` 限定到
   单个 `Text`，不要动 `Typography`。

5. **`SonTypography` 的 15 行 Emphasized 可以删**（见 §4：与 `Typography()` 默认值逐项同值）。
   但这是纯清理，优先级低——除非同时要把 Brand 族换成 Roboto Flex。

6. **上 beta01 前先跑一次 API diff**。已知至少 3 个 opt-in API 在 main 上已废弃
   （两个 `DropdownMenuItem` 重载、`SliderDefaults.Thumb` 的一个重载）。项目当前没用它们，所以
   升级到 `1.5.0-beta01` 大概率是"只差 `LoadingIndicator` 签名"这种小改动，但要用
   `javap` / `current.txt` 复核，不要凭 release notes 猜。

---

## 附录 A：本地验证

### A.1 artifact 绝对路径

主 artifact（用于 `classes.jar` 与 AAR metadata）：

```
E:\AndroidDev\Gradle\caches\modules-2\files-2.1\androidx.compose.material3\material3-android\1.5.0-alpha29\473974471875087c1b82fd3112636010add0e7f4\material3.aar
```

sources JAR（用于读取真实 Kotlin 源码与 KDoc）：

```
E:\AndroidDev\Gradle\caches\modules-2\files-2.1\androidx.compose.material3\material3-android\1.5.0-alpha29\df6e49e7caf47f85655de1d8ec0379778eeeae4e\material3-android-1.5.0-alpha29-sources.jar
```

> `GRADLE_USER_HOME` 在本机被设为 `E:\AndroidDev\Gradle`（不是默认的 `~/.gradle`），
> 所以路径不在 `C:\Users\cycli\.gradle` 下。

### A.2 AAR 结构

| 条目 | 大小 | 说明 |
| --- | --- | --- |
| `classes.jar` | 4,739,870 B（4.52 MB） | 全部字节码 |
| `material3.aar` 整体 | 4.61 MB | — |
| `material3-android-1.5.0-alpha29-sources.jar` | 0.86 MB / 280 个 `.kt` | — |
| `lint.jar` | 67,052 B | — |
| `public.txt` | 0 字节 | ⚠️ 空的，不能当 API 清单用 |
| `META-INF/aar-metadata.properties` | — | `minCompileSdk=37`、`minAndroidGradlePluginVersion=9.1.0` |

`classes.jar` 共 **1303** 个 `.class`，其中 `androidx/compose/material3/` 根包 **941** 个，
子包：`carousel` 43、`internal` 177、`pulltorefresh` 17、`tokens` 125。

### A.3 关键类条目（`jar tf` 实测）

**已在 artifact 中找到**：

```
androidx/compose/material3/FloatingActionButtonMenuKt.class
androidx/compose/material3/FloatingActionButtonMenuKt$FloatingActionButtonMenu$1$1.class
androidx/compose/material3/FloatingActionButtonMenuKt$FloatingActionButtonMenuItem$1$2$1$1$1$1.class
androidx/compose/material3/FloatingActionButtonMenuKt$ToggleFloatingActionButton$3$4$scope$1$1.class
androidx/compose/material3/FloatingActionButtonMenuScope.class
androidx/compose/material3/ToggleFloatingActionButtonDefaults.class
androidx/compose/material3/ToggleFloatingActionButtonDefaults$containerSize$1$1.class
androidx/compose/material3/ToggleFloatingActionButtonScope.class

androidx/compose/material3/ButtonGroupKt.class
androidx/compose/material3/ButtonGroupKt$waitUntil$1.class
androidx/compose/material3/ButtonGroupDefaults.class
androidx/compose/material3/ButtonGroupScope.class
androidx/compose/material3/ButtonGroupScopeImpl.class
androidx/compose/material3/ButtonGroupMenuState.class
androidx/compose/material3/ButtonGroupMeasurePolicy.class
androidx/compose/material3/ButtonGroupOverflowState.class
androidx/compose/material3/ButtonGroupItem.class
androidx/compose/material3/ButtonGroupItemProvider.class
androidx/compose/material3/ButtonGroupElement.class
androidx/compose/material3/ButtonGroupNode.class
androidx/compose/material3/ButtonGroupParentData.class
androidx/compose/material3/ClickableButtonGroupItem.class
androidx/compose/material3/ToggleableButtonGroupItem.class
androidx/compose/material3/CustomButtonGroupItem.class
androidx/compose/material3/NonAdaptiveButtonGroupMeasurePolicy.class

androidx/compose/material3/SplitButtonKt.class
androidx/compose/material3/SplitButtonKt$SplitButtonLayout$2$1.class
androidx/compose/material3/SplitButtonDefaults.class
androidx/compose/material3/SplitButtonShapes.class

androidx/compose/material3/FloatingToolbarKt.class
androidx/compose/material3/FloatingToolbarKt$settleFloatingToolbar$1.class
androidx/compose/material3/FloatingToolbarKt$HorizontalFloatingToolbarWithFabLayout-z3vpotQ$$inlined$animateFloat$1.class
androidx/compose/material3/FloatingToolbarKt$VerticalFloatingToolbarWithFabLayout-Nur2B3k$$inlined$animateFloat$1.class
androidx/compose/material3/FloatingToolbarDefaults.class
androidx/compose/material3/FloatingToolbarDefaults$VerticalNestedScrollExpansionElement.class
androidx/compose/material3/FloatingToolbarDefaults$VerticalNestedScrollExpansionNode.class
androidx/compose/material3/FloatingToolbarColors.class
androidx/compose/material3/FloatingToolbarExitDirection.class
androidx/compose/material3/FloatingToolbarHorizontalFabPosition.class
androidx/compose/material3/FloatingToolbarVerticalFabPosition.class
androidx/compose/material3/FloatingToolbarScrollBehavior.class
androidx/compose/material3/FloatingToolbarState.class
androidx/compose/material3/FloatingToolbarStateImpl.class
androidx/compose/material3/ExitAlwaysFloatingToolbarScrollBehavior.class

androidx/compose/material3/AppBarKt.class
androidx/compose/material3/AppBarKt$TwoRowsTopAppBar$11$1.class
androidx/compose/material3/AppBarKt$BottomAppBarLayout$appBarDragModifier$2$1.class
androidx/compose/material3/TopAppBarDefaults.class
androidx/compose/material3/TopAppBarScrollBehavior.class
androidx/compose/material3/BottomAppBarDefaults.class
androidx/compose/material3/BottomAppBarScrollBehavior.class
androidx/compose/material3/BottomAppBarState.class
androidx/compose/material3/AppBarColumnKt.class
androidx/compose/material3/AppBarRowKt.class
androidx/compose/material3/AppBarDslKt.class
androidx/compose/material3/AppBarScope.class
androidx/compose/material3/AppBarColumnScope.class
androidx/compose/material3/AppBarRowScope.class
androidx/compose/material3/AppBarItem.class
androidx/compose/material3/AppBarMenuState.class
androidx/compose/material3/AppBarWithSearchColors.class
androidx/compose/material3/AppBarWithSearchProperties.class
androidx/compose/material3/AppBarWithSearchStyle.class

androidx/compose/material3/ShortNavigationBarKt.class
androidx/compose/material3/ShortNavigationBarDefaults.class
androidx/compose/material3/ShortNavigationBarItemDefaults.class
androidx/compose/material3/ShortNavigationBarArrangement.class
androidx/compose/material3/ShortNavigationBarArrangement$Companion.class

androidx/compose/material3/LoadingIndicatorKt.class
androidx/compose/material3/LoadingIndicatorKt$LoadingIndicatorImpl$6$1$morphAnimationBlock$1$1.class
androidx/compose/material3/LoadingIndicatorKt$LoadingIndicatorImpl$6$1$rotationAnimationBlock$1$1.class
androidx/compose/material3/LoadingIndicatorDefaults.class

androidx/compose/material3/WavyProgressIndicatorKt.class
androidx/compose/material3/WavyProgressIndicatorDefaults.class

androidx/compose/material3/carousel/CarouselKt.class        (子包!)
androidx/compose/material3/carousel/CarouselState.class
androidx/compose/material3/carousel/CarouselItemScope.class
androidx/compose/material3/carousel/CarouselParallaxScrollEffect.class

androidx/compose/material3/MaterialShapes.class
androidx/compose/material3/MaterialShapes$Companion.class
androidx/compose/material3/MaterialShapes$Companion$PointNRound.class
androidx/compose/material3/MaterialShapesKt.class
androidx/compose/material3/MaterialShapesKt$toShape$1$1.class

androidx/compose/material3/MotionScheme.class
androidx/compose/material3/MotionScheme$Companion.class
androidx/compose/material3/MotionScheme$ExpressiveMotionSchemeImpl.class
androidx/compose/material3/MotionScheme$StandardMotionSchemeImpl.class
androidx/compose/material3/MotionSchemeKt.class

androidx/compose/material3/ExperimentalMaterial3ExpressiveApi.class
androidx/compose/material3/Shapes.class
androidx/compose/material3/ShapeDefaults.class
androidx/compose/material3/Typography.class
androidx/compose/material3/ButtonShapes.class
androidx/compose/material3/ToggleButtonShapes.class
androidx/compose/material3/ListItemShapes.class
androidx/compose/material3/IconButtonShapes.class
androidx/compose/material3/IconToggleButtonShapes.class
androidx/compose/material3/ChipShapes.class
androidx/compose/material3/MenuGroupShapes.class
androidx/compose/material3/MenuItemShapes.class
androidx/compose/material3/DragHandleShapes.class
androidx/compose/material3/TimePickerShapes.class
```

**确认不存在**（全 artifact 源码 + 字节码 + `current.txt` 三方 grep 均 0 命中）：

```
ToggleButtonGroup              ← 不存在（只有单控件 ToggleButton）
VerticalCarousel               ← 不存在（只有三种 Horizontal*）
Modifier.animateShape          ← 不存在（只有 internal/AnimatedShape.kt 的 animateToShape）
SplitButton（作为 composable）  ← 不存在（只有 SplitButtonLayout + SplitButtonDefaults.*Button）
```

### A.4 opt-in 的字节码实证（`javap -v -p`，统计 `ExperimentalMaterial3ExpressiveApi` 出现次数）

| 类 | expressive 引用次数 | 结论 |
| --- | --- | --- |
| `AppBarKt` | **0** | Flexible top/bottom app bar 全部 stable |
| `ButtonGroupKt` | **0** | stable |
| `SplitButtonKt` | **0** | stable |
| `FloatingActionButtonMenuKt` | **0** | stable |
| `FloatingToolbarKt` | **0** | stable |
| `ShortNavigationBarKt` | **0** | stable |
| `WavyProgressIndicatorKt` | **0** | stable |
| `carousel.CarouselKt` | **0** | stable |
| `MotionScheme` | **0** | `expressive()` stable |
| `Typography` | **0** | Emphasized 全部 stable |
| `MaterialThemeKt` | **0** | `MaterialExpressiveTheme` stable |
| `MaterialShapes` | **2** | ⚠️ 需要 opt-in |
| `LoadingIndicatorKt` | **5** | ⚠️ 需要 opt-in |

> `public.txt` 是 0 字节，不可用；以上以 `classes.jar` + sources + `javap` 为准。

### A.5 交叉验证（androidx-main）

`androidx-main` 的公开 API 清单
[`compose/material3/material3/api/current.txt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/api/current.txt)
（本地副本 997,596 B / 5,383 行）里，`ExperimentalMaterial3ExpressiveApi` 只出现在 **27** 处，集中在：

- `LoadingIndicatorDefaults` + `LoadingIndicator` / `ContainedLoadingIndicator`（第 2149、2172–2179 行）
- `MaterialShapes` + `Companion` + `toPath` / `toShape`（第 2182–2265 行）
- `DropdownMenuItemChecked` / `DropdownMenuItemSelected`（第 2510–2511 行，**已 `@Deprecated`**）
- `SliderDefaults.Thumb` 的一个重载（第 3668 行，**已 `@Deprecated`**）
- `PullToRefreshDefaults` 的 loading 相关成员（第 5333–5355 行）

与本地 alpha29 的字节码/源码结论**逐项吻合**（🔁 交叉验证）。差别只在于 androidx-main 已经把
两个 `DropdownMenuItem` 重载和 `SliderDefaults.Thumb` 标成 `@Deprecated` —— 这是 alpha29 之后才发生的，
正好说明这批 opt-in API 正在被重构。

`current.txt` 里同样确认：`animateShape` **0 命中**、`VerticalCarousel` **0 命中**、
`SplitButtonKt` 只有 `SplitButtonLayout`。

### A.6 其它本地引用

| 用途 | 路径 |
| --- | --- |
| 版本钉定 | `gradle/libs.versions.toml:10,14` |
| 编译配置 | `presentation/build.gradle.kts:14-15`（`compileSdk=37`, `compileSdkMinor=1`） |
| 主题装配 | `presentation/src/main/java/com/cycling/core/ui/theme/Theme.kt:65-71`（`MaterialExpressiveTheme` + `MotionScheme.expressive()`） |
| 形状标尺 | `presentation/src/main/java/com/cycling/core/ui/theme/Shape.kt:15-24` |
| 动效 token | `presentation/src/main/java/com/cycling/core/ui/theme/Motion.kt:21-32` |
| 排版 | `presentation/src/main/java/com/cycling/core/ui/theme/Typography.kt:52-71` |
| 现有 opt-in 分布 | **13** 个 `*.kt` 带 `@file:OptIn(…ExperimentalMaterial3ExpressiveApi…)`；其中 11 个确实在用 `LoadingIndicator`，**2 个（`PlayerScreen.kt`、`MiniPlayer.kt`）已多余** |
| 现有 Expressive 组件使用 | `LoadingIndicator` 11 个文件、`LinearWavyProgressIndicator` 4 个文件、`*Emphasized` 字号 30+ 处 |
| 未使用 | `ShortNavigationBar`、`ButtonGroup`、`SplitButton`、`FloatingToolbar`、`FloatingActionButtonMenu`、`Carousel`、`MaterialShapes`、`FlexibleTopAppBar`、`FlexibleBottomAppBar` |

### A.7 参考链接

| 来源 | URL | 本盘点的用法 |
| --- | --- | --- |
| Google Maven 元数据 | https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/maven-metadata.xml | 确认 alpha29 存在、beta01 已发布 |
| androidx-main API 清单 | https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/api/current.txt | 🔁 交叉验证 opt-in 范围 |
| M3 Expressive 公告 | https://m3.material.io/blog/building-with-m3-expressive | 规范方向（📄，站点为 SPA，正文无法机读） |
| M3 Shape | https://m3.material.io/styles/shape/shape-morph | shape morph 规范（📄，同上） |
| M3 Loading indicator | https://m3.material.io/components/loading-indicator/overview | LoadingIndicator 规范（📄，同上） |
| Compose Material3 发布说明 | https://developer.android.com/jetpack/androidx/releases/compose-material3 | ⚠️ **本环境无法访问**（`fetch failed`），未能直接引用 alpha29 release notes；结论改由 artifact 本身 + Maven 元数据 + `current.txt` 三方支撑 |
| Roboto Flex 字体元数据 | https://raw.githubusercontent.com/google/fonts/main/ofl/robotoflex/METADATA.pb | OFL 许可、13 轴、无 CJK 子集 |
| Material Symbols 许可 | https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE | Apache 2.0 |

> ⚠️ **明确标注的不确定性**：`developer.android.com` 在本执行环境网络不可达（多次 `fetch failed`），
> `fonts.google.com` 同样不可达。因此**本盘点没有引用 alpha29 的官方 release notes 原文**，
> 所有"已转正/仍需 opt-in"的结论都建立在**本地 AAR 字节码 + 源码**与 **androidx-main `current.txt`**
> 这两个可独立验证的硬证据上，而不是发布说明。这对结论强度影响很小（artifact 就是最终事实），
> 但如果需要引用官方"哪一版把 ButtonGroup 转正"的**版本号说法**，需要在能访问 developer.android.com
> 的环境里补一次。
