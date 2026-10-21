package com.cycling.data.player.audio

import com.cycling.domain.model.BeatState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频线程 → 界面线程的节拍信箱。
 *
 * 音频回调在 128 采样的块粒度上写（约 344Hz @44.1kHz），界面按 30Hz 读，所以这里只留一个
 * `@Volatile` 快照：写方不阻塞、读方不需要加锁，也不必为每一次分析结果都发一次 Flow 事件。
 *
 * 写方把 `(pulse, level, beat)` 推到共享快照，读方按自己的节奏取最新值。
 */
@Singleton
class AudioReactiveBus @Inject constructor() {

    @Volatile
    var latest: BeatState = BeatState.Idle
        private set

    /**
     * 可视化开关。像处理器的 `isActive()` 那样"始终激活、内部旁路"—— 运行中把它翻成 false
     * 不需要等一次音频管线重配就能立刻省掉分析。
     */
    @Volatile
    var enabled: Boolean = true

    /** 由音频线程调用；参数在这里夹紧，界面拿到的一定是合法范围。 */
    fun publish(level: Float, pulse: Float, beat: Float) {
        latest = BeatState(
            level = level.coerceIn(0f, 1f),
            pulse = pulse.coerceIn(0f, 1f),
            beat = beat.coerceIn(0f, 1f)
        )
    }

    /** 暂停 / 切歌 / 关闭可视化时归零，界面据此冻结动画。 */
    fun reset() {
        latest = BeatState.Idle
    }
}
