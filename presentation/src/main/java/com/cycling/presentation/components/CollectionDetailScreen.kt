@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonShapeMorph
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.SonicTheme
import com.cycling.core.ui.theme.sonColorFieldDetailFade
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.collectiondetail.CollectionDetailEffect
import com.cycling.presentation.collectiondetail.CollectionDetailUiState
import com.cycling.presentation.collectiondetail.SongCollectionIntent
import com.cycling.presentation.player.rememberArtworkTheme
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

/** 视差速率：封面以滚动量的这个比例跟随，越小纵深越强，但过小会脱节。 */
private const val HeroParallaxRate = 0.5f

/** 顶栏标题（含缩略图）开始显现的折叠进度。 */
private const val HeaderRevealStart = 0.45f

/** 顶栏标题完全显现的折叠进度。 */
private const val HeaderRevealEnd = 0.7f

/** 顶栏底色加速比：必须早于标题显形，否则白字会落在半透明底上。 */
private const val ContainerRampRate = 1.6f

/** 封面文字的次级层级。 */
private const val HeroSubTextAlpha = 0.88f

/** 多选操作栏的高度，用来给列表补底部留白。 */
private val SelectionBarHeight = 72.dp

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
    onRemoveSelected: (() -> Unit)? = null,
    removeLabel: String? = null,
    songSubtitle: (Song) -> String? = { null },
    songTrailing: @Composable (Song) -> Unit = {},
    songMoreMenu: (Song) -> List<MenuItem> = { emptyList() },
    headerExtra: @Composable (ColumnScope.() -> Unit)? = null,
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

    // C：整页配色由封面派生。应用级主题跟随的是播放页封面，这里单独覆盖一层。
    val artworkTheme = rememberArtworkTheme(imageUrl = artwork, darkTheme = isSystemInDarkTheme())

    // 头部边长取容器宽度（方形封面 = 宽 = 高），无需测量回调，首帧即正确。
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val heroSide = maxWidth
        // 密度必须在 derivedStateOf 之外读取：其计算块不是 composable 作用域。
        val density = LocalDensity.current
        // 拖拽落点的换算基准：曲目行是两行 ListItem，固定 72dp 高。
        val reorderRowHeightPx = with(density) { 72.dp.toPx() }

        // hero 折叠 overlay 的终点：top bar 里的 32dp 缩略图槽位。
        // 先由布局回填；未测量时停在左上角，只影响折叠末端的一帧。
        var topBarArtworkOffset by remember { mutableStateOf(Offset.Zero) }
        // 顶栏底色 band 的高度：状态栏 + TopAppBar。
        val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // 折叠进度 0→1。头部整体离开视口后固定为 1，避免首项索引变化导致回弹。
        val collapse = remember(heroSide, density) {
            derivedStateOf {
                val sidePx = with(density) { heroSide.toPx() }
                if (sidePx <= 0f) {
                    0f
                } else {
                    val offsetPx = if (listState.firstVisibleItemIndex == 0) {
                        listState.firstVisibleItemScrollOffset
                    } else {
                        sidePx.toInt()
                    }
                    (offsetPx.toFloat() / sidePx).coerceIn(0f, 1f)
                }
            }
        }

        SonicTheme(colorScheme = artworkTheme.colorScheme) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                // 内容自行处理状态栏：顶栏透明，封面必须一路铺到屏幕顶端，
                // 若沿用 Scaffold 默认的 contentWindowInsets，内容区会被下推一个状态栏高度，
                // 封面底部就会露出等高的空白带。
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    // 顶栏保持透明；底色由内容层顶部的 surface band 提供。
                    // 这样折叠中的封面可以画在 band 之上、图标/标题之下。
                    val navigationIconColor = lerp(
                        LocalSonExtendedColors.current.onArtwork,
                        MaterialTheme.colorScheme.onSurface,
                        collapse.value
                    )
                    val actionIconColor = lerp(
                        LocalSonExtendedColors.current.onArtwork,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        collapse.value
                    )
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 缩略图槽位：hero 折叠 overlay 的终点对齐这里。
                                // 位置在布局后回填，避免在页面里猜 top bar padding。
                                Spacer(
                                    modifier = Modifier
                                        .size(SonSizes.appBarArtwork)
                                        .onGloballyPositioned { coordinates ->
                                            topBarArtworkOffset = coordinates.positionInRoot()
                                        }
                                )
                                Spacer(modifier = Modifier.width(SonSpacing.small))
                                val headerAlpha = collapse.headerAlpha()
                                if (headerAlpha > 0f) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .graphicsLayer { alpha = headerAlpha }
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            // 顶栏透明：图标颜色随折叠进度从 onArtwork 过渡到 onSurface。
                            DetailTopBarAction(
                                onClick = {
                                    when {
                                        reorderMode -> reorderMode = false
                                        uiState.selectionMode -> onIntent(
                                            SongCollectionIntent.ExitSelectionMode
                                        )
                                        else -> onNavigateBack()
                                    }
                                },
                                icon = if (reorderMode || uiState.selectionMode) {
                                    SymbolIconName.Close
                                } else {
                                    SymbolIconName.ArrowBack
                                },
                                contentDescription = when {
                                    reorderMode -> stringResource(
                                        R.string.collection_detail_exit_reorder_desc
                                    )
                                    uiState.selectionMode -> stringResource(
                                        R.string.collection_detail_exit_selection_desc
                                    )
                                    else -> backContentDescription
                                },
                                contentColor = navigationIconColor
                            )
                        },
                        actions = {
                            when {
                                uiState.selectionMode -> {
                                    val allSelected = songs.isNotEmpty() &&
                                        uiState.selectedSongIds.size == songs.size
                                    DetailTopBarAction(
                                        onClick = {
                                            onIntent(
                                                if (allSelected) {
                                                    SongCollectionIntent.ClearSelection
                                                } else {
                                                    SongCollectionIntent.SelectAll
                                                }
                                            )
                                        },
                                        icon = SymbolIconName.SelectAll,
                                        contentDescription = stringResource(
                                            if (allSelected) {
                                                R.string.songs_clear_selection_desc
                                            } else {
                                                R.string.songs_select_all_desc
                                            }
                                        ),
                                        contentColor = actionIconColor
                                    )
                                }
                                reorderMode -> {
                                    DetailTopBarAction(
                                        onClick = { reorderMode = false },
                                        icon = SymbolIconName.Check,
                                        contentDescription = stringResource(
                                            R.string.collection_detail_reorder_done_desc
                                        ),
                                        contentColor = actionIconColor
                                    )
                                }
                                songs.isNotEmpty() -> {
                                    if (onExport != null) {
                                        DetailTopBarAction(
                                            onClick = onExport,
                                            icon = SymbolIconName.IosShare,
                                            contentDescription = stringResource(
                                                R.string.playlist_detail_export_m3u
                                            ),
                                            contentColor = actionIconColor
                                        )
                                    }
                                    DetailTopBarAction(
                                        onClick = {
                                            onIntent(SongCollectionIntent.EnterSelectionMode)
                                        },
                                        icon = SymbolIconName.Checklist,
                                        contentDescription = stringResource(R.string.songs_select_desc),
                                        contentColor = actionIconColor
                                    )
                                    if (canReorder) {
                                        DetailTopBarAction(
                                            onClick = { reorderMode = true },
                                            icon = SymbolIconName.SwapVert,
                                            contentDescription = stringResource(
                                                R.string.collection_detail_reorder_desc
                                            ),
                                            contentColor = actionIconColor
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            navigationIconContentColor = navigationIconColor,
                            titleContentColor = navigationIconColor,
                            actionIconContentColor = actionIconColor
                        )
                    )
                }
            ) { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 安静面：旧的四段色面已删除，正文从 surface 开始。
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = SonLayout.bottomBarClearance + bottomPadding +
                                if (uiState.selectionMode) SelectionBarHeight else 0.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(SonSpacing.extraLarge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            }
                        } else {
                            // hero 由列表之上的 fold overlay 绘制；列表只保留同高的滚动占位，
                            // collapse 仍由首项的 scroll offset 驱动。
                            item {
                                Spacer(modifier = Modifier.height(heroSide))
                            }

                            if (firstSong != null) {
                                item {
                                    CollectionActions(
                                        onPlayAll = {
                                            onIntent(SongCollectionIntent.SongClick(firstSong))
                                        },
                                        onShuffle = {
                                            onIntent(SongCollectionIntent.ShufflePlay)
                                        },
                                        onAddToPlaylist = {
                                            onIntent(SongCollectionIntent.AddSongsToPlaylist)
                                        }
                                    )
                                }
                            }

                            headerExtra?.let { extra ->
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        extra()
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
                                            onClick = {
                                                onIntent(SongCollectionIntent.SongClick(song))
                                            },
                                            onToggleSelection = {
                                                onIntent(
                                                    SongCollectionIntent.ToggleSelection(song.id)
                                                )
                                            },
                                            trailingContent = { songTrailing(song) },
                                            moreMenuItems = songMoreMenu(song),
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
                        }
                    }

                    // 顶栏底色：在列表之上、折叠封面之下。旧的四段页面渐变已删除，
                    // 这里只负责折叠到 top bar 后的 surface 实底。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusBarInset + SonSizes.topAppBarHeight)
                            // 在 layer 内读 collapse：滚动时只更新这一层的 alpha，
                            // 不让整段 content（含 LazyColumn）重组。
                            .graphicsLayer { alpha = collapse.containerAlpha() }
                            .background(MaterialTheme.colorScheme.surface)
                    ) { }

                    if (!uiState.isLoading) {
                        CollectionHeroFold(
                            name = title,
                            subtitle = subtitle,
                            artwork = artwork,
                            placeholderIcon = placeholderIcon,
                            side = heroSide,
                            collapse = collapse,
                            targetOffset = topBarArtworkOffset
                        )
                    }

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
 * 顶栏动作按钮：顶栏保持透明，图标颜色由调用方按折叠进度传入。
 * 压在封面上时是 onArtwork，滚到 surface 后是 onSurface / onSurfaceVariant；
 * 不再加第二层 scrim 浮标（hero 顶部 scrim 已经负责可读性）。
 */
@Composable
fun DetailTopBarAction(
    onClick: () -> Unit,
    icon: SymbolIconName,
    contentDescription: String?,
    contentColor: Color
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
    ) {
        M3SymbolIcon(name = icon, filled = true, contentDescription = contentDescription)
    }
}

/**
 * 动作区：一个 tonal 控制簇，而不是三个互不相干的圆钮。
 *
 * 官方 Expressive 的“按钮组”感来自共享容器：次要动作（随机/添加）收进簇里，
 * 播放全部仍是唯一的填充主按钮。曲目为空时整行不渲染，由调用方保证。
 *
 * [onAddToPlaylist] 可空：收藏曲目不属于任何歌单，我的音乐页传 null，此时按钮不占位。
 */
@Composable
fun CollectionActions(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // hero 下沿最多 12dp 的 primaryContainer fade；画在动作区 top padding 里，
            // 不增加布局高度，也不进入首行。
            .sonColorFieldDetailFade()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            // 动作簇是这一屏唯一的受限饱和色块；播放全部仍是 primary/onPrimary。
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(SonSpacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle) {
                    M3SymbolIcon(
                        name = SymbolIconName.Shuffle,
                        filled = true,
                        contentDescription = stringResource(R.string.collection_detail_shuffle_desc)
                    )
                }

                Button(onClick = onPlayAll) {
                    M3SymbolIcon(
                        name = SymbolIconName.PlayArrow,
                        filled = true,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon)
                    )
                    Spacer(modifier = Modifier.width(SonSpacing.small))
                    Text(text = stringResource(R.string.collection_detail_play_all))
                }

                if (onAddToPlaylist != null) {
                    IconButton(onClick = onAddToPlaylist) {
                        M3SymbolIcon(
                            name = SymbolIconName.PlaylistAdd,
                            filled = true,
                            contentDescription = stringResource(R.string.collection_detail_add_desc)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 集合介绍，默认折叠为若干行，溢出时才出现「更多」。
 *
 * 折叠高度用「行数」而非固定 dp：介绍有自然段，行高随排版缩放变化，
 * 写死 dp 会在大字号下截断在半行上。内容为空且不在生成中时整块不占位，
 * 避免留一段空白。歌单详情页与我的音乐页（收藏曲目介绍）共用。
 */
@Composable
fun PlaylistDescription(
    description: String,
    expanded: Boolean,
    isGenerating: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 折叠态的行数。够放下开头，又不至于把首屏曲目挤下去。
    val collapsedLines = 3
    var hasOverflow by remember(description) { mutableStateOf(false) }

    if (description.isBlank() && !isGenerating) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small)
    ) {
        if (isGenerating) {
            Text(
                text = stringResource(R.string.playlist_detail_desc_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // 只在折叠态需要判断溢出；展开后恒为 false。
                if (!expanded) {
                    hasOverflow = result.hasVisualOverflow || result.lineCount > collapsedLines
                }
            }
        )

        if (hasOverflow || expanded) {
            Spacer(modifier = Modifier.height(SonSpacing.extraSmall))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onToggle)
            ) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.playlist_detail_collapse else R.string.playlist_detail_more
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(SonSpacing.extraSmall))

                M3SymbolIcon(
                    name = if (expanded) SymbolIconName.ExpandLess else SymbolIconName.ExpandMore,
                    filled = true,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
            }
        }
    }
}

/**
 * 沉浸式头部：方形封面满铺（不裁圆角），从屏幕顶端一路铺开，标题与副标题压在封面底部。
 *
 * 几何：封面自身就是 side × side，正好填满外层方框；滚动时以 [HeroParallaxRate] 的
 * 速率跟随并同时放大（scaleX/Y），使放大的部分始终盖住上移露出的区域 —— 只平移不放大
 * 会在顶部露出空白。`clipToBounds` 裁掉溢出到下方的部分。
 *
 * 遮罩与标题留在外层、不参与平移 —— 否则与封面基准不一致，标题会脱离封面底部。
 *
 * 我的音乐页的收藏 hero 仍用这个视差版本；CollectionDetail 的连续折叠见 [CollectionHeroFold]。
 */
@Composable
fun CollectionHero(
    name: String,
    subtitle: String,
    artwork: String?,
    placeholderIcon: SymbolIconName,
    side: Dp,
    collapse: State<Float>
) {
    val extended = LocalSonExtendedColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
    ) {
        M3Artwork(
            artwork = artwork,
            placeholderIcon = placeholderIcon,
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxSize()
                // 在 graphics layer 内读折叠进度：滚动时只重绘，不重组整页。
                .graphicsLayer {
                    val t = collapse.value * HeroParallaxRate
                    // 放大量要略大于上移量，保证任何时刻顶部都不露白。
                    val grow = 1f + t * 2f
                    scaleX = grow
                    scaleY = grow
                    translationY = -side.toPx() * t
                }
        )

        HeroScrim()

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                // 海报级标题：与正文拉开一个数量级，这一屏的 expressive moment 由它承担。
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = extended.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SonSpacing.small))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = extended.onArtwork.copy(alpha = HeroSubTextAlpha),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * hero 的四段式封面遮罩：顶部托住状态栏图标 → 长坡降到通透 → 缓慢回压 → 底部压实托住标题。
 * 中段刻意留长并保持通透，任何封面都能完整显色；两个转折都靠多停一点避免硬边。
 */
@Composable
private fun HeroScrim(scrimAlpha: Float = 1f) {
    val extended = LocalSonExtendedColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrimAlpha }
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
 * CollectionDetail 的 hero 折叠。
 *
 * 旧 [CollectionHero] 是 list item 内的视差滚动 + 顶栏缩略图 crossfade；这里把同一个
 * 封面画在列表之上，尺寸、圆角、位置全部由滚动 collapse 连续插值，最后落到 top bar
 * 的 32dp 缩略图槽位（[targetOffset] 由槽位测量得到）。列表用同高 Spacer 保留滚动
 * 距离，因此不需要 shared element API 也能得到真实的 fold。
 *
 * 标题按 prototype 的曲线在折叠前 45% 内淡出；顶栏标题从 45% 起交叉淡入。
 * 尺寸/圆角/位置来自 [SonShapeMorph.heroFold]，页面不手写形状或弹簧。
 */
@Composable
private fun CollectionHeroFold(
    name: String,
    subtitle: String,
    artwork: String?,
    placeholderIcon: SymbolIconName,
    side: Dp,
    collapse: State<Float>,
    targetOffset: Offset
) {
    val progress = collapse.value
    val density = LocalDensity.current
    val targetX = with(density) { targetOffset.x.toDp() }
    val targetY = with(density) { targetOffset.y.toDp() }
    val fold = SonShapeMorph.heroFold(
        progress = progress,
        startSize = side,
        endSize = SonSizes.appBarArtwork,
        endX = targetX,
        endY = targetY
    )
    val titleAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
    val scale = if (side > 0.dp) (fold.size / side).coerceIn(0f, 1f) else 0f
    val extended = LocalSonExtendedColors.current

    // 用 graphicsLayer 的 scale/translation 走折叠：不每帧改布局，封面不会被反复测量/解码。
    // 圆角也在 layer 局部坐标里按 scale 反算，保证视觉圆角仍是 0 → 8dp。
    Box(
        modifier = Modifier
            .size(side)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = fold.x.toPx()
                translationY = fold.y.toPx()
                transformOrigin = TransformOrigin(0f, 0f)
                shape = SonShapeMorph.heroFoldLayerShape(progress, scale)
                clip = true
            }
    ) {
        M3Artwork(
            artwork = artwork,
            placeholderIcon = placeholderIcon,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        )

        // 缩略图阶段不再需要遮罩，随折叠淡出，留下封面本身的颜色。
        HeroScrim(scrimAlpha = (1f - progress).coerceIn(0f, 1f))
    }

    // 标题不参与 cover 的 scale：字体保持固定，只随 cover 的可见宽度被裁切并淡出，
    // 避免 fold 变成标题形变。顶栏标题从 45% 起接手。
    if (titleAlpha > 0f) {
        Box(
            modifier = Modifier
                .offset(x = fold.x, y = fold.y)
                .size(fold.size)
                .clip(SonShapeMorph.heroFoldShape(progress))
                .graphicsLayer { alpha = titleAlpha }
        ) {
            Column(
                // 保持 hero 原始宽度排版，只让外层 clip 随 cover 收窄；字体和换行都不参与 fold。
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .requiredWidth(side)
                    .padding(
                        horizontal = SonLayout.screenMargin,
                        vertical = SonSpacing.extraLarge
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = extended.onArtwork,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SonSpacing.small))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.onArtwork.copy(alpha = HeroSubTextAlpha),
                    textAlign = TextAlign.Center
                )
            }
        }
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
