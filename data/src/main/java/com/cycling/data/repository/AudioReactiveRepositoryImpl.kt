package com.cycling.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.cycling.data.player.audio.AudioReactiveBus
import com.cycling.data.store.audioEffectsDataStore
import com.cycling.domain.model.BeatState
import com.cycling.domain.repository.AudioReactiveRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 把音频线程的节拍快照节流成界面可用的 Flow。
 *
 * 为什么不让处理器直接发 Flow：音频回调每 128 采样就结算一次（约 344Hz），逐条发事件会让
 * Compose 每帧重组多次。这里按 30Hz 轮询快照，并使用 [distinctUntilChanged] 把重复值吃掉 ——
 * 静止（暂停、静音段）时值恒为 [BeatState.Idle]，下游收不到新值，动画自然停住。
 *
 * 静止时把轮询降到 250ms，避免暂停后还在空转唤醒。
 */
@Singleton
class AudioReactiveRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val bus: AudioReactiveBus
) : AudioReactiveRepository {

    private val dataStore = context.audioEffectsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val visualizationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VISUALIZATION_KEY] ?: true
    }

    override val beat: Flow<BeatState> = flow {
        while (true) {
            val snapshot = bus.latest
            emit(snapshot)
            delay(if (snapshot.isSilent) IDLE_POLL_INTERVAL_MS else ACTIVE_POLL_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    init {
        scope.launch {
            visualizationEnabled.collect { enabled ->
                bus.enabled = enabled
                if (!enabled) bus.reset()
            }
        }
    }

    override suspend fun setVisualizationEnabled(enabled: Boolean) {
        dataStore.edit { it[VISUALIZATION_KEY] = enabled }
    }

    private companion object {
        val VISUALIZATION_KEY = booleanPreferencesKey("beat_visualization_enabled")

        /** 约 30Hz，够动画顺滑又不至于每帧多次重组。 */
        const val ACTIVE_POLL_INTERVAL_MS = 33L

        /** 安静时降频，暂停后基本不唤醒。 */
        const val IDLE_POLL_INTERVAL_MS = 250L
    }
}
