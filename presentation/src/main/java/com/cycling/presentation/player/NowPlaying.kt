@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.cycling.presentation.player

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.FlamingoIcon
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.core.ui.theme.sonDynamicBackdrop
import com.cycling.domain.model.BeatState
import com.cycling.domain.model.HiResBadge
import com.cycling.domain.model.RepeatMode
import com.cycling.presentation.R
import com.cycling.presentation.lyrics.LyricsPage
import kotlinx.coroutines.delay

/**
 * 播放场景（Apple Music 版式）的几何常量。
 *
 * 参考 MeloX-Android 的 `MeloXIOSNowPlayingScene`：封面页居中满宽方形封面、歌词 / 队列页
 * 顶部一条 64dp 的歌曲栏、底部一整块常驻控制区。数值取整到 4dp 栅格，改版式只改这里。
 */
internal object PlayerSceneSpec {
    val grabberWidth = 36.dp
    val grabberHeight = 5.dp
    val grabberTop = SonSpacing.small
    /** 把手的拖拽命中高度：5dp 的胶囊按不住，整行都算拖拽区。 */
    val grabberTouchHeight = 28.dp

    /** 手势行程：整屏高度的这个比例对应收起进度 1（MeloX 同样取 0.86）。 */
    const val DISMISS_DRAG_RANGE = 0.86f
    /**
     * 松手时的判定线：**展开**进度低于这个值就收起。
     *
     * 对应 MeloX 的 0.42（那边 0 = 展开 / 1 = 收起，这里正好反过来，所以是 1 - 0.42）。
     * 别写成 `progress >= 0.42`：那样拖得越深反而会弹回去。
     */
    const val DISMISS_SETTLE_PROGRESS = 0.58f
    /** 或者松手时往下甩得超过这个速度（px/s）也收起 —— 只看距离的话快速一甩会弹回来。 */
    const val DISMISS_FLING_VELOCITY = 1200f

    /** 卡片圆角上限：满屏为 0，脱离屏幕边缘后到这一档（见 [PlayerMorph.cornerFactor]）。 */
    val cardCorner = 28.dp

    /** 播放页封面的投影高度。 */
    val artworkShadow = 28.dp
    /** 封面圆角（= `MaterialTheme.shapes.extraLarge`）：飞行的两端必须用同一个数。 */
    val artworkCorner = 28.dp

    val contentSide = 28.dp
    val artworkTop = SonSpacing.mediumSmall
    val artworkBottom = SonSpacing.large
    val artworkTitleGap = SonSpacing.large

    val headerHeight = 64.dp
    val headerArtwork = 48.dp
    val headerTextGap = SonSpacing.mediumSmall
    val headerActionSize = 40.dp
    val headerIcon = 22.dp

    val controlSide = 28.dp
    val controlBottom = SonSpacing.medium
    val seekTouchHeight = 28.dp
    val seekTrackHeight = 4.dp
    val seekTrackScrubHeight = 6.dp
    val seekThumb = 6.dp
    /** minimal_seekbar_padding_vertical */
    val seekVerticalPadding = 5.dp
    /** minimal_seekbar_time_margin_vertical */
    val timeRowMargin = 10.dp
    /** 时间行（含中间那枚品质徽标）。16dp 放得下 12sp 的字，但徽标带 18dp 的图标，抬到 18dp。 */
    val timeRowHeight = 18.dp

    val transportSize = 56.dp
    val transportIcon = 34.dp

    /**
     * 主播放键的图标尺寸。
     *
     * Apple 的三角到边缘留白多，48dp 才压得住两侧；Flamingo 的三角是贴着画布画的实心块，
     * 同样 48dp 会明显头重脚轻（实测比两侧的双三角大一圈）。40dp 是两套几何共用的折中：
     * 仍然比运输键大一档，但不会把视觉重心整个吃掉。
     */
    val playIcon = 40.dp
    /** control_buttons_distance_padding —— 运输键之间的实测间距 */
    val transportGap = 40.dp

    val selectorHeight = 48.dp
    val selectorIcon = 24.dp

    /** 控制区在布局里的固定高度：隐藏时只让内容淡出，不塌陷，歌词不会因此跳一行。 */
    val controlsHeight = 208.dp

    /**
     * 控制区整体再抬一点。
     *
     * 内容本身已经贴着导航栏留白，页脚图标离手势条只剩几 dp；抬 16dp 之后
     * 标题行 / 进度条 / 运输键 / 页脚一起上移，底部才不会再顶住导航栏。
     */
    val controlsLift = SonSpacing.medium

    /** 歌词页停止操作多久后收起控制区。 */
    const val CONTROL_IDLE_MS = 3_000L
}

/**
 * 整页压暗的强度。
 *
 * 整页内容都是白字，色面本身亮暗不定，不压会在亮色主题下糊成一片；但压过头就把动态色面
 * 盖死了（实测 0.56/0.30/0.66 的渐变只剩一块黑）。取 0.34 作基准，再按上中下三段微调。
 */
private const val SceneScrimAlpha = 0.34f

/** 副标题透明度。Apple Music 的歌手名压得很低，只作辨识用。 */
private const val SubtitleAlpha = 0.55f

/** 控制区收起时内容下滑的距离：比纯淡出更像「缩回去」，又不会真的把布局撑开。 */
private val ControlHideSlide = 32.dp

/** 封面在暂停态回缩的比例：Apple Music 用轻微缩放表达「停住了」，比内缩更稳。 */
private const val ArtworkPausedScale = 0.94f

/** 三页封面共用的共享元素 key。 */
private const val ArtworkSharedKey = "nowPlayingArtwork"

/**
 * 封面共享元素：满宽大图与顶部 48dp 小图之间连续变形。
 *
 * 两端各自 `rememberSharedContentState` 同一 key，[AnimatedContent] 的过渡期把它们匹配起来；
 * 任一端拿不到共享作用域时（如独立歌词路由）退化为普通布局。
 */
@Composable
private fun Modifier.sharedArtwork(
    sharedScope: SharedTransitionScope?,
    animatedScope: AnimatedVisibilityScope?
): Modifier {
    val base = this
    if (sharedScope == null || animatedScope == null) return base
    return with(sharedScope) {
        base.sharedBounds(rememberSharedContentState(ArtworkSharedKey), animatedScope)
    }
}

/**
 * 播放页外壳（Apple Music 版式）。
 *
 * 一屏三页（封面 / 歌词 / 队列）在同一个 [AnimatedContent] 里上下滑动交叉淡入：新页从
 * 下方 1/16 屏高处淡入、旧页向上淡出；封面作为共享元素在三页之间连续变形。顶部一条可下拉
 * 的把手，底部一整块控制区；落在歌词页时控制区 3s 无操作自动收起，点任意处再唤出。
 *
 * 页面状态走 [PlayerUiState.viewMode]，不额外引入本地状态 —— 从迷你播放器进来时要能直接
 * 落到歌词页，两套状态迟早会不一致。
 */
@Composable
fun NowPlayingSurface(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    /** 收起进度：1 = 完全展开，0 = 已收起。由 [PlayerOverlay] 持有，手势只往里写。 */
    progress: Float,
    onProgressChange: (Float) -> Unit,
    /** 松手后由外层去跑收尾动画；`true` = 收起。 */
    onSettle: (Boolean) -> Unit,
    beat: () -> BeatState,
    modifier: Modifier = Modifier,
    /** 封面由外层飞行图接管，页面自己的封面节点要让开。 */
    artworkOwned: Boolean = false
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    val onLyricsPage = uiState.viewMode == PlayerViewMode.LYRICS

    // ── 下拉收起 ──────────────────────────────────────────────────────────
    // 做法对齐 MeloX：手势只推进外层那个 0..1 的「收起进度」，位移 / 缩放由 PlayerOverlay 派生；
    // 松手时按「进度过半 或 甩得够快」决定收起还是弹回（只看距离的话，快速一甩会弹回来）。
    // 内容层不再重排，量到的高度就是满屏高度。
    var surfaceHeightPx by remember { mutableIntStateOf(1) }
    val dragRangePx = (surfaceHeightPx * PlayerSceneSpec.DISMISS_DRAG_RANGE).coerceAtLeast(1f)
    // 松手后的收尾动画在跑时不接受新手势，否则两处同时写进度会抖。
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        if (progress <= 0f || progress >= 1f) settling = false
    }

    val settleNow: (Float) -> Unit = { velocity ->
        if (!settling) {
            val closed = progress <= PlayerSceneSpec.DISMISS_SETTLE_PROGRESS ||
                velocity >= PlayerSceneSpec.DISMISS_FLING_VELOCITY
            when {
                closed -> {
                    settling = true
                    onSettle(true)
                }

                // 往上拖到顶再松手：进度已经贴在 1，外层 animateTo(1f) 是空动画，
                // progress 不会再变 —— 若这里置了 settling，复位条件就永远等不到，
                // 之后所有下拉都被 `if (!settling)` 吞掉。所以「已经满屏」不进收尾流程。
                progress < 1f -> {
                    settling = true
                    onSettle(false)
                }

                else -> Unit
            }
        }
    }

    // 兜底：外层若判定「无需动画」直接返回，progress 不变，settling 会一直卡住。
    // 收尾动画最长 CLOSE_MS，超时后无条件放开手势；新手指按下时外层也会取消动画，不会打架。
    LaunchedEffect(settling) {
        if (!settling) return@LaunchedEffect
        delay(PlayerMotion.CLOSE_MS + 160L)
        settling = false
    }

    // 指针事件是在**缩放后的坐标系**里收到的：整页缩到 0.62 时，手指走 100px 本地只报 62px。
    // 乘回当前缩放，跟手的距离才和屏幕上的手指一致，不会越拖越快。
    val dismissDragState = rememberDraggableState { delta ->
        if (!settling) {
            // delta 向下为正；progress 1 = 展开、0 = 收起，所以这里要**减**。
            onProgressChange(
                (progress - delta * PlayerMorph.scaleAt(progress) / dragRangePx)
                    .coerceIn(0f, 1f)
            )
        }
    }

    // 速度是 px/s：往下甩为正。MeloX 的阈值就是 0.42 进度 / 1200 速度。
    val dismissDragModifier = Modifier.draggable(
        state = dismissDragState,
        orientation = Orientation.Vertical,
        onDragStopped = { velocity -> settleNow(velocity) }
    )

    // 歌词页的列表滚到顶之后继续往下拉，接着走收起（MeloX 的 nestedScroll 交接）。
    // 列表自己还能滚动时 available.y 是 0 / 负值，这里一律不碰。
    //
    // 这个 connection 必须 remember 住（每帧重建 modifier 会连节点一起重建），所以闭包里
    // 只能读 *Ref：进度、行程、回调都是随手指每帧变化的，直接捕获会拿到创建时那一帧的旧值。
    val progressRef = rememberUpdatedState(progress)
    val settlingRef = rememberUpdatedState(settling)
    val dragRangeRef = rememberUpdatedState(dragRangePx)
    val settleNowRef = rememberUpdatedState(settleNow)
    val onProgressChangeRef = rememberUpdatedState(onProgressChange)

    val overscrollCollapse = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || settlingRef.value) return Offset.Zero
                val current = progressRef.value
                if (current <= 0f && available.y <= 0f) return Offset.Zero
                // available.y 向下为正：手往下拉 = 收起，所以是减。
                onProgressChangeRef.value(
                    (current - available.y * PlayerMorph.scaleAt(current) / dragRangeRef.value)
                        .coerceIn(0f, 1f)
                )
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (progressRef.value <= 0f || settlingRef.value) return Velocity.Zero
                settleNowRef.value(available.y)
                return available
            }
        }
    }

    // 只有在歌词页才自动隐藏：封面页与队列页的控件是常驻的，收起反而让人找不到播放键。
    LaunchedEffect(onLyricsPage, controlsVisible, lastInteraction) {
        if (!onLyricsPage || !controlsVisible) return@LaunchedEffect
        delay(PlayerSceneSpec.CONTROL_IDLE_MS)
        if (SystemClock.elapsedRealtime() - lastInteraction >= PlayerSceneSpec.CONTROL_IDLE_MS) {
            controlsVisible = false
        }
    }

    fun touch() {
        lastInteraction = SystemClock.elapsedRealtime()
        controlsVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { surfaceHeightPx = it.height }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SceneGrabber(
                dragModifier = dismissDragModifier,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PlayerSceneSpec.grabberTop)
            )

            // transitionSpec 不是 @Composable 上下文，动效 token 必须先在这里取好再传进去。
            val motionScheme = MaterialTheme.motionScheme
            val enterFade = remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }
            val exitFade = remember(motionScheme) { motionScheme.fastEffectsSpec<Float>() }

            // 三页共用一张封面：切换时封面在满宽大图与顶部 48dp 小图之间连续变形，
            // 其余内容照旧交叉淡入。共享元素渲染在 overlay 上，不受页面整体淡入淡出影响。
            SharedTransitionLayout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val sharedScope = this
                AnimatedContent(
                    targetState = uiState.viewMode,
                    // Apple 的 ViewSwitcher 用的是 fade_in / fade_out：纯交叉淡入，不做位移。
                    // 封面由下面的共享元素负责连续变形，页面本身再位移一次就是重复表达。
                    transitionSpec = {
                        fadeIn(enterFade).togetherWith(fadeOut(exitFade))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "playerPage"
                ) { page ->
                    when (page) {
                        PlayerViewMode.COVER -> ArtworkPage(
                            uiState = uiState,
                            onIntent = onIntent,
                            onInteract = ::touch,
                            dragModifier = dismissDragModifier,
                            artworkOwned = artworkOwned,
                            sharedScope = sharedScope,
                            animatedScope = this@AnimatedContent
                        )

                        PlayerViewMode.LYRICS -> LyricsScene(
                            uiState = uiState,
                            onIntent = onIntent,
                            onInteract = ::touch,
                            overscrollModifier = Modifier.nestedScroll(overscrollCollapse),
                            sharedScope = sharedScope,
                            animatedScope = this@AnimatedContent
                        )

                        PlayerViewMode.QUEUE -> QueueScene(
                            uiState = uiState,
                            onIntent = onIntent,
                            onInteract = ::touch,
                            sharedScope = sharedScope,
                            animatedScope = this@AnimatedContent
                        )
                    }
                }
            }

            // 控制区收起时整块高度一起收掉：留白不塌陷的做法在矮屏上会白占三分之一屏，
            // 歌词该顺势铺到底。歌词的锚点是从顶部算的固定值，高度变化不会让当前行跳位置。
            val controlsShown = !onLyricsPage || controlsVisible
            val controlAlpha by animateFloatAsState(
                targetValue = if (controlsShown) 1f else 0f,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "controlAlpha"
            )
            val controlsHeight by animateDpAsState(
                targetValue = if (controlsShown) PlayerSceneSpec.controlsHeight else 0.dp,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "controlsHeight"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 底部先让出 [PlayerSceneSpec.controlsLift]：整块控制区连同上面的标题行
                    // 一起上移，留白都留在手指最容易误触的导航栏那一侧。收起时一并归零，
                    // 歌词页不会平白少掉一条底边。
                    .padding(bottom = PlayerSceneSpec.controlsLift * controlAlpha)
                    .height(controlsHeight)
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .graphicsLayer {
                            alpha = controlAlpha
                            translationY = (1f - controlAlpha) * ControlHideSlide.toPx()
                        }
                ) {
                    PlayerControls(
                        uiState = uiState,
                        onIntent = onIntent,
                        onInteract = ::touch
                    )
                }
            }
        }

        // 频谱条压在导航栏留白里：放在最下沿不干扰版式。跟手层的一员，收起时一起滑走。
        if (uiState.playerSpectrumEnabled) {
            BeatSpectrum(
                beat = { if (uiState.isPlaying) beat() else BeatState.Idle },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(4.dp)
            )
        }
    }
}

/**
 * 播放页那张「纸」的底面：动态色面 + 上下压深的 scrim。
 *
 * 由 [PlayerOverlay] 的卡片层绘制，而不是画在页面内容里 —— 收起时卡片会被压成迷你胶囊
 * 的形状，色面必须跟着一起变形；内容层则被单独裁掉、淡出。两层分开，色面就不会在
 * 「内容已经没了、卡片还在缩」的那一段留下一个空洞。
 */
@Composable
internal fun PlayerCardBackdrop(playing: Boolean, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val scrim = colorScheme.scrim

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sonDynamicBackdrop(playing = playing, dim = 0f)
        )
        // 上下压深、中间放亮：顶部要压住状态栏图标，底部要托住控制区，中间留给色面呼吸。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = SceneScrimAlpha + 0.1f),
                        0.35f to scrim.copy(alpha = SceneScrimAlpha - 0.16f),
                        1f to scrim.copy(alpha = SceneScrimAlpha + 0.2f)
                    )
                )
        )
    }
}

/**
 * 顶部把手：36×5dp 的横条。整条把手行是一条 [PlayerSceneSpec.grabberTouchHeight] 高的拖拽区，
 * 下拖跟手，超过阈值松手就收起。
 */
@Composable
private fun SceneGrabber(
    dragModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val extended = LocalSonExtendedColors.current

    Box(
        modifier = modifier
            .height(PlayerSceneSpec.grabberTouchHeight)
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = PlayerSceneSpec.grabberWidth,
                    height = PlayerSceneSpec.grabberHeight
                )
                .background(extended.onArtwork.copy(alpha = 0.32f), CircleShape)
        )
    }
}

// ── 封面页 ────────────────────────────────────────────────────────────────

/**
 * 封面页：居中满宽方形封面 + 底部标题行。
 *
 * 播放时封面回到 1.0，暂停时回缩到 0.94 —— 与 Apple Music 一致，用极轻的缩放表达状态，
 * 幅度再大就会把封面拉糊。
 *
 * 收起进度没走完时封面自己是不画的：那段时间由 [PlayerArtworkFlight] 独占这一张图，
 * 展开到 [ArtworkFlightEnd] 之后再把节点交回来（两端矩形重合，交接看不出来）。
 */
@Composable
private fun ArtworkPage(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit,
    dragModifier: Modifier,
    artworkOwned: Boolean,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope
) {
    val extended = LocalSonExtendedColors.current
    val song = uiState.currentSong
    val transition = LocalPlayerArtworkTransition.current
    val scale by animateFloatAsState(
        targetValue = if (uiState.isPlaying) 1f else ArtworkPausedScale,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "artworkScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 封面页没有可滚动内容，整页都能拉着收起 —— iOS 就是在这一页任意位置往下拖。
            .then(dragModifier)
            .padding(
                top = PlayerSceneSpec.artworkTop,
                start = PlayerSceneSpec.contentSide,
                end = PlayerSceneSpec.contentSide,
                bottom = PlayerSceneSpec.artworkBottom
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val coverShape = MaterialTheme.shapes.extraLarge
            val coverShadowPx = with(LocalDensity.current) { PlayerSceneSpec.artworkShadow.toPx() }
            // 测量挂在没有缩放的那一层：暂停回缩会把它带偏，飞行的终点就不准了。
            Box(
                modifier = Modifier
                    // alpha 必须在最外层（连投影一起藏）：放在投影里面会留下一块空阴影。
                    .graphicsLayer { alpha = if (artworkOwned) 0f else 1f }
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .sharedArtwork(sharedScope, animatedScope)
                    .onGloballyPositioned { coords -> transition?.recordCover(coords.rootRect()) }
            ) {
                M3Artwork(
                    artwork = song?.albumArt,
                    placeholderGlyph = SonGlyph.Music,
                    shape = coverShape,
                    contentDescription = song?.title,
                    // 阴影和封面必须写在**同一个 layer**：分开两层时暂停回缩只缩内容，
                    // 阴影仍按未缩放的方框落笔，看起来就是封面外面套了一圈"框"。
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            if (uiState.isPlaying) {
                                shadowElevation = coverShadowPx
                                shape = coverShape
                                clip = false
                            }
                        }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PlayerSceneSpec.artworkTitleGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: stringResource(R.string.player_now_playing),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = extended.onArtwork,
                    maxLines = 1,
                    // 长标题跑马灯：截断成「I Knew You Were Tr…」既认不出歌，也没法确认点的是哪首。
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song?.artist.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = extended.onArtwork.copy(alpha = SubtitleAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (uiState.hiResBadge != HiResBadge.OFF) {
                        Spacer(modifier = Modifier.width(SonSpacing.small))
                        // Apple Music 这一位是「Hi-Res Lossless」字样，不是图标。
                        Text(
                            text = stringResource(R.string.player_hires_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.onArtwork.copy(alpha = SubtitleAlpha)
                        )
                    }
                }
            }

            NowPlayingActions(
                isFavorite = uiState.isFavorite,
                onToggleFavorite = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleFavorite)
                },
                onOpenSettings = {
                    onInteract()
                    onIntent(PlayerIntent.TogglePlaybackSettings)
                }
            )
        }
    }
}

/**
 * 收藏 + 更多。
 *
 * 用 Material 的 [IconButton] 而不是自绘触点：按压反馈（涟漪与形状变形）由组件负责，
 * 这里只管语义与图标。
 */
@Composable
private fun NowPlayingActions(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenSettings: () -> Unit,
    /** 右边那颗的图标。封面页与队列页用三点，歌词页换成歌词图标（见 [NowPlayingBar]）。 */
    @DrawableRes overflowIcon: Int = R.drawable.ic_flamingo_more,
    overflowDescription: String? = null
) {
    val extended = LocalSonExtendedColors.current
    val overflowLabel = overflowDescription ?: stringResource(R.string.player_settings_desc)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleFavorite) {
            FlamingoIcon(
                res = if (isFavorite) {
                    R.drawable.ic_flamingo_favorite_filled
                } else {
                    R.drawable.ic_flamingo_favorite
                },
                contentDescription = if (isFavorite) {
                    stringResource(R.string.player_unfavorite_desc)
                } else {
                    stringResource(R.string.player_favorite_desc)
                },
                tint = extended.onArtwork,
                size = PlayerSceneSpec.headerIcon
            )
        }
        IconButton(onClick = onOpenSettings) {
            FlamingoIcon(
                res = overflowIcon,
                contentDescription = overflowLabel,
                tint = extended.onArtwork,
                size = PlayerSceneSpec.headerIcon
            )
        }
    }
}

// ── 歌词页 / 队列页的顶部歌曲栏 ────────────────────────────────────────────

/**
 * 歌词页 / 队列页顶部的歌曲栏：48dp 封面 + 标题 + 收藏 / 更多。
 *
 * 封面是播放页共享元素的一端：点它回封面页时，封面从 48dp 展开成满宽大图。
 */
@Composable
fun NowPlayingBar(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onArtworkClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOverflow: (() -> Unit)? = null,
    /**
     * 顶栏是否放「译文」开关。
     *
     * 只有歌词页传 true。即使传了，当前歌词一个译文都没有时也不显示 —— 一个按了没反应的
     * 开关比没有这个入口更让人困惑。
     */
    showTranslationToggle: Boolean = false,
    /**
     * 右侧那颗按钮的图标与语义。
     *
     * 歌词页面板里全是歌词相关的项（切换 / 重新获取 / 界面设置），用「更多」三点表达不对，
     * 那边传歌词图标；播放页与队列页保持三点。
     */
    @DrawableRes overflowIcon: Int = R.drawable.ic_flamingo_more,
    overflowDescription: String? = null,
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null
) {
    val extended = LocalSonExtendedColors.current
    val song = uiState.currentSong
    // 译文是逐行可有可无的：整份歌词一行都没有翻译时（纯外语 LRC 常见）就没有可开关的东西
    val hasTranslation = remember(uiState.lyrics) { uiState.lyrics?.hasTranslation == true }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerSceneSpec.headerHeight)
            .padding(horizontal = PlayerSceneSpec.contentSide),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Artwork(
            artwork = song?.albumArt,
            placeholderGlyph = SonGlyph.Music,
            shape = MaterialTheme.shapes.small,
            contentDescription = song?.title,
            modifier = Modifier
                .size(PlayerSceneSpec.headerArtwork)
                .sharedArtwork(sharedScope, animatedScope)
                .clickable(onClick = onArtworkClick)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = PlayerSceneSpec.headerTextGap)
        ) {
            Text(
                text = song?.title ?: stringResource(R.string.player_now_playing),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = extended.onArtwork,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = song?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.onArtwork.copy(alpha = SubtitleAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showTranslationToggle && hasTranslation) {
            val translationOn = uiState.lyricsTranslationEnabled
            IconButton(
                onClick = {
                    onIntent(PlayerIntent.SetLyricsTranslationEnabled(!translationOn))
                }
            ) {
                FlamingoIcon(
                    res = if (translationOn) {
                        R.drawable.ic_flamingo_translate_on
                    } else {
                        R.drawable.ic_flamingo_translate
                    },
                    contentDescription = stringResource(
                        if (translationOn) {
                            R.string.lyrics_translation_hide_desc
                        } else {
                            R.string.lyrics_translation_show_desc
                        }
                    ),
                    tint = extended.onArtwork.copy(alpha = if (translationOn) 1f else SubtitleAlpha),
                    size = PlayerSceneSpec.headerIcon
                )
            }
        }
        NowPlayingActions(
            isFavorite = uiState.isFavorite,
            onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) },
            onOpenSettings = onOverflow ?: { onIntent(PlayerIntent.TogglePlaybackSettings) },
            overflowIcon = overflowIcon,
            overflowDescription = overflowDescription
        )
    }
}

// ── 歌词页 ────────────────────────────────────────────────────────────────

/**
 * 歌词页。
 *
 * 版式与独立歌词路由共用 [LyricsPage]，这里只负责装配顶部歌曲栏、控制区唤出，
 * 以及在深色底上取白字（`onDarkSurface = true`）。
 */
@Composable
private fun LyricsScene(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit,
    overscrollModifier: Modifier,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope
) {
    LyricsPage(
        uiState = uiState,
        onIntent = onIntent,
        onDarkSurface = true,
        modifier = Modifier
            .fillMaxSize()
            .then(overscrollModifier)
            .revealOnTap(onInteract),
        // 歌词铺满整屏、从顶栏后面淡出；焦点行要让出栏高，否则高亮行会被压住
        headerInset = PlayerSceneSpec.headerHeight,
        header = {
            NowPlayingBar(
                uiState = uiState,
                onIntent = onIntent,
                onArtworkClick = {
                    onInteract()
                    onIntent(PlayerIntent.SetViewMode(PlayerViewMode.COVER))
                },
                showTranslationToggle = true,
                // 这一页的右上角就是「换一份歌词」：不管当前这份是内嵌的、旁车的还是自动下载的，
                // 都能在这里换成任意在线平台的版本。以前这颗按钮是「更多 → 播放设置」，
                // 于是有歌词的时候反而没有任何换词的入口；播放设置仍在封面页的三点里，没有丢。
                onOverflow = {
                    onInteract()
                    onIntent(PlayerIntent.OpenLyricsCandidates)
                },
                overflowIcon = R.drawable.ic_flamingo_lyrics,
                overflowDescription = stringResource(R.string.lyrics_pick_online),
                sharedScope = sharedScope,
                animatedScope = animatedScope
            )
        }
    )
}

/**
 * 只观察不消费的「点一下唤出控制区」。
 *
 * 不能用 `clickable`：它会拦下 down，底下的歌词列表就再也滚不动。这里在 Initial 阶段
 * 旁听，位移超过 touchSlop 判为滑动、事件原样留给列表，只有「按下未动就抬起」才回调。
 */
private fun Modifier.revealOnTap(onReveal: () -> Unit): Modifier = pointerInput(onReveal) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var moved = false
            var released = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    released = true
                } else if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    moved = true
                }
                if (event.changes.all { !it.pressed }) break
            }
            if (released && !moved) onReveal()
        }
    }
}

// ── 队列页 ────────────────────────────────────────────────────────────────

/** 队列页：随机 / 循环两个开关 + 可拖动重排的当前队列。 */
@Composable
private fun QueueScene(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope
) {
    val extended = LocalSonExtendedColors.current
    Column(Modifier.fillMaxSize()) {
        NowPlayingBar(
            uiState = uiState,
            onIntent = onIntent,
            onArtworkClick = {
                onInteract()
                onIntent(PlayerIntent.SetViewMode(PlayerViewMode.COVER))
            },
            sharedScope = sharedScope,
            animatedScope = animatedScope
        )

        // 四档播放模式：随机 / 循环 / 智能续播 / 淡入淡出。
        // 版式照 MeloX 的队列面板：等宽胶囊，选中那档反白（白 70% 底 + 黑图标），
        // 未选中是白 12% 底 + 白图标 —— 队列页永远压在封面色面上，所以这里只做浅色档。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PlayerSceneSpec.contentSide,
                    end = PlayerSceneSpec.contentSide,
                    top = SonSpacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
        ) {
            ModePill(
                icon = R.drawable.ic_flamingo_shuffle,
                active = uiState.shuffleMode,
                description = stringResource(R.string.player_shuffle_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleShuffleMode)
                },
                modifier = Modifier.weight(1f)
            )
            ModePill(
                icon = if (uiState.repeatMode == RepeatMode.ONE) {
                    R.drawable.ic_flamingo_repeat_one
                } else {
                    R.drawable.ic_flamingo_repeat
                },
                active = uiState.repeatMode != RepeatMode.OFF,
                description = stringResource(R.string.player_repeat_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleRepeatMode)
                },
                modifier = Modifier.weight(1f)
            )
            ModePill(
                icon = R.drawable.ic_mode_infinity,
                active = uiState.autoContinueEnabled,
                description = stringResource(R.string.player_auto_continue_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleAutoContinue)
                },
                modifier = Modifier.weight(1f)
            )
            ModePill(
                icon = R.drawable.ic_mode_automix,
                active = uiState.audioFadeEnabled,
                description = stringResource(R.string.player_automix_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleAudioFade)
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.playbackQueue.isNotEmpty()) {
            Text(
                text = "${(uiState.queueIndex + 1).coerceAtLeast(0)} / ${uiState.playbackQueue.size}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PlayerSceneSpec.contentSide,
                        end = PlayerSceneSpec.contentSide,
                        top = SonSpacing.extraSmall
                    ),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                color = extended.onArtwork.copy(alpha = SubtitleAlpha)
            )
        }

        if (uiState.playbackQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.player_empty_queue),
                    style = MaterialTheme.typography.bodyLarge,
                    color = extended.onArtwork.copy(alpha = SubtitleAlpha)
                )
            }
        } else {
            DraggableQueueList(
                queue = uiState.playbackQueue,
                currentIndex = uiState.queueIndex,
                onPlayFromQueue = { index ->
                    onInteract()
                    onIntent(PlayerIntent.PlayFromQueue(index))
                },
                onRemoveFromQueue = { index -> onIntent(PlayerIntent.RemoveFromQueue(index)) },
                onMoveItem = { from, to -> onIntent(PlayerIntent.MoveQueueItem(from, to)) },
                onDarkSurface = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = SonSpacing.small)
                    .padding(top = SonSpacing.small)
            )
        }
    }
}

/**
 * 队列页的一颗模式胶囊：48dp 高、22dp 圆角，选中档反白。
 *
 * 底色直接用白色透明度而不是主题色 —— 这排按钮永远铺在封面色面（深色玻璃）上，
 * 跟着明暗主题走反而会在浅色封面上糊成一片。
 */
@Composable
private fun ModePill(
    @DrawableRes icon: Int,
    active: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(PlayerSceneSpec.selectorHeight)
            .clip(RoundedCornerShape(22.dp))
            .background(
                when {
                    active -> Color.White.copy(alpha = if (pressed) 0.82f else 0.70f)
                    pressed -> Color.White.copy(alpha = 0.20f)
                    else -> Color.White.copy(alpha = 0.12f)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { selected = active },
        contentAlignment = Alignment.Center
    ) {
        FlamingoIcon(
            res = icon,
            contentDescription = description,
            tint = if (active) Color.Black.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.86f),
            size = PlayerSceneSpec.headerIcon
        )
    }
}
