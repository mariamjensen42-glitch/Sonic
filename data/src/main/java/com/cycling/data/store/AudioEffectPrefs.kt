package com.cycling.data.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * `sonic_audio_effects` 里的键。
 *
 * 均衡器与耳机校正共用同一个 DataStore，键集中声明 —— 两个仓库都写 `enabled`
 * （应用档案时会顺手打开均衡器），分散在各自的 companion 里迟早写错一处。
 */
internal object AudioEffectPrefs {

    val ENABLED = booleanPreferencesKey("equalizer_enabled")
    val PRESET = stringPreferencesKey("equalizer_preset")
    val GAINS = stringPreferencesKey("equalizer_gains")
    val PREAMP = floatPreferencesKey("equalizer_preamp_db")

    val AUTO_EQ_ID = stringPreferencesKey("autoeq_profile_id")
    val AUTO_EQ_NAME = stringPreferencesKey("autoeq_profile_name")
    val AUTO_EQ_SOURCE = stringPreferencesKey("autoeq_profile_source")
    val AUTO_EQ_PREAMP = floatPreferencesKey("autoeq_preamp_db")
    val AUTO_EQ_FILTERS = stringPreferencesKey("autoeq_filters")
}
