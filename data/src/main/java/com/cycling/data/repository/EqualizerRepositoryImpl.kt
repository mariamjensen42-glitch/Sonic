package com.cycling.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.cycling.data.player.audio.SoftwareEqualizerProcessor
import com.cycling.data.store.AudioEffectPrefs
import com.cycling.data.store.audioEffectsDataStore
import com.cycling.domain.model.AppliedAutoEq
import com.cycling.domain.model.EqualizerBands
import com.cycling.domain.model.EqualizerPreset
import com.cycling.domain.model.EqFilterSpec
import com.cycling.domain.repository.AutoEqRepository
import com.cycling.domain.repository.EqualizerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 均衡器设置的唯一真相来源，同时负责把快照推给音频处理器。
 *
 * UI 只写仓库，仓库把
 * "滤波器列表 + 预放大 + 开关"整体交给处理器，音频线程不做任何 IO。
 *
 * `init` 里那条 `combine` 兼作启动恢复：进程重启后 DataStore 一读出旧值就会推给处理器，
 * 不需要界面先打开一次均衡器页。
 *
 * 耳机校正（AutoEQ）与图形均衡**叠加**：档案滤波器排在前面，10 段增益接在后面，
 * 预放大取两者之和。这样选耳机不会冲掉用户手调的曲线，取消校正也只撤掉自己那一层。
 */
@Singleton
class EqualizerRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val processor: SoftwareEqualizerProcessor,
    private val autoEqRepository: AutoEqRepository
) : EqualizerRepository {

    private val dataStore = context.audioEffectsDataStore

    /**
     * 仓库是单例、生命周期与进程一致，这里的 scope 不需要手动取消；用 Default 以免
     * 主线程上做 `10.0.pow(...)` 这类计算。
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val enabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AudioEffectPrefs.ENABLED] ?: false
    }

    override val preset: Flow<EqualizerPreset> = dataStore.data.map { prefs ->
        val stored = prefs[AudioEffectPrefs.PRESET]
        EqualizerPreset.entries.firstOrNull { it.name == stored } ?: EqualizerPreset.FLAT
    }

    override val bandGains: Flow<List<Float>> = dataStore.data.map { prefs ->
        EqualizerBands.decodeGains(prefs[AudioEffectPrefs.GAINS])
    }

    override val preampDb: Flow<Float> = dataStore.data.map { prefs ->
        (prefs[AudioEffectPrefs.PREAMP] ?: DEFAULT_PREAMP_DB)
            .coerceIn(EqualizerBands.MIN_PREAMP_DB, EqualizerBands.MAX_PREAMP_DB)
    }

    init {
        scope.launch {
            combine(
                enabled,
                bandGains,
                preampDb,
                autoEqRepository.applied
            ) { isEnabled, gains, preamp, autoEq ->
                processor.update(
                    filters = composeFilters(autoEq, gains),
                    preampDb = preamp + (autoEq?.preampDb ?: 0f),
                    enabled = isEnabled
                )
            }.collect { /* processor.update 已经在 transform 里完成 */ }
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[AudioEffectPrefs.ENABLED] = enabled }
    }

    override suspend fun setPreset(preset: EqualizerPreset) {
        // CUSTOM 是"用户改过"的标记，不是可选预设。
        if (preset == EqualizerPreset.CUSTOM) return
        dataStore.edit { prefs ->
            prefs[AudioEffectPrefs.PRESET] = preset.name
            prefs[AudioEffectPrefs.GAINS] = EqualizerBands.encodeGains(preset.gains)
        }
    }

    override suspend fun setBandGain(index: Int, gainDb: Float) {
        if (index !in 0 until EqualizerBands.count) return
        dataStore.edit { prefs ->
            val gains = EqualizerBands.decodeGains(prefs[AudioEffectPrefs.GAINS]).toMutableList()
            gains[index] = EqualizerBands.snapGain(gainDb)
            prefs[AudioEffectPrefs.GAINS] = EqualizerBands.encodeGains(gains)
            prefs[AudioEffectPrefs.PRESET] = EqualizerPreset.CUSTOM.name
        }
    }

    override suspend fun setPreampDb(preampDb: Float) {
        val clamped = preampDb.coerceIn(EqualizerBands.MIN_PREAMP_DB, EqualizerBands.MAX_PREAMP_DB)
        dataStore.edit { it[AudioEffectPrefs.PREAMP] = clamped }
    }

    override suspend fun reset() {
        dataStore.edit { prefs ->
            prefs[AudioEffectPrefs.GAINS] = EqualizerBands.encodeGains(EqualizerBands.DEFAULT_GAINS)
            prefs[AudioEffectPrefs.PRESET] = EqualizerPreset.FLAT.name
            prefs[AudioEffectPrefs.PREAMP] = DEFAULT_PREAMP_DB
        }
        // "恢复默认"要连耳机校正一起撤掉，否则界面平直、声音还带着校正曲线。
        autoEqRepository.clear()
    }

    /** 档案滤波器在前、图形增益在后；未应用档案时就是原来的 10 段。 */
    private fun composeFilters(autoEq: AppliedAutoEq?, gains: List<Float>): List<EqFilterSpec> {
        val bands = EqualizerBands.filtersFor(gains)
        val profile = autoEq?.filters ?: return bands
        if (profile.isEmpty()) return bands
        return ArrayList<EqFilterSpec>(profile.size + bands.size).apply {
            addAll(profile)
            addAll(bands)
        }
    }

    private companion object {
        const val DEFAULT_PREAMP_DB = 0f
    }
}
