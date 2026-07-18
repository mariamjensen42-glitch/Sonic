package com.cycling.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKeyKey = stringPreferencesKey("deepseek_api_key")

    suspend fun getApiKey(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[apiKeyKey]
        }.first()
    }

    suspend fun setApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[apiKeyKey] = apiKey
        }
    }

    suspend fun hasApiKey(): Boolean {
        return getApiKey() != null
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(apiKeyKey)
        }
    }
}
