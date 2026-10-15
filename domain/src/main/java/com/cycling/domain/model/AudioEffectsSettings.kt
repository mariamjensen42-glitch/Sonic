package com.cycling.domain.model

/**
 * Persisted audio-effect intent.
 *
 * This is what the *user asked for*, not what the device can do — a phone whose ROM reports no
 * equalizer still stores `equalizer.enabled = true` here; the controller exposes the mismatch
 * separately as runtime state. Keeping the two apart is what lets the settings survive moving to
 * a device with different DSP support.
 */
data class AudioEffectsSettings(
    val equalizer: EqualizerSettings = EqualizerSettings(),
    val bassBoost: ToggleEffectSettings = ToggleEffectSettings(),
    val virtualizer: ToggleEffectSettings = ToggleEffectSettings(),
    val loudness: LoudnessSettings = LoudnessSettings()
)

data class EqualizerSettings(
    val enabled: Boolean = false,
    /**
     * Index into the device's preset list, or [CUSTOM_PRESET] once the user drags a band.
     * Only meaningful while [enabled].
     */
    val presetIndex: Int = 0,
    /**
     * Per-band gains in millibels, in device band order.
     *
     * Length depends on the device that wrote it; a reader with a different band count must
     * ignore the tail rather than assume it matches.
     */
    val bandLevelsMdB: List<Int> = emptyList()
) {
    companion object {
        /** Sentinel meaning "the bands no longer match any preset". */
        const val CUSTOM_PRESET = -1
    }
}

data class ToggleEffectSettings(
    val enabled: Boolean = false,
    /** 0..1000, matching `BassBoost` / `Virtualizer` strength units. */
    val strength: Int = 500
)

data class LoudnessSettings(
    val enabled: Boolean = false,
    /** Target gain in millibels, as `LoudnessEnhancer` expects. */
    val targetGainMdB: Int = 0
)
