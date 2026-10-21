@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.stats

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3MediaCard
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.ArtistListeningStat
import com.cycling.domain.model.DailyListening
import com.cycling.domain.model.HourlyListening
import com.cycling.domain.model.ListeningPeriod
import com.cycling.domain.model.ListeningStats
import com.cycling.domain.model.PeriodListening
import com.cycling.domain.model.SongListeningStat
import com.cycling.presentation.R
import com.cycling.presentation.components.HourPulseChart
import com.cycling.presentation.components.ListeningSongRow
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.SonSectionHeader
import com.cycling.presentation.components.WeeklyRhythmChart
import com.cycling.presentation.components.YearHeatmap
import com.cycling.presentation.components.formatListenCount
import com.cycling.presentation.components.formatListenDuration
import com.cycling.presentation.components.formatListenHour
import com.cycling.presentation.components.formatListenHourEnd
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 听歌统计：本周累计、时间脉络、听歌时刻、常听。
 *
 * 版式从上到下：英雄卡讲累计，三格讲今日/本周/本月，
 * 然后时间脉络（本周节奏 + 近一年热力）、听歌时刻（24 小时分布）、常听（单曲 + 歌手），
 * 最后是"清空本地统计"。
 *
 * 数据全部来自本地 `play_events`，投递方式是 Room 的 Flow，所以这一屏没有刷新按钮，
 * 也没有网络载入态；`SonLoadingState` 只在真的第一次取数时出现。
 */
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFullRanking by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StatsEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                StatsEffect.NavigateToLeaderboard -> onNavigateToLeaderboard()
                StatsEffect.NavigateBack -> onNavigateBack()
                is StatsEffect.ShowToast ->
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(StatsIntent.NavigateBack) }) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.stats_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.handleIntent(StatsIntent.NavigateToLeaderboard)
                    }) {
                        M3SymbolIcon(
                            name = SymbolIconName.GraphicEq,
                            filled = false,
                            contentDescription = stringResource(R.string.leaderboard_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SonLoadingState()
            }

            !uiState.stats.hasData -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SonEmptyState(
                    icon = SymbolIconName.BarChart,
                    title = stringResource(R.string.stats_empty_title),
                    subtitle = stringResource(R.string.stats_empty_subtitle)
                )
            }

            else -> StatsContent(
                uiState = uiState,
                innerPadding = innerPadding,
                bottomPadding = bottomPadding,
                onIntent = viewModel::handleIntent,
                onOpenFullRanking = { showFullRanking = true }
            )
        }
    }

    // ── 当天详情 ──
    val selectedDay = uiState.selectedDay
    if (selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.handleIntent(StatsIntent.DismissDay) },
            sheetState = sheetState
        ) {
            DayDetailContent(
                date = selectedDay,
                songs = uiState.daySongs,
                isLoading = uiState.isLoadingDay,
                onPlay = { viewModel.handleIntent(StatsIntent.PlaySong(it)) }
            )
        }
    }

    // ── 完整榜单 ──
    if (showFullRanking) {
        ModalBottomSheet(
            onDismissRequest = { showFullRanking = false },
            sheetState = sheetState
        ) {
            FullRankingContent(
                songs = uiState.stats.topSongs,
                onPlay = { viewModel.handleIntent(StatsIntent.PlaySong(it)) }
            )
        }
    }

    // ── 清空确认 ──
    if (uiState.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(StatsIntent.DismissClearStats) },
            title = { Text(text = stringResource(R.string.stats_clear_confirm_title)) },
            text = { Text(text = stringResource(R.string.stats_clear_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.handleIntent(StatsIntent.ConfirmClearStats) }) {
                    Text(text = stringResource(R.string.stats_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(StatsIntent.DismissClearStats) }) {
                    Text(text = stringResource(R.string.stats_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatsContent(
    uiState: StatsUiState,
    innerPadding: PaddingValues,
    bottomPadding: Dp,
    onIntent: (StatsIntent) -> Unit,
    onOpenFullRanking: () -> Unit
) {
    val stats = uiState.stats
    val busiestHour = stats.hours
        .filter { it.playMs > 0L }
        .maxByOrNull { it.playMs }
        ?.hour

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = SonSpacing.large + bottomPadding)
    ) {
        item { HeroCard(stats = stats) }
        item { PeriodOverview(periods = stats.periods) }

        item {
            SonSectionHeader(
                title = stringResource(R.string.stats_section_timeline),
                subtitle = stringResource(R.string.stats_section_timeline_subtitle)
            )
        }
        item {
            WeekRhythmCard(
                weekDays = stats.weekDays,
                selectedDate = uiState.selectedDay,
                onSelectDay = { onIntent(StatsIntent.SelectDay(it)) }
            )
        }
        item { YearHeatmapCard(days = stats.yearDays) }

        item {
            SonSectionHeader(
                title = stringResource(R.string.stats_section_hours),
                subtitle = stringResource(R.string.stats_section_hours_subtitle)
            )
        }
        item { HourCard(hours = stats.hours, busiestHour = busiestHour) }

        item {
            SonSectionHeader(
                title = stringResource(R.string.stats_section_top),
                subtitle = stringResource(R.string.stats_section_top_subtitle)
            )
        }
        item { TopSongsRail(songs = stats.topSongs, onPlay = { onIntent(StatsIntent.PlaySong(it)) }) }
        item { TopArtistsRail(artists = stats.topArtists) }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small)
            ) {
                FilledTonalButton(onClick = onOpenFullRanking, modifier = Modifier.fillMaxWidth()) {
                    M3SymbolIcon(
                        name = SymbolIconName.ExpandMore,
                        filled = false,
                        contentDescription = null,
                        modifier = Modifier.size(SonSizes.buttonIcon)
                    )
                    Spacer(modifier = Modifier.width(SonSpacing.small))
                    Text(text = stringResource(R.string.stats_expand_ranking))
                }
            }
        }

        item { SonSectionHeader(title = stringResource(R.string.stats_settings)) }
        item {
            ClearStatsRow(onClick = { onIntent(StatsIntent.RequestClearStats) })
        }
    }
}

/** 累计听歌时长的英雄卡：一屏里唯一的"大数字"。 */
@Composable
private fun HeroCard(stats: ListeningStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(SonSpacing.large)) {
            Text(
                text = stringResource(R.string.stats_hero_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = formatListenDuration(stats.totalPlayMs),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = stringResource(
                    R.string.stats_hero_plays_days,
                    formatListenCount(stats.totalPlayCount),
                    stats.totalDays
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(
                    R.string.stats_average_per_song,
                    formatListenDuration(stats.averagePlayMs)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** 今日 / 本周 / 本月 三格累计。 */
@Composable
private fun PeriodOverview(periods: List<PeriodListening>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        periods.forEach { period ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(SonSpacing.mediumSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = periodLabel(period.period),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(SonSpacing.extraSmall))
                    Text(
                        text = formatListenDuration(period.playMs),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(
                            R.string.stats_period_plays,
                            formatListenCount(period.playCount)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 本周节奏：7 根柱子。
 *
 * 点某一天直接打开当天详情，让"时间脉络"真的可下钻，而不只是一张装饰图。
 */
@Composable
private fun WeekRhythmCard(
    weekDays: List<DailyListening>,
    selectedDate: LocalDate?,
    onSelectDay: (LocalDate) -> Unit
) {
    val weekTotal = weekDays.sumOf { it.playMs }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_week_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        R.string.stats_week_total,
                        formatListenDuration(weekTotal)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            WeeklyRhythmChart(
                days = weekDays,
                selectedDate = selectedDate,
                onSelectDay = onSelectDay
            )
        }
    }
}

/** 近一年热力图。 */
@Composable
private fun YearHeatmapCard(days: List<DailyListening>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Text(
                text = stringResource(R.string.stats_year_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = stringResource(R.string.stats_year_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            YearHeatmap(days = days)
        }
    }
}

/** 听歌时刻：24 小时分布 + "最常在几点"的一句话。 */
@Composable
private fun HourCard(hours: List<HourlyListening>, busiestHour: Int?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            if (busiestHour != null) {
                Text(
                    text = stringResource(
                        R.string.stats_busiest_hour,
                        formatListenHour(busiestHour),
                        formatListenHourEnd(busiestHour)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SonSpacing.medium))
            }
            HourPulseChart(hours = hours, busiestHour = busiestHour)
        }
    }
}

/** 常听单曲：横向卡片流，点开即播。 */
@Composable
private fun TopSongsRail(songs: List<SongListeningStat>, onPlay: (Long) -> Unit) {
    if (songs.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonLayout.gridGap)
    ) {
        items(items = songs.take(TOP_SONG_PREVIEW), key = { it.songId }) { song ->
            M3MediaCard(
                title = song.title,
                subtitle = song.artist,
                artwork = song.coverUrl,
                onClick = { onPlay(song.songId) }
            )
        }
    }
}

/** 常听歌手：芯片式的紧凑卡片，歌手没有本地封面，所以不做成媒体卡。 */
@Composable
private fun TopArtistsRail(artists: List<ArtistListeningStat>) {
    if (artists.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SonSpacing.small),
        contentPadding = PaddingValues(horizontal = SonLayout.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        items(items = artists.take(TOP_ARTIST_PREVIEW), key = { it.artist }) { artist ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(SonSpacing.mediumSmall)) {
                    Text(
                        text = artist.artist,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatListenDuration(artist.playMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/** 统计设置区唯一的动作：清空本地统计（带二次确认）。 */
@Composable
private fun ClearStatsRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(SonSizes.thumbnail)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            M3SymbolIcon(
                name = SymbolIconName.Delete,
                filled = true,
                contentDescription = null,
                modifier = Modifier.size(SonSizes.iconMedium),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(modifier = Modifier.width(SonSpacing.mediumSmall))
        Text(
            text = stringResource(R.string.stats_clear),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 当天详情弹层的内容。
 *
 * 直接用一个 [LazyColumn] 当根（而不是"Column 里嵌 LazyColumn"）：底部弹层给的是**有界高度**，
 * 但只有列表自己是根时才能正确吃掉剩余空间；嵌在 Column 里会让可滚动组件拿到不定高约束。
 */
@Composable
private fun DayDetailContent(
    date: LocalDate,
    songs: List<SongListeningStat>,
    isLoading: Boolean,
    onPlay: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = SonSpacing.large)
    ) {
        item {
            Text(
                text = stringResource(R.string.stats_day_detail_title, dayLabel(date)),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = SonLayout.screenMargin,
                    vertical = SonSpacing.small
                )
            )
        }
        when {
            isLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SonSizes.buttonLargeHeight),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

            songs.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.stats_day_detail_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = SonLayout.screenMargin,
                        vertical = SonSpacing.large
                    )
                )
            }

            else -> items(items = songs, key = { it.songId }) { song ->
                ListeningSongRow(
                    song = song,
                    valueText = formatListenDuration(song.playMs),
                    onClick = { onPlay(song.songId) }
                )
            }
        }
    }
}

/** 完整榜单弹层：常听前 N 首的完整排名。 */
@Composable
private fun FullRankingContent(
    songs: List<SongListeningStat>,
    onPlay: (Long) -> Unit
) {
    val totalPlayMs = songs.sumOf { it.playMs }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = SonSpacing.large)
    ) {
        item {
            Text(
                text = stringResource(R.string.stats_top_songs),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = SonLayout.screenMargin,
                    vertical = SonSpacing.small
                )
            )
        }
        itemsIndexed(items = songs, key = { _, song -> song.songId }) { index, song ->
            ListeningSongRow(
                song = song,
                valueText = formatListenDuration(song.playMs),
                rank = index + 1,
                share = if (totalPlayMs > 0L) song.playMs.toFloat() / totalPlayMs else 0f,
                onClick = { onPlay(song.songId) }
            )
        }
    }
}

@Composable
private fun periodLabel(period: ListeningPeriod): String = when (period) {
    ListeningPeriod.TODAY -> stringResource(R.string.stats_period_today)
    ListeningPeriod.WEEK -> stringResource(R.string.stats_period_week)
    ListeningPeriod.MONTH -> stringResource(R.string.stats_period_month)
}

/** `10月20日 周一`：日详情弹层的标题。 */
private fun dayLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.getDefault()))

/** 常听单曲横向卡片流露出多少张。 */
private const val TOP_SONG_PREVIEW = 10

/** 常听歌手露出多少个。 */
private const val TOP_ARTIST_PREVIEW = 10
