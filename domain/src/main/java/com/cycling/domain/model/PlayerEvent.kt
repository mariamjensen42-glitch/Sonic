package com.cycling.domain.model

/**
 * 播放侧的一次性事件 —— 用状态差分表达不了的那些。
 *
 * 当前只有一种：某首歌放不出来（文件损坏、被删、解码失败）。[PlayerState] 里没有位置放它：
 * 错误是"发生了一次"，不是"当前处于某个状态"，写成状态字段还得有人负责把它清掉。
 */
sealed interface PlayerEvent {

    /**
     * [songId] 是失败的那首歌，队列被外部改写时可能为空。
     *
     * [skipped] 表示播放实现层是否已经自动跳到下一首 —— 提示语要区分"已跳过"和"停在这里"，
     * 否则用户会以为音乐还在放。
     */
    data class PlaybackFailed(val songId: Long?, val skipped: Boolean) : PlayerEvent
}
