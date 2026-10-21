@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp3

import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.Writer
import com.cycling.salt.audiotag.rw.data.Picture
import com.cycling.salt.audiotag.util.SystemFileSystemUtil
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write

/**
 * MP3 ID3v2 标签写入器。
 *
 * 写入策略：
 * 1. 读取源文件中已有的 ID3v2 标签帧
 * 2. 检测源文件末尾是否残留 ID3v1 标签（128 字节的 `TAG` 魔数）
 * 3. 根据 [WriteOperation] 应用新的元数据，保留封面图与未知帧
 * 4. 将新的 ID3v2.4 标签写入临时文件，后续追加音频数据（跳过旧 ID3v2 和 ID3v1）
 * 5. 原子替换目标文件
 */
@OptIn(UnstableAudioTagApi::class, kotlin.uuid.ExperimentalUuidApi::class)
class Mp3Writer : Writer {

    companion object {
        /** Padding factor: 100% of frame data size. */
        private const val PADDING_FACTOR = 1.0f
    }

    @UnstableAudioTagApi
    override fun write(src: Path, dst: Path, vararg operation: WriteOperation) {
        val tmp = SystemFileSystemUtil.tempFilePath()
        try {
            val fileSize = SystemFileSystem.metadataOrNull(src)?.size ?: 0L

            // ---- 1. 读取已有 ID3v2 标签 ----
            var existingFrames = emptyList<Id3v2Frame>()
            var id3v2TagSize = 0L
            SystemFileSystem.source(src).buffered().use { source ->
                if (source.request(3)) {
                    val magic = source.readByteArray(3).decodeToString()
                    if (magic == "ID3") {
                        source.readByteArray(3) // 跳过版本号 + 标志位
                        id3v2TagSize = ID3_HEADER_SIZE.toLong() + readSyncsafeInt(source)
                        val frameDataSize = (id3v2TagSize - ID3_HEADER_SIZE).toInt()
                        if (frameDataSize > 0) {
                            existingFrames = parseFrames(source.readByteArray(frameDataSize))
                        }
                    }
                }
            }

            // ---- 2. 检测末尾是否残留 ID3v1 标签 ----
            var id3v1Size = 0L
            if (fileSize > 128) {
                SystemFileSystem.source(src).buffered().use { source ->
                    source.skip(fileSize - 128)
                    if (source.request(128)) {
                        val raw = source.readByteArray(128)
                        if (raw.size >= 3 &&
                            raw[0] == 0x54.toByte() &&
                            raw[1] == 0x41.toByte() &&
                            raw[2] == 0x47.toByte() // "TAG"
                        ) {
                            id3v1Size = 128
                        }
                    }
                }
            }

            // ---- 3. 应用写入操作 ----
            val op = operation.find { it is WriteOperation.AllMetadata } as WriteOperation.AllMetadata?
            val newFrames = buildModifiedFrames(existingFrames, op)

            // ---- 4. 写入临时文件：ID3v2 头部 + 帧 + 填充 + 音频数据 ----
            val audioSize = (fileSize - id3v2TagSize - id3v1Size).coerceAtLeast(0)
            SystemFileSystem.sink(tmp).buffered().use { out ->
                // ID3v2.4 标签头（无 unsynchronisation）
                out.write(byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00, 0x00))
                val fb = buildFrameBytes(newFrames)
                out.write(writeSyncsafeInt(fb.size))
                if (fb.isNotEmpty()) out.write(fb)
                // Dynamic padding: 100% of frame data size, minimum 2048 bytes
                val paddingSize = 2048.coerceAtLeast((fb.size * PADDING_FACTOR).toInt())
                out.write(ByteArray(paddingSize))

                // 追加音频数据（跳过旧 ID3v2 和 ID3v1）
                SystemFileSystem.source(src).buffered().use { srcIn ->
                    if (id3v2TagSize > 0) srcIn.skip(id3v2TagSize)
                    out.write(srcIn, audioSize)
                }
            }

            // ---- 5. 原子替换 ----
            if (SystemFileSystem.exists(dst)) SystemFileSystem.delete(dst)
            SystemFileSystem.atomicMove(tmp, dst)
        } finally {
            if (SystemFileSystem.exists(tmp)) SystemFileSystem.delete(tmp)
        }
    }

    /**
     * 根据已有的帧和 [WriteOperation.AllMetadata] 构建新的帧列表。
     *
     * **保留规则：**
     * - `ApicFrame`（封面图）原样保留
     * - `UnknownFrame`（未识别帧）原样保留
     *
     * **替换规则：**
     * - `COMMENT` 和 `LYRICS` 先于 [Id3v2FrameId.fromMetadataKey] 检查，
     *   确保以 [CommentFrame] / [UnsynchronizedLyricsFrame] 格式写入，
     *   避免被路由为 [TextFrame] 导致回读失败
     * - 标准键映射为 [TextFrame]
     * - 无法映射的键以 `TXXX` 用户自定义文本帧写入
     */
    private fun buildModifiedFrames(
        existing: List<Id3v2Frame>,
        op: WriteOperation.AllMetadata?
    ): List<Id3v2Frame> {
        if (op == null) return existing
        val out = mutableListOf<Id3v2Frame>()

        // Keep unknown frames, but skip old APIC frames — they get replaced from op.pictures
        for (f in existing) {
            if (f is Id3v2Frame.UnknownFrame) out.add(f)
        }

        // Add pictures from operation (replaces all old APIC frames)
        val pics = op.pictures
        if (pics != null) {
            for (pic in pics) {
                val d = buildApicFrameData(pic)
                out.add(Id3v2Frame.ApicFrame(
                    frameId = "APIC",
                    size = d.size,
                    rawData = d,
                    encoding = TextEncoding.UTF_8,
                    mimeType = pic.mediaType,
                    pictureType = pic.pictureType,
                    description = pic.description,
                    pictureData = pic.pictureData,
                ))
            }
        }

        for (m in op.metadatas) {
            when (m.key) {
                "COMMENT" -> {
                    val d = buildCommentFrameData(m.value)
                    out.add(Id3v2Frame.CommentFrame("COMM", d.size, d, TextEncoding.UTF_8, "eng", m.value))
                }
                "LYRICS" -> {
                    val d = buildLyricsFrameData(m.value)
                    out.add(Id3v2Frame.UnsynchronizedLyricsFrame("USLT", d.size, d, TextEncoding.UTF_8, "eng", m.value))
                }
                else -> {
                    val fid = Id3v2FrameId.fromMetadataKey(m.key)
                    if (fid != null) {
                        val d = buildTextFrameData(m.value)
                        out.add(Id3v2Frame.TextFrame(fid, d.size, d, TextEncoding.UTF_8, m.value))
                    } else {
                        val d = buildUserTextFrameData(m.key, m.value)
                        out.add(Id3v2Frame.UserTextFrame("TXXX", d.size, d, TextEncoding.UTF_8, m.key, m.value))
                    }
                }
            }
        }
        return out
    }

    /**
     * 构建原始帧字节数组。
     *
     * **双遍策略：**
     * 第一遍计算各帧载荷大小总和，预分配精确大小的 [ByteArray]。
     * 第二遍直接写入，避免 [MutableList] 装箱开销。
     *
     * 帧大小字段使用 ID3v2.4 规范的 **syncsafe** 编码。
     */
    private fun buildFrameBytes(frames: List<Id3v2Frame>): ByteArray {
        // 第一遍：计算总大小
        var total = 0
        val datas = arrayOfNulls<ByteArray>(frames.size)
        for ((i, f) in frames.withIndex()) {
            val d = when (f) {
                is Id3v2Frame.TextFrame -> buildTextFrameData(f.value)
                is Id3v2Frame.ApicFrame -> buildApicFrameData(
                    com.cycling.salt.audiotag.rw.data.Picture(
                        f.pictureType, f.mimeType, f.description,
                        0, 0, 0, 0, f.pictureData, -1, f.pictureData.size
                    )
                )
                is Id3v2Frame.CommentFrame -> buildCommentFrameData(f.content)
                is Id3v2Frame.UnsynchronizedLyricsFrame -> buildLyricsFrameData(f.content)
                is Id3v2Frame.UserTextFrame -> buildUserTextFrameData(f.description, f.value)
                is Id3v2Frame.UnknownFrame -> f.rawData
            }
            total += ID3_FRAME_HEADER_SIZE // 帧头：ID(4) + 大小(4) + 标志(2)
            total += d.size
            datas[i] = d
        }

        // 第二遍：写入
        val buf = ByteArray(total)
        var pos = 0
        for ((i, f) in frames.withIndex()) {
            val d = datas[i]!!
            f.frameId.encodeToByteArray().copyInto(buf, pos); pos += 4
            val sz = d.size
            // syncsafe 编码（ID3v2.4）
            buf[pos] = ((sz ushr 21) and 0x7F).toByte()
            buf[pos + 1] = ((sz ushr 14) and 0x7F).toByte()
            buf[pos + 2] = ((sz ushr 7) and 0x7F).toByte()
            buf[pos + 3] = (sz and 0x7F).toByte(); pos += 4
            pos += 2 // 标志位（均置 0）
            d.copyInto(buf, pos); pos += d.size
        }
        return buf
    }
}
