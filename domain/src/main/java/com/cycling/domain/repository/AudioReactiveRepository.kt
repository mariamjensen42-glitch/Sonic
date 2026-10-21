package com.cycling.domain.repository

import com.cycling.domain.model.BeatState
import kotlinx.coroutines.flow.Flow

/**
 * 播放中音频的节拍 / 响度快照，供播放页律动可视化使用。
 *
 * 数据来自音频链路上的分析器（`BeatAnalyzerProcessor`），不是系统 Visualizer：后者要
 * `RECORD_AUDIO` 权限，而且拿不到 ExoPlayer 处理后的采样。
 *
 * 约定：
 * - [beat] 约 30Hz 更新；安静时降频轮询，暂停后必须回到 [BeatState.Idle]；
 * - 界面若只把值读在绘制阶段，就不会因为 30Hz 更新而重组整页。
 */
interface AudioReactiveRepository {

    val beat: Flow<BeatState>

    /** 律动可视化的开关（关闭后分析器直接旁路，省电）。 */
    val visualizationEnabled: Flow<Boolean>

    suspend fun setVisualizationEnabled(enabled: Boolean)
}
