package com.cycling.data.local.dao

/**
 * `play_events` 的聚合投影。
 *
 * 这些不是实体，只作为 `@Query` 的返回类型存在：字段名必须与 SQL 的 `AS` 别名逐字一致，
 * 否则 Room 在编译期就会报 "no such column"。
 */

/** 总览：累计次数 / 累计时长 / 听歌天数。 */
data class StatsOverviewRow(
    val totalCount: Int,
    val totalPlayMs: Long,
    val totalDays: Int
)

/** 一个自然周期内的累计。 */
data class StatsRangeRow(
    val playCount: Int,
    val playMs: Long
)

/** 按天聚合。[dayKey] 是"本地日历日"的 epochDay。 */
data class DailyPlayRow(
    val dayKey: Long,
    val playCount: Int,
    val playMs: Long
)

/** 按小时聚合，[hourKey] 取 0..23。 */
data class HourStatsRow(
    val hourKey: Int,
    val playCount: Int,
    val playMs: Long
)

/** 单曲成绩：跨天聚合后的"这首歌听了多少遍、多久"。 */
data class SongStatsRow(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val playCount: Int,
    val playMs: Long,
    val lastPlayedAt: Long
)

/** 单个歌手的成绩。 */
data class ArtistStatsRow(
    val artist: String,
    val playCount: Int,
    val playMs: Long
)
