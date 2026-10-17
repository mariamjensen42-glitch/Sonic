package com.cycling.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 间距标尺。基于 4dp 栅格；页面级间距只允许从这里取值。
 */
object SonSpacing {
    /** 4dp —— 图标与相邻文字、紧凑标签对。 */
    val extraSmall = 4.dp
    /** 8dp —— 组件内部元素、列表项内图标与文本。 */
    val small = 8.dp
    /** 12dp —— 列表项纵向内边距、卡片内部紧凑间距。 */
    val mediumSmall = 12.dp
    /** 16dp —— 屏幕左右安全边距（紧凑窗口）、卡片内边距。 */
    val medium = 16.dp
    /** 24dp —— 区块之间、屏幕左右安全边距（中大型窗口）。 */
    val large = 24.dp
    /** 32dp —— 页面主要区块分割。 */
    val extraLarge = 32.dp
    /** 48dp —— 空态与英雄区块的呼吸空间。 */
    val extraExtraLarge = 48.dp
    /** 64dp —— 大面积留白（播放页封面与信息之间）。 */
    val huge = 64.dp
}

/**
 * 布局常量，语义化命名，避免在页面里写魔法数字。
 */
object SonLayout {
    /** 屏幕水平安全边距：紧凑窗口。 */
    val screenMargin = 16.dp

    /** 屏幕水平安全边距：宽窗口 / 平板。 */
    val screenMarginWide = 24.dp

    /** 列表底部额外留白，避免被底部导航与迷你播放器遮挡。 */
    val bottomBarClearance = 96.dp

    /** 迷你播放器存在时的额外底部留白。 */
    val miniPlayerClearance = 64.dp

    /** 网格间距（专辑/歌手卡片）。 */
    val gridGap = 12.dp

    /** 卡片之间的纵向间距。 */
    val cardGap = 16.dp

    /** 页面首个区块距顶部的距离（App Bar 之下）。 */
    val contentTopPadding = 8.dp

    /** 区块标题与内容间距。 */
    val sectionTitleGap = 12.dp

    /** 纵向列表最大内容宽度（宽屏居中）。 */
    val maxContentWidth = 720.dp
}

/**
 * 组件尺寸，取自 Material 3 组件 token。
 */
object SonSizes {

    /** 最小触摸目标。 */
    val minTouchTarget = 48.dp

    // ── 按钮高度（XSmall → XLarge） ─────────────────────────────────
    val buttonXSmallHeight = 32.dp
    val buttonSmallHeight = 40.dp
    val buttonMediumHeight = 56.dp
    val buttonLargeHeight = 96.dp
    val buttonXLargeHeight = 136.dp

    /** 按钮内图标。 */
    val buttonIcon = 18.dp

    /** 通用图标档位。 */
    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconExtraLarge = 40.dp

    // ── FAB ────────────────────────────────────────────────────────
    val fabSmall = 40.dp
    val fab = 56.dp
    val fabLarge = 96.dp
    val fabIcon = 24.dp
    val fabLargeIcon = 36.dp

    // ── 导航 ───────────────────────────────────────────────────────
    /** 导航栏指示器：宽 64dp、高 32dp。 */
    val navigationIndicatorWidth = 64.dp
    val navigationIndicatorHeight = 32.dp
    /** 导航栏图标。 */
    val navigationIcon = 24.dp
    /** 导航栏最小高度。 */
    val shortNavigationBarHeight = 64.dp

    // ── 列表 ───────────────────────────────────────────────────────
    /** 列表项单行最小高度。 */
    val listItemMinHeight = 56.dp
    /** 列表项双行最小高度。 */
    val listItemTwoLineHeight = 72.dp
    /** 列表项左右内边距。 */
    val listItemHorizontalPadding = 16.dp
    /** 列表项纵向内边距。 */
    val listItemVerticalPadding = 12.dp

    // ── 媒体 ───────────────────────────────────────────────────────
    /** 列表中的封面缩略图。 */
    val thumbnail = 48.dp
    /** 列表中的大封面缩略图。 */
    val thumbnailLarge = 56.dp
    /** 顶部应用栏内的封面缩略图（折叠头部的收拢态）。 */
    val appBarArtwork = 32.dp
    /** 媒体卡宽度。 */
    val mediaCardWidth = 160.dp
    /** 歌手圆形头像直径。 */
    val artistCardSize = 104.dp
    /** 详情页头部封面边长。四个详情页共用同一档，保证跨页版式一致。 */
    val headerArtwork = 240.dp
}
