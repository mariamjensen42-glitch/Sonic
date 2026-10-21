@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.leaderboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.model.ListenerProfile
import com.cycling.domain.model.SongListeningStat
import com.cycling.presentation.R
import com.cycling.presentation.components.ShareMeter
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.formatListenDuration

/**
 * 听歌排行榜：时长榜 / 次数榜 × 日榜 / 周榜 / 月榜 / 总榜，外加"我的乐迷档案"。
 *
 * 服务端榜单把全站用户排在一起；Sonic 没有账号体系，于是排名对象换成**自己的歌**，
 * 而"乐迷"那一栏落到自己身上 —— 累计时长决定等级，连续天数与个人最佳单日表达听歌资历
 * （见 [ListenerProfile]）。
 */
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LeaderboardEffect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                LeaderboardEffect.NavigateToStats -> onNavigateToStats()
                LeaderboardEffect.NavigateBack -> onNavigateBack()
                is LeaderboardEffect.ShowToast ->
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.leaderboard_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleIntent(LeaderboardIntent.NavigateBack) }) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.leaderboard_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(LeaderboardIntent.NavigateToStats) }) {
                        M3SymbolIcon(
                            name = SymbolIconName.BarChart,
                            filled = false,
                            contentDescription = stringResource(R.string.stats_title)
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SonLoadingState()
            }
        } else {
            LeaderboardContent(
                uiState = uiState,
                innerPadding = innerPadding,
                bottomPadding = bottomPadding,
                onIntent = viewModel::handleIntent
            )
        }
    }
}

@Composable
private fun LeaderboardContent(
    uiState: LeaderboardUiState,
    innerPadding: PaddingValues,
    bottomPadding: Dp,
    onIntent: (LeaderboardIntent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            // 这两个页面是 pushed 路由：底栏与迷你播放器都已隐藏，Scaffold 的 inset 已经
            // 让出了系统栏，这里只需要给最后一行一点呼吸空间。
            bottom = SonSpacing.large + bottomPadding
        )
    ) {
        item { FilterSection(uiState = uiState, onIntent = onIntent) }
        item { ListenerProfileCard(profile = uiState.board.profile) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SonLayout.screenMargin,
                        end = SonLayout.screenMargin,
                        top = SonLayout.cardGap,
                        bottom = SonLayout.sectionTitleGap
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_list_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = rangeHint(uiState.range),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.board.entries.isEmpty()) {
            item {
                SonEmptyState(
                    icon = SymbolIconName.GraphicEq,
                    title = stringResource(R.string.leaderboard_empty_title),
                    subtitle = stringResource(R.string.leaderboard_empty_subtitle)
                )
            }
        } else {
            items(items = uiState.board.entries, key = { it.song.songId }) { entry ->
                LeaderboardRow(
                    rank = entry.rank,
                    song = entry.song,
                    metric = uiState.metric,
                    shareOfTotal = if (uiState.board.totalPlayMs > 0L) {
                        entry.song.playMs.toFloat() / uiState.board.totalPlayMs
                    } else {
                        0f
                    },
                    onClick = { onIntent(LeaderboardIntent.PlaySong(entry.song.songId)) }
                )
            }
        }
    }
}

/** 口径 + 窗口两组 chip。选中即查询，没有"确定"按钮。 */
@Composable
private fun FilterSection(
    uiState: LeaderboardUiState,
    onIntent: (LeaderboardIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
        verticalArrangement = Arrangement.spacedBy(SonSpacing.small)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
            LeaderboardMetric.entries.forEach { metric ->
                FilterChip(
                    selected = uiState.metric == metric,
                    onClick = { onIntent(LeaderboardIntent.SelectMetric(metric)) },
                    label = { Text(text = metricLabel(metric)) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
            LeaderboardRange.entries.forEach { range ->
                FilterChip(
                    selected = uiState.range == range,
                    onClick = { onIntent(LeaderboardIntent.SelectRange(range)) },
                    label = { Text(text = rangeLabel(range)) }
                )
            }
        }
    }
}

/**
 * 我的乐迷档案。
 *
 * 服务端榜单里这里是"全站 X 位乐迷共同参与 + 我的名次"；单机化之后换成同一批指标落到
 * 自己身上：等级由累计时长决定，下面是天数、连击与最佳单日。
 */
@Composable
private fun ListenerProfileCard(profile: ListenerProfile) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonLayout.screenMargin, vertical = SonSpacing.small),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(SonSpacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_profile_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = SonSpacing.mediumSmall, vertical = SonSpacing.extraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_level, profile.level),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(SonSpacing.mediumSmall))
            LinearProgressIndicator(
                progress = { profile.levelProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = profile.nextLevelPlayMs?.let { next ->
                    stringResource(
                        R.string.leaderboard_level_next,
                        formatListenDuration(next - profile.totalPlayMs),
                        profile.level + 1
                    )
                } ?: stringResource(R.string.leaderboard_level_max),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SonSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStat(
                    value = formatListenDuration(profile.totalPlayMs),
                    caption = stringResource(R.string.leaderboard_stat_total),
                    modifier = Modifier.weight(1f)
                )
                ProfileStat(
                    value = profile.listeningDays.toString(),
                    caption = stringResource(R.string.leaderboard_stat_days),
                    modifier = Modifier.weight(1f)
                )
                ProfileStat(
                    value = profile.streakDays.toString(),
                    caption = stringResource(R.string.leaderboard_stat_streak),
                    modifier = Modifier.weight(1f)
                )
                ProfileStat(
                    value = formatListenDuration(profile.bestDayPlayMs),
                    caption = stringResource(R.string.leaderboard_stat_best),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * 榜单一行的版式：名次 → 封面 → 标题/歌手 → 成绩，下面一条占比条。
 *
 * 名次前三用 `tertiary` 强调，其余用弱化的 onSurfaceVariant：前三名值得一个颜色，
 * 4 名以后全是重点就等于没有重点。
 */
@Composable
private fun LeaderboardRow(
    rank: Int,
    song: SongListeningStat,
    metric: LeaderboardMetric,
    shareOfTotal: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = SonLayout.screenMargin,
                vertical = SonSpacing.mediumSmall
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = if (rank <= TOP_RANK_COUNT) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.width(RankWidth)
            )
            M3Artwork(
                artwork = song.coverUrl,
                placeholderIcon = SymbolIconName.MusicNote,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
            Spacer(modifier = Modifier.width(SonSpacing.mediumSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(SonSpacing.small))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = metricValue(song, metric),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.leaderboard_plays_count, song.playCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(SonSpacing.small))
        ShareMeter(fraction = shareOfTotal)
    }
}

@Composable
private fun metricLabel(metric: LeaderboardMetric): String = when (metric) {
    LeaderboardMetric.DURATION -> stringResource(R.string.leaderboard_metric_duration)
    LeaderboardMetric.PLAYS -> stringResource(R.string.leaderboard_metric_plays)
}

@Composable
private fun rangeLabel(range: LeaderboardRange): String = when (range) {
    LeaderboardRange.DAY -> stringResource(R.string.leaderboard_range_day)
    LeaderboardRange.WEEK -> stringResource(R.string.leaderboard_range_week)
    LeaderboardRange.MONTH -> stringResource(R.string.leaderboard_range_month)
    LeaderboardRange.ALL -> stringResource(R.string.leaderboard_range_all)
}

@Composable
private fun rangeHint(range: LeaderboardRange): String =
    if (range.days > 0) {
        stringResource(R.string.leaderboard_range_hint, range.days)
    } else {
        stringResource(R.string.leaderboard_range_hint_all)
    }

@Composable
private fun metricValue(song: SongListeningStat, metric: LeaderboardMetric): String =
    when (metric) {
        LeaderboardMetric.DURATION -> formatListenDuration(song.playMs)
        LeaderboardMetric.PLAYS -> stringResource(
            R.string.leaderboard_plays_count,
            song.playCount
        )
    }

/** 名次数字的固定槽宽：不进版心，但要让标题在所有行里左对齐。 */
private val RankWidth = 32.dp

/** 前三名用强调色。 */
private const val TOP_RANK_COUNT = 3
