package com.cycling.data.repository

import com.cycling.data.local.dao.ArtistStatsRow
import com.cycling.data.local.dao.DailyPlayRow
import com.cycling.data.local.dao.HourStatsRow
import com.cycling.data.local.dao.PlayEventDao
import com.cycling.data.local.dao.SongStatsRow
import com.cycling.data.local.dao.StatsOverviewRow
import com.cycling.data.local.dao.StatsRangeRow
import com.cycling.domain.model.ArtistListeningStat
import com.cycling.domain.model.DailyListening
import com.cycling.domain.model.HourlyListening
import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.model.ListeningLeaderboard
import com.cycling.domain.model.ListeningPeriod
import com.cycling.domain.model.ListeningStats
import com.cycling.domain.model.ListenerProfile
import com.cycling.domain.model.ListenerRankEntry
import com.cycling.domain.model.PeriodListening
import com.cycling.domain.model.SongListeningStat
import com.cycling.domain.repository.ListeningStatsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 听歌统计与排行榜的本地实现。
 *
 * 两条原则：
 *
 * 1. **一次算齐**。统计页要的总览、时间脉络、小时分布、常听全部来自同一张表、同一批参数，
 *    分开查询只会让界面多闪几次；这里用 `combine` 把 9 个聚合拼成一次发射。
 * 2. **边界现算**。时间窗口（今天 / 本周 / 本月 / 近一年）在**每次收集时**重新计算，而不是在
 *    构造函数里定死 —— 否则跨过零点、跨过周一之后页面还停在上一个窗口。
 *
 * 时区偏移每次查询都现取，夏令时切换时不会把一天算成 23 或 25 小时。
 */
@Singleton
class ListeningStatsRepositoryImpl @Inject constructor(
    private val dao: PlayEventDao
) : ListeningStatsRepository {

    override fun observeStats(): Flow<ListeningStats> = flow {
        val now = System.currentTimeMillis()
        val tz = currentTzOffsetMs()
        val today = LocalDate.now()
        val todayStart = today.atStartOfDayMs(tz)
        val weekStartDate = today.with(DayOfWeek.MONDAY)
        val weekStart = weekStartDate.atStartOfDayMs(tz)
        val monthStart = today.withDayOfMonth(1).atStartOfDayMs(tz)
        val end = now + 1

        val overview = dao.overview(since = 0L, tzOffsetMs = tz)
        val todayTotals = dao.rangeTotals(todayStart, end)
        val weekTotals = dao.rangeTotals(weekStart, end)
        val monthTotals = dao.rangeTotals(monthStart, end)
        val weekDays = dao.dailySince(weekStart, tz)
        val yearDays = dao.dailySince(now - YEAR_WINDOW_MS, tz)
        val hours = dao.hourlySince(since = 0L, tzOffsetMs = tz)
        val topSongs = dao.topSongsSince(since = 0L, limit = TOP_SONG_LIMIT)
        val topArtists = dao.topArtistsSince(since = 0L, limit = TOP_ARTIST_LIMIT)

        val core = combine(overview, todayTotals, weekTotals, monthTotals, weekDays) {
                overviewRow, todayRow, weekRow, monthRow, weekRows ->
            StatsCore(
                overview = overviewRow,
                periods = listOf(
                    todayRow.toPeriod(ListeningPeriod.TODAY),
                    weekRow.toPeriod(ListeningPeriod.WEEK),
                    monthRow.toPeriod(ListeningPeriod.MONTH)
                ),
                weekDays = fillWeek(weekRows, weekStartDate)
            )
        }
        val extra = combine(yearDays, hours, topSongs, topArtists) {
                yearRows, hourRows, songRows, artistRows ->
            StatsExtra(
                yearDays = yearRows.map { it.toDaily() },
                hours = fillHours(hourRows),
                topSongs = songRows.map { it.toStat() },
                topArtists = artistRows.map { it.toStat() }
            )
        }

        emitAll(
            combine(core, extra) { base, more ->
                ListeningStats(
                    totalPlayCount = base.overview.totalCount,
                    totalPlayMs = base.overview.totalPlayMs,
                    totalDays = base.overview.totalDays,
                    periods = base.periods,
                    weekDays = base.weekDays,
                    yearDays = more.yearDays,
                    hours = more.hours,
                    topSongs = more.topSongs,
                    topArtists = more.topArtists
                )
            }
        )
    }

    override fun observeLeaderboard(
        range: LeaderboardRange,
        metric: LeaderboardMetric
    ): Flow<ListeningLeaderboard> = flow {
        val now = System.currentTimeMillis()
        val end = now + 1
        val start = if (range.days > 0) now - range.days * DAY_MS else 0L

        val ranked = when (metric) {
            LeaderboardMetric.DURATION -> dao.rankSongsByDuration(start, end, LEADERBOARD_LIMIT)
            LeaderboardMetric.PLAYS -> dao.rankSongsByPlays(start, end, LEADERBOARD_LIMIT)
        }
        val windowTotals = dao.rangeTotals(start, end)

        emitAll(
            combine(ranked, windowTotals, observeListenerProfile()) { songs, totals, profile ->
                var rank = 0
                val entries = songs.map { row ->
                    rank += 1
                    ListenerRankEntry(rank = rank, song = row.toStat())
                }
                ListeningLeaderboard(
                    range = range,
                    metric = metric,
                    entries = entries,
                    totalPlayMs = totals.playMs,
                    totalPlayCount = totals.playCount,
                    profile = profile
                )
            }
        )
    }

    override fun observeListenerProfile(): Flow<ListenerProfile> = flow {
        val now = System.currentTimeMillis()
        val tz = currentTzOffsetMs()
        val today = LocalDate.now()
        val end = now + 1
        val todayStart = today.atStartOfDayMs(tz)
        val weekStart = today.with(DayOfWeek.MONDAY).atStartOfDayMs(tz)

        val overview = dao.overview(since = 0L, tzOffsetMs = tz)
        val recentDays = dao.recentDayKeys(tz, STREAK_WINDOW_DAYS)
        val bestDay = dao.bestDay(tz)
        val todayTotals = dao.rangeTotals(todayStart, end)
        val weekTotals = dao.rangeTotals(weekStart, end)

        emitAll(
            combine(
                overview,
                recentDays,
                bestDay,
                todayTotals,
                weekTotals
            ) { overviewRow, dayKeys, bestRows, todayRow, weekRow ->
                val bestRow = bestRows.firstOrNull()
                ListenerProfile(
                    totalPlayMs = overviewRow.totalPlayMs,
                    totalPlayCount = overviewRow.totalCount,
                    listeningDays = overviewRow.totalDays,
                    streakDays = streakEndingAt(dayKeys, today.toEpochDay()),
                    todayPlayMs = todayRow.playMs,
                    weekPlayMs = weekRow.playMs,
                    bestDayPlayMs = bestRow?.playMs ?: 0L,
                    bestDay = bestRow?.let { LocalDate.ofEpochDay(it.dayKey) }                )
            }
        )
    }

    override fun observeSongsOfDay(date: LocalDate, limit: Int): Flow<List<SongListeningStat>> {
        val start = date.atStartOfDayMs(currentTzOffsetMs())
        return dao.songsBetween(start, start + DAY_MS, limit).map { rows -> rows.map { it.toStat() } }
    }

    override suspend fun clearStats() = dao.clearAll()

    // ── 时间窗口 ─────────────────────────────────────────────────────────

    private fun currentTzOffsetMs(): Long =
        TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()

    private fun LocalDate.atStartOfDayMs(tzOffsetMs: Long): Long = toEpochDay() * DAY_MS - tzOffsetMs

    /**
     * 本周节奏补齐成周一到周日 7 个桶。
     *
     * 数据库只返回"有记录的那几天"，而柱状图需要固定的 7 根柱子：缺失的天补 0，
     * 否则用户会看到 3 根柱子挤在一起，误以为这一周只有 3 天。
     */
    private fun fillWeek(rows: List<DailyPlayRow>, weekStart: LocalDate): List<DailyListening> {
        val byDay = rows.associateBy { it.dayKey }
        return (0L until 7L).map { offset ->
            val date = weekStart.plusDays(offset)
            val row = byDay[date.toEpochDay()]
            DailyListening(
                date = date,
                playCount = row?.playCount ?: 0,
                playMs = row?.playMs ?: 0L
            )
        }
    }

    /** 听歌时刻补齐 0..23，理由同上：24 根柱子才是一天。 */
    private fun fillHours(rows: List<HourStatsRow>): List<HourlyListening> {
        val byHour = rows.associateBy { it.hourKey }
        return (0 until 24).map { hour ->
            val row = byHour[hour]
            HourlyListening(
                hour = hour,
                playCount = row?.playCount ?: 0,
                playMs = row?.playMs ?: 0L
            )
        }
    }

    /**
     * 连续听歌天数，算法见文件末尾的 [listeningStreakDays]。
     *
     * 拆成顶层函数是为了能脱离 Room 单独测：它只吃"有记录的日历日 + 今天"，是一段纯逻辑。
     */
    private fun streakEndingAt(dayKeysDesc: List<Long>, todayEpoch: Long): Int =
        listeningStreakDays(dayKeysDesc, todayEpoch)

    // ── 映射 ─────────────────────────────────────────────────────────────

    private fun StatsRangeRow.toPeriod(period: ListeningPeriod): PeriodListening =
        PeriodListening(period = period, playCount = playCount, playMs = playMs)

    private fun DailyPlayRow.toDaily(): DailyListening = DailyListening(
        date = LocalDate.ofEpochDay(dayKey),
        playCount = playCount,
        playMs = playMs
    )

    private fun SongStatsRow.toStat(): SongListeningStat = SongListeningStat(
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        playCount = playCount,
        playMs = playMs,
        lastPlayedAt = lastPlayedAt
    )

    private fun ArtistStatsRow.toStat(): ArtistListeningStat = ArtistListeningStat(
        artist = artist,
        playCount = playCount,
        playMs = playMs
    )

    private class StatsCore(
        val overview: StatsOverviewRow,
        val periods: List<PeriodListening>,
        val weekDays: List<DailyListening>
    )

    private class StatsExtra(
        val yearDays: List<DailyListening>,
        val hours: List<HourlyListening>,
        val topSongs: List<SongListeningStat>,
        val topArtists: List<ArtistListeningStat>
    )

    private companion object {
        const val DAY_MS = 86_400_000L
        const val YEAR_WINDOW_MS = 365L * DAY_MS

        /** 常听榜单取多少条；界面默认只露前几条，展开后能看到全部。 */
        const val TOP_SONG_LIMIT = 50
        const val TOP_ARTIST_LIMIT = 20

        /** 排行榜取多少条。 */
        const val LEADERBOARD_LIMIT = 100

        /**
         * 算连续天数时最多回看多少天。400 天足够覆盖一年连击，
         * 又不会让"从第一天开始听"的用户把整表拉回来。
         */
        const val STREAK_WINDOW_DAYS = 400
    }
}

/**
 * 截至今天（或昨天）的连续听歌天数。
 *
 * 入参是**从近到远**排好的本地日历日（epochDay），由 DAO 的 `recentDayKeys` 保证顺序。
 *
 * 允许"今天还没听"：只要有昨天，连击就还没断 —— 用户早上打开应用看到 0 天会很困惑。
 * 最近一次听歌早于昨天则归零。
 */
internal fun listeningStreakDays(dayKeysDesc: List<Long>, todayEpoch: Long): Int {
    if (dayKeysDesc.isEmpty()) return 0
    val latest = dayKeysDesc.first()
    if (latest < todayEpoch - 1) return 0

    var streak = 0
    var expected = latest
    for (day in dayKeysDesc) {
        if (day != expected) break
        streak += 1
        expected -= 1
    }
    return streak
}
