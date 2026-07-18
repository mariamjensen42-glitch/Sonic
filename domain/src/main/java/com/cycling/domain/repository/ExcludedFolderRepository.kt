package com.cycling.domain.repository

import com.cycling.domain.model.ExcludedFolder
import kotlinx.coroutines.flow.Flow

interface ExcludedFolderRepository {
    val excludedFolders: Flow<List<ExcludedFolder>>
    
    suspend fun addExcludedFolder(folder: ExcludedFolder)
    
    suspend fun removeExcludedFolder(path: String)
    
    suspend fun getExcludedPaths(): List<String>
}
