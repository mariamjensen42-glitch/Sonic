package com.cycling.domain.model

/**
 * Which colour scheme the app should use.
 *
 * [SYSTEM] follows the device; the app previously ignored the system entirely and was hard-coded
 * to a dark palette.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
