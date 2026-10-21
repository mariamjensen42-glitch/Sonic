package com.cycling.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lyrics_cache_meta")

/**
 * `lyrics_cache` 的迁移代号。
 *
 * 缓存行只记住「这份歌词来自哪个来源」，不记得「还有没有比它优先的来源没问过」。所以在
 * 在线来源集合变化时（新增一个 provider、调整默认优先级），之前缓存的网易云歌词会一直赢到
 * TTL 到期 —— 新来源对已经放过的歌等于不存在。代号变化时把整表清一次，让下一次播放重新按
 * 当前来源顺序查一遍；用户手写的歌词在 `lyrics_override` 里，不受影响。
 *
 * 只在**来源集合/默认顺序变了**的时候递增这个常量，不要因为改了解析逻辑就清库。
 */
@Singleton
class LyricsCacheGenerationStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val generationKey = intPreferencesKey("generation")

    suspend fun get(): Int = context.dataStore.data.map { it[generationKey] ?: 0 }.first()

    suspend fun set(generation: Int) {
        context.dataStore.edit { it[generationKey] = generation }
    }
}
