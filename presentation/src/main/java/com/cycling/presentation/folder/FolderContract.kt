package com.cycling.presentation.folder

import androidx.annotation.StringRes
import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.FolderRoot
import com.cycling.domain.model.Song

/**
 * 浏览来源。
 *
 * - [LIBRARY]：媒体库目录树，来自 MediaStore/Room 扫描产物，无需授权，但未入库的音频看不见；
 * - [DEVICE_FILES]：SAF 直读文件系统，用户逐目录授权，未扫描过的文件同样能看能播。
 */
enum class FolderSource { LIBRARY, DEVICE_FILES }

data class FolderUiState(
    val source: FolderSource = FolderSource.LIBRARY,
    /** 已授权的浏览目录树（SAF）。 */
    val roots: List<FolderRoot> = emptyList(),
    /** SAF 模式：进入目录树后为 tree uri；媒体库模式：仓库的媒体库标记。 */
    val currentRootUri: String = "",
    /** 当前目录：媒体库模式为文件路径，SAF 模式为 document id。 */
    val currentPath: String = "",
    val parentPath: String? = null,
    val folders: List<FolderItem> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val title: String = "文件夹"
) {
    /** SAF 根列表（未进入任何目录树）。 */
    val atSafRootList: Boolean get() = source == FolderSource.DEVICE_FILES && currentRootUri.isEmpty()

    /**
     * 是否停在某个「列表根」：SAF 根列表，或媒体库树的顶层目录。
     * 此时再按返回应当退出本页，而不是继续回退。
     */
    val atListRoot: Boolean
        get() = if (source == FolderSource.LIBRARY) parentPath == null else currentRootUri.isEmpty()
}

sealed interface FolderIntent {
    data object LoadRoots : FolderIntent
    data class SelectSource(val source: FolderSource) : FolderIntent
    data class AddRoot(val treeUri: String) : FolderIntent
    data class NavigateToFolder(val rootUri: String, val path: String) : FolderIntent
    data class NavigateToLibraryFolder(val path: String) : FolderIntent
    data object NavigateBack : FolderIntent
    data class SongClick(val song: Song) : FolderIntent
}

sealed interface FolderEffect {
    data class NavigateToPlayer(val songId: Long, val songs: List<Song>) : FolderEffect
    data class ShowToast(@StringRes val message: Int) : FolderEffect
}
