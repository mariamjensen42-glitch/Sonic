package com.cycling.domain.usecase

import com.cycling.domain.repository.ListeningStatsRepository
import javax.inject.Inject

/** 清空本地听歌统计。用户点「清空本地统计」并确认后才会走到这里。 */
class ClearListeningStatsUseCase @Inject constructor(
    private val repository: ListeningStatsRepository
) {
    suspend operator fun invoke() = repository.clearStats()
}
