package com.cycling.data.player

import com.cycling.data.local.dao.PlayEventDao
import com.cycling.data.local.entity.PlayEventEntity
import com.cycling.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 把播放过程写成 `play_events` 行。
 *
 * 之所以单独一个类而不是塞进 [PlayerManager]：写入有节流、有"先插入后更新"的两段式状态，
 * 和队列/状态机是两件事；分开之后 [PlayerManager] 只需要报三个事实（开始、听了多久、结束）。
 *
 * ## 顺序靠单消费者保证
 *
 * 三个入口都是 `trySend` 到一个无限容量的 `Channel`，由**同一个**协程按入队顺序消费。
 * 用 `Mutex` 或者"直接 launch 一个 IO 协程"都不行：那两条路径的先后顺序不保证，
 * 可能出现 `Tick` 先于 `Begin` 落库，把这一首的次数记到上一首头上。
 *
 * ## 两段式写入
 *
 * `Begin` 先插一行 `playMs = 0`，播放过程中每 [COMMIT_INTERVAL_MS] 更新一次，`Finish` 收尾。
 * 只写一次（播完才插）在进程被杀时会整段丢失；只写内存则统计永远是旧的。
 * 代价是统计页可能看到"正在播放的那一首已经有一条 0 秒记录"，这比丢一段数据好。
 */
@Singleton
class PlayEventRecorder @Inject constructor(
    private val dao: PlayEventDao
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Channel<Command>(Channel.UNLIMITED)

    private var session: Session? = null

    init {
        scope.launch {
            for (command in commands) {
                try {
                    handle(command)
                } catch (error: Exception) {
                    // 记统计失败绝不能让播放崩溃：这条记录丢了就丢了。
                    Timber.w(error, "PlayEventRecorder: could not handle %s", command)
                }
            }
        }
    }

    /** 开始一首歌的播放记录。上一首若还没收尾，会被自动按"未完成"结束。 */
    fun begin(song: Song, source: String) {
        commands.trySend(Command.Begin(song, source))
    }

    /** 本次播放又听了 [deltaMs] 毫秒。 */
    fun addListened(deltaMs: Long) {
        if (deltaMs <= 0L) return
        commands.trySend(Command.Tick(deltaMs))
    }

    /**
     * 结束当前记录。[completed] 表示是否听完了整首。
     *
     * [songId] 只用于丢弃迟到的一次收尾：调用方在切歌时才发 Finish，正常情况下永远对得上。
     */
    fun finish(completed: Boolean, songId: Long? = null) {
        commands.trySend(Command.Finish(completed = completed, songId = songId))
    }

    private suspend fun handle(command: Command) {
        when (command) {
            is Command.Begin -> {
                finishSession(completed = false)
                val rowId = dao.insert(
                    PlayEventEntity(
                        platform = PLATFORM_LOCAL,
                        songId = command.song.id,
                        title = command.song.title,
                        artist = command.song.artist,
                        artistId = command.song.artistId,
                        album = command.song.album,
                        coverUrl = command.song.albumArt,
                        durationMs = command.song.duration,
                        playedAt = System.currentTimeMillis(),
                        playMs = 0L,
                        isCompleted = false,
                        source = command.source
                    )
                )
                session = Session(rowId = rowId, songId = command.song.id)
            }

            is Command.Tick -> {
                val current = session ?: return
                current.listenedMs += command.deltaMs
                if (current.listenedMs - current.lastPersistedMs >= COMMIT_INTERVAL_MS) {
                    persist(current, completed = false)
                }
            }

            is Command.Finish -> {
                val current = session ?: return
                // 迟到的 Finish 不该把下一首收掉。
                if (command.songId != null && command.songId != current.songId) return
                finishSession(command.completed)
            }
        }
    }

    private suspend fun finishSession(completed: Boolean) {
        val current = session ?: return
        session = null
        if (current.listenedMs < MIN_RECORDED_MS) {
            // 一闪而过（连点切歌、起播即失败）不配算一次播放。
            dao.deleteById(current.rowId)
            return
        }
        persist(current, completed)
    }

    private suspend fun persist(current: Session, completed: Boolean) {
        current.lastPersistedMs = current.listenedMs
        dao.updateProgress(
            id = current.rowId,
            playMs = current.listenedMs,
            isCompleted = completed
        )
    }

    private class Session(
        val rowId: Long,
        val songId: Long
    ) {
        var listenedMs: Long = 0L
        var lastPersistedMs: Long = 0L
    }

    private sealed interface Command {
        data class Begin(val song: Song, val source: String) : Command
        data class Tick(val deltaMs: Long) : Command
        data class Finish(val completed: Boolean, val songId: Long? = null) : Command
    }

    private companion object {
        const val PLATFORM_LOCAL = "local"

        /** 落库节流：最长每 10 秒把已听时长写回一次。 */
        const val COMMIT_INTERVAL_MS = 10_000L

        /** 少于这个时长不算一次播放。 */
        const val MIN_RECORDED_MS = 1_000L
    }
}
