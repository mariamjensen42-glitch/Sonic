package com.cycling.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 音频效果相关的偏好（均衡器 + 律动可视化）。
 *
 * 单独一个 DataStore 文件而不是并入 `sonic_settings`：均衡器要整组读写（预设 = 10 个增益一起
 * 变），混进用户设置会让一次拖滑杆触发一整套无关流。`preferencesDataStore` 委托对同一文件名
 * 只允许创建一次，所以这里集中声明、两个仓库共用。
 */
internal val Context.audioEffectsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sonic_audio_effects"
)
