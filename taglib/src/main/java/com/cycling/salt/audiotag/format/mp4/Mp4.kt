@file:Suppress("SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp4

import com.cycling.salt.audiotag.rw.data.Metadata
import com.cycling.salt.audiotag.rw.data.Picture

/**
 * MP4/M4A ISOBMFF 容器常量与工具方法。
 *
 * ## 盒子格式
 *
 * ISOBMFF 使用统一的盒子结构：
 * ```
 * [4 字节大端序大小] [4 字节类型] [数据...]
 * ```
 * 若大小为 1，则紧随其后有 8 字节扩展大小字段。
 *
 * ## iTunes 元数据原子命名
 *
 * 多数 Apple 定义的元数据原子名以 `0xA9`（`©`）作为首字节，
 * 例如 `©nam`、`©ART`、`©alb`。解码时去掉首字节的 `0xA9` 前缀，
 * 仅用后 3 字节的 ASCII 字符串做匹配。
 */
internal object Mp4Constants {
    // ---- 容器盒子类型 ----
    const val ATOM_MOOV = "moov"
    const val ATOM_MVHD = "mvhd"
    const val ATOM_UDTA = "udta"
    const val ATOM_META = "meta"
    const val ATOM_ILST = "ilst"
    const val ATOM_DATA = "data"

    /**
     * 将 iTunes 元数据原子的 4 字节编码 [atomBytes] 映射为内部元数据键。
     *
     * 典型用法：
     * ```kotlin
     * val key = Mp4Constants.atomToMetadataKey(atomBytes) // [0xA9,0x6E,0x61,0x6D] → "TITLE"
     * ```
     *
     * @return 对应的元数据键；无法映射时返回 `null`
     */
    fun atomToMetadataKey(atomBytes: ByteArray): String? {
        if (atomBytes.size < 4) return null
        val name = decodeAtomName(atomBytes)
        return METADATA_NAMES[name]
    }

    /**
     * 反向映射：内部元数据键 → iTunes 原子名（不含 `0xA9` 前缀）。
     *
     * 用于将来扩展 MP4 标签写入功能。
     *
     * @return 对应的 3 字节原子名；无法映射时返回 `null`
     */
    fun metadataKeyToAtomName(key: String): String? = METADATA_TO_ATOM[key.uppercase()]

    /**
     * 解码 4 字节原子编码为规范字符串。
     *
     * 按 Apple 规范，首字节若为 `0xA9`（`©`）则省略，
     * 仅保留后 3 字节作为 ASCII 字符串。
     */
    private fun decodeAtomName(b: ByteArray): String {
        val start = if (b.size >= 4 && (b[0].toInt() and 0xFF) == 0xA9) 1 else 0
        val len = 4 - start
        return buildString {
            for (i in 0 until len) {
                val ch = b[start + i].toInt() and 0xFF
                if (ch in 0x20..0x7E) append(ch.toChar())
            }
        }
    }

    /**
     * 原子名 → 元数据键 映射表。
     *
     * 以去掉 `0xA9` 前缀后的 3 字节 ASCII 字符串为键。
     * 覆盖 Apple QuickTime 元数据规范中定义的常用字段。
     */
    private val METADATA_NAMES: Map<String, String> = mapOf(
        // ---- 基本信息 ----
        "nam"  to "TITLE",          // ©nam  标题
        "ART"  to "ARTIST",         // ©ART  艺人
        "alb"  to "ALBUM",          // ©alb  专辑
        "aART" to "ALBUM_ARTIST",   // aART  专辑艺人
        "day"  to "DATE",           // ©day  日期
        "gen"  to "GENRE",          // ©gen  自定义流派
        "gnre" to "GENRE",          // gnre  传统数字流派码

        // ---- 音轨 / 碟号 ----
        "trkn" to "TRACKNUMBER",    // trkn  音轨号（含总数）
        "disk" to "DISCID",         // disk  碟号（含总数）

        // ---- 演职人员 ----
        "wrt"  to "COMPOSER",       // ©wrt  作曲
        "grp"  to "GROUPING",       // ©grp  分组
        "cmt"  to "COMMENT",        // ©cmt  评论
        "lyr"  to "LYRICS",         // ©lyr  歌词

        // ---- 技术 ----
        "too"  to "ENCODED_BY",     // ©too  编码工具
        "tmpo" to "BPM",            // tmpo  每分钟节拍数
        "cpil" to "COMPILATION",    // cpil  合集标记（iTunes）
        "cprt" to "COPYRIGHT",      // cprt  版权信息

        // ---- 描述 ----
        "desc" to "DESCRIPTION",    // ©desc 描述
        "stik" to "CONTENT_TYPE",   // stik  iTunes 媒体类别

        // ---- 乐章 ----
        "mvn"  to "MOVEMENT",       // ©mvn  乐章名
        "mvi"  to "MOVEMENT_TOTAL", // ©mvi  乐章总数
        "mvc"  to "MOVEMENT_NAME",  // ©mvc  乐章编号

        // ---- 播客 / 有声读物 ----
        "tvsh" to "TV_SHOW",        // tvsh  剧集/播客名
        "tven" to "TV_EPISODE_ID",  // tven  剧集 ID
        "tves" to "TV_EPISODE",     // tves  剧集编号
        "tvsn" to "TV_SEASON",      // tvsn  季编号
        "sdes" to "SUBTITLE",       // sdes  副标题

        // ---- 排序字段 ----
        "sonm" to "SORT_TITLE",           // sonm  按标题排序
        "soal" to "SORT_ALBUM",           // soal  按专辑排序
        "soar" to "SORT_ARTIST",          // soar  按艺人排序
        "soaa" to "SORT_ALBUM_ARTIST",    // soaa  按专辑艺人排序
        "soco" to "SORT_COMPOSER",        // soco  按作曲排序

        // ---- 商店 / 购买信息 ----
        "apID" to "APPLE_ID",       // apID  购买者 Apple ID
        "atID" to "ARTIST_ID",      // atID  艺人 ID
        "cnID" to "CATALOG_ID",     // cnID  iTunes 编录 ID
        "sfID" to "STORE_ID",       // sfID  商店国家代码
        "xid " to "XID",            // xid   商家自定义 ID

        // ---- 评分 ----
        "rtng" to "RATING",         // rtng  分级（如 explicit / clean）

        // ---- 其他 ----
        "purd" to "PURCHASE_DATE",  // purd  购买日期
        "pgap" to "GAPLESS",        // pgap  无缝播放标记
        "akID" to "ACCOUNT_TYPE",   // akID  账户类型
        "geID" to "GENRE_ID",       // geID  流派 ID
        "plID" to "PLAYLIST_ID",    // plID  播放列表 ID
    )

    /**
     * 元数据键 → 原子名 反向映射表。
     *
     * 不含 `0xA9` 前缀，写入时由调用方根据目标格式决定是否补回前缀。
     * 部分键（如 `DISCNUMBER`）在 MP4 下与 `DISCID` 共用同一原子。
     */
    private val METADATA_TO_ATOM: Map<String, String> = buildMap {
        put("TITLE", "nam")
        put("ARTIST", "ART")
        put("ALBUM", "alb")
        put("ALBUM_ARTIST", "aART")
        put("DATE", "day")
        put("GENRE", "gen")
        put("TRACKNUMBER", "trkn")
        put("DISCID", "disk")
        put("DISCNUMBER", "disk") // MP4 无独立碟号原子
        put("COMPOSER", "wrt")
        put("GROUPING", "grp")
        put("COMMENT", "cmt")
        put("LYRICS", "lyr")
        put("ENCODED_BY", "too")
        put("ENCODEDBY", "too")
        put("BPM", "tmpo")
        put("COMPILATION", "cpil")
        put("COPYRIGHT", "cprt")
        put("DESCRIPTION", "desc")
        put("CONTENT_TYPE", "stik")
        put("MOVEMENT", "mvn")
        put("MOVEMENT_TOTAL", "mvi")
        put("MOVEMENT_NAME", "mvc")
        put("TV_SHOW", "tvsh")
        put("TV_EPISODE_ID", "tven")
        put("TV_EPISODE", "tves")
        put("TV_SEASON", "tvsn")
        put("SUBTITLE", "sdes")
        put("SORT_TITLE", "sonm")
        put("SORT_ALBUM", "soal")
        put("SORT_ARTIST", "soar")
        put("SORT_ALBUM_ARTIST", "soaa")
        put("SORT_COMPOSER", "soco")
        put("RATING", "rtng")
        put("PURCHASE_DATE", "purd")
        // ISRC 在 MP4 下无标准原子；跳过
    }

    // ---------------------------------------------------------------------------
    // Atom building utilities (for Mp4Writer)
    // ---------------------------------------------------------------------------

    /**
     * Encode a Metadata key to a 4-byte ISOBMFF atom name.
     *
     * If the atom name is 3 bytes (e.g. "nam", "ART"), prepends 0xA9 (©).
     * If the atom name is 4 bytes (e.g. "trkn", "cpil"), uses as-is.
     *
     * @return 4-byte atom name ByteArray
     */
    fun encodeAtomName(metadataKey: String): ByteArray {
        val baseName = METADATA_TO_ATOM[metadataKey.uppercase()] ?: return byteArrayOf()
        val bytes = if (baseName.length == 3 && baseName.all { it in ' '..'~' }) {
            byteArrayOf(0xA9.toByte()) + baseName.encodeToByteArray()
        } else {
            baseName.encodeToByteArray()
        }
        // Pad or truncate to exactly 4 bytes
        return if (bytes.size >= 4) bytes.copyOf(4) else bytes + ByteArray(4 - bytes.size)
    }

    /**
     * Build a complete ilst atom containing metadata items and cover art.
     *
     * @param metadatas text metadata entries
     * @param pictures cover art images to include (optional)
     * @return the full ilst box
     */
    fun buildIlstAtom(metadatas: List<Metadata>, pictures: List<Picture>? = null): ByteArray {
        val childAtoms = mutableListOf<ByteArray>()

        for (meta in metadatas) {
            val atomName = encodeAtomName(meta.key)
            if (atomName.isEmpty()) continue

            val key = meta.key.trim().uppercase()
            val dataPayload = when (key) {
                // trkn / disk 是二进制原子（[0][0][index BE16][total BE16]…），写成 UTF-8 文本
                // 只有本库自己的 reader 能读，iTunes/Android 会当未知数据丢掉。
                "TRACKNUMBER" -> buildDataAtom(buildOrdinalPayload(meta.value, size = 8), typeIndex = 0)
                "DISCNUMBER", "DISCID" -> buildDataAtom(buildOrdinalPayload(meta.value, size = 6), typeIndex = 0)
                else -> buildDataAtom(meta.value.encodeToByteArray(), typeIndex = 1)
            }
            val atomSize = 8 + dataPayload.size

            val atomBuf = mutableListOf<Byte>()
            writeU32BE(atomBuf, atomSize)
            atomBuf.addAll(atomName.toList())
            atomBuf.addAll(dataPayload.toList())
            childAtoms.add(atomBuf.toByteArray())
        }

        // Add cover art atoms
        if (pictures != null) {
            for (pic in pictures) {
                val covrAtom = buildCovrAtom(pic)
                childAtoms.add(covrAtom)
            }
        }

        val totalContent = childAtoms.fold(0) { acc, arr -> acc + arr.size }
        val result = mutableListOf<Byte>()
        writeU32BE(result, 8 + totalContent)
        result.addAll("ilst".encodeToByteArray().toList())
        for (child in childAtoms) result.addAll(child.toList())
        return result.toByteArray()
    }

    /**
     * Build a single "covr" atom for one picture.
     *
     * Structure:
     *   [covrAtomSize(4 BE)]["covr"(4)][data sub-atom]
     *
     * The data sub-atom uses typeIndex 13 for JPEG or 14 for PNG.
     */
    fun buildCovrAtom(picture: Picture): ByteArray {
        val picData = picture.pictureData
        val mime = picture.mediaType.lowercase()
        val typeIndex = when {
            mime.contains("jpeg") || mime.contains("jpg") -> 13
            mime.contains("png") -> 14
            else -> 13 // default to JPEG
        }
        val dataPayload = buildDataAtom(picData, typeIndex = typeIndex)
        val atomSize = 8 + dataPayload.size
        val atomBuf = mutableListOf<Byte>()
        writeU32BE(atomBuf, atomSize)
        atomBuf.addAll("covr".encodeToByteArray().toList())
        atomBuf.addAll(dataPayload.toList())
        return atomBuf.toByteArray()
    }

    /**
     * iTunes `trkn` (8 bytes) / `disk` (6 bytes) 的二进制载荷：`[0][0][index BE16][total BE16]…`。
     * `3/12` 或 `3` 都能编码；解析不了的数字按 0 处理。
     */
    private fun buildOrdinalPayload(value: String, size: Int): ByteArray {
        val parts = value.split('/', limit = 2)
        val index = parts.getOrNull(0)?.trim()?.toIntOrNull()?.coerceIn(0, 0xFFFF) ?: 0
        val total = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(0, 0xFFFF) ?: 0
        val payload = ByteArray(size)
        payload[2] = (index ushr 8).toByte()
        payload[3] = index.toByte()
        if (size >= 6) {
            payload[4] = (total ushr 8).toByte()
            payload[5] = total.toByte()
        }
        return payload
    }

    /**
     * Build a "data" sub-atom containing the value bytes with the specified typeIndex.
     *
     * Structure:
     *   [4-byte BE total size][4-byte "data"][4-byte BE typeIndex][4-byte BE locale (0)][value bytes]
     *
     * Common typeIndex values:
     *   0  = implicit (UTF-8 text)
     *   1  = UTF-8 text
     *   13 = JPEG cover art
     *   14 = PNG cover art
     *   21 = big-endian signed integer
     */
    private fun buildDataAtom(valueBytes: ByteArray, typeIndex: Int = 1): ByteArray {
        val totalSize = 16 + valueBytes.size
        val buf = mutableListOf<Byte>()
        writeU32BE(buf, totalSize)
        buf.addAll("data".encodeToByteArray().toList())
        writeU32BE(buf, typeIndex)
        writeU32BE(buf, 0) // locale = 0
        buf.addAll(valueBytes.toList())
        return buf.toByteArray()
    }

    /**
     * Build a generic ISOBMFF box.
     *   [4-byte BE size][4-byte type][content]
     */
    fun buildBox(type: String, content: ByteArray): ByteArray {
        val buf = mutableListOf<Byte>()
        writeU32BE(buf, 8 + content.size)
        buf.addAll(type.encodeToByteArray().toList())
        buf.addAll(content.toList())
        return buf.toByteArray()
    }

    /** Write a 32-bit big-endian unsigned integer to the buffer. */
    private fun writeU32BE(buf: MutableList<Byte>, value: Int) {
        buf.add((value ushr 24 and 0xFF).toByte())
        buf.add((value ushr 16 and 0xFF).toByte())
        buf.add((value ushr 8 and 0xFF).toByte())
        buf.add((value and 0xFF).toByte())
    }
}

// ---------------------------------------------------------------------------
// ByteArray 大端序读取扩展
// ---------------------------------------------------------------------------

/**
 * 从 [off] 偏移处读取 4 字节大端序无符号 32 位整数。
 */
internal fun ByteArray.readU32(off: Int): Long =
    ((this[off].toLong() and 0xFF) shl 24) or
    ((this[off + 1].toLong() and 0xFF) shl 16) or
    ((this[off + 2].toLong() and 0xFF) shl 8) or
    (this[off + 3].toLong() and 0xFF)

/**
 * 从 [off] 偏移处读取 4 字节大端序有符号 32 位整数。
 */
internal fun ByteArray.readS32(off: Int): Int =
    ((this[off].toInt() and 0xFF) shl 24) or
    ((this[off + 1].toInt() and 0xFF) shl 16) or
    ((this[off + 2].toInt() and 0xFF) shl 8) or
    (this[off + 3].toInt() and 0xFF)

/**
 * 从 [off] 偏移处读取 2 字节大端序无符号 16 位整数。
 */
internal fun ByteArray.readU16(off: Int): Int =
    ((this[off].toInt() and 0xFF) shl 8) or (this[off + 1].toInt() and 0xFF)
