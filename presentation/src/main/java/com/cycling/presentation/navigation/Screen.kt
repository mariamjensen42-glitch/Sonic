package com.cycling.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {

    @Serializable
    data object Home : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data object Library : Screen

    @Serializable
    data class SongDetail(val songId: Long) : Screen

    @Serializable
    data object Player : Screen

    @Serializable
    data class AiInfo(val type: String, val name: String, val artist: String = "") : Screen
}

sealed interface LibraryDestination : Screen {
    @Serializable
    data object Songs : LibraryDestination

    @Serializable
    data object Albums : LibraryDestination

    @Serializable
    data object Artists : LibraryDestination

    @Serializable
    data object MyMusic : LibraryDestination

    @Serializable
    data object Folders : LibraryDestination

    @Serializable
    data class AlbumDetail(val albumId: Long) : LibraryDestination

    @Serializable
    data class ArtistDetail(val artistId: Long) : LibraryDestination

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : LibraryDestination
}

sealed interface SettingsDestination : Screen {
    @Serializable
    data object Main : SettingsDestination
}
