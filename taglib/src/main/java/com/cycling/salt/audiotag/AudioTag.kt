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

package com.cycling.salt.audiotag

import com.cycling.salt.audiotag.format.cda.CdaReader
import com.cycling.salt.audiotag.format.flac.FlacReader
import com.cycling.salt.audiotag.format.flac.FlacWriter
import com.cycling.salt.audiotag.format.mp3.Mp3Reader
import com.cycling.salt.audiotag.format.mp3.Mp3Writer
import com.cycling.salt.audiotag.format.mp4.Mp4Reader
import com.cycling.salt.audiotag.format.mp4.Mp4Writer
import com.cycling.salt.audiotag.format.ogg.OggOpusReader
import com.cycling.salt.audiotag.format.ogg.OggVorbisReader
import com.cycling.salt.audiotag.format.ogg.OggVorbisWriter
import com.cycling.salt.audiotag.rw.ReadStrategy
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.data.AudioTagData
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Salt Audio Tag
 *
 * @author Moriafly
 */
object AudioTag {

    /**
     * 从 [source] 读取音频标签，自动检测文件格式。
     *
     * 通过读取前 4 字节魔数自动判定格式，然后委托给对应的格式读取器。
     * 已消费的魔数字节会通过 [Buffer] 归还，对下游读取器透明。
     *
     * **支持的魔数：**
     * - `ID3`（0x49 0x44 0x33）→ MP3
     * - `fLaC`（0x66 0x4C 0x61 0x43）→ FLAC
     * - `RIFF`（0x52 0x49 0x46 0x46）→ CDA
     * - MPEG sync（0xFF 0xEx）→ MP3（无 ID3 标签的裸 MPEG 流）
     * - `ftyp` 容器盒（偏移 4 字节处检测）→ MP4/M4A
     *
     * 示例：
     * ```kotlin
     * source.use {
     *     val result = AudioTag.read(source = it, strategy = ReadStrategy.All)
     * }
     * ```
     *
     * @param source 音频文件字节流
     * @param strategy 读取策略
     * @return [Result]<[AudioTag]>
     */
    @UnstableAudioTagApi
    fun read(
        source: Source,
        strategy: ReadStrategy
    ): Result<AudioTagData> = runCatching {
        val (format, bufferedSource) = sniffFormat(source)
        when (format) {
            "mp3" -> Mp3Reader().read(bufferedSource, strategy)
            "flac" -> FlacReader().read(bufferedSource, strategy)
            "mp4" -> Mp4Reader().read(bufferedSource, strategy)
            "cda" -> CdaReader().read(bufferedSource, strategy)
            "ogg" -> {
                // Auto-detect codec: check first packet via lightweight probe
                val reader = OggVorbisReader()
                reader.read(bufferedSource, strategy)
            }
            else -> throw UnsupportedOperationException("无法识别的音频格式")
        }
    }

    /**
     * 从 [source] 读取音频标签，按指定的 [extension] 选择格式读取器。
     *
     * 示例：
     * ```kotlin
     * source.use {
     *     val result = AudioTag.read(
     *         source = it,
     *         extension = "flac",
     *         strategy = ReadStrategy.All
     *     )
     * }
     * ```
     *
     * @param source 音频文件字节流
     * @param extension 文件扩展名，大小写不敏感
     * @param strategy 读取策略
     * @return [Result]<[AudioTag]>
     */
    @UnstableAudioTagApi
    fun read(
        source: Source,
        extension: String,
        strategy: ReadStrategy
    ): Result<AudioTagData> = runCatching {
        when (extension.lowercase()) {
            "flac" -> FlacReader().read(source, strategy)
            "mp3" -> Mp3Reader().read(source, strategy)
            "mp4", "m4a", "aac" -> Mp4Reader().read(source, strategy)
            "cda" -> CdaReader().read(source, strategy)
            "ogg", "oga" -> OggVorbisReader().read(source, strategy)
            "opus" -> OggOpusReader().read(source, strategy)
            else -> throw UnsupportedOperationException("不支持的文件扩展名 $extension")
        }
    }

    /**
     * 从文件路径读取音频标签，自动检测格式。
     *
     * @param path [SystemFileSystem] 中的文件路径
     * @param strategy 读取策略
     * @return [Result]<[AudioTag]>
     */
    @UnstableAudioTagApi
    fun read(
        path: Path,
        strategy: ReadStrategy
    ): Result<AudioTagData> = runCatching {
        SystemFileSystem.source(path).buffered().use { source ->
            read(source, strategy).getOrThrow()
        }
    }

    /**
     * 从文件路径读取音频标签。
     *
     * @param path [SystemFileSystem] 中的文件路径
     * @param extension 文件扩展名
     * @param strategy 读取策略
     * @return [Result]<[AudioTag]>
     */
    @UnstableAudioTagApi
    fun read(
        path: Path,
        extension: String,
        strategy: ReadStrategy
    ): Result<AudioTagData> = runCatching {
        SystemFileSystem.source(path).buffered().use { source ->
            read(source, extension, strategy).getOrThrow()
        }
    }

    /**
     * 写入音频标签。
     *
     * @param src 源文件路径
     * @param dst 目标文件路径
     * @param extension 文件扩展名，如 `"flac"`、`"mp3"`
     * @param operation 写入操作
     * @return [Result]<[Unit]>
     */
    @UnstableAudioTagApi
    fun write(
        src: Path,
        dst: Path,
        extension: String,
        vararg operation: WriteOperation
    ): Result<Unit> = runCatching {
        when (extension) {
            "flac" -> FlacWriter().write(src, dst, *operation)
            "mp3" -> Mp3Writer().write(src, dst, *operation)
            "mp4", "m4a", "aac" -> Mp4Writer().write(src, dst, *operation)
            "ogg", "oga" -> OggVorbisWriter().write(src, dst, *operation)
            else -> throw UnsupportedOperationException("不支持的文件扩展名 $extension")
        }
    }

    // ------------------------------------------------------------------
    // 格式嗅探
    // ------------------------------------------------------------------

    /**
     * 从 [source] 的魔数字节判定音频格式。
     *
     * **策略：**
     * 1. 从 [source] 读取前 N 个字节（取决于格式最长魔数）
     * 2. 将已读取的字节写入 [Buffer]，同时把 [source] 剩余数据追加到同一个 [Buffer]
     * 3. 返回的字符串既是格式标识，也是一个可被 Reader 消费的完整数据源
     *
     * **注意：调用方必须用返回值作为 [Source] 传给 Reader，不能用原始的 [source]！**
     */
    private fun sniffFormat(source: Source): Pair<String, Source> {
        if (!source.request(8)) {
            // 数据不足 8 字节时无法安全判断格式
            return "" to source
        }

        // 尝试读取 12 字节：4 字节魔数 + 4 字节 ISOBMFF box 大小 + 4 字节 box 类型
        val head = source.readByteArray(12)
        val buf = Buffer().apply {
            write(head)
            transferFrom(source) // 追加剩余数据
        }

        val bufferedSource: Source = (buf as RawSource).buffered()
        return detectFormat(head) to bufferedSource
    }

    /** 从已读取的头部字节判定格式。 */
    private fun detectFormat(head: ByteArray): String {
        if (head.size < 4) return ""

        val b0 = head[0].toInt() and 0xFF
        val b1 = head[1].toInt() and 0xFF

        return when {
            // ID3v2 魔数
            b0 == 0x49 && b1 == 0x44 -> "mp3"

            // FLAC 魔数：fLaC
            b0 == 0x66 && b1 == 0x4C && (head[2].toInt() and 0xFF) == 0x61 && (head[3].toInt() and 0xFF) == 0x43 -> "flac"

            // OGG capture pattern: OggS
            b0 == 0x4F && b1 == 0x67 && (head[2].toInt() and 0xFF) == 0x67 && (head[3].toInt() and 0xFF) == 0x53 -> "ogg"

            // RIFF（CDA）
            b0 == 0x52 && b1 == 0x49 -> "cda"

            // MPEG 同步字（裸 MPEG 流，无 ID3 标签）
            b0 == 0xFF && (b1 and 0xE0) == 0xE0 -> "mp3"

            // ISOBMFF（MP4/M4A）：ftyp 盒在偏移 4 处
            // 格式：前 4 字节 = box 大小，后 4 字节 = "ftyp"
            head.size >= 8 && head[4] == 0x66.toByte() && head[5] == 0x74.toByte() && head[6] == 0x79.toByte() && head[7] == 0x70.toByte() -> "mp4"

            else -> ""
        }
    }
}

