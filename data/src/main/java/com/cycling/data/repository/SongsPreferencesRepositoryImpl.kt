package com.cycling.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.model.SortOrder
import com.cycling.domain.model.ViewMode
import com.cycling.domain.repository.SongsPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.songsDataStore: DataStore<Preferences> by preferencesDataStore(name = "songs_preferences")

@Singleton
class SongsPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SongsPreferencesRepository {

    companion object {
        private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
        private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
        private val SORT_ASCENDING_KEY = stringPreferencesKey("sort_ascending")
    }

    override suspend fun getViewMode(): ViewMode {
        val preferences = context.songsDataStore.data.first()
        val value = preferences[VIEW_MODE_KEY] ?: ViewMode.LIST.name
        return try { ViewMode.valueOf(value) } catch (e: Exception) { ViewMode.LIST }
    }

    override suspend fun getSortOrder(): SortOrder {
        val preferences = context.songsDataStore.data.first()
        val value = preferences[SORT_ORDER_KEY] ?: SortOrder.TITLE.name
        return try { SortOrder.valueOf(value) } catch (e: Exception) { SortOrder.TITLE }
    }

    override suspend fun getSortAscending(): Boolean {
        val preferences = context.songsDataStore.data.first()
        return preferences[SORT_ASCENDING_KEY]?.toBoolean() ?: true
    }

    override suspend fun saveViewMode(viewMode: ViewMode) {
        context.songsDataStore.edit { it[VIEW_MODE_KEY] = viewMode.name }
    }

    override suspend fun saveSortOrder(sortOrder: SortOrder) {
        context.songsDataStore.edit { it[SORT_ORDER_KEY] = sortOrder.name }
    }

    override suspend fun saveSortAscending(ascending: Boolean) {
        context.songsDataStore.edit { it[SORT_ASCENDING_KEY] = ascending.toString() }
    }
}
