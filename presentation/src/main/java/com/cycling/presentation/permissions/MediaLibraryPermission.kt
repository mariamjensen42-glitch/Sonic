package com.cycling.presentation.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 读取设备上的音频文件所需要的运行时权限。
 *
 * Android 13 起媒体权限按类型细分，只申请音频那一个；更早的版本仍然只有整块的外部存储权限。
 * 权限名和"是否已授予"的判断放在一起，是因为这两个地方最容易各写一份然后走偏 —— 申请了
 * `READ_MEDIA_AUDIO` 却检查 `READ_EXTERNAL_STORAGE`，界面上就永远显示成未授权。
 */
object MediaLibraryPermission {

    val names: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun isGranted(context: Context): Boolean = names.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
