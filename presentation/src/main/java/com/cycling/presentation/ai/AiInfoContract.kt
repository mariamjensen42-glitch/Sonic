package com.cycling.presentation.ai

import com.cycling.domain.model.AiInfo
import com.cycling.domain.model.AiInfoType

data class AiInfoUiState(
    val isLoading: Boolean = false,
    val info: AiInfo? = null,
    val error: String? = null,
    val hasApiKey: Boolean = false
)

sealed interface AiInfoIntent {
    data class LoadSongInfo(val songTitle: String, val artist: String) : AiInfoIntent
    data class LoadArtistInfo(val artistName: String) : AiInfoIntent
    data class LoadAlbumInfo(val albumTitle: String, val artist: String) : AiInfoIntent
    data object Dismiss : AiInfoIntent
    data object Retry : AiInfoIntent
}

sealed interface AiInfoEffect {
    data class ShowToast(val message: String) : AiInfoEffect
    data object NavigateToApiKeyConfig : AiInfoEffect
}
