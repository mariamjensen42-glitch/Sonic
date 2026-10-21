package com.cycling.presentation.lyrics.components

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastRoundToInt
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonMotionTiming
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.lyrics.model.ISyncedLine
import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.karaoke.KaraokeAlignment
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.presentation.lyrics.utils.isRtl
import com.cycling.presentation.lyrics.utils.modifier.dynamicFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** 行尾滚动余量：让最后几行也能滚到高亮位置。 */
private val ScrollTailHeight = 2000.dp

/**
 * 目标行离视口首行超过这么多行时，直接跳过去而不是滚动过去。
 *
 * 正常播放每次只前进一行（动画跟手）；拖动进度条、切歌、重新对齐则可能是几十行 —— 那一段
 * 动画会边滚边组合中间所有行，是「歌词页一进来就卡」的来源。
 */
private const val MAX_ANIMATED_SCROLL_LINES = 2

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    isPlaying: Boolean,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        textMotion = TextMotion.Animated,
    ),
    accompanimentLineTextStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
        textMotion = TextMotion.Animated,
    ),
    textColor: Color = Color.Unspecified,
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    useBlurEffect: Boolean = true,
    /** 行文本在可用宽度内居中；关掉就是按语言方向靠起始侧。 */
    alignCenter: Boolean = false,
    offset: Dp = SonSpacing.extraLarge,
) {
    val density = LocalDensity.current
    val extended = LocalSonExtendedColors.current
    val motionScheme = MaterialTheme.motionScheme
    val activeTextColor = if (textColor == Color.Unspecified) extended.lyricsActive else textColor
    val mutedTextColor = extended.lyricsInactive
    val fadeMaskColor = MaterialTheme.colorScheme.surface

    val stableNormalTextStyle = remember(normalLineTextStyle) { normalLineTextStyle }
    val stableAccompanimentTextStyle =
        remember(accompanimentLineTextStyle) { accompanimentLineTextStyle }
    val stableOffset = remember(offset) { offset }
    val stableOffsetPx =
        remember(stableOffset) { with(density) { stableOffset.toPx().fastRoundToInt() } }

    val textMeasurer = rememberTextMeasurer()
    // 一次性写入的布局缓存：逐条写入会让每个已读过 map 的行都重组一次，代价是 O(n²)
    val layoutCache = remember { mutableStateOf<Map<Int, List<SyllableLayout>>?>(null) }

    LaunchedEffect(lyrics, stableNormalTextStyle, stableAccompanimentTextStyle) {
        layoutCache.value = null
        val measured = withContext(Dispatchers.Default) {
            val normalStyle = stableNormalTextStyle.copy(textDirection = TextDirection.Content)
            val accompanimentStyle =
                stableAccompanimentTextStyle.copy(textDirection = TextDirection.Content)

            val normalSpaceWidth = textMeasurer.measure(" ", normalStyle).size.width.toFloat()
            val accompanimentSpaceWidth =
                textMeasurer.measure(" ", accompanimentStyle).size.width.toFloat()

            val result = HashMap<Int, List<SyllableLayout>>()
            lyrics.lines.forEachIndexed { index, line ->
                if (!isActive) return@withContext result
                if (line is KaraokeLine) {
                    val style = if (line.isAccompaniment) accompanimentStyle else normalStyle
                    val spaceWidth =
                        if (line.isAccompaniment) accompanimentSpaceWidth else normalSpaceWidth

                    val processedSyllables = if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }

                    result[index] = measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = style,
                        isAccompanimentLine = line.isAccompaniment,
                        spaceWidth = spaceWidth
                    )
                }
            }
            result
        }
        layoutCache.value = measured
    }

    val currentTimeMs: () -> Int = currentPosition

    val timeProvider = remember { currentPosition }

    // 每帧 tick：只被绘制阶段读取，用于逐帧刷新进度而不触发重组。
    // 只有 Animating 状态的行会去读它，静态行不会每帧失效，
    // 离屏层 / 模糊效果因此可以复用，也避免每帧重做 saveLayer
    //
    // 只在**真正在播放**时跑，并且尊重系统动画缩放。暂停时没有行在读它（实测 0 帧），
    // tick 只是每 vsync 空唤醒一次；系统关动画时它却会让整页继续按刷新率重绘
    // （`animator_duration_scale=0` 实测仍是 124 fps）—— 那时本该停在第一帧。
    val frameTick = remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying || ValueAnimator.getDurationScale() <= 0f) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { frameTick.intValue++ }
        }
    }
    val animatedTimeProvider = remember { { frameTick.intValue; timeProvider() } }

    val firstFocusedLineIndex by remember(lyrics.lines) {
        derivedStateOf {
            val time = currentTimeMs()
            val allActiveIndices = lyrics.getCurrentAllHighlightLineIndicesByTime(time)

            if (allActiveIndices.isNotEmpty()) {
                val currentMainLine = allActiveIndices.find { index ->
                    val line = lyrics.lines.getOrNull(index)
                    line !is KaraokeLine || !line.isAccompaniment
                }

                if (currentMainLine != null) {
                    currentMainLine
                } else {
                    val firstActiveAccomp = allActiveIndices.first()
                    var targetIndex = firstActiveAccomp
                    for (i in firstActiveAccomp downTo 0) {
                        val line = lyrics.lines.getOrNull(i)
                        if (line !is KaraokeLine || !line.isAccompaniment) {
                            targetIndex = i
                            break
                        }
                    }
                    targetIndex
                }
            } else {
                val nextIndex = lyrics.lines.indexOfFirst { it.start > time }
                if (nextIndex != -1) {
                    var targetIndex = nextIndex
                    for (i in nextIndex downTo 0) {
                        val line = lyrics.lines.getOrNull(i)
                        if (line !is KaraokeLine || !line.isAccompaniment) {
                            targetIndex = i
                            break
                        }
                    }
                    targetIndex
                } else {
                    lyrics.lines.lastIndex
                }
            }
        }
    }

    val firstLine = lyrics.lines.firstOrNull()

    val haveDotsIntro by remember(firstLine) {
        derivedStateOf {
            if (firstLine == null) false
            else (firstLine.start > 5000)
        }
    }

    val allFocusedLineIndex by remember(lyrics) {
        derivedStateOf {
            lyrics.getCurrentAllHighlightLineIndicesByTime(currentTimeMs())
        }
    }

    // 已经开始（start <= now）的最后一行索引：用于区分"已唱完"和"还没开始"
    val currentStartedLineIndex by remember(lyrics) {
        derivedStateOf {
            val time = currentTimeMs()
            lyrics.lines.indexOfLast { it.start <= time }
        }
    }

    val accompanimentVisibilityRanges = remember(lyrics.lines) {
        val map = mutableMapOf<Int, IntRange>()
        val mainLines = lyrics.lines.filter { it !is KaraokeLine || !it.isAccompaniment }
        if (mainLines.isNotEmpty()) {
            lyrics.lines.forEachIndexed { index, line ->
                if (line is KaraokeLine && line.isAccompaniment) {
                    val entryAnchor =
                        mainLines.findLast { it.start <= line.start } ?: mainLines.firstOrNull()
                    val exitAnchor =
                        mainLines.firstOrNull { it.end >= line.end } ?: mainLines.lastOrNull()
                    val visualStartTime = (entryAnchor?.start ?: line.start) - 600
                    val visualEndTime = (exitAnchor?.end ?: line.end) + 600
                    map[index] = visualStartTime..visualEndTime
                }
            }
        }
        map
    }

    val scrollInCode = remember { mutableStateOf(false) }

    // 进页面 / 换歌词后的第一次对齐是否已经做过。
    // 第一次必须是**瞬移**，不能动画：从第 0 行「滚」到当前行会一边滚一边组合中间每一行
    // （每行都要做音节测量、渐隐遮罩、模糊层），几十行的距离能卡住好几帧到几百毫秒 ——
    // 用户看到的就是「进歌词页时很卡地滚到当前歌词」。
    var initialAligned by remember(lyrics) { mutableStateOf(false) }

    LaunchedEffect(
        firstFocusedLineIndex,
        layoutCache,
        stableOffsetPx,
    ) {
        if (scrollInCode.value) return@LaunchedEffect
        val target = firstFocusedLineIndex
        val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }
        try {
            scrollInCode.value = true
            when {
                // 第一次对齐：直接跳到当前行
                !initialAligned -> {
                    initialAligned = true
                    listState.scrollToItem(target, -stableOffsetPx)
                }
                // 目标行已经在屏幕上：只做小幅修正，动画跟手
                targetItem != null -> {
                    val scrollOffset =
                        (targetItem.offset -
                            (listState.layoutInfo.viewportStartOffset + stableOffsetPx)).toFloat()
                    listState.animateScrollBy(scrollOffset, SonMotionTiming.scrollSpec())
                }
                // 拖动进度条或跨越多行：同样直接跳，别让列表一行行滚过去
                abs(target - listState.firstVisibleItemIndex) > MAX_ANIMATED_SCROLL_LINES -> {
                    listState.scrollToItem(target, -stableOffsetPx)
                }
                else -> listState.animateScrollToItem(target, -stableOffsetPx)
            }
        } catch (_: Exception) {
        } finally {
            scrollInCode.value = false
        }
    }

    Crossfade(targetState = lyrics, animationSpec = motionScheme.defaultEffectsSpec()) { lyrics ->
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = stableOffset)
        ) {
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.start}-${line.end}-$index" }
            ) { index, line ->
                val isCurrentFocusLine by rememberUpdatedState(index in allFocusedLineIndex)

                // 只有正在推进的行需要逐帧重绘，静态行不该随时间变化
                val linePhase = when {
                    index in allFocusedLineIndex -> KaraokeLinePhase.Animating
                    index <= currentStartedLineIndex -> KaraokeLinePhase.AfterEnd
                    else -> KaraokeLinePhase.BeforeStart
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dynamicFadingEdge(listState, index, fadeMaskColor)
                ) {
                    val animDuration = SonMotionTiming.ACCOMPANIMENT_MS

                    val previousLine = lyrics.lines.getOrNull(index - 1)

                    val showDotsInterlude by remember(line, previousLine) {
                        derivedStateOf {
                            val currentTime = currentPosition()
                            (previousLine != null && (line.start - previousLine.end > 5000) && (currentTime in previousLine.end..line.start))
                        }
                    }
                    val showDotsIntro by remember(firstLine) {
                        derivedStateOf {
                            haveDotsIntro && (currentTimeMs() in 0 until firstLine!!.start) && index == 0
                        }
                    }

                    AnimatedVisibility(showDotsInterlude || showDotsIntro) {
                        KaraokeBreathingDots(
                            alignment = when (val line = previousLine ?: firstLine) {
                                is KaraokeLine -> line.alignment
                                is SyncedLine -> if (line.content.isRtl()) KaraokeAlignment.End else KaraokeAlignment.Start
                                else -> KaraokeAlignment.Start
                            },
                            startTimeMs = previousLine?.end ?: 0,
                            endTimeMs = if (showDotsIntro) firstLine!!.start else line.start,
                            currentTimeProvider = animatedTimeProvider,
                            defaults = breathingDotsDefaults,
                            center = alignCenter,
                            modifier = Modifier.padding(vertical = SonSpacing.medium)
                        )
                    }

                    val distanceWeightState =
                        remember(useBlurEffect) {
                            derivedStateOf {
                                if (!useBlurEffect) return@derivedStateOf 0

                                val (baseStart, baseEnd) = if (allFocusedLineIndex.isNotEmpty()) {
                                    allFocusedLineIndex.first() to allFocusedLineIndex.last()
                                } else {
                                    val next = (currentStartedLineIndex + 1)
                                        .coerceAtMost(lyrics.lines.lastIndex)
                                    next to next
                                }

                                when {
                                    index < baseStart -> baseStart - index
                                    index > baseEnd -> index - baseEnd
                                    else -> 0
                                }.coerceAtMost(10)
                            }
                        }

                    val blurRadiusState = animateFloatAsState(
                        targetValue = if (distanceWeightState.value > 0 && (!listState.isScrollInProgress || scrollInCode.value)) {
                            distanceWeightState.value * 4f
                        } else {
                            0f
                        },
                        animationSpec = motionScheme.defaultEffectsSpec(),
                        label = "blurRadius"
                    )

                    val lineAlpha by animateFloatAsState(
                        targetValue = if (isCurrentFocusLine) 1f else 0.5f,
                        animationSpec = motionScheme.defaultEffectsSpec(),
                        label = "lineAlpha"
                    )

                    when (line) {
                        is KaraokeLine -> {
                            val isLineRtl =
                                remember(line.syllables) { line.syllables.any { it.content.isRtl() } }
                            val isVisualRightAligned = remember(line.alignment, isLineRtl) {
                                when (line.alignment) {
                                    KaraokeAlignment.Start, KaraokeAlignment.Unspecified -> isLineRtl
                                    KaraokeAlignment.End -> !isLineRtl
                                }
                            }

                            if (!line.isAccompaniment) {
                                LyricsLineItem(
                                    isFocused = isCurrentFocusLine,
                                    isRightAligned = isVisualRightAligned,
                                    isCentered = alignCenter,
                                    onLineClicked = { onLineClicked(line) },
                                    onLinePressed = { onLinePressed(line) },
                                    blurRadius = { blurRadiusState.value },
                                    activeAlpha = lineAlpha,
                                    inactiveAlpha = 0.5f,
                                ) {
                                    KaraokeLineText(
                                        line = line,
                                        currentTimeProvider = animatedTimeProvider,
                                        phase = linePhase,
                                        normalLineTextStyle = stableNormalTextStyle,
                                        accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                        activeColor = activeTextColor,
                                        alignCenter = alignCenter,
                                        precalculatedLayouts = layoutCache.value?.get(index)
                                    )
                                }
                            } else {
                                val visibilityRange = accompanimentVisibilityRanges[index]
                                val isAccompanimentVisible by remember(visibilityRange) {
                                    derivedStateOf {
                                        val currentTime = currentTimeMs()
                                        if (visibilityRange != null) {
                                            currentTime in visibilityRange
                                        } else {
                                            currentTime >= (line.start - animDuration) && currentTime <= (line.end + animDuration)
                                        }
                                    }
                                }

                                val accompanimentOrigin = TransformOrigin(
                                    if (isVisualRightAligned) 1f else 0f, 0f
                                )

                                AnimatedVisibility(
                                    visible = isAccompanimentVisible,
                                    enter = scaleIn(
                                        SonMotionTiming.accompanimentSpec(entering = true),
                                        transformOrigin = accompanimentOrigin
                                    ) + fadeIn(
                                        SonMotionTiming.accompanimentSpec(entering = true)
                                    ) + slideInVertically(
                                        SonMotionTiming.accompanimentSpec(entering = true)
                                    ) + expandVertically(
                                        SonMotionTiming.accompanimentSpec(entering = true)
                                    ),
                                    exit = scaleOut(
                                        SonMotionTiming.accompanimentSpec(entering = false),
                                        transformOrigin = accompanimentOrigin
                                    ) + fadeOut(
                                        SonMotionTiming.accompanimentSpec(entering = false)
                                    ) + slideOutVertically(
                                        SonMotionTiming.accompanimentSpec(entering = false)
                                    ) + shrinkVertically(
                                        SonMotionTiming.accompanimentSpec(entering = false)
                                    ),
                                ) {
                                    LyricsLineItem(
                                        isFocused = isCurrentFocusLine,
                                        isRightAligned = isVisualRightAligned,
                                        isCentered = alignCenter,
                                        onLineClicked = { onLineClicked(line) },
                                        onLinePressed = { onLinePressed(line) },
                                        blurRadius = { blurRadiusState.value },
                                        activeAlpha = 0.6f,
                                        inactiveAlpha = 0.3f
                                    ) {
                                        KaraokeLineText(
                                            line = line,
                                            currentTimeProvider = animatedTimeProvider,
                                            phase = linePhase,
                                            normalLineTextStyle = stableNormalTextStyle,
                                            accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                            activeColor = activeTextColor,
                                            alignCenter = alignCenter,
                                            precalculatedLayouts = layoutCache.value?.get(index)
                                        )
                                    }
                                }
                            }
                        }

                        is SyncedLine -> {
                            val isLineRtl = remember(line.content) { line.content.isRtl() }
                            LyricsLineItem(
                                isFocused = isCurrentFocusLine,
                                isRightAligned = isLineRtl,
                                isCentered = alignCenter,
                                onLineClicked = { onLineClicked(line) },
                                onLinePressed = { onLinePressed(line) },
                                blurRadius = { blurRadiusState.value },
                                activeAlpha = lineAlpha,
                                inactiveAlpha = 0.5f,
                            ) {
                                SyncedLineText(
                                    line = line,
                                    isLineRtl = isLineRtl,
                                    textStyle = stableNormalTextStyle.copy(lineHeight = 1.2.em),
                                    textColor = if (isCurrentFocusLine) activeTextColor else mutedTextColor,
                                    alignCenter = alignCenter,
                                )
                            }
                        }
                    }
                }
            }
            item("BottomSpacing") {
                Spacer(
                    modifier = Modifier.fillMaxWidth().height(ScrollTailHeight)
                )
            }
        }
    }
}
