package com.cycling.presentation.player

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.theme.M3ColorExtractor
import com.cycling.core.ui.theme.M3SeedColors
import com.cycling.core.ui.theme.M3ThemeManager
import com.cycling.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object DynamicColorController {

    suspend fun extractColorFromSong(
        song: Song?,
        fallbackColor: Color = M3SeedColors.Purple
    ): Color {
        if (song == null) return fallbackColor
        
        val albumArt = song.albumArt
        if (albumArt.isNullOrBlank()) return fallbackColor

        return try {
            withContext(Dispatchers.IO) {
                val file = File(albumArt)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(albumArt)
                    bitmap?.let {
                        val color = M3ColorExtractor.extractColorFromBitmap(it, fallbackColor)
                        it.recycle()
                        color
                    } ?: fallbackColor
                } else {
                    fallbackColor
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract color from song artwork")
            fallbackColor
        }
    }

    fun applyDynamicColor(color: Color) {
        M3ThemeManager.setDynamicColorEnabled(false)
        M3ThemeManager.setSeedColor(color)
    }

    fun enableSystemDynamicColor() {
        M3ThemeManager.setDynamicColorEnabled(true)
    }
}

@Composable
fun DynamicColorEffect(
    currentSong: Song?,
    enabled: Boolean = true
) {
    LaunchedEffect(currentSong?.path, enabled) {
        if (!enabled) {
            DynamicColorController.enableSystemDynamicColor()
            return@LaunchedEffect
        }
        
        val color = DynamicColorController.extractColorFromSong(currentSong)
        DynamicColorController.applyDynamicColor(color)
        Timber.d("Applied dynamic color: $color for song: ${currentSong?.title}")
    }
}
