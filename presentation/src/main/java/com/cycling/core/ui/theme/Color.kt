package com.cycling.core.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════
// Spotify-Inspired Dark Theme Colors
// ════════════════════════════════════════════════════════════════
object SpotifyColors {
    /** The main app background — very dark, almost black */
    val Background = Color(0xFF0B0B0F)
    /** Card / grid item background */
    val GridBackground = Color(0xFF15151A)
    /** Surface color for elevated cards */
    val Surface = Color(0xFF1A1A1E)
    /** Surface variant for secondary elements */
    val SurfaceVariant = Color(0xFF242429)
    /** Accent blue used for liked songs & highlights */
    val Accent = Color(0xFF618DFF)
    /** Primary text on dark backgrounds */
    val TextPrimary = Color(0xFFFFFFFF)
    /** Secondary / muted text */
    val TextSecondary = Color(0xFFB3B3B3)
    /** Tertiary / very muted text */
    val TextTertiary = Color(0xFF6A6A6A)
    /** Divider / separator lines */
    val Divider = Color(0xFF2A2A2F)
    /** The green accent used for play buttons */
    val PlayGreen = Color(0xFF1DB954)
    /** Gradient overlay for cards (transparent → background) */
    val CardGradient = Color(0xFF0B0B0F)
}
