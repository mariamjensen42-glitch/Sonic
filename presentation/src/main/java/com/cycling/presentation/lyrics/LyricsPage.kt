package com.cycling.presentation.lyrics

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.core.ui.theme.LocalSonExtendedColors
import com.cycling.core.ui.theme.SonFontFamily
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.lyrics.model.ISyncedLine
import com.cycling.presentation.R
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.lyrics.components.KaraokeLyricsView
import com.cycling.presentation.player.PlayerIntent
import com.cycling.presentation.player.PlayerUiState

/**
 * 当前行的锚点：距视口顶部 112dp。
 *
 * 与列表的 contentPadding 取同一个值，歌曲前几行才不会被列表起点夹住、等滚到能滚的位置再跳。
 * 这个位置差不多是视口的 1/6 到 1/4，与 Apple Music 的观感一致。
 */
private val LyricFocusInset = SonSpacing.huge + SonSpacing.extraExtraLarge

/** 行高比。Apple Music 的行距比正文松得多，靠它撑出「一句一行」的呼吸感。 */
private const val LyricLineHeightRatio = 1.32f

/** 底部渐隐的高度占比（原版用一条 DstIn 垂直渐变把首尾吃掉）。顶部不按比例，以顶栏下沿为界。 */
private const val FadeEdgeFraction = 0.16f

/** 顶栏下沿之下，歌词从全透明渐亮到实心所经过的距离。 */
private val FadeTopRamp = SonSpacing.extraExtraLarge

/** 帧间隔超过这个值就认为掉帧严重（例如刚从后台回来），插值不再外推。 */
private const val StallThresholdNanos = 250_000_000L

/** 辉光的底色透明度：只做"发光"，不该长出一圈可见的描边。 */
private const val LyricsGlowAlpha = 0.45f

/** 辉光半径按字号取倍数。 */
private const val LyricsGlowRadiusRatio = 1.1f

/** 伴奏行字号相对主行的比例。 */
private const val AccompanimentSizeRatio = 0.62f

/**
 * 歌词页（Apple Music 版式）。
 *
 * 结构是 `[顶部歌曲栏] + [歌词滚动区]`：滚动区上下用 DstIn 渐变吃掉边缘，当前行满亮、
 * 加粗，其它行压到 0.38 左右并随距离虚化，点某一行跳转到该句；当前行固定在距顶
 * [LyricFocusInset] 处。
 *
 * 逐字高亮、呼吸点、滚动对齐全部复用 [KaraokeLyricsView] —— 这套引擎已经做过
 * 「暂停时 0 帧重绘」「跨行跳转不逐行动画」的收口，重写只会丢掉这些约束。
 */
@Composable
fun LyricsPage(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    /**
     * 顶部浮动栏的高度。歌词列表铺满整屏、从栏后面淡出（与 Apple 一致），
     * 焦点行要让出这段高度，否则高亮行会被顶栏压住。
     * 这段高度同时也是顶部渐隐的边界：栏内歌词全透明，栏下沿才开始渐亮。
     */
    headerInset: Dp = 0.dp,
    /** 站在这块表面上的是深色底（播放页压过遮罩）还是主题底色，决定歌词用白字还是主题前景色。 */
    onDarkSurface: Boolean = false,
    onLineLongPressed: () -> Unit = {}
) {
    val extended = LocalSonExtendedColors.current
    val lyrics = uiState.lyrics
    val positionProvider = rememberSmoothLyricsPosition(uiState)

    // 整页唯一取色点：正文、非当前行、伴奏、译文、占位文案全部由它派生。
    // 通过 LocalLyricsPalette 下发，子组件不再各自去主题里取色。
    val palette = remember(onDarkSurface, extended) { LyricsPalette.of(onDarkSurface, extended) }

    val baseStyle = MaterialTheme.typography.headlineMediumEmphasized
    val sizeSp = uiState.lyricsTextSizeSp
    // 辉光是**文字投影**，不是文字色：它要的是与正文相反方向的暗色光晕，
    // 所以刻意用中性的 `lyricsInactive`（两种主题下都是中灰），不参与上面的色阶。
    val normalStyle = remember(baseStyle, sizeSp, uiState.lyricsGlowEnabled, extended) {
        lyricStyle(
            base = baseStyle,
            sizeSp = sizeSp.toFloat(),
            weight = FontWeight.Bold,
            glowEnabled = uiState.lyricsGlowEnabled,
            glowColor = extended.lyricsInactive
        )
    }
    val accompanimentStyle = remember(baseStyle, sizeSp, uiState.lyricsGlowEnabled, extended) {
        lyricStyle(
            base = baseStyle,
            sizeSp = sizeSp * AccompanimentSizeRatio,
            weight = FontWeight.Normal,
            glowEnabled = uiState.lyricsGlowEnabled,
            glowColor = extended.lyricsInactive
        )
    }

    CompositionLocalProvider(LocalLyricsPalette provides palette) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 歌词行自带 16dp 内边距，这里补到与顶部歌曲栏的 28dp 对齐。
                    .padding(horizontal = SonSpacing.mediumSmall)
            ) {
                when {
                    lyrics != null -> {
                        // 列表直接从当前行铺起，不要先渲染第 0 行再跳 —— 一进来就滚到当前句的卡顿
                        // 全部来自那一次白组合。
                        val initialLineIndex = remember(lyrics) {
                            val time = uiState.playbackPosition.toInt() - uiState.lyricsOffsetMs.toInt()
                            val lines = lyrics.lines
                            if (lines.isEmpty()) {
                                0
                            } else {
                                lines.indexOfLast { it.start <= time }.coerceIn(0, lines.lastIndex)
                            }
                        }
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = initialLineIndex
                        )
                        KaraokeLyricsView(
                            listState = listState,
                            lyrics = lyrics,
                            currentPosition = positionProvider,
                            isPlaying = uiState.isPlaying,
                            onLineClicked = { line: ISyncedLine ->
                                onIntent(PlayerIntent.SeekTo(line.start.toLong()))
                            },
                            onLinePressed = { onLineLongPressed() },
                            normalLineTextStyle = normalStyle,
                            accompanimentLineTextStyle = accompanimentStyle,
                            useBlurEffect = uiState.lyricsBlurEnabled,
                            alignCenter = uiState.lyricsAlignCenter,
                            showTranslation = uiState.lyricsTranslationEnabled,
                            offset = LyricFocusInset + headerInset,
                            modifier = Modifier
                                .fillMaxSize()
                                // DstIn 必须在自己的层里跑：否则它会把下面已经画好的动态色面一起擦掉，
                                // 整块歌词区就变成一块硬边的黑方块。
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .lyricFadeMask(palette.textColor, headerInset)
                        )
                    }

                    uiState.isLoadingLyrics -> SonLoadingState()

                    // 没有歌词时给一个可点的出口：以前这里只有一行「暂无歌词」，用户既不知道
                    // 歌词是从哪来的，也没有任何办法补救。
                    else -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.player_lyrics_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.hintColor
                        )
                        Spacer(modifier = Modifier.height(SonSpacing.medium))
                        Text(
                            text = stringResource(R.string.lyrics_fetch_action),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textColor,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = palette.textColor.copy(alpha = 0.32f),
                                    shape = CircleShape
                                )
                                .clickable { onIntent(PlayerIntent.OpenLyricsCandidates) }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // 顶栏浮在歌词之上：歌词列表铺满整屏，滚到上沿的行从栏后淡出，
            // 不会在栏的下沿被硬切成半行。
            header?.invoke()
        }
    }
}

/**
 * 上下渐隐：画完内容后用 DstIn 抹掉首尾。
 *
 * 顶部以 [topInset]（顶栏高度）为界：栏内一律全透明，歌词滚进去就消失，不会和栏里的
 * 歌曲名 / 歌手名重合；栏下沿再用 [FadeTopRamp] 渐亮回来，避免出现硬边。
 *
 * 颜色只借它的 alpha，RGB 不参与（DstIn 取源 alpha），所以传什么色都一样。
 */
private fun Modifier.lyricFadeMask(tint: Color, topInset: Dp): Modifier =
    drawWithContent {
        drawContent()
        val height = size.height
        val edge = height * FadeEdgeFraction
        if (height <= 0f || edge <= 0f) return@drawWithContent
        val transparent = tint.copy(alpha = 0f)
        val solid = tint.copy(alpha = 1f)
        val half = tint.copy(alpha = 0.35f)

        val clearEnd = (topInset.toPx() / height).coerceIn(0f, 1f)
        val topSolid = ((topInset + FadeTopRamp).toPx() / height).coerceIn(clearEnd, 1f)
        // 视口过矮时上下两段会撞在一起，压住底部保证 stop 单调递增。
        val bottomSolid = (1f - FadeEdgeFraction).coerceAtLeast(topSolid)
        val bottomHalf = (1f - 0.5f * FadeEdgeFraction).coerceAtLeast(bottomSolid)

        drawRect(
            brush = Brush.verticalGradient(
                0f to transparent,
                clearEnd to transparent,
                (clearEnd + (topSolid - clearEnd) * 0.5f) to half,
                topSolid to solid,
                bottomSolid to solid,
                bottomHalf to half,
                1f to transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * 歌词字阶。
 *
 * 字号跟随用户设置（`lyricsTextSizeSp`），其余（字体族、字重、字距）整份继承主题的强调
 * 字阶 —— 只有行高按歌词版式重算，因为歌词是一行一句，不能用正文的 1.28 倍行距。
 */
private fun lyricStyle(
    base: TextStyle,
    sizeSp: Float,
    weight: FontWeight,
    glowEnabled: Boolean,
    glowColor: Color
): TextStyle {
    val size = sizeSp
    val style = base.copy(
        fontFamily = SonFontFamily.Plain,
        fontSize = size.sp,
        lineHeight = (size * LyricLineHeightRatio).sp,
        fontWeight = weight
    )
    if (!glowEnabled) return style
    return style.copy(
        shadow = Shadow(
            color = glowColor.copy(alpha = LyricsGlowAlpha),
            offset = Offset.Zero,
            blurRadius = size * LyricsGlowRadiusRatio
        )
    )
}

/**
 * 播放位置的帧间插值。
 *
 * 播放器每秒才上报一次位置，直接拿去画歌词会一秒跳一截；这里用帧时钟在两次上报之间外推，
 * 使逐字高亮连续推进。掉帧严重（刚从后台回来）时不外推，避免冲到播放位置前面。
 */
@Composable
private fun rememberSmoothLyricsPosition(uiState: PlayerUiState): () -> Int {
    val positionRef = remember { intArrayOf(0) }
    val lineTrigger = remember { mutableIntStateOf(0) }
    val latestPosition by rememberUpdatedState(uiState.playbackPosition.toInt())
    val latestIsPlaying by rememberUpdatedState(uiState.isPlaying)
    val latestOffset by rememberUpdatedState(uiState.lyricsOffsetMs)
    val latestLines by rememberUpdatedState(uiState.lyrics?.lines ?: emptyList())

    LaunchedEffect(Unit) {
        var basePosition = Int.MIN_VALUE
        var baseNanos = 0L
        var lastFrameNanos = 0L
        var smoothPosition = 0
        var lastLineIndex = Int.MIN_VALUE
        while (true) {
            withFrameNanos { frameNanos ->
                val stalled = frameNanos - lastFrameNanos > StallThresholdNanos
                lastFrameNanos = frameNanos

                val reported = latestPosition
                if (reported != basePosition || stalled) {
                    basePosition = reported
                    baseNanos = frameNanos
                    smoothPosition = reported
                } else if (latestIsPlaying) {
                    smoothPosition = basePosition + ((frameNanos - baseNanos) / 1_000_000L).toInt()
                }
                positionRef[0] = smoothPosition

                val currentLineIndex =
                    latestLines.indexOfLast { it.start <= smoothPosition - latestOffset.toInt() }
                if (currentLineIndex != lastLineIndex) {
                    lastLineIndex = currentLineIndex
                    lineTrigger.intValue = currentLineIndex
                }
            }
        }
    }

    return remember { { lineTrigger.intValue; positionRef[0] - latestOffset.toInt() } }
}
