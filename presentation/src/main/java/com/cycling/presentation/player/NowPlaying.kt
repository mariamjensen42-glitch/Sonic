@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.cycling.presentation.player

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.SonGlyph
import com.cycling.core.ui.components.SonGlyphIcon
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

    /** 下拉超过这个距离就收起到迷你播放器。 */
    const val DISMISS_PX = 120f

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
    val timeRowHeight = 16.dp

    val transportSize = 56.dp
    val transportIcon = 34.dp
    val playIcon = 48.dp
    /** control_buttons_distance_padding —— 运输键之间的实测间距 */
    val transportGap = 40.dp

    val selectorHeight = 48.dp
    val selectorIcon = 24.dp

    /** 控制区在布局里的固定高度：隐藏时只让内容淡出，不塌陷，歌词不会因此跳一行。 */
    val controlsHeight = 208.dp

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
    onNavigateBack: () -> Unit,
    beat: () -> BeatState,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    val onLyricsPage = uiState.viewMode == PlayerViewMode.LYRICS

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
                .sonDynamicBackdrop(playing = uiState.isPlaying, dim = 0f)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SceneGrabber(
                onMinimize = onNavigateBack,
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
                            sharedScope = sharedScope,
                            animatedScope = this@AnimatedContent
                        )

                        PlayerViewMode.LYRICS -> LyricsScene(
                            uiState = uiState,
                            onIntent = onIntent,
                            onInteract = ::touch,
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

        // 频谱条压在导航栏留白里：放在最下沿不干扰版式。
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
 * 顶部把手：36×5dp 的横条，下拖超过 [PlayerSceneSpec.DISMISS_PX] 收起到迷你播放器。
 */
@Composable
private fun SceneGrabber(
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSonExtendedColors.current
    var dragged by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = PlayerSceneSpec.grabberWidth,
                    height = PlayerSceneSpec.grabberHeight
                )
                .background(extended.onArtwork.copy(alpha = 0.32f), CircleShape)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged > PlayerSceneSpec.DISMISS_PX) onMinimize()
                            dragged = 0f
                        },
                        onDragCancel = { dragged = 0f },
                        onVerticalDrag = { _, amount -> dragged += amount }
                    )
                }
        )
    }
}

// ── 封面页 ────────────────────────────────────────────────────────────────

/**
 * 封面页：居中满宽方形封面 + 底部标题行。
 *
 * 播放时封面回到 1.0，暂停时回缩到 0.94 —— 与 Apple Music 一致，用极轻的缩放表达状态，
 * 幅度再大就会把封面拉糊。
 */
@Composable
private fun ArtworkPage(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onInteract: () -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope
) {
    val extended = LocalSonExtendedColors.current
    val song = uiState.currentSong
    val scale by animateFloatAsState(
        targetValue = if (uiState.isPlaying) 1f else ArtworkPausedScale,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "artworkScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            val coverModifier = if (uiState.isPlaying) {
                Modifier.shadow(
                    elevation = 28.dp,
                    shape = MaterialTheme.shapes.extraLarge,
                    clip = false
                )
            } else {
                Modifier
            }
            M3Artwork(
                artwork = song?.albumArt,
                placeholderGlyph = SonGlyph.Music,
                shape = MaterialTheme.shapes.extraLarge,
                contentDescription = song?.title,
                modifier = coverModifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .sharedArtwork(sharedScope, animatedScope)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
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
                    overflow = TextOverflow.Ellipsis
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
    onOpenSettings: () -> Unit
) {
    val extended = LocalSonExtendedColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleFavorite) {
            SonGlyphIcon(
                glyph = SonGlyph.Heart,
                filled = isFavorite,
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
            SonGlyphIcon(
                glyph = SonGlyph.Ellipsis,
                contentDescription = stringResource(R.string.player_settings_desc),
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
    sharedScope: SharedTransitionScope? = null,
    animatedScope: AnimatedVisibilityScope? = null
) {
    val extended = LocalSonExtendedColors.current
    val song = uiState.currentSong

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
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.onArtwork.copy(alpha = SubtitleAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        NowPlayingActions(
            isFavorite = uiState.isFavorite,
            onToggleFavorite = { onIntent(PlayerIntent.ToggleFavorite) },
            onOpenSettings = onOverflow ?: { onIntent(PlayerIntent.TogglePlaybackSettings) }
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
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope
) {
    LyricsPage(
        uiState = uiState,
        onIntent = onIntent,
        onDarkSurface = true,
        modifier = Modifier
            .fillMaxSize()
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PlayerSceneSpec.contentSide,
                    end = PlayerSceneSpec.contentSide,
                    top = SonSpacing.small
                )
                .height(PlayerSceneSpec.selectorHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonSpacing.extraSmall)
        ) {
            ModeToggle(
                glyph = SonGlyph.Shuffle,
                active = uiState.shuffleMode,
                description = stringResource(R.string.player_shuffle_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleShuffleMode)
                }
            )
            ModeToggle(
                glyph = if (uiState.repeatMode == RepeatMode.ONE) {
                    SonGlyph.RepeatOne
                } else {
                    SonGlyph.Repeat
                },
                active = uiState.repeatMode != RepeatMode.OFF,
                description = stringResource(R.string.player_repeat_desc),
                onClick = {
                    onInteract()
                    onIntent(PlayerIntent.ToggleRepeatMode)
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(uiState.queueIndex + 1).coerceAtLeast(0)} / ${uiState.playbackQueue.size}",
                style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun ModeToggle(
    glyph: SonGlyph,
    active: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val extended = LocalSonExtendedColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(PlayerSceneSpec.headerActionSize)) {
        SonGlyphIcon(
            glyph = glyph,
            contentDescription = description,
            tint = extended.onArtwork.copy(alpha = if (active) 1f else SubtitleAlpha),
            size = PlayerSceneSpec.headerIcon
        )
    }
}
