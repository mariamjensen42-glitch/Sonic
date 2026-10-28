@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleGlassCircleButton
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.applePress
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonColorFieldBackground
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonShapeMorph
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.sonColorFieldDetailFade
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.collectiondetail.CollectionDetailEffect
import com.cycling.presentation.collectiondetail.CollectionDetailUiState
import com.cycling.presentation.collectiondetail.SongCollectionIntent
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

/** 顶部 scrim 浓度：只用来托住状态栏图标，压太重会显脏。 */
private const val HeroScrimTopAlpha = 0.45f

/** scrim 降到完全通透的位置：这段之下封面本体不蒙灰。 */
private const val HeroScrimClearStop = 0.42f

/** 通透区与标题区之间的回压起点，把长坡切成两段更缓的斜线。 */
private const val HeroScrimEaseStop = 0.56f

private const val HeroScrimEaseAlpha = 0.12f

/**
 * 标题区开始压暗的位置。必须远高于 [HeroScrimClearStop] ——
 * 两者之间是"通透 → 压暗"的长坡，坡太短就会出现可见的硬边（亮度断崖）。
 */
private const val HeroTitleZoneStart = 0.72f

/** 标题区起始浓度，之后一路加深到 [SonExtendedColors.artworkScrim]。 */
private const val HeroScrimTitleAlpha = 0.42f

/** hero 标题在折叠前 45% 淡出；顶栏标题从同一进度接手。 */
private const val TitleFadeEnd = 0.45f

/** hero cover 折叠后的尺寸比例与上移比例；顶栏缩略图继续用原有 crossfade。 */
private const val HeroFoldEndScale = 0.72f
private const val HeroFoldEndShift = 0.14f

/** 顶栏标题（含缩略图）开始显现的折叠进度。 */
private const val HeaderRevealStart = TitleFadeEnd

/** 顶栏标题完全显现的折叠进度。 */
private const val HeaderRevealEnd = 0.7f

/** 顶栏底色加速比：必须早于标题显形，否则白字会落在半透明底上。 */
private const val ContainerRampRate = 1.6f

/** 封面文字的次级层级。 */
private const val HeroSubTextAlpha = 0.88f

/** 多选操作栏的高度，用来给列表补底部留白。 */
private val SelectionBarHeight = 72.dp

/**
 * 头部版式。
 *
 * - [FullBleed]：封面满铺成方形、标题压在其底部（专辑 / 歌手详情沿用）。
 * - [CenteredCover]：整屏封面模糊背景 + MeloX 英雄区（歌单详情，见 [MeloXPlaylistHero]）。
 *
 * 两种版式的折叠进度都按头部真实高度归一，所以外壳其余部分（顶栏 crossfade、
 * 顶栏底色 band）不需要区分版本。
 */
enum class CollectionHeroStyle {
    FullBleed,
    CenteredCover
}

/** 顶栏标题的显现曲线：折叠过半起步，四分之一程内走完。 */
private fun State<Float>.headerAlpha(): Float =
    ((value - HeaderRevealStart) / (HeaderRevealEnd - HeaderRevealStart)).coerceIn(0f, 1f)

/** 顶栏底色比标题先到位，否则 [HeaderRevealStart] 附近白字会落在半透明底上。 */
private fun State<Float>.containerAlpha(): Float = (value * ContainerRampRate).coerceIn(0f, 1f)

/**
 * 歌曲集合详情页外壳：沉浸式折叠头图 + 折叠顶栏 + 动作区 + 多选 + 曲目列表。
 *
 * 歌单 / 专辑 / 歌手三个详情页共用这一页，页面差异通过参数接入：
 * [songSubtitle] / [songTrailing] / [songMoreMenu] 定制行，[headerExtra] 放
 * 介绍等头部附加内容，[onRemoveSelected] + [removeLabel] 仅歌单传入（批量移出歌单）。
 */
@Composable
fun CollectionDetailScreen(
    uiState: CollectionDetailUiState,
    titleFallback: String,
    subtitle: String,
    placeholderIcon: SymbolIconName,
    onNavigateBack: () -> Unit,
    backContentDescription: String,
    onIntent: (SongCollectionIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    /** 头部版式，见 [CollectionHeroStyle]。 */
    heroStyle: CollectionHeroStyle = CollectionHeroStyle.FullBleed,
    /** [CollectionHeroStyle.CenteredCover] 的元数据行短语，按顺序用圆点连接。 */
    headerMeta: List<String> = emptyList(),
    /**
     * 顶栏是否随滚动收起（标题 + 封面缩略图 + 底色）。歌单页传 false：
     * 顶栏换成 MeloX 的 58dp 工具条，不收起、也不显示大标题。
     */
    topBarCollapse: Boolean = true,
    /** 曲目行是否显示序号。歌单页传 false，行首改显封面（MeloX 排版）。 */
    showTrackNumbers: Boolean = true,
    onRemoveSelected: (() -> Unit)? = null,
    removeLabel: String? = null,
    songSubtitle: (Song) -> String? = { null },
    songTrailing: @Composable (Song) -> Unit = {},
    songMoreMenu: (Song) -> List<MenuItem> = { emptyList() },
    /** 队列与详情动作，由外壳接线；传了之后每首歌的菜单里都有「下一首播放 / 加入队列」。 */
    queueActions: SongQueueActions? = null,
    /** 行菜单里的收藏 / 取消收藏；行尾已经有心形的页面（歌手页）不必传。 */
    onToggleFavorite: ((Song) -> Unit)? = null,
    headerExtra: @Composable (ColumnScope.() -> Unit)? = null,
    /** 曲目表之后的附加区块（歌手页的「专辑」横滑）。 */
    songsFooter: (@Composable () -> Unit)? = null,
    emptyText: String? = null,
    /** 传了才开启拖拽排序；专辑/歌手等固定顺序的列表不传，行为完全不变。 */
    onMoveSong: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    /** 传了才在顶栏出现「导出」；目前只有歌单有可导出的 M3U 语义。 */
    onExport: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // 排序模式：把手只在编辑态出现，平时列表与头部共用同一条左边线。
    var reorderMode by rememberSaveable { mutableStateOf(false) }

    val title = uiState.title ?: titleFallback
    val songs = uiState.songs
    val canReorder = onMoveSong != null && songs.size > 1
    val firstSong = songs.firstOrNull()
    val artwork = uiState.artwork

    // 多选或列表已无可排序空间时自动退出：此刻把手不再渲染，模式不能残留。
    LaunchedEffect(uiState.selectionMode, canReorder) {
        if (uiState.selectionMode || !canReorder) reorderMode = false
    }
    BackHandler(enabled = reorderMode) { reorderMode = false }

    // 整页配色跟随系统壁纸（应用级主题），这里不再按封面单独覆盖一层：
    // 封面只喂播放页 / 歌词页的动态色面，不应该顺带把整页的配色也换掉。

    // 头部边长取容器宽度（方形封面 = 宽 = 高），无需测量回调，首帧即正确。
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val heroSide = maxWidth
        // 密度必须在 derivedStateOf 之外读取：其计算块不是 composable 作用域。
        val density = LocalDensity.current
        // 拖拽落点的换算基准：曲目行是两行 ListItem，固定 72dp 高。
        val reorderRowHeightPx = with(density) { 72.dp.toPx() }

        // 顶栏底色 band 的高度：状态栏 + TopAppBar。
        val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // 折叠进度 0→1。头部整体离开视口后固定为 1，避免首项索引变化导致回弹。
        // 归一基准取头部**实测高度**：满铺 hero 恰好等于边长，居中封面版式则要高得多，
        // 若仍按边长算，后者滚过三分之一就已经「折叠完成」，顶栏标题会提前抢跑。
        var headerHeightPx by remember { mutableIntStateOf(0) }

        val collapse = remember(heroSide, density, headerHeightPx) {
            derivedStateOf {
                val span = headerHeightPx.takeIf { it > 0 }
                    ?: with(density) { heroSide.toPx() }.toInt()
                if (span <= 0) {
                    0f
                } else {
                    val offsetPx = if (listState.firstVisibleItemIndex == 0) {
                        listState.firstVisibleItemScrollOffset
                    } else {
                        span
                    }
                    (offsetPx.toFloat() / span).coerceIn(0f, 1f)
                }
            }
        }

        // 顶栏的收起进度。歌单页（topBarCollapse = false）恒为 0：不显标题与缩略图、
        // 也不铺底色 band，顶栏一直是「返回 + 动作按钮」的初始形态。
        val barCollapse: State<Float> = if (topBarCollapse) {
            collapse
        } else {
            remember { mutableStateOf(0f) }
        }

        // ── 顶栏的颜色与动作：浮层顶栏与 MeloX 工具条共用同一份，
        // 两处各写一遍必然走样。
        val extended = LocalSonExtendedColors.current
        val navigationIconColor = lerp(
            extended.onArtwork,
            MaterialTheme.colorScheme.onSurface,
            barCollapse.value
        )
        // 动作图标用品牌红（MeloX 歌单详情顶栏的做法），折叠后过渡回 onSurfaceVariant。
        val actionIconColor = lerp(
            AppleTheme.colors.systemRed,
            MaterialTheme.colorScheme.onSurfaceVariant,
            barCollapse.value
        )
        // 托底是「跟着图标明暗走的玻璃」而不是 60% 黑 scrim（见 AppleGlassCircleButton）：
        // 白图标落在任意封面上都有对比度，又不会压出一块死色。它必须先于 surface band
        // 退干净（band 在 62.5% 折叠时不透明，这里按 2 倍速率在 50% 归零）。
        val glassFade = (1f - barCollapse.value * 2f).coerceIn(0f, 1f)
        // 浮层顶栏随折叠淡出托底；歌单页（MeloX 工具条）一直是玻璃圆钮。
        val barGlassAlpha: Float = if (topBarCollapse) glassFade else 1f
        val barNavigationColor = if (topBarCollapse) navigationIconColor else AppleTheme.colors.label
        val barIconColor = if (topBarCollapse) actionIconColor else AppleTheme.colors.systemRed

        val barBackIcon = if (reorderMode || uiState.selectionMode) {
            SymbolIconName.Close
        } else {
            SymbolIconName.ArrowBack
        }
        val barBackDescription = when {
            reorderMode -> stringResource(R.string.collection_detail_exit_reorder_desc)
            uiState.selectionMode -> stringResource(R.string.collection_detail_exit_selection_desc)
            else -> backContentDescription
        }
        val onBarBack: () -> Unit = {
            when {
                reorderMode -> reorderMode = false
                uiState.selectionMode -> onIntent(SongCollectionIntent.ExitSelectionMode)
                else -> onNavigateBack()
            }
        }
        val barActions: @Composable () -> Unit = {
            val allSelected = songs.isNotEmpty() && uiState.selectedSongIds.size == songs.size
            when {
                uiState.selectionMode -> CollectionBarAction(
                    icon = SymbolIconName.SelectAll,
                    contentDescription = stringResource(
                        if (allSelected) {
                            R.string.songs_clear_selection_desc
                        } else {
                            R.string.songs_select_all_desc
                        }
                    ),
                    onClick = {
                        onIntent(
                            if (allSelected) {
                                SongCollectionIntent.ClearSelection
                            } else {
                                SongCollectionIntent.SelectAll
                            }
                        )
                    },
                    contentColor = barIconColor,
                    glassAlpha = barGlassAlpha
                )

                reorderMode -> CollectionBarAction(
                    icon = SymbolIconName.Check,
                    contentDescription = stringResource(
                        R.string.collection_detail_reorder_done_desc
                    ),
                    onClick = { reorderMode = false },
                    contentColor = barIconColor,
                    glassAlpha = barGlassAlpha
                )

                songs.isNotEmpty() -> {
                    if (onExport != null) {
                        CollectionBarAction(
                            icon = SymbolIconName.IosShare,
                            contentDescription = stringResource(R.string.playlist_detail_export_m3u),
                            onClick = onExport,
                            contentColor = barIconColor,
                            glassAlpha = barGlassAlpha
                        )
                    }
                    CollectionBarAction(
                        icon = SymbolIconName.Checklist,
                        contentDescription = stringResource(R.string.songs_select_desc),
                        onClick = { onIntent(SongCollectionIntent.EnterSelectionMode) },
                        contentColor = barIconColor,
                        glassAlpha = barGlassAlpha
                    )
                    if (canReorder) {
                        CollectionBarAction(
                            icon = SymbolIconName.SwapVert,
                            contentDescription = stringResource(
                                R.string.collection_detail_reorder_desc
                            ),
                            onClick = { reorderMode = true },
                            contentColor = barIconColor,
                            glassAlpha = barGlassAlpha
                        )
                    }
                }
            }
        }

        Scaffold(
            containerColor = AppleTheme.colors.background,
            // 内容自行处理状态栏：顶栏透明，封面必须一路铺到屏幕顶端，
            // 若沿用 Scaffold 默认的 contentWindowInsets，内容区会被下推一个状态栏高度，
            // 封面底部就会露出等高的空白带。
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (topBarCollapse) {
                    // 透明浮层顶栏（专辑 / 歌手页）：标题与缩略图随折叠淡入。
                    CenterAlignedTopAppBar(
                        title = {
                            val headerAlpha = barCollapse.headerAlpha()
                            if (headerAlpha > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    M3Artwork(
                                        artwork = artwork,
                                        placeholderIcon = placeholderIcon,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .size(SonSizes.appBarArtwork)
                                            .graphicsLayer { alpha = headerAlpha }
                                    )
                                    Spacer(modifier = Modifier.width(SonSpacing.small))
                                    Text(
                                        text = title,
                                        style = AppleType.subheadEmphasized,
                                        color = AppleTheme.colors.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            CollectionBarAction(
                                icon = barBackIcon,
                                contentDescription = barBackDescription,
                                onClick = onBarBack,
                                contentColor = barNavigationColor,
                                glassAlpha = barGlassAlpha
                            )
                        },
                        // CenterAlignedTopAppBar 的 actions 是 RowScope 作用域，
                        // 共用那份是无作用域的 @Composable () -> Unit，这里包一层。
                        actions = { barActions() },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            navigationIconContentColor = barNavigationColor,
                            titleContentColor = barNavigationColor,
                            actionIconContentColor = barIconColor
                        )
                    )
                } else {
                    // 歌单页工具条（MeloX `MeloXPlaylistToolbar`）：58dp 一行，
                    // 返回玻璃圆钮在左、动作玻璃圆钮在右，背景透明。
                    MeloXPlaylistToolbar(
                        onBack = onBarBack,
                        backContentDescription = barBackDescription,
                        backIcon = barBackIcon,
                        contentColor = barNavigationColor,
                        actions = barActions
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 歌单页：整屏封面模糊背景（透明底，让模糊层透上来）；
                    // 其它页：安静面，正文从 surface 开始。
                    .background(
                        if (topBarCollapse) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color.Transparent
                        }
                    )
            ) {
                if (!topBarCollapse) {
                    // MeloX 的整页背景：封面模糊 + 明暗可读性层。列表浮在上面。
                    MeloXPlaylistBackdrop(
                        artwork = artwork,
                        placeholderIcon = placeholderIcon
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        // 实底顶栏占位：内容从栏下方开始，不从栏底下穿过。
                        top = if (topBarCollapse) {
                            0.dp
                        } else {
                            innerPadding.calculateTopPadding()
                        },
                        bottom = appleBottomBarClearance() + bottomPadding +
                            if (uiState.selectionMode) SelectionBarHeight else 0.dp
                    )
                ) {
                    if (uiState.isLoading) {
                        item {
                            // 歌单 / 专辑的曲目是本地数据，几乎瞬间就到：只有真等久了才画转圈，
                            // 否则进页面会闪一帧（见 rememberLoadingIndicatorVisible）。
                            if (rememberLoadingIndicatorVisible()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(SonSpacing.extraLarge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            }
                        }
                    } else {
                        // hero 自己就是 list item：色面、cover、scrim、标题都在 item 内，
                        // 随滚动离开；fold 只改变 cover 的尺寸/圆角/位置。
                        if (heroStyle == CollectionHeroStyle.CenteredCover) {
                            // 歌单版式（照 MeloX `MeloXStandardPlaylistHero`）：封面按屏宽比例、
                            // 标题回到封面下方，动作区是「圆钮 + 胶囊 + 圆钮」。
                            item {
                                MeloXPlaylistHero(
                                    title = title,
                                    subtitle = subtitle,
                                    meta = headerMeta.joinToString(" · "),
                                    artwork = artwork,
                                    placeholderIcon = placeholderIcon,
                                    onPlay = {
                                        firstSong?.let {
                                            onIntent(SongCollectionIntent.SongClick(it))
                                        }
                                    },
                                    onShuffle = {
                                        onIntent(SongCollectionIntent.ShufflePlay)
                                    },
                                    hasSongs = firstSong != null,
                                    showSaveAction = firstSong != null,
                                    saveIcon = SymbolIconName.PlaylistAdd,
                                    saveContentDescription = stringResource(
                                        R.string.collection_detail_add_desc
                                    ),
                                    onSave = {
                                        onIntent(SongCollectionIntent.AddSongsToPlaylist)
                                    },
                                    onHeightChanged = { headerHeightPx = it },
                                    footer = headerExtra
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier.onSizeChanged {
                                        headerHeightPx = it.height
                                    }
                                ) {
                                    CollectionHeroFold(
                                        name = title,
                                        subtitle = subtitle,
                                        artwork = artwork,
                                        placeholderIcon = placeholderIcon,
                                        side = heroSide,
                                        collapse = collapse
                                    )
                                }
                            }

                            if (firstSong != null) {
                                item {
                                    // 12dp 下沿 fade 只属于 CollectionDetail；共享的
                                    // CollectionActions 保持中性，不把色面带到 MyMusic。
                                    Box(modifier = Modifier.sonColorFieldDetailFade()) {
                                        CollectionActions(
                                            onPlayAll = {
                                                onIntent(
                                                    SongCollectionIntent.SongClick(firstSong)
                                                )
                                            },
                                            onShuffle = {
                                                onIntent(SongCollectionIntent.ShufflePlay)
                                            },
                                            onAddToPlaylist = {
                                                onIntent(SongCollectionIntent.AddSongsToPlaylist)
                                            },
                                            containerColor = MaterialTheme.colorScheme
                                                .secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme
                                                .onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            headerExtra?.let { extra ->
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        extra()
                                    }
                                }
                            }
                        }

                        if (songs.isEmpty()) {
                            if (emptyText != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(SonSpacing.extraLarge),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emptyText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = songs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                val isDragging = index == draggingIndex
                                val dragShape = MaterialTheme.shapes.large
                                val draggedContainer by animateColorAsState(
                                    targetValue = if (isDragging) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                    label = "draggedContainer"
                                )
                                Box(
                                    modifier = Modifier
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffset else 0f
                                            val dragScale = if (isDragging) 1.02f else 1f
                                            scaleX = dragScale
                                            scaleY = dragScale
                                            shadowElevation =
                                                if (isDragging) 12.dp.toPx() else 0f
                                            shape = dragShape
                                        }
                                        .background(draggedContainer, dragShape)
                                ) {
                                    CollectionSongRow(
                                        song = song,
                                        isCurrentSong = song.id == uiState.currentSongId,
                                        selectionMode = uiState.selectionMode,
                                        selected = song.id in uiState.selectedSongIds,
                                        subtitle = songSubtitle(song),
                                        // 歌单页按 MeloX 的排版显示封面（不显序号），
                                        // 专辑 / 歌手页仍是序号 + 音柱。
                                        trackNumber = if (showTrackNumbers) index + 1 else null,
                                        onClick = {
                                            onIntent(SongCollectionIntent.SongClick(song))
                                        },
                                        onToggleSelection = {
                                            onIntent(
                                                SongCollectionIntent.ToggleSelection(song.id)
                                            )
                                        },
                                        trailingContent = { songTrailing(song) },
                                        moreMenuItems = songMenuItems(
                                            song = song,
                                            queueActions = queueActions,
                                            onToggleFavorite = onToggleFavorite,
                                            // 页面特有的动作（歌单的「移出」）接在通用动作之后。
                                            extra = songMoreMenu(song)
                                        ),
                                        reorderMode = reorderMode,
                                        dragHandle = if (canReorder && reorderMode) {
                                            {
                                                ReorderHandle(
                                                    onDragStart = {
                                                        draggingIndex = index
                                                        dragOffset = 0f
                                                    },
                                                    onDragEnd = {
                                                        val steps = (dragOffset /
                                                            reorderRowHeightPx).roundToInt()
                                                        val target = (index + steps)
                                                            .coerceIn(0, songs.lastIndex)
                                                        if (target != index) {
                                                            onMoveSong?.invoke(index, target)
                                                        }
                                                        draggingIndex = -1
                                                        dragOffset = 0f
                                                    },
                                                    onDrag = { delta -> dragOffset += delta }
                                                )
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }

                        songsFooter?.let { footer ->
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    footer()
                                }
                            }
                        }
                    }
                }

                // 顶栏底色：在列表之上；cover 从列表滚过时由这层 surface 实底接管。
                // 旧的四段页面渐变已删除，这里只负责滚动态的顶栏底色。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // M3 TopAppBar 与 ShortNavigationBar 同为 64dp；沿用现有
                        // layout token，不新增尺寸 token。
                        .height(statusBarInset + SonSizes.shortNavigationBarHeight)
                        // 在 layer 内读 collapse：滚动时只更新这一层的 alpha，
                        // 不让整段 content（含 LazyColumn）重组。
                        .graphicsLayer { alpha = barCollapse.containerAlpha() }
                        .background(MaterialTheme.colorScheme.surface)
                ) { }

                if (uiState.selectionMode) {
                    CollectionSelectionBar(
                        selectedCount = uiState.selectedSongIds.size,
                        onRemove = onRemoveSelected,
                        removeLabel = removeLabel,
                        onFavorite = {
                            onIntent(SongCollectionIntent.BatchSetFavorite(true))
                        },
                        onUnfavorite = {
                            onIntent(SongCollectionIntent.BatchSetFavorite(false))
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // 「添加到歌单」弹窗盖在整页之上，外壳统一挂载，三个页面行为一致。
        PlaylistPickerDialog(
            visible = uiState.showAddToPlaylistDialog,
            playlists = uiState.playlists,
            onDismiss = { onIntent(SongCollectionIntent.DismissAddToPlaylistDialog) },
            onPlaylistSelected = { playlistId ->
                onIntent(SongCollectionIntent.AddSongsToPlaylistTo(playlistId))
            },
            onCreateNewPlaylist = { name ->
                onIntent(SongCollectionIntent.CreatePlaylistAndAddSongs(name))
            }
        )
    }
}

/**
 * 一次性收集三个详情页共用的副作用：起播导航、随机播放队列、Toast。
 * 页面只需把 [CollectionDetailEffect] 流交给这里，不必各写一遍 collect。
 */
@Composable
fun CollectCollectionDetailEffects(
    effects: SharedFlow<CollectionDetailEffect>,
    onNavigateToPlayer: (Long) -> Unit,
    onPlayShuffled: (List<Song>) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is CollectionDetailEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is CollectionDetailEffect.PlayShuffled -> onPlayShuffled(effect.queue)
                is CollectionDetailEffect.ShowToast -> {
                    val message = effect.arg
                        ?.let { context.getString(effect.message, it) }
                        ?: context.getString(effect.message)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * 顶栏动作按钮：44dp 玻璃圆钮（`AppleGlassCircleButton`），无涟漪。
 *
 * 压在封面上时图标是 `onArtwork`（白）或品牌红，**必须**配一层玻璃托底 —— 封面亮度不可控，
 * 白图标落在亮封面上就看不见了（顶部那点渐变 scrim 不够）。玻璃的明暗跟着图标色走，
 * 所以两种图标色都成立；滚到 surface 之后 [glassAlpha] 归零，剩下的只是一枚图标。
 */
@Composable
fun DetailTopBarAction(
    onClick: () -> Unit,
    icon: SymbolIconName,
    contentDescription: String?,
    contentColor: Color,
    glassAlpha: Float = 1f
) {
    AppleGlassCircleButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        tint = contentColor,
        glassAlpha = glassAlpha
    )
}

/**
 * 动作区：一个 tonal 控制簇，而不是三个互不相干的圆钮。
 *
 * 官方 Expressive 的“按钮组”感来自共享容器：次要动作（随机/添加）收进簇里，
 * 播放全部仍是唯一的填充主按钮。曲目为空时整行不渲染，由调用方保证。
 *
 * [onAddToPlaylist] 可空：收藏曲目不属于任何歌单，我的音乐页传 null，此时按钮不占位。
 *
 * [containerColor] / [contentColor] 默认是中性的 surfaceContainerHigh /
 * onSurfaceVariant，只有 CollectionDetail 显式传 secondaryContainer 让它成为该屏的
 * 受限饱和色块；共享组件不把色面带去 MyMusic。
 */
@Composable
fun CollectionActions(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    containerColor: Color = AppleTheme.colors.fill,
    contentColor: Color = AppleTheme.colors.label
) {
    val colors = AppleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppleDims.padding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放：实心胶囊，用品牌红
        Row(
            modifier = Modifier
                .weight(1f)
                .height(AppleDims.actionHeight)
                .clip(RoundedCornerShape(AppleDims.cornerPill))
                .background(colors.systemRed)
                .applePress(onPlayAll),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3SymbolIcon(
                name = SymbolIconName.PlayArrow,
                filled = true,
                contentDescription = null,
                tint = Color.White,
                size = 20.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.collection_detail_play_all),
                style = AppleType.subheadEmphasized,
                color = Color.White
            )
        }

        // 随机播放：描边胶囊
        Row(
            modifier = Modifier
                .weight(1f)
                .height(AppleDims.actionHeight)
                .clip(RoundedCornerShape(AppleDims.cornerPill))
                .background(containerColor)
                .applePress(onShuffle),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Shuffle,
                filled = true,
                contentDescription = null,
                tint = contentColor,
                size = 20.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.collection_detail_shuffle_desc),
                style = AppleType.subheadEmphasized,
                color = contentColor
            )
        }

        if (onAddToPlaylist != null) {
            Box(
                modifier = Modifier
                    .size(AppleDims.actionHeight)
                    .clip(CircleShape)
                    .background(containerColor)
                    .applePress(onAddToPlaylist),
                contentAlignment = Alignment.Center
            ) {
                M3SymbolIcon(
                    name = SymbolIconName.PlaylistAdd,
                    filled = true,
                    contentDescription = stringResource(R.string.collection_detail_add_desc),
                    tint = contentColor,
                    size = 20.dp
                )
            }
        }
    }
}

/**
 * hero 标题与副标题：海报级标题 + label 级副标题。CollectionHeroFold 用它，
 * 保证字体、颜色、层级只有一份。
 */
@Composable
private fun HeroCopy(
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val extended = LocalSonExtendedColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            // Apple 的大标题位：32sp Bold，压在封面之上
            style = AppleType.title0,
            color = extended.onArtwork,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = AppleType.body,
            color = extended.onArtwork.copy(alpha = HeroSubTextAlpha),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * hero 的四段式封面遮罩：顶部托住状态栏图标 → 长坡降到通透 → 缓慢回压 → 底部压实托住标题。
 * 中段刻意留长并保持通透，任何封面都能完整显色；两个转折都靠多停一点避免硬边。
 */
@Composable
private fun HeroScrim() {
    val extended = LocalSonExtendedColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to extended.artworkScrim.copy(alpha = HeroScrimTopAlpha),
                    HeroScrimClearStop to Color.Transparent,
                    HeroScrimEaseStop to extended.artworkScrim.copy(alpha = HeroScrimEaseAlpha),
                    HeroTitleZoneStart to extended.artworkScrim.copy(alpha = HeroScrimTitleAlpha),
                    1f to extended.artworkScrim
                )
            )
    ) { }
}

/**
 * CollectionDetail 的 hero：色面 + cover + scrim + 标题都在同一个 list item 内。
 *
 * cover 在前 45% 的滚动量内连续改变尺寸、圆角、位置（field 从四周露出来），
 * 然后随 item 一起滚出；顶栏缩略图/标题沿用原有 crossfade。标题固定在 hero
 * 底部、字体和排版不参与 fold，只做 alpha crossfade，因此 glyph 不会被收缩框裁切。
 *
 * 所有 collapse 读取都在 graphicsLayer 内：滚动不触发布局，也不重组列表。
 * 真正的 shared-element fold（cover 离开 list item 落进顶栏）需要 overlay + 每帧
 * 布局，和本屏的折叠顶栏/列表架构冲突；这里采用 closest honest behavior，见报告。
 */
@Composable
private fun CollectionHeroFold(
    name: String,
    subtitle: String,
    artwork: String?,
    placeholderIcon: SymbolIconName,
    side: Dp,
    collapse: State<Float>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
    ) {
        // 色面在 cover 背后；cover 收缩时从四周露出来。
        SonColorFieldBackground(
            heroBottom = side,
            modifier = Modifier.fillMaxSize()
        )

        M3Artwork(
            artwork = artwork,
            placeholderIcon = placeholderIcon,
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scroll = collapse.value
                    val fold = SonShapeMorph.heroFold(
                        progress = scroll,
                        startSize = side,
                        endSize = side * HeroFoldEndScale,
                        endY = -side * HeroFoldEndShift
                    )
                    val scale = if (side > 0.dp) (fold.size / side).coerceIn(0f, 1f) else 0f
                    scaleX = scale
                    scaleY = scale
                    translationY = fold.y.toPx()
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    shape = SonShapeMorph.heroFoldLayerShape(scroll, scale)
                    clip = true
                }
        )

        HeroScrim()

        // 标题不参与 cover 的 scale/clip：固定在 hero 底部，只做 alpha crossfade。
        HeroCopy(
            name = name,
            subtitle = subtitle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.extraLarge)
                .graphicsLayer {
                    alpha = (1f - collapse.value / TitleFadeEnd).coerceIn(0f, 1f)
                }
        )
    }
}

/**
 * 多选时浮在曲目列表之上的操作栏。
 *
 * 「移出歌单」用错误色且仅歌单页提供（[onRemove] 非空才渲染）：它是这几个动作里
 * 唯一会改数据且不可撤销的，其余两个只是标记。专辑/歌手没有"移出"语义，按钮不出现。
 */
@Composable
private fun CollectionSelectionBar(
    selectedCount: Int,
    onRemove: (() -> Unit)?,
    removeLabel: String?,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = selectedCount > 0
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onRemove != null && removeLabel != null) {
                FilledTonalButton(
                    onClick = onRemove,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    M3SymbolIcon(
                        name = SymbolIconName.PlaylistRemove,
                        filled = true,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(SonSpacing.extraSmall))
                    Text(text = removeLabel)
                }
            }
            FilledTonalButton(onClick = onFavorite, enabled = enabled) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = true,
                    contentDescription = stringResource(R.string.songs_batch_favorite)
                )
            }
            FilledTonalButton(onClick = onUnfavorite, enabled = enabled) {
                M3SymbolIcon(
                    name = SymbolIconName.Favorite,
                    filled = false,
                    contentDescription = stringResource(R.string.songs_batch_unfavorite)
                )
            }
        }
    }
}

/**
 * 曲目行：多选模式下单击是勾选，普通模式单击是播放。
 * 多选时收起每行的更多菜单 —— 这一轮的意图已经由底部操作栏接管了。
 */
@Composable
private fun CollectionSongRow(
    song: Song,
    isCurrentSong: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    subtitle: String?,
    /** 传 null 时行首改显封面（歌单页的 MeloX 排版）。 */
    trackNumber: Int?,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    trailingContent: @Composable () -> Unit,
    moreMenuItems: List<MenuItem>,
    reorderMode: Boolean,
    dragHandle: (@Composable () -> Unit)? = null
) {
    var menuExpanded by remember(song.id) { mutableStateOf(false) }

    SongListItem(
        song = song,
        subtitle = subtitle,
        isCurrentSong = isCurrentSong,
        trackNumber = trackNumber,
        isPlaying = isCurrentSong,
        selected = if (selectionMode) selected else null,
        // 排序模式下整行不再响应点击：此刻的触摸意图是调整顺序，误触起播会打断手势。
        onClick = when {
            selectionMode -> onToggleSelection
            reorderMode -> null
            else -> onClick
        },
        showDuration = !reorderMode,
        showMoreButton = !selectionMode && !reorderMode && moreMenuItems.isNotEmpty(),
        moreMenuExpanded = menuExpanded,
        onMoreClick = { menuExpanded = true },
        onMoreDismiss = { menuExpanded = false },
        moreMenuItems = moreMenuItems,
        trailingContent = trailingContent,
        // quiet surface：集合页不再画行分割线，行与行之间交给留白和封面。
        showDivider = false,
        dragHandle = dragHandle
    )
}

/**
 * 排序把手：只在排序模式出现在行尾。
 *
 * 它独占一块 48dp 手势区，按下即可拖，不必再长按；圆形底把裸图标抬成一个明确的控件。
 * 回调通过 [rememberUpdatedState] 保持最新 —— 指针手势只注册一次，列表重排后索引不能是旧值。
 */
@Composable
private fun ReorderHandle(
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .size(SonSizes.minTouchTarget)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        // 拿起这一行时给一次长按级触感，告诉手指"已经抓住了"。
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnDragStart()
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        currentOnDrag(amount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(SonSizes.iconExtraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            M3SymbolIcon(
                name = SymbolIconName.DragIndicator,
                filled = true,
                contentDescription = stringResource(R.string.player_drag_handle_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SonSizes.iconSmall)
            )
        }
    }
}

/**
 * 顶栏动作：两种栏都是玻璃圆钮（浮层栏随折叠淡出托底，MeloX 工具条始终满托底）。
 */
@Composable
private fun CollectionBarAction(
    icon: SymbolIconName,
    contentDescription: String?,
    onClick: () -> Unit,
    contentColor: Color,
    glassAlpha: Float
) {
    DetailTopBarAction(
        onClick = onClick,
        icon = icon,
        contentDescription = contentDescription,
        contentColor = contentColor,
        glassAlpha = glassAlpha
    )
}
