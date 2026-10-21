package com.cycling.domain.repository

import com.cycling.domain.model.LeaderboardMetric
import com.cycling.domain.model.LeaderboardRange
import com.cycling.domain.model.ListeningLeaderboard
import com.cycling.domain.model.ListeningStats
import com.cycling.domain.model.ListenerProfile
import com.cycling.domain.model.SongListeningStat
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * 听歌足迹的**读取**侧：统计与排行榜。
 *
 * 写入侧不在这里 —— 播放事件的记录由 `:data` 的 `PlayEventRecorder` 独占，和
 * `SongLibraryWriter` 独占曲库写入是同一个思路：让唯一的写入口不可被绕过。
 *
 * 所有聚合都在 SQL 里完成（见 `PlayEventDao`）：时间脉络、小时分布、常听、排行榜
 * 都只是同一张 `play_events` 表的不同 GROUP BY，不需要把原始事件搬进内存再算。
 */
interface ListeningStatsRepository {

    /**
     * 统计整屏数据。
     *
     * 是 [Flow] 因为它必须跟着播放实时刷新：每次播放事件落库，Room 会让这个 Flow
     * 重新发射一次，用户从播放页退回来就能看到数字变了。
     */
    fun observeStats(): Flow<ListeningStats>

    /** 某个窗口 + 口径下的歌曲排行榜，附带窗口总计与本地乐迷档案。 */
    fun observeLeaderboard(
        range: LeaderboardRange,
        metric: LeaderboardMetric
    ): Flow<ListeningLeaderboard>

    /** 本地乐迷档案：累计时长、天数、连续天数、个人最佳单日。 */
    fun observeListenerProfile(): Flow<ListenerProfile>

    /** 某一天听过的歌，按次数、时长排序，供「这一天最常听」详情用。 */
    fun observeSongsOfDay(date: LocalDate, limit: Int): Flow<List<SongListeningStat>>

    /** 清空本地统计（排行榜也随之归零）。 */
    suspend fun clearStats()
}
