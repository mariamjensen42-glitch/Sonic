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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<FolderEffect>()
    val uiEffect: SharedFlow<FolderEffect> = _uiEffect.asSharedFlow()

    init {
        loadRootFolders()
    }

    fun handleIntent(intent: FolderIntent) {
        when (intent) {
            is FolderIntent.LoadRootFolders -> loadRootFolders()
            is FolderIntent.NavigateToFolder -> navigateToFolder(intent.path)
            is FolderIntent.NavigateBack -> navigateBack()
            is FolderIntent.SongClick -> onSongClick(intent.song)
        }
    }

    private fun loadRootFolders() {
        Timber.d("loadRootFolders: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = songRepository.getFolderContent("")
                _uiState.update { 
                    it.copy(
                        currentPath = "",
                        parentPath = null,
                        folders = content.folders,
                        songs = emptyList(),
                        isLoading = false,
                        title = "文件夹"
                    )
                }
                Timber.d("loadRootFolders: found ${content.folders.size} folders")
            } catch (e: Exception) {
                Timber.e(e, "loadRootFolders: error")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(FolderEffect.ShowToast("加载文件夹失败"))
            }
        }
    }

    private fun navigateToFolder(path: String) {
        Timber.d("navigateToFolder: path=$path")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = songRepository.getFolderContent(path)
                val title = path.substringAfterLast("/")
                _uiState.update {
                    it.copy(
                        currentPath = content.currentPath,
                        parentPath = content.parentPath,
                        folders = content.folders,
                        songs = content.songs,
                        isLoading = false,
                        title = if (title.isEmpty()) "文件夹" else title
                    )
                }
                Timber.d("navigateToFolder: found ${content.folders.size} folders, ${content.songs.size} songs")
            } catch (e: Exception) {
                Timber.e(e, "navigateToFolder: error")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(FolderEffect.ShowToast("加载文件夹失败"))
            }
        }
    }

    private fun navigateBack() {
        val parentPath = _uiState.value.parentPath
        if (parentPath != null) {
            navigateToFolder(parentPath)
        } else {
            loadRootFolders()
        }
    }

    private fun onSongClick(song: Song) {
        viewModelScope.launch {
            _uiEffect.emit(FolderEffect.NavigateToPlayer(song.id, _uiState.value.songs))
        }
    }
}
