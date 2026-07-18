package com.cycling.presentation.folder

import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.Song

data class FolderUiState(
    val currentPath: String = "",
    val parentPath: String? = null,
    val folders: List<FolderItem> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val title: String = "文件夹"
)

sealed interface FolderIntent {
    data object LoadRootFolders : FolderIntent
    data class NavigateToFolder(val path: String) : FolderIntent
    data object NavigateBack : FolderIntent
    data class SongClick(val song: Song) : FolderIntent
}

sealed interface FolderEffect {
    data class NavigateToPlayer(val songId: Long, val songs: List<Song>) : FolderEffect
    data class ShowToast(val message: String) : FolderEffect
}
