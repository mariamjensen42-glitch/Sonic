package com.cycling.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.model.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_state")

@Singleton
class PlaybackStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val queueSongIdsKey = stringPreferencesKey("queue_song_ids")
    private val currentIndexKey = intPreferencesKey("current_index")
    private val playbackPositionKey = longPreferencesKey("playback_position")
    private val repeatModeKey = stringPreferencesKey("repeat_mode")
    private val shuffleModeKey = booleanPreferencesKey("shuffle_mode")
    private val wasPlayingKey = booleanPreferencesKey("was_playing")

    data class SavedState(
        val queueSongIds: List<Long>,
        val currentIndex: Int,
        val playbackPosition: Long,
        val repeatMode: RepeatMode,
        val shuffleMode: Boolean,
        val wasPlaying: Boolean
    )

    suspend fun getSavedState(): SavedState? {
        return context.playerDataStore.data.map { prefs ->
            val idsJson = prefs[queueSongIdsKey] ?: return@map null
            val currentIndex = prefs[currentIndexKey] ?: return@map null
            val playbackPosition = prefs[playbackPositionKey] ?: 0L
            val repeatModeStr = prefs[repeatModeKey] ?: return@map null
            val shuffleMode = prefs[shuffleModeKey] ?: false
            val wasPlaying = prefs[wasPlayingKey] ?: false
            val repeatMode = try { RepeatMode.valueOf(repeatModeStr) } catch (_: Exception) { return@map null }
            val ids = try { Json.decodeFromString<List<Long>>(idsJson) } catch (_: Exception) { return@map null }
            if (ids.isEmpty()) return@map null
            SavedState(
                queueSongIds = ids,
                currentIndex = currentIndex,
                playbackPosition = playbackPosition,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                wasPlaying = wasPlaying
            )
        }.first()
    }

    suspend fun saveQueue(queueSongIds: List<Long>, currentIndex: Int) {
        context.playerDataStore.edit { prefs ->
            prefs[queueSongIdsKey] = Json.encodeToString(queueSongIds)
            prefs[currentIndexKey] = currentIndex
        }
    }

    suspend fun savePlaybackPosition(position: Long) {
        context.playerDataStore.edit { prefs ->
            prefs[playbackPositionKey] = position
        }
    }

    suspend fun saveRepeatMode(mode: RepeatMode) {
        context.playerDataStore.edit { prefs ->
            prefs[repeatModeKey] = mode.name
        }
    }

    suspend fun saveShuffleMode(shuffle: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[shuffleModeKey] = shuffle
        }
    }

    suspend fun saveWasPlaying(wasPlaying: Boolean) {
        context.playerDataStore.edit { prefs ->
            prefs[wasPlayingKey] = wasPlaying
        }
    }

    suspend fun saveAll(state: SavedState) {
        context.playerDataStore.edit { prefs ->
            prefs[queueSongIdsKey] = Json.encodeToString(state.queueSongIds)
            prefs[currentIndexKey] = state.currentIndex
            prefs[playbackPositionKey] = state.playbackPosition
            prefs[repeatModeKey] = state.repeatMode.name
            prefs[shuffleModeKey] = state.shuffleMode
            prefs[wasPlayingKey] = state.wasPlaying
        }
    }

    suspend fun clear() {
        context.playerDataStore.edit { it.clear() }
    }
}
