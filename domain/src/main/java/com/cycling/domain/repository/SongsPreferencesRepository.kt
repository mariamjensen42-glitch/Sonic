package com.cycling.domain.repository

import com.cycling.domain.model.SortOrder
import com.cycling.domain.model.ViewMode

interface SongsPreferencesRepository {
    suspend fun getViewMode(): ViewMode
    suspend fun getSortOrder(): SortOrder
    suspend fun getSortAscending(): Boolean
    suspend fun saveViewMode(viewMode: ViewMode)
    suspend fun saveSortOrder(sortOrder: SortOrder)
    suspend fun saveSortAscending(ascending: Boolean)
}
