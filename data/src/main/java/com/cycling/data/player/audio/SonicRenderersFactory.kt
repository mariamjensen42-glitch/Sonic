package com.cycling.data.player.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * 把自定义 [AudioProcessor] 接进 ExoPlayer 的唯一入口。
 *
 * 为什么必须在这里接：Media3 没有 `ExoPlayer.Builder.setAudioProcessors` —— 处理器是 AudioSink
 * 的一部分，而 AudioSink 由 [DefaultRenderersFactory.buildAudioSink] 创建。覆写它并返回带
 * `setAudioProcessors(...)` 的 [DefaultAudioSink] 是官方文档给出的做法。
 *
 * 注意 media3 1.9.0 的 `buildAudioSink` 是 **3 个参数**（`enableFloatOutput`、
 * `enableAudioOutputPlaybackParameters`）；早期版本多一个 `enableOffload`，沿用旧签名会直接编译失败。
 *
 * `setAudioProcessors` 内部会包成 `DefaultAudioProcessorChain`，其中已经追加了跳过静音与倍速
 * 处理，所以接上均衡器不会丢掉 Sonic 已有的 `setSkipSilence` / 倍速能力。处理链只在 PCM
 * 播放时生效（透传 / 离屏解码不经过这里），这与 ExoPlayer 的既有约束一致。
 */
@UnstableApi
@OptIn(UnstableApi::class)
class SonicRenderersFactory(
    context: Context,
    private val audioProcessors: Array<AudioProcessor>
) : DefaultRenderersFactory(context) {

    @OptIn(UnstableApi::class)
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
        .setAudioProcessors(audioProcessors)
        .build()
}
