package com.cycling.core.ui.apple

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.presentation.R

/**
 * Apple Music 设计令牌。
 *
 * 全部数值来自 com.apple.android.music 7.0.0-beta (build 1611) 的资源表，逐条标注来源：
 * 颜色取 res/values/colors.xml 的语义色（已按 day/night 配置解析到底），
 * 字号取 res/values/styles.xml 的文本样式，尺寸取 dimens.xml。
 *
 * 这是 AGENTS.md 强制规则的一处**显式例外**：Apple 观感要求独立色板与非 Material 字体，
 * 无法用 MaterialTheme.colorScheme / typography 表达。例外范围限于本包与引用它的页面。
 */
object AppleTheme {

    val colors: AppleColors
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) AppleColors.Dark else AppleColors.Light

    val type: AppleType get() = AppleType

    val dims: AppleDims get() = AppleDims
}

/** iOS 语义色板（`label / secondaryLabel / separator / systemBackground` 体系）。 */
@Immutable
data class AppleColors(
    /** color/background_color */
    val background: Color,
    /** color/background_color_layer1 */
    val backgroundLayer1: Color,
    /** color/secondary_background_color */
    val secondaryBackground: Color,
    /** color/tertiary_background_color */
    val tertiaryBackground: Color,
    /** color/label_color */
    val label: Color,
    /** color/secondary_label_color */
    val secondaryLabel: Color,
    /** color/tertiary_label_color */
    val tertiaryLabel: Color,
    /** color/quaternary_label_color */
    val quaternaryLabel: Color,
    /** color/separator_color */
    val separator: Color,
    /** color/translucent_separator_color */
    val translucentSeparator: Color,
    /** color/system_red */
    val systemRed: Color,
    /** color/key_color —— Apple Music 的强调蓝 */
    val key: Color,
    /** color/tertiary_fill_color */
    val fill: Color,
    /** color/secondary_fill_color */
    val secondaryFill: Color,
    /** color/apple_material_background —— 毛玻璃材质的实色替身 */
    val material: Color,
    /** color/apple_material_stroke —— 材质描边，0.5dp */
    val materialStroke: Color,
    /**
     * 玻璃材质：iOS `systemChromeMaterial` 的等效色（半透明 + 底噪）。
     *
     * Android 版 Apple Music 的底栏是实色（`background_color_layer1`），玻璃是 iOS 的做法；
     * 这里按 iOS 还原。没有真实背景模糊时 alpha 不能压得太低，否则下层文字会透上来糊成一片。
     */
    val glass: Color
) {
    companion object {
        /** 数值取自 colors.xml 的 default 配置。 */
        val Light = AppleColors(
            background = Color(0xFFFFFFFF),
            backgroundLayer1 = Color(0xFFFFFFFF),
            secondaryBackground = Color(0xFFF2F2F7),
            tertiaryBackground = Color(0xFFFFFFFF),
            label = Color(0xFF000000),
            secondaryLabel = Color(0x993C3C43),
            tertiaryLabel = Color(0x4D3C3C43),
            quaternaryLabel = Color(0x2E3C3C43),
            separator = Color(0xFFC6C6C8),
            translucentSeparator = Color(0x4A3C3C43),
            systemRed = Color(0xFFFF3B30),
            key = Color(0xFF0088FF),
            fill = Color(0x1F767680),
            secondaryFill = Color(0x29787880),
            material = Color(0xFFF5F5F7),
            materialStroke = Color(0x1A000000),
            glass = Color(0xD9F2F2F7)
        )

        /** 数值取自 colors.xml 的 night 配置。 */
        val Dark = AppleColors(
            background = Color(0xFF000000),
            backgroundLayer1 = Color(0xFF1C1C1E),
            secondaryBackground = Color(0xFF1C1C1E),
            tertiaryBackground = Color(0xFF2C2C2E),
            label = Color(0xFFFFFFFF),
            secondaryLabel = Color(0x99EBEBF5),
            tertiaryLabel = Color(0x4DEBEBF5),
            quaternaryLabel = Color(0x29EBEBF5),
            separator = Color(0xFF38383A),
            translucentSeparator = Color(0xA6545458),
            systemRed = Color(0xFFFF3B30),
            key = Color(0xFF0091FF),
            fill = Color(0x3D767680),
            secondaryFill = Color(0x52787880),
            material = Color(0xFF262628),
            materialStroke = Color(0x33FFFFFF),
            glass = Color(0xD9262628)
        )
    }
}

/**
 * SF Pro 变量字体（res/font/sf_pro_android_ui.ttf）。
 *
 * Apple 的做法是同一个可变字体文件配 77 个字体族 XML，用 fontVariationSettings 的 'wght'
 * 轴切字重。这里同样用 XML（apple_regular/medium/semibold/bold）承载变体设置，
 * 代码侧只做常规的按字重取字族。CJK 无字形，回落系统字体 —— Apple Music 也是如此。
 */
val AppleFontFamily: FontFamily = FontFamily(
    Font(R.font.apple_regular, FontWeight.Normal),
    Font(R.font.apple_medium, FontWeight.Medium),
    Font(R.font.apple_semibold, FontWeight.SemiBold),
    Font(R.font.apple_bold, FontWeight.Bold)
)

/**
 * iOS 字阶。字号取自 styles.xml（Title0 32 / Title1 28 / Title2 20 / ... / Caption2 11），
 * 行高与字距 Apple 未写进资源，按 iOS HIG 的对应值补齐。
 */
object AppleType {

    /** SystemFont40dp：34sp Bold */
    val largeTitle = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 34.sp, lineHeight = 41.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp
    )

    /** Title0：32sp Bold */
    val title0 = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 32.sp, lineHeight = 39.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp
    )

    /** Title1：28sp */
    val title1 = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 28.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp
    )

    /** Title2ShortEmphasized：22sp Bold */
    val title2Emphasized = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 22.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp
    )

    /** MusicTitle2：20sp Bold —— 分区标题用的就是这一档 */
    val sectionTitle = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 20.sp, lineHeight = 25.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp
    )

    /** Title2：20sp */
    val title2 = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 20.sp, lineHeight = 25.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp
    )

    /** Title2Medium：20sp Medium —— 合集页头部标题用的就是这一档（style/Title2Medium） */
    val title2Medium = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 20.sp, lineHeight = 25.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp
    )

    /** Headline：17sp Medium */
    val headline = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 17.sp, lineHeight = 22.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp
    )

    /** Body：17sp */
    val body = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 17.sp, lineHeight = 22.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp
    )

    /** Subhead：15sp */
    val subhead = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp
    )

    /** SubheadEmphasized：15sp Medium */
    val subheadEmphasized = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp
    )

    /** Footnote：13sp */
    val footnote = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    )

    /** FootnoteEmphasized：13sp Medium */
    val footnoteEmphasized = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    )

    /** Caption1：12sp */
    val caption1 = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    )

    /** Caption1Emphasized：12sp Medium —— 迷你播放器标题用的就是这一档 */
    val caption1Emphasized = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )

    /** Caption2：11sp */
    val caption2 = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
        fontWeight = FontWeight.Normal
    )

    /** Caption2Emphasized：11sp Medium —— 合集页头部的「42 首歌曲 · 2 小时」元数据行 */
    val caption2Emphasized = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
        fontWeight = FontWeight.Medium
    )

    /** ButtonB：18sp Medium —— button_b 的文本样式（parent SubheadEmphasized 覆写 textSize） */
    val button = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 18.sp, lineHeight = 23.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp
    )

    /** 设置项标题：`item_preference_title_description` 实测 16sp */
    val settingsTitle = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 16.sp, lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    )

    /** 设置项副标题：`item_preference_switch` 实测 14sp */
    val settingsSubtitle = TextStyle(
        fontFamily = AppleFontFamily, fontSize = 14.sp, lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    )
}

/** 尺寸令牌，单位 dp，数值取自 dimens.xml。 */
object AppleDims {

    // 圆角：corner_radius_xs/s/m/l/xl/xxl/pill
    val cornerXs = 3.dp
    val cornerS = 5.dp
    val cornerM = 7.dp
    val cornerL = 10.dp
    val cornerXl = 14.dp
    val cornerXxl = 24.dp
    val cornerPill = 80.dp

    /** default_padding */
    val padding = 16.dp

    /** default_top_margin */
    val topMargin = 32.dp

    /** default_list_item_height */
    val listItemHeight = 48.dp

    /** large_list_image_height —— 资料库大列表行与封面同高 */
    val largeListImage = 88.dp

    /** dynamic_grid_item_spacing_gutter */
    val gridGutter = 10.dp

    /** dynamic_grid_peek —— 横滑露出的下一张卡宽度 */
    val gridPeek = 36.dp

    /** 分区标题上下留白，取自 header_section_a 的 paddingTop/paddingBottom */
    val sectionTitleTop = 13.dp
    val sectionTitleBottom = 10.dp

    /** 菜单行上下留白与末端留白，取自 large_list_a_item */
    val menuRowVertical = 13.dp
    val menuRowEnd = 20.dp

    /** default_padding —— 菜单行图标与文字间距 */
    val menuRowGap = 16.dp

    /** list_action_icon_width */
    val trailingIcon = 26.dp

    /** library_section_list_bottom_margin */
    val sectionListBottom = 20.dp

    // ── 迷你播放器（mini_player.xml + 相关 dimens） ──────────────────

    /** miniplayer_height */
    val miniPlayerHeight = 48.dp

    /** miniplayer_to_navtabs_spacing */
    val miniPlayerGap = 8.dp

    /** mini_player_thumbnail_height */
    val miniPlayerThumbnail = 30.dp

    /** mini_player_button_size */
    val miniPlayerButton = 32.dp

    /** mini_player_horizontal_margin（endMargin） */
    val miniPlayerMargin = 16.dp

    /** apple_material_bg_stroke */
    val hairline = 0.5.dp

    /** search_view_background_height */
    val searchFieldHeight = 48.dp

    /** 详情页动作条胶囊按钮高度 */
    val actionHeight = 40.dp

    // ── 合集页头部（epoxy_layout_collection_page_uber_header） ──────────

    /** header_non_uber_cover_art_margin_horiz —— 封面左右留白，封面边长 = 屏宽 − 144dp */
    val headerCoverInset = 72.dp

    /**
     * 合集页头部顶部的 `Space`：`layout_height=?attr/actionBarSize`。
     * 它不是状态栏占位 —— 顶栏（返回 / 分享 / 更多）是压在这条带上，封面从它**下面**开始。
     */
    val headerToolbarSpace = 56.dp

    /** header_non_uber_cover_art_margin_top */
    val headerCoverTop = 10.dp

    /** header_non_uber_cover_art_shadow_elev */
    val headerCoverShadow = 10.dp

    /** header_non_uber_title_margin_top —— 封面底沿到标题 */
    val headerTitleTop = 15.dp

    /** smaller_margin_4 —— 副标题到元数据行 */
    val headerMetaTop = 4.dp

    /** button_b 的文本行高，也即胶囊按钮高度（44dp） */
    val headerButtonHeight = 44.dp

    /** button_b 组内间隔 */
    val headerButtonGap = 8.dp

    // ── 底部 Tab 条（bottom_navigation.xml） ────────────────────────

    /** Material BottomNavigationView 默认内容高度 */
    val tabBarHeight = 56.dp
}
