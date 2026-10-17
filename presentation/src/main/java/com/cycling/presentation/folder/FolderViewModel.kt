package com.cycling.presentation.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cycling.presentation.R
import timber.log.Timber
import javax.inject.Inject

/**
 * 文件夹浏览，两种来源用户自选：
 *
 * - 媒体库（[SongRepository.getLibraryFolderContent]）：扫描产物的目录树，免授权；
 * - 本机文件夹（[SongRepository.getFolderContent]）：SAF 直读文件系统，未入库的也能播。
 *
 * 无论哪种来源，点文件都以该目录的歌曲列表起播。
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<FolderEffect>()
    val uiEffect: SharedFlow<FolderEffect> = _uiEffect.asSharedFlow()

    init {
        // 默认媒体库：与改造前的入口行为一致，老用户零学习成本
        loadLibraryFolder("")
    }

    fun handleIntent(intent: FolderIntent) {
        when (intent) {
            is FolderIntent.LoadRoots -> loadRoots()
            is FolderIntent.SelectSource -> selectSource(intent.source)
            is FolderIntent.AddRoot -> addRoot(intent.treeUri)
            is FolderIntent.NavigateToFolder -> navigateToFolder(intent.rootUri, intent.path)
            is FolderIntent.NavigateToLibraryFolder -> loadLibraryFolder(intent.path)
            is FolderIntent.NavigateBack -> navigateBack()
            is FolderIntent.SongClick -> onSongClick(intent.song)
        }
    }

    private fun selectSource(source: FolderSource) {
        if (_uiState.value.source == source) return
        when (source) {
            FolderSource.LIBRARY -> loadLibraryFolder("")
            FolderSource.DEVICE_FILES -> loadRoots()
        }
    }

    private fun loadLibraryFolder(path: String) {
        Timber.d("loadLibraryFolder: path=$path")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = songRepository.getLibraryFolderContent(path)
                _uiState.update {
                    it.copy(
                        source = FolderSource.LIBRARY,
                        roots = emptyList(),
                        currentRootUri = content.rootUri,
                        currentPath = content.currentPath,
                        parentPath = content.parentPath,
                        folders = content.folders,
                        songs = content.songs,
                        isLoading = false,
                        title = content.currentPath.substringAfterLast('/').ifEmpty { "文件夹" }
                    )
                }
                Timber.d("loadLibraryFolder: ${content.folders.size} folders, ${content.songs.size} songs")
            } catch (e: Exception) {
                Timber.e(e, "loadLibraryFolder: error")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(FolderEffect.ShowToast(R.string.folder_vm_load_failed))
            }
        }
    }

    private fun loadRoots() {
        Timber.d("loadRoots: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val roots = songRepository.getFolderRoots()
                _uiState.update {
                    it.copy(
                        source = FolderSource.DEVICE_FILES,
                        roots = roots,
                        currentRootUri = "",
                        currentPath = "",
                        parentPath = null,
                        folders = emptyList(),
                        songs = emptyList(),
                        isLoading = false,
                        title = "文件夹"
                    )
                }
                Timber.d("loadRoots: ${roots.size} roots")
            } catch (e: Exception) {
                Timber.e(e, "loadRoots: error")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(FolderEffect.ShowToast(R.string.folder_vm_load_failed))
            }
        }
    }

    private fun addRoot(treeUri: String) {
        viewModelScope.launch {
            val added = songRepository.addFolderRoot(treeUri)
            if (!added) {
                _uiEffect.emit(FolderEffect.ShowToast(R.string.folder_vm_add_root_failed))
                return@launch
            }
            // 授权成功直接进入这棵树，省一次「根列表 -> 点击」
            navigateToFolder(treeUri, "")
        }
    }

    private fun navigateToFolder(rootUri: String, path: String) {
        Timber.d("navigateToFolder: $rootUri path=$path")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = songRepository.getFolderContent(rootUri, path)
                _uiState.update {
                    it.copy(
                        currentRootUri = content.rootUri,
                        currentPath = content.currentPath,
                        parentPath = content.parentPath,
                        folders = content.folders,
                        songs = content.songs,
                        isLoading = false,
                        title = content.currentPath.substringAfterLast('/').ifEmpty { "文件夹" }
                    )
                }
                Timber.d("navigateToFolder: ${content.folders.size} folders, ${content.songs.size} songs")
            } catch (e: Exception) {
                Timber.e(e, "navigateToFolder: error")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(FolderEffect.ShowToast(R.string.folder_vm_load_failed))
            }
        }
    }

    private fun navigateBack() {
        when (_uiState.value.source) {
            // 媒体库顶层再往上没有内容，停在原地；SAF 树根再往上回到根列表
            FolderSource.LIBRARY -> _uiState.value.parentPath?.let(::loadLibraryFolder)
            FolderSource.DEVICE_FILES -> {
                val parentPath = _uiState.value.parentPath
                if (parentPath != null) {
                    navigateToFolder(_uiState.value.currentRootUri, parentPath)
                } else {
                    loadRoots()
                }
            }
        }
    }

    private fun onSongClick(song: Song) {
        viewModelScope.launch {
            _uiEffect.emit(FolderEffect.NavigateToPlayer(song.id, _uiState.value.songs))
        }
    }
}
