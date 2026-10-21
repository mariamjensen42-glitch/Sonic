package com.cycling.presentation.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders as materialColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cycling.core.ui.theme.CoverArtworkLoader
import com.cycling.core.ui.theme.SonicSeed
import com.cycling.core.ui.theme.sonColorSchemeOf
import com.cycling.domain.model.PlayerState
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.presentation.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 桌面小组件。
 *
 * 数据直接来自 [PlayerRepository]：播放状态本来就是一个全局单例的 `StateFlow`，小组件只是
 * 多一个订阅者，不需要绕到 Activity 或 MediaController。
 *
 * 两种尺寸共用同一个 `SizeMode.Responsive`：宽度不足 220dp 时收成 2×1 的精简版（封面 +
 * 标题 + 播放/下一首），否则是 4×2 的完整版。宿主缩放时自动切换，不需要用户添加两个组件。
 */
class SonicWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(CompactSize, FullSize)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val playerRepository = entryPoint.playerRepository()
        // 先取一次快照：provideGlance 返回之前组合还没开始，这段时间内不能靠 Flow 首帧。
        val initial = playerRepository.getCurrentState()

        provideContent {
            val state by playerRepository.playerState.collectAsState(initial)
            SonicWidgetContent(state)
        }
    }

    private companion object {
        val CompactSize = DpSize(180.dp, 60.dp)
        val FullSize = DpSize(280.dp, 140.dp)
    }
}

/**
 * 小组件回调：在 App 进程里执行，因此可以直接调用仓库。
 *
 * Glance 的 `clickable { }` lambda 只能做同步的事，播放控制都是 suspend 的 `updateAll` 或
 * 需要 IO 的曲库查询，所以统一走 [ActionCallback]。
 */
class SonicWidgetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[ActionKey]
        val entryPoint = widgetEntryPoint(context)
        val playerRepository = entryPoint.playerRepository()
        val songRepository = entryPoint.songRepository()
        val state = playerRepository.getCurrentState()

        try {
            when (action) {
                ActionToggle -> {
                    if (state.currentSong != null) {
                        playerRepository.playPause()
                    } else {
                        shuffleAll(playerRepository, songRepository)
                    }
                }

                ActionNext -> {
                    if (state.currentSong != null) {
                        playerRepository.skipToNext()
                    } else {
                        shuffleAll(playerRepository, songRepository)
                    }
                }

                ActionPrevious -> {
                    if (state.currentSong != null) playerRepository.skipToPrevious()
                }

                ActionShuffle -> shuffleAll(playerRepository, songRepository)
            }
        } catch (e: Exception) {
            // 小组件不能弹 Toast，也不能让异常穿透到 Glance 的广播接收器。
            Timber.w(e, "SonicWidgetAction: action=%s failed", action)
        }

        // 播放状态的变化会由 SonicWidgetUpdater 推过来，但这里主动刷一次能让按钮在
        // 冷启动（没有后台订阅者）时也立刻变成正确的图标。
        runCatching { SonicWidget().updateAll(context) }
    }

    private suspend fun shuffleAll(
        playerRepository: PlayerRepository,
        songRepository: SongRepository
    ) {
        val songs = songRepository.getAllSongs().first()
        if (songs.isEmpty()) return
        val queue = songs.shuffled()
        playerRepository.playSong(queue.first(), queue)
    }
}

private const val ActionToggle = "toggle"
private const val ActionNext = "next"
private const val ActionPrevious = "previous"
private const val ActionShuffle = "shuffle"

private val ActionKey = ActionParameters.Key<String>("sonic.widget.action")

/**
 * 把播放状态推给所有已添加的小组件。
 *
 * 只关心「哪首歌 / 是否在播 / 队列有没有变」：`PlayerState` 每秒都会因为进度更新而发射，
 * 不筛的话小组件会被每秒重绘一次。位置不在小组件里显示，所以它不参与比较。
 */
object SonicWidgetUpdater {

    @Volatile
    private var started = false

    fun start(context: Context) {
        if (started) return
        if (!hasWidgets(context)) return
        started = true

        val appContext = context.applicationContext
        val entryPoint = widgetEntryPoint(appContext)
        val playerRepository = entryPoint.playerRepository()

        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate
        ).launch {
            playerRepository.playerState
                .map { it.widgetSignature() }
                .distinctUntilChanged()
                .collect {
                    runCatching { SonicWidget().updateAll(appContext) }
                        .onFailure { Timber.w(it, "SonicWidgetUpdater: updateAll failed") }
                }
        }
    }

    private fun hasWidgets(context: Context): Boolean = runCatching {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(
            android.content.ComponentName(context, SonicWidgetReceiver::class.java)
        ).isNotEmpty()
    }.getOrDefault(false)
}

private fun PlayerState.widgetSignature(): WidgetSignature = WidgetSignature(
    songId = currentSong?.id,
    isPlaying = isPlaying,
    queueSize = playbackQueue.size,
    favorite = currentSong?.isFavorite == true
)

private data class WidgetSignature(
    val songId: Long?,
    val isPlaying: Boolean,
    val queueSize: Int,
    val favorite: Boolean
)

class SonicWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SonicWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 组件是在 App 已经启动之后才被添加的：Application 里那次 start() 找不到 id 会直接返回，
        // 这里补一次，之后的播放状态变化才会继续推过来。
        SonicWidgetUpdater.start(context)
    }
}

// ── 主题 ──────────────────────────────────────────────────────────────────

/**
 * 品牌色生成的日/夜两套方案。生成一次后缓存：HCT 色调映射要做上千次迭代，不能每次组合都跑。
 */
private val WidgetColors: ColorProviders by lazy {
    materialColorProviders(
        light = sonColorSchemeOf(SonicSeed, darkTheme = false),
        dark = sonColorSchemeOf(SonicSeed, darkTheme = true)
    )
}

// ── UI ────────────────────────────────────────────────────────────────────

@Composable
private fun SonicWidgetContent(state: PlayerState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val compact = size.width < 220.dp
    val openApp = remember(context) { openAppIntent(context) }

    GlanceTheme(colors = WidgetColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(actionStartActivity(openApp))
        ) {
            if (compact) {
                CompactWidgetContent(state)
            } else {
                FullWidgetContent(state)
            }
        }
    }
}

@Composable
private fun CompactWidgetContent(state: PlayerState) {
    val song = state.currentSong
    val artwork = rememberWidgetArtwork(song?.albumArt, size = 96)

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkBitmap(
            bitmap = artwork,
            size = 40,
            corner = 12,
            fallbackIcon = R.drawable.widget_note
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = song?.title ?: stringResourceSafe(R.string.widget_empty_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = song?.artist ?: stringResourceSafe(R.string.widget_empty_subtitle),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        ControlButton(
            iconRes = if (state.isPlaying) R.drawable.widget_pause else R.drawable.widget_play,
            contentDescription = stringResourceSafe(
                if (state.isPlaying) R.string.player_pause_desc else R.string.player_play_desc
            ),
            action = ActionToggle,
            filled = true,
            buttonSize = 36
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        ControlButton(
            iconRes = R.drawable.widget_next,
            contentDescription = stringResourceSafe(R.string.player_next_desc),
            action = ActionNext,
            buttonSize = 36
        )
    }
}

@Composable
private fun FullWidgetContent(state: PlayerState) {
    val song = state.currentSong
    val artwork = rememberWidgetArtwork(song?.albumArt, size = 192)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkBitmap(
                bitmap = artwork,
                size = 64,
                corner = 16,
                fallbackIcon = R.drawable.widget_note
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = song?.title ?: stringResourceSafe(R.string.widget_empty_title),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = song?.artist ?: stringResourceSafe(R.string.widget_empty_subtitle),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
            ControlButton(
                iconRes = R.drawable.widget_shuffle,
                contentDescription = stringResourceSafe(R.string.player_shuffle_desc),
                action = ActionShuffle,
                buttonSize = 36
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                iconRes = R.drawable.widget_previous,
                contentDescription = stringResourceSafe(R.string.player_previous_desc),
                action = ActionPrevious,
                buttonSize = 40
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            ControlButton(
                iconRes = if (state.isPlaying) R.drawable.widget_pause else R.drawable.widget_play,
                contentDescription = stringResourceSafe(
                    if (state.isPlaying) R.string.player_pause_desc else R.string.player_play_desc
                ),
                action = ActionToggle,
                filled = true,
                buttonSize = 44
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            ControlButton(
                iconRes = R.drawable.widget_next,
                contentDescription = stringResourceSafe(R.string.player_next_desc),
                action = ActionNext,
                buttonSize = 40
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (state.playbackQueue.isNotEmpty()) {
                Text(
                    text = "${state.queueIndex + 1}/${state.playbackQueue.size}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ArtworkBitmap(bitmap: Bitmap?, size: Int, corner: Int, fallbackIcon: Int) {
    Box(
        modifier = GlanceModifier.size(size.dp).cornerRadius(corner.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(corner.dp),
                contentScale = androidx.glance.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(corner.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(fallbackIcon),
                    contentDescription = null,
                    modifier = GlanceModifier.size((size * 0.45f).dp),
                    colorFilter = androidx.glance.ColorFilter.tint(
                        GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    iconRes: Int,
    contentDescription: String,
    action: String,
    buttonSize: Int,
    filled: Boolean = false
) {
    val background: ColorProvider = if (filled) {
        GlanceTheme.colors.primary
    } else {
        GlanceTheme.colors.surfaceVariant
    }
    val tint: ColorProvider = if (filled) {
        GlanceTheme.colors.onPrimary
    } else {
        GlanceTheme.colors.onSurface
    }

    Box(
        modifier = GlanceModifier
            .size(buttonSize.dp)
            .background(background)
            .cornerRadius((buttonSize / 2).dp)
            .clickable(
                actionRunCallback<SonicWidgetAction>(
                    actionParametersOf(ActionKey to action)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size((buttonSize * 0.55f).dp),
            colorFilter = androidx.glance.ColorFilter.tint(tint)
        )
    }
}

@Composable
private fun rememberWidgetArtwork(artwork: String?, size: Int): Bitmap? {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = artwork) {
        value = if (artwork.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { CoverArtworkLoader.load(context, artwork, sizePx = size) }
                    .onFailure { Timber.w(it, "widget: artwork load failed for %s", artwork) }
                    .getOrNull()
            }
        }
    }
    return bitmap
}

private fun openAppIntent(context: Context): Intent =
    Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(context.packageName)

@Composable
private fun stringResourceSafe(id: Int): String = LocalContext.current.getString(id)

// ── Hilt 入口 ─────────────────────────────────────────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SonicWidgetEntryPoint {
    fun playerRepository(): PlayerRepository
    fun songRepository(): SongRepository
}

private fun widgetEntryPoint(context: Context): SonicWidgetEntryPoint =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        SonicWidgetEntryPoint::class.java
    )
