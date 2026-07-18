package com.cycling.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.model.ExcludedFolder
import com.cycling.domain.repository.ExcludedFolderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.excludeDataStore: DataStore<Preferences> by preferencesDataStore(name = "excluded_folders")

@Singleton
class ExcludedFolderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExcludedFolderRepository {

    companion object {
        private val EXCLUDED_FOLDERS_KEY = stringSetPreferencesKey("excluded_folder_paths")
    }

    override val excludedFolders: Flow<List<ExcludedFolder>> = context.excludeDataStore.data
        .map { preferences ->
            preferences[EXCLUDED_FOLDERS_KEY]?.map { path ->
                ExcludedFolder(path = path)
            } ?: emptyList()
        }

    override suspend fun addExcludedFolder(folder: ExcludedFolder) {
        context.excludeDataStore.edit { preferences ->
            val currentPaths = preferences[EXCLUDED_FOLDERS_KEY]?.toMutableSet() ?: mutableSetOf()
            currentPaths.add(folder.path)
            preferences[EXCLUDED_FOLDERS_KEY] = currentPaths
        }
    }

    override suspend fun removeExcludedFolder(path: String) {
        context.excludeDataStore.edit { preferences ->
            val currentPaths = preferences[EXCLUDED_FOLDERS_KEY]?.toMutableSet() ?: mutableSetOf()
            currentPaths.remove(path)
            preferences[EXCLUDED_FOLDERS_KEY] = currentPaths
        }
    }

    override suspend fun getExcludedPaths(): List<String> {
        val preferences = context.excludeDataStore.data.first()
        return preferences[EXCLUDED_FOLDERS_KEY]?.toList() ?: emptyList()
    }
}
