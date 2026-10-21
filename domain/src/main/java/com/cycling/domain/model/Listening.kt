package com.cycling.domain.model

import java.time.LocalDate

/**
 * 排行榜的统计口径。
 *
 * 同一份听歌记录可以按"听了多久"或"听了多少遍"排序 —— 时长榜看
 * [DURATION]，次数榜看 [PLAYS]。两者共用同一个窗口，切换口径不需要重新查库以外的事。
 */
enum class LeaderboardMetric {
    DURATION,
    PLAYS
}

/**
 * 排行榜的统计窗口。
 *
 * [days] 是滚动窗口的天数，0 表示不限（总榜）。
 * 页面上写作「日榜 / 周榜 / 月榜 / 总榜」，副标题会说明"近 N 天"。
 *
 * 注意它与「本周累计」不是一回事：本周累计是**自然周**（周一 00:00 起），
 * 周榜是**近 7 天**的滚动窗口。
 */
enum class LeaderboardRange(val days: Int) {
    DAY(1),
    WEEK(7),
    MONTH(30),
    ALL(0)
}

/**
 * 听歌统计里的聚合周期，用于总览三格（今日 / 本周 / 本月）。
 *
 * 这几个都是自然周期而不是滚动窗口：本周从周一 00:00 起算，本月从 1 日 00:00 起算。
 */
enum class ListeningPeriod {
    TODAY,
    WEEK,
    MONTH
}

/** 某个自然周期内的累计成绩。 */
data class PeriodListening(
    val period: ListeningPeriod,
    val playCount: Int,
    val playMs: Long
)

/**
 * 按天聚合的听歌量。
 *
 * [date] 是**本地日历日**：数据库里存的是 epoch 毫秒，聚合时加上本地时区偏移再除以
 * 一天的毫秒数，因此跨时区/夏令时都不会把一天劈开成两天。
 */
data class DailyListening(
    val date: LocalDate,
    val playCount: Int,
    val playMs: Long
)

/** 按小时聚合的听歌量，[hour] 取 0..23（本地时间）。 */
data class HourlyListening(
    val hour: Int,
    val playCount: Int,
    val playMs: Long
)

/**
 * 一首歌在某个窗口内的听歌成绩。
 *
 * 字段与写入时的快照一致（标题、歌手、封面都取记录当时的版本）：歌被重命名或删掉之后，
 * 历史榜单仍然保留它当时的样子，不会因为一次重新扫描就变成空白行。
 */
data class SongListeningStat(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val playCount: Int,
    val playMs: Long,
    val lastPlayedAt: Long
) {
    /** 播放次数为 1 时也至少给出一点长度，避免榜单条在短时长下缩成一条线。 */
    val averagePlayMs: Long get() = if (playCount > 0) playMs / playCount else 0L
}

/** 一位歌手在某个窗口内的听歌成绩。 */
data class ArtistListeningStat(
    val artist: String,
    val playCount: Int,
    val playMs: Long
)

/**
 * 听歌统计整屏的数据。
 *
 * 一次查询把总览、时间脉络、小时分布、常听四块都算好：它们全部来自同一张
 * `play_events` 表、同一组参数（时区偏移 + 窗口起点），分开查只会让界面多闪几次。
 */
data class ListeningStats(
    val totalPlayCount: Int = 0,
    val totalPlayMs: Long = 0,
    val totalDays: Int = 0,
    val periods: List<PeriodListening> = emptyList(),
    /** 时间脉络：本周周一到周日 7 桶，没有听歌的天也会补成 0。 */
    val weekDays: List<DailyListening> = emptyList(),
    /** 时间脉络：近一年按天升序，只含有记录的天，调用方自行补齐空缺。 */
    val yearDays: List<DailyListening> = emptyList(),
    /** 听歌时刻：0..23 共 24 桶。 */
    val hours: List<HourlyListening> = emptyList(),
    val topSongs: List<SongListeningStat> = emptyList(),
    val topArtists: List<ArtistListeningStat> = emptyList()
) {
    val hasData: Boolean get() = totalPlayCount > 0

    /** 累计平均每首歌听多久，用于总览副标题。 */
    val averagePlayMs: Long get() = if (totalPlayCount > 0) totalPlayMs / totalPlayCount else 0L
}

/**
 * 本地"乐迷档案"。
 *
 * "乐迷排名"需要多用户互相比较；单机应用没有这个数据，
 * 于是把同一批指标落到**自己**身上：累计时长、天数、连续天数、个人最佳单日，
 * 再按累计时长折算乐迷等级。这样页面上"乐迷"这一栏仍然有真实含义。
 */
data class ListenerProfile(
    val totalPlayMs: Long = 0,
    val totalPlayCount: Int = 0,
    val listeningDays: Int = 0,
    val streakDays: Int = 0,
    val todayPlayMs: Long = 0,
    val weekPlayMs: Long = 0,
    val bestDayPlayMs: Long = 0,
    val bestDay: LocalDate? = null
) {
    /** 乐迷等级，1 起步；门槛见 [LEVEL_THRESHOLDS_MS]。 */
    val level: Int get() = LEVEL_THRESHOLDS_MS.count { totalPlayMs >= it } + 1

    /** 升到下一级所需的总时长；已满级时为 null。 */
    val nextLevelPlayMs: Long? get() = LEVEL_THRESHOLDS_MS.firstOrNull { totalPlayMs < it }

    /** 当前等级已完成的进度，0f..1f；满级为 1f。用于等级进度条。 */
    val levelProgress: Float
        get() {
            val next = nextLevelPlayMs ?: return 1f
            val previous = LEVEL_THRESHOLDS_MS.lastOrNull { totalPlayMs >= it } ?: 0L
            val span = next - previous
            return if (span <= 0L) 1f else ((totalPlayMs - previous).toFloat() / span).coerceIn(0f, 1f)
        }

    companion object {
        /** 升到 2..9 级所需的累计听歌时长：1h / 5h / 20h / 50h / 100h / 200h / 500h / 1000h。 */
        val LEVEL_THRESHOLDS_MS = longArrayOf(
            1L * 60 * 60 * 1000,
            5L * 60 * 60 * 1000,
            20L * 60 * 60 * 1000,
            50L * 60 * 60 * 1000,
            100L * 60 * 60 * 1000,
            200L * 60 * 60 * 1000,
            500L * 60 * 60 * 1000,
            1000L * 60 * 60 * 1000
        )
    }
}

/** 榜单里的一行：名次 + 成绩。 */
data class ListenerRankEntry(
    val rank: Int,
    val song: SongListeningStat
)

/**
 * 一首歌进入排行榜所需的最小信息，也是 [SongListeningStat] 的排行视图。
 *
 * 名次在 ViewModel 层按列表下标生成，数据层只负责按口径排好序。
 */
data class ListeningLeaderboard(
    val range: LeaderboardRange = LeaderboardRange.WEEK,
    val metric: LeaderboardMetric = LeaderboardMetric.DURATION,
    val entries: List<ListenerRankEntry> = emptyList(),
    /** 窗口内的总计，用于榜单条的占比。 */
    val totalPlayMs: Long = 0,
    val totalPlayCount: Int = 0,
    val profile: ListenerProfile = ListenerProfile()
) {
    val hasData: Boolean get() = entries.isNotEmpty()

    /** 时间跨度是否只有一天，决定标题写「今日」还是「本周」。 */
    val isSingleDay: Boolean get() = range == LeaderboardRange.DAY
}
