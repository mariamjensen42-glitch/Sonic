package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cycling.data.local.entity.PlayEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 播放事件的读写。
 *
 * ## 为什么聚合都在 SQL 里
 *
 * 一年的播放记录是几万行，把原始事件取到内存再按天/按小时分组，等于让每次进统计页都做一遍
 * 全表搬运。SQLite 本来就是干这个的：`GROUP BY` + `COUNT` + `SUM` 全部走索引，一次游标扫过
 * 就出结果，返回的只有 7 / 24 / N 行。
 *
 * ## 时区
 *
 * `playedAt` 存的是 UTC epoch 毫秒，而"每天""几点"都是本地概念。所有按天/按小时的查询都带
 * `:tzOffsetMs`，由调用方传入**当前时区偏移**，SQL 里先平移再整除：
 * `(playedAt + :tzOffsetMs) / 86400000`。这样跨时区不会把一天劈成两天，夏令时也只需要
 * 传当时的偏移（本项目不缓存，每次查询现取）。
 *
 * ## 刷新
 *
 * 读侧全部返回 [Flow]：只要 `play_events` 有写入，Room 就会让这些 Flow 重新发射，
 * 统计页不需要自己轮询或监听播放器。
 */
@Dao
interface PlayEventDao {

    // ── 写入（由 PlayEventRecorder 独占调用） ────────────────────────────

    @Insert
    suspend fun insert(event: PlayEventEntity): Long

    @Query("UPDATE play_events SET playMs = :playMs, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: Long, playMs: Long, isCompleted: Boolean)

    /** 一秒都没听的行不该留在统计里（起播即失败、连点切歌）。 */
    @Query("DELETE FROM play_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM play_events")
    suspend fun clearAll()

    // ── 总览 ─────────────────────────────────────────────────────────────

    @Query(
        """
        SELECT COUNT(*) AS totalCount,
               COALESCE(SUM(playMs), 0) AS totalPlayMs,
               COUNT(DISTINCT CAST((playedAt + :tzOffsetMs) / 86400000 AS INTEGER)) AS totalDays
        FROM play_events
        WHERE playedAt >= :since
        """
    )
    fun overview(since: Long, tzOffsetMs: Long): Flow<StatsOverviewRow>

    @Query(
        """
        SELECT COUNT(*) AS playCount, COALESCE(SUM(playMs), 0) AS playMs
        FROM play_events
        WHERE playedAt >= :start AND playedAt < :end
        """
    )
    fun rangeTotals(start: Long, end: Long): Flow<StatsRangeRow>

    // ── 时间脉络 ─────────────────────────────────────────────────────────

    @Query(
        """
        SELECT CAST((playedAt + :tzOffsetMs) / 86400000 AS INTEGER) AS dayKey,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs
        FROM play_events
        WHERE playedAt >= :since
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """
    )
    fun dailySince(since: Long, tzOffsetMs: Long): Flow<List<DailyPlayRow>>

    @Query(
        """
        SELECT CAST(((playedAt + :tzOffsetMs) / 3600000) % 24 AS INTEGER) AS hourKey,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs
        FROM play_events
        WHERE playedAt >= :since
        GROUP BY hourKey
        ORDER BY hourKey ASC
        """
    )
    fun hourlySince(since: Long, tzOffsetMs: Long): Flow<List<HourStatsRow>>

    // ── 常听 ─────────────────────────────────────────────────────────────

    @Query(
        """
        SELECT songId, title, artist, album, coverUrl,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs,
               MAX(playedAt) AS lastPlayedAt
        FROM play_events
        WHERE playedAt >= :since
        GROUP BY platform, songId
        ORDER BY playCount DESC, playMs DESC, lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun topSongsSince(since: Long, limit: Int): Flow<List<SongStatsRow>>

    @Query(
        """
        SELECT artist,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs
        FROM play_events
        WHERE playedAt >= :since
        GROUP BY artist
        ORDER BY playCount DESC, playMs DESC
        LIMIT :limit
        """
    )
    fun topArtistsSince(since: Long, limit: Int): Flow<List<ArtistStatsRow>>

    /** 某一天内最常听的歌：窗口是 [start, end)，调用方按本地日历日算好边界。 */
    @Query(
        """
        SELECT songId, title, artist, album, coverUrl,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs,
               MAX(playedAt) AS lastPlayedAt
        FROM play_events
        WHERE playedAt >= :start AND playedAt < :end
        GROUP BY platform, songId
        ORDER BY playCount DESC, playMs DESC, lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun songsBetween(start: Long, end: Long, limit: Int): Flow<List<SongStatsRow>>

    // ── 排行榜 ───────────────────────────────────────────────────────────

    /** 时长榜：同样听了几遍时，听得更久的排前面。 */
    @Query(
        """
        SELECT songId, title, artist, album, coverUrl,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs,
               MAX(playedAt) AS lastPlayedAt
        FROM play_events
        WHERE playedAt >= :start AND playedAt < :end
        GROUP BY platform, songId
        ORDER BY playMs DESC, playCount DESC, lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun rankSongsByDuration(
        start: Long,
        end: Long,
        limit: Int
    ): Flow<List<SongStatsRow>>

    /** 次数榜：同样听了几遍时，听得更久的排前面。 */
    @Query(
        """
        SELECT songId, title, artist, album, coverUrl,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs,
               MAX(playedAt) AS lastPlayedAt
        FROM play_events
        WHERE playedAt >= :start AND playedAt < :end
        GROUP BY platform, songId
        ORDER BY playCount DESC, playMs DESC, lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun rankSongsByPlays(
        start: Long,
        end: Long,
        limit: Int
    ): Flow<List<SongStatsRow>>

    // ── 乐迷档案 ─────────────────────────────────────────────────────────

    /**
     * 有记录的日历日，从近到远。
     *
     * 连续天数不能只靠 `COUNT(DISTINCT day)`：那只是"一共听过多少天"，算不出"连续"。
     * 取回最近 [limit] 个有记录的天在内存里走一遍，比写一个递归 CTE 简单得多。
     */
    @Query(
        """
        SELECT DISTINCT CAST((playedAt + :tzOffsetMs) / 86400000 AS INTEGER) AS dayKey
        FROM play_events
        ORDER BY dayKey DESC
        LIMIT :limit
        """
    )
    fun recentDayKeys(tzOffsetMs: Long, limit: Int): Flow<List<Long>>

    /** 个人最佳单日：按当天总时长取最高的那一天（没有记录时返回空列表）。 */
    @Query(
        """
        SELECT CAST((playedAt + :tzOffsetMs) / 86400000 AS INTEGER) AS dayKey,
               COUNT(*) AS playCount,
               COALESCE(SUM(playMs), 0) AS playMs
        FROM play_events
        GROUP BY dayKey
        ORDER BY playMs DESC
        LIMIT 1
        """
    )
    fun bestDay(tzOffsetMs: Long): Flow<List<DailyPlayRow>>
}
