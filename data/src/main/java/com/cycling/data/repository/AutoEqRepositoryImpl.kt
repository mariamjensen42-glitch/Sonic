package com.cycling.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.cycling.data.local.autoeq.AutoEqCatalog
import com.cycling.data.store.AudioEffectPrefs
import com.cycling.data.store.audioEffectsDataStore
import com.cycling.domain.audio.AutoEqFilterCodec
import com.cycling.domain.model.AppliedAutoEq
import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import com.cycling.domain.repository.AutoEqRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 耳机校正的持久化。
 *
 * 应用档案时把滤波器链整串写进 DataStore，而不是只存 id：进程重启后均衡器要立刻恢复，
 * 那时去解压 8850 条目录只为找一条记录，代价与收益不成比例。目录只服务于"挑选"这一步。
 */
@Singleton
class AutoEqRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val catalog: AutoEqCatalog
) : AutoEqRepository {

    private val dataStore = context.audioEffectsDataStore

    override val applied: Flow<AppliedAutoEq?> = dataStore.data.map { prefs ->
        val id = prefs[AudioEffectPrefs.AUTO_EQ_ID] ?: return@map null
        val encoded = prefs[AudioEffectPrefs.AUTO_EQ_FILTERS] ?: return@map null
        val filters = AutoEqFilterCodec.decodeFilters(encoded)
        if (filters.isEmpty()) return@map null
        AppliedAutoEq(
            id = id,
            name = prefs[AudioEffectPrefs.AUTO_EQ_NAME].orEmpty(),
            source = prefs[AudioEffectPrefs.AUTO_EQ_SOURCE].orEmpty(),
            preampDb = prefs[AudioEffectPrefs.AUTO_EQ_PREAMP] ?: 0f,
            filters = filters
        )
    }

    override suspend fun totalCount(): Int = catalog.totalCount()

    override suspend fun search(
        query: String,
        formFactor: AutoEqFormFactor?,
        limit: Int
    ): List<AutoEqEntry> = catalog.search(query, formFactor, limit)

    override suspend fun apply(id: String): Boolean {
        val profile = catalog.find(id) ?: return false
        dataStore.edit { prefs ->
            prefs[AudioEffectPrefs.AUTO_EQ_ID] = profile.entry.id
            prefs[AudioEffectPrefs.AUTO_EQ_NAME] = profile.entry.name
            prefs[AudioEffectPrefs.AUTO_EQ_SOURCE] = profile.entry.source
            prefs[AudioEffectPrefs.AUTO_EQ_PREAMP] = profile.preampDb
            prefs[AudioEffectPrefs.AUTO_EQ_FILTERS] =
                AutoEqFilterCodec.encodeFilters(profile.filters)
            // 选了耳机却没开均衡器等于没选，这里顺手打开。
            prefs[AudioEffectPrefs.ENABLED] = true
        }
        return true
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(AudioEffectPrefs.AUTO_EQ_ID)
            prefs.remove(AudioEffectPrefs.AUTO_EQ_NAME)
            prefs.remove(AudioEffectPrefs.AUTO_EQ_SOURCE)
            prefs.remove(AudioEffectPrefs.AUTO_EQ_PREAMP)
            prefs.remove(AudioEffectPrefs.AUTO_EQ_FILTERS)
        }
    }
}
