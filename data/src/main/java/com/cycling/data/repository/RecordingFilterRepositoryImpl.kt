package com.cycling.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.repository.RecordingFilterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recordingFilterDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "recording_filter")

@Singleton
class RecordingFilterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RecordingFilterRepository {

    override val excludeRecordings: Flow<Boolean> = context.recordingFilterDataStore.data
        .map { it[EXCLUDE_RECORDINGS_KEY] ?: DEFAULT }

    override suspend fun setExcludeRecordings(enabled: Boolean) {
        context.recordingFilterDataStore.edit { it[EXCLUDE_RECORDINGS_KEY] = enabled }
    }

    override suspend fun isExcludeRecordingsEnabled(): Boolean =
        context.recordingFilterDataStore.data.first()[EXCLUDE_RECORDINGS_KEY] ?: DEFAULT

    companion object {
        private val EXCLUDE_RECORDINGS_KEY = booleanPreferencesKey("exclude_recordings")

        /** 默认开启：曲库里混着通话录音几乎没人想要。 */
        const val DEFAULT = true
    }
}
