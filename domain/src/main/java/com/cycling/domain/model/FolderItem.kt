package com.cycling.domain.model

data class FolderItem(
    val name: String,
    val path: String,
    val songCount: Int = 0
)

data class FolderContent(
    val folders: List<FolderItem>,
    val songs: List<Song>,
    val currentPath: String,
    val parentPath: String? = null
)
