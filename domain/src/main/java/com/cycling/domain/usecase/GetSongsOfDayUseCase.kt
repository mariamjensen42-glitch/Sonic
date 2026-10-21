package com.cycling.domain.usecase

import com.cycling.domain.model.SongListeningStat
import com.cycling.domain.repository.ListeningStatsRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 某一天最常听的歌。时间脉络里点开某一天时使用。 */
class GetSongsOfDayUseCase @Inject constructor(
    private val repository: ListeningStatsRepository
) {
    operator fun invoke(date: LocalDate, limit: Int): Flow<List<SongListeningStat>> =
        repository.observeSongsOfDay(date, limit)
}
