package com.cycling.presentation.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.repository.AiRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AiCreateState(
    val showAiCreateDialog: Boolean = false,
    val aiCreateMode: AiCreateMode = AiCreateMode.RANDOM,
    val aiCreateStep: AiCreateStep = AiCreateStep.SELECT_MODE,
    val selectedSongCount: Int = 10,
    val themeInput: String = "",
    val isAiGenerating: Boolean = false,
    val aiGeneratedSongs: List<com.cycling.domain.model.Song> = emptyList(),
    val aiGeneratedPlaylistName: String = "",
    val aiCreateError: String? = null,
    val hasApiKey: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _playlistToRename = MutableStateFlow<com.cycling.domain.model.Playlist?>(null)
    private val _showRenameDialog = MutableStateFlow(false)
    private val _aiCreateState = MutableStateFlow(AiCreateState())

    val uiState = combine(
        playlistRepository.getAllPlaylists(),
        _playlistToRename,
        _showRenameDialog,
        _aiCreateState
    ) { playlists, playlistToRename, showRenameDialog, aiCreateState ->
        PlaylistsUiState(
            playlists = playlists,
            isLoading = false,
            playlistToRename = playlistToRename,
            showRenameDialog = showRenameDialog,
            showAiCreateDialog = aiCreateState.showAiCreateDialog,
            aiCreateMode = aiCreateState.aiCreateMode,
            aiCreateStep = aiCreateState.aiCreateStep,
            selectedSongCount = aiCreateState.selectedSongCount,
            themeInput = aiCreateState.themeInput,
            isAiGenerating = aiCreateState.isAiGenerating,
            aiGeneratedSongs = aiCreateState.aiGeneratedSongs,
            aiGeneratedPlaylistName = aiCreateState.aiGeneratedPlaylistName,
            aiCreateError = aiCreateState.aiCreateError,
            hasApiKey = aiCreateState.hasApiKey
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlaylistsUiState()
    )

    private val _uiEffect = kotlinx.coroutines.channels.Channel<PlaylistsEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: PlaylistsIntent) {
        when (intent) {
            is PlaylistsIntent.PlaylistClick -> {
                Timber.d("handleIntent: PlaylistClick playlistId=${intent.playlist.id}")
                viewModelScope.launch {
                    _uiEffect.send(PlaylistsEffect.NavigateToPlaylistDetail(intent.playlist.id))
                }
            }
            is PlaylistsIntent.CreatePlaylist -> {
                Timber.d("handleIntent: CreatePlaylist name=${intent.name}")
                viewModelScope.launch {
                    playlistRepository.createPlaylist(intent.name)
                    _uiEffect.send(PlaylistsEffect.ShowToast("播放列表已创建"))
                }
            }
            is PlaylistsIntent.DeletePlaylist -> {
                Timber.d("handleIntent: DeletePlaylist playlistId=${intent.playlistId}")
                viewModelScope.launch {
                    playlistRepository.deletePlaylist(intent.playlistId)
                    _uiEffect.send(PlaylistsEffect.ShowToast("播放列表已删除"))
                }
            }
            is PlaylistsIntent.ShowRenameDialog -> {
                Timber.d("handleIntent: ShowRenameDialog playlistId=${intent.playlist.id}")
                _playlistToRename.value = intent.playlist
                _showRenameDialog.value = true
            }
            is PlaylistsIntent.RenamePlaylist -> {
                Timber.d("handleIntent: RenamePlaylist playlistId=${intent.playlistId} newName=${intent.newName}")
                viewModelScope.launch {
                    playlistRepository.renamePlaylist(intent.playlistId, intent.newName)
                    _showRenameDialog.value = false
                    _playlistToRename.value = null
                    _uiEffect.send(PlaylistsEffect.ShowToast("播放列表已重命名"))
                }
            }
            is PlaylistsIntent.DismissRenameDialog -> {
                Timber.d("handleIntent: DismissRenameDialog")
                _showRenameDialog.value = false
                _playlistToRename.value = null
            }
            is PlaylistsIntent.ShowAiCreateDialog -> {
                Timber.d("handleIntent: ShowAiCreateDialog")
                viewModelScope.launch {
                    val hasKey = aiRepository.hasApiKey()
                    if (hasKey) {
                        _aiCreateState.value = AiCreateState(hasApiKey = true, showAiCreateDialog = true)
                    } else {
                        _uiEffect.send(PlaylistsEffect.ShowToast("请先配置 API Key"))
                    }
                }
            }
            is PlaylistsIntent.DismissAiCreateDialog -> {
                Timber.d("handleIntent: DismissAiCreateDialog")
                _aiCreateState.value = AiCreateState()
            }
            is PlaylistsIntent.SetAiCreateMode -> {
                Timber.d("handleIntent: SetAiCreateMode mode=${intent.mode}")
                _aiCreateState.value = _aiCreateState.value.copy(
                    aiCreateMode = intent.mode,
                    aiCreateStep = AiCreateStep.INPUT_DETAILS,
                    aiCreateError = null
                )
            }
            is PlaylistsIntent.SetSelectedSongCount -> {
                Timber.d("handleIntent: SetSelectedSongCount count=${intent.count}")
                _aiCreateState.value = _aiCreateState.value.copy(
                    selectedSongCount = intent.count.coerceIn(1, 100)
                )
            }
            is PlaylistsIntent.SetThemeInput -> {
                Timber.d("handleIntent: SetThemeInput theme=${intent.theme}")
                _aiCreateState.value = _aiCreateState.value.copy(themeInput = intent.theme)
            }
            is PlaylistsIntent.GenerateAiPlaylist -> {
                Timber.d("handleIntent: GenerateAiPlaylist")
                handleGenerateAiPlaylist()
            }
            is PlaylistsIntent.ConfirmAiPlaylist -> {
                Timber.d("handleIntent: ConfirmAiPlaylist")
                handleConfirmAiPlaylist()
            }
        }
    }

    private fun handleGenerateAiPlaylist() {
        viewModelScope.launch {
            _aiCreateState.value = _aiCreateState.value.copy(
                isAiGenerating = true,
                aiCreateError = null
            )

            try {
                val allSongs = songRepository.getAllSongs().first()
                if (allSongs.isEmpty()) {
                    _aiCreateState.value = _aiCreateState.value.copy(
                        isAiGenerating = false,
                        aiCreateError = "没有可用的歌曲"
                    )
                    return@launch
                }

                val currentState = _aiCreateState.value
                when (currentState.aiCreateMode) {
                    AiCreateMode.RANDOM -> {
                        val selectedSongs = allSongs.shuffled().take(currentState.selectedSongCount)
                        _aiCreateState.value = _aiCreateState.value.copy(
                            isAiGenerating = false,
                            aiGeneratedSongs = selectedSongs,
                            aiGeneratedPlaylistName = "随机歌单",
                            aiCreateStep = AiCreateStep.PREVIEW
                        )
                    }
                    AiCreateMode.THEME -> {
                        val theme = currentState.themeInput.trim()
                        if (theme.isBlank()) {
                            _aiCreateState.value = _aiCreateState.value.copy(
                                isAiGenerating = false,
                                aiCreateError = "请输入主题"
                            )
                            return@launch
                        }

                        val result = aiRepository.generatePlaylistByTheme(theme, allSongs)
                        result.fold(
                            onSuccess = { songIds ->
                                val selectedSongs = allSongs.filter { it.id in songIds }
                                if (selectedSongs.isEmpty()) {
                                    _aiCreateState.value = _aiCreateState.value.copy(
                                        isAiGenerating = false,
                                        aiCreateError = "未找到匹配的歌曲"
                                    )
                                } else {
                                    _aiCreateState.value = _aiCreateState.value.copy(
                                        isAiGenerating = false,
                                        aiGeneratedSongs = selectedSongs,
                                        aiGeneratedPlaylistName = "${theme}歌单",
                                        aiCreateStep = AiCreateStep.PREVIEW
                                    )
                                }
                            },
                            onFailure = { error ->
                                Timber.e(error, "generatePlaylistByTheme failed")
                                _aiCreateState.value = _aiCreateState.value.copy(
                                    isAiGenerating = false,
                                    aiCreateError = "生成失败: ${error.message}"
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "handleGenerateAiPlaylist failed")
                _aiCreateState.value = _aiCreateState.value.copy(
                    isAiGenerating = false,
                    aiCreateError = "生成失败: ${e.message}"
                )
            }
        }
    }

    private fun handleConfirmAiPlaylist() {
        viewModelScope.launch {
            val currentState = _aiCreateState.value
            val songs = currentState.aiGeneratedSongs
            if (songs.isEmpty()) {
                _aiCreateState.value = currentState.copy(aiCreateError = "没有可添加的歌曲")
                return@launch
            }

            val playlistName = currentState.aiGeneratedPlaylistName.ifBlank { "AI歌单" }
            
            try {
                val playlistId = playlistRepository.createPlaylist(playlistName)
                val songIds = songs.map { it.id }
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                _aiCreateState.value = AiCreateState()
                _uiEffect.send(PlaylistsEffect.ShowToast("歌单已创建"))
            } catch (e: Exception) {
                Timber.e(e, "handleConfirmAiPlaylist failed")
                _aiCreateState.value = _aiCreateState.value.copy(aiCreateError = "创建歌单失败: ${e.message}")
            }
        }
    }
}
