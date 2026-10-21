/*
 * Salt Audio Tag
 * Copyright (C) 2025 Moriafly
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

@file:Suppress("unused")

package com.cycling.salt.audiotag

import android.content.Context
import android.net.Uri
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.data.AudioTagData
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * 从 Android [uri] 读取音频标签，自动检测格式。
 *
 * 适用于从系统文件选择器或 MediaStore 获取的 `content://` URI，
 * 无需手动传入扩展名。
 */
@UnstableAudioTagApi
fun AudioTag.read(
    context: Context,
    uri: Uri,
    strategy: ReadStrategy
): Result<AudioTagData> = runCatching {
    val inputStream = context.contentResolver.openInputStream(uri)

    require(inputStream != null) {
        "Failed to open input stream form $uri"
    }

    inputStream.asSource().buffered().use { source ->
        com.cycling.salt.audiotag.AudioTag.read(source, strategy).getOrThrow()
    }
}

/**
 * 从 Android [uri] 读取音频标签。
 *
 * @param extension 文件扩展名，大小写不敏感；若为空白则自动从魔数检测
 */
@UnstableAudioTagApi
fun AudioTag.read(
    context: Context,
    uri: Uri,
    extension: String,
    strategy: ReadStrategy
): Result<AudioTagData> = runCatching {
    val inputStream = context.contentResolver.openInputStream(uri)

    require(inputStream != null) {
        "Failed to open input stream form $uri"
    }

    inputStream.asSource().buffered().use { source ->
        if (extension.isBlank()) {
            com.cycling.salt.audiotag.AudioTag.read(source, strategy).getOrThrow()
        } else {
            com.cycling.salt.audiotag.AudioTag.read(source, extension, strategy).getOrThrow()
        }
    }
}
