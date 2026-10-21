package com.cycling.domain.usecase

import com.cycling.domain.model.ListeningStats
import com.cycling.domain.repository.ListeningStatsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 听歌统计整屏数据：总览、时间脉络、听歌时刻、常听。 */
class GetListeningStatsUseCase @Inject constructor(
    private val repository: ListeningStatsRepository
) {
    operator fun invoke(): Flow<ListeningStats> = repository.observeStats()
}
