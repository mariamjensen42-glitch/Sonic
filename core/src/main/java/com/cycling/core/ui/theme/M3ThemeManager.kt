package com.cycling.core.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

object M3ThemeManager {
    private const val PREFS_NAME = "m3_theme_prefs"
    private const val KEY_SEED_COLOR = "seed_color"
    private const val KEY_CONTRAST_LEVEL = "contrast_level"
    private const val KEY_COLOR_STYLE = "color_style"
    private const val KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _themeState = MutableStateFlow(M3ThemeState())
    val themeState: StateFlow<M3ThemeState> = _themeState.asStateFlow()

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedTheme()
    }

    private fun loadSavedTheme() {
        val seedColorValue = prefs.getLong(KEY_SEED_COLOR, M3SeedColors.Purple.value.toLong())
        val contrastLevelOrdinal = prefs.getInt(KEY_CONTRAST_LEVEL, M3ContrastLevel.Default.ordinal)
        val colorStyleOrdinal = prefs.getInt(KEY_COLOR_STYLE, M3ColorStyle.TonalSpot.ordinal)
        val dynamicColorEnabled = prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true)
        val darkMode = prefs.getInt(KEY_DARK_MODE, -1)

        _themeState.value = M3ThemeState(
            seedColor = Color(seedColorValue.toULong()),
            contrastLevel = M3ContrastLevel.entries.getOrElse(contrastLevelOrdinal) { M3ContrastLevel.Default },
            colorStyle = M3ColorStyle.entries.getOrElse(colorStyleOrdinal) { M3ColorStyle.TonalSpot },
            dynamicColorEnabled = dynamicColorEnabled,
            darkMode = if (darkMode == -1) null else darkMode == 1
        )
    }

    fun setSeedColor(color: Color) {
        prefs.edit { putLong(KEY_SEED_COLOR, color.value.toLong()) }
        _themeState.value = _themeState.value.copy(seedColor = color)
    }

    fun setContrastLevel(level: M3ContrastLevel) {
        prefs.edit { putInt(KEY_CONTRAST_LEVEL, level.ordinal) }
        _themeState.value = _themeState.value.copy(contrastLevel = level)
    }

    fun setColorStyle(style: M3ColorStyle) {
        prefs.edit { putInt(KEY_COLOR_STYLE, style.ordinal) }
        _themeState.value = _themeState.value.copy(colorStyle = style)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled) }
        _themeState.value = _themeState.value.copy(dynamicColorEnabled = enabled)
    }

    fun setDarkMode(enabled: Boolean?) {
        val value = when (enabled) {
            true -> 1
            false -> 0
            null -> -1
        }
        prefs.edit { putInt(KEY_DARK_MODE, value) }
        _themeState.value = _themeState.value.copy(darkMode = enabled)
    }

    fun resetToDefault() {
        setSeedColor(M3SeedColors.Purple)
        setContrastLevel(M3ContrastLevel.Default)
        setColorStyle(M3ColorStyle.TonalSpot)
        setDynamicColorEnabled(true)
        setDarkMode(null)
    }

    fun applyPreset(preset: M3ThemePreset) {
        setSeedColor(preset.seedColor)
        setColorStyle(preset.colorStyle)
    }
}

data class M3ThemeState(
    val seedColor: Color = M3SeedColors.Purple,
    val contrastLevel: M3ContrastLevel = M3ContrastLevel.Default,
    val colorStyle: M3ColorStyle = M3ColorStyle.TonalSpot,
    val dynamicColorEnabled: Boolean = true,
    val darkMode: Boolean? = null
)

data class M3ThemePreset(
    val name: String,
    val seedColor: Color,
    val colorStyle: M3ColorStyle = M3ColorStyle.TonalSpot
)

object M3ThemePresets {
    val PurpleDream = M3ThemePreset(
        name = "Purple Dream",
        seedColor = M3SeedColors.Purple,
        colorStyle = M3ColorStyle.TonalSpot
    )

    val OceanBlue = M3ThemePreset(
        name = "Ocean Blue",
        seedColor = M3SeedColors.Blue,
        colorStyle = M3ColorStyle.Vibrant
    )

    val SunsetOrange = M3ThemePreset(
        name = "Sunset Orange",
        seedColor = M3SeedColors.Orange,
        colorStyle = M3ColorStyle.Expressive
    )

    val ForestGreen = M3ThemePreset(
        name = "Forest Green",
        seedColor = M3SeedColors.Green,
        colorStyle = M3ColorStyle.Neutral
    )

    val PinkBlush = M3ThemePreset(
        name = "Pink Blush",
        seedColor = M3SeedColors.Pink,
        colorStyle = M3ColorStyle.Vibrant
    )

    val TealWave = M3ThemePreset(
        name = "Teal Wave",
        seedColor = M3SeedColors.Teal,
        colorStyle = M3ColorStyle.TonalSpot
    )

    val All = listOf(
        PurpleDream,
        OceanBlue,
        SunsetOrange,
        ForestGreen,
        PinkBlush,
        TealWave
    )
}

class M3ThemeViewModel : ViewModel() {
    val themeState: StateFlow<M3ThemeState> = M3ThemeManager.themeState

    fun setSeedColor(color: Color) = M3ThemeManager.setSeedColor(color)
    fun setContrastLevel(level: M3ContrastLevel) = M3ThemeManager.setContrastLevel(level)
    fun setColorStyle(style: M3ColorStyle) = M3ThemeManager.setColorStyle(style)
    fun setDynamicColorEnabled(enabled: Boolean) = M3ThemeManager.setDynamicColorEnabled(enabled)
    fun setDarkMode(enabled: Boolean?) = M3ThemeManager.setDarkMode(enabled)
    fun resetToDefault() = M3ThemeManager.resetToDefault()
    fun applyPreset(preset: M3ThemePreset) = M3ThemeManager.applyPreset(preset)
}

@Composable
fun rememberM3ThemeState(): State<M3ThemeState> {
    return M3ThemeManager.themeState.collectAsState()
}

@Composable
fun rememberM3SeedColor(): State<Color> {
    val state = rememberM3ThemeState()
    return remember { 
        derivedStateOf { state.value.seedColor }
    }
}

@Composable
fun M3ManagedTheme(
    content: @Composable () -> Unit
) {
    val themeState = rememberM3ThemeState().value
    val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    M3DynamicTheme(
        darkTheme = themeState.darkMode ?: systemDarkTheme,
        dynamicColor = themeState.dynamicColorEnabled,
        seedColor = if (themeState.dynamicColorEnabled) null else themeState.seedColor,
        contrastLevel = themeState.contrastLevel,
        colorStyle = themeState.colorStyle,
        content = content
    )
}
