@file:Suppress("unused", "SpellCheckingInspection")

package com.cycling.salt.audiotag.format.mp3

import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString

/**
 * MP3 文件签名及 ID3v2 常量与工具方法。
 */
internal object Mp3Signature {
    /** ID3v2 魔数：`ID3` */
    val ID3_HEADER = ByteString(0x49, 0x44, 0x33)

    /**
     * 检查 [source] 是否以 ID3v2 标签开头。
     */
    fun check(source: Source): Boolean {
        return source.readByteString(3) == ID3_HEADER
    }
}

/**
 * 从 [source] 读取 4 字节 syncsafe 整数。
 *
 * ID3v2.4 使用 syncsafe 编码，每字节仅低 7 位有效，
 * 最高位固定为 0，以此避免在流中误触发同步标记。
 *
 * @see writeSyncsafeInt
 */
internal fun readSyncsafeInt(source: Source): Int {
    var value = 0
    for (i in 0 until 4) {
        value = (value shl 7) or (source.readByte().toInt() and 0x7F)
    }
    return value
}

/**
 * 将 [value] 编码为 4 字节 syncsafe 整数。
 *
 * ID3v2.4 要求帧大小字段使用 syncsafe 编码。
 * 每一位仅使用 7 位，因此 4 字节可表示的最大值为 2²⁸−1。
 *
 * @see readSyncsafeInt
 */
internal fun writeSyncsafeInt(value: Int): ByteArray {
    val bytes = ByteArray(4)
    bytes[3] = (value and 0x7F).toByte()
    bytes[2] = ((value ushr 7) and 0x7F).toByte()
    bytes[1] = ((value ushr 14) and 0x7F).toByte()
    bytes[0] = ((value ushr 21) and 0x7F).toByte()
    return bytes
}

/** ID3v2 标签头大小（字节）。 */
internal const val ID3_HEADER_SIZE: Int = 10

/** ID3v2 帧头大小（字节）：帧 ID(4) + 帧大小(4) + 标志位(2)。 */
internal const val ID3_FRAME_HEADER_SIZE: Int = 10

/**
 * ID3v2 文本编码字节常量。
 *
 * @property ISO_8859_1  Latin-1 编码
 * @property UTF_16      UTF-16 with BOM
 * @property UTF_16BE    UTF-16BE without BOM
 * @property UTF_8       UTF-8
 */
internal object TextEncoding {
    const val ISO_8859_1: Byte = 0x00
    const val UTF_16: Byte = 0x01
    const val UTF_16BE: Byte = 0x02
    const val UTF_8: Byte = 0x03
}

/**
 * ID3v2 标准帧 ID 常量及映射表。
 *
 * 提供两方向映射：
 * - [fromMetadataKey]：内部元数据键 → ID3v2 帧 ID（写标签用）
 * - [toMetadataKey]：ID3v2 帧 ID → 内部元数据键（读标签用）
 */
internal object Id3v2FrameId {
    // ---- 基本字段 ----
    const val TITLE = "TIT2"        // 标题
    const val ARTIST = "TPE1"       // 艺人（主唱）
    const val ALBUM = "TALB"        // 专辑
    const val TRACK_NUMBER = "TRCK" // 音轨号，支持 `N/M` 格式
    const val YEAR = "TYER"         // 年份（ID3v2.3）
    const val DATE = "TDRC"         // 日期（ID3v2.4，可含时间戳）
    const val GENRE = "TCON"        // 流派
    const val COMMENT = "COMM"      // 评论
    const val LYRICS = "USLT"       // 同步/非同步歌词
    const val ALBUM_ARTIST = "TPE2" // 专辑艺人
    const val COMPOSER = "TCOM"     // 作曲
    const val BPM = "TBPM"          // 每分钟节拍数
    const val COPYRIGHT = "TCOP"    // 版权信息
    const val PUBLISHER = "TPUB"    // 发行方
    const val PICTURE = "APIC"      // 专辑封面/图片
    const val USER_TEXT = "TXXX"    // 用户自定义字段

    // ---- 扩展帧 ID ----
    const val GROUPING = "TIT1"               // 内容组描述
    const val SUBTITLE = "TIT3"               // 副标题 / 描述优化
    const val CONTENT_TYPE = "TCAT"           // 播客分类 / 有声读物流派
    const val COMPILATION = "TCMP"            // 是否为合集（iTunes 扩展）
    const val RATING = "POPM"                 // 评分（Popularimeter）
    const val DISC_NUMBER = "TPOS"            // 碟号，支持 `N/M` 格式
    const val ISRC = "TSRC"                   // 国际标准录音码
    const val ENCODED_BY_PERSON = "TENC"      // 编码者（个人/组织）
    const val ENCODED_BY_SOFTWARE = "TSSE"    // 编码软件/硬件设置

    /**
     * 将内部元数据键映射为 ID3v2 帧 ID。
     *
     * @return 对应的帧 ID；若 [key] 无法映射则返回 `null`
     */
    fun fromMetadataKey(key: String): String? = when (key) {
        "TITLE" -> TITLE
        "ARTIST" -> ARTIST
        "ALBUM" -> ALBUM
        "TRACKNUMBER" -> TRACK_NUMBER
        "DATE" -> DATE
        "YEAR" -> YEAR
        "GENRE" -> GENRE
        "COMMENT" -> COMMENT
        "LYRICS" -> LYRICS
        "ALBUMARTIST", "ALBUM_ARTIST" -> ALBUM_ARTIST
        "COMPOSER" -> COMPOSER
        "BPM" -> BPM
        "COPYRIGHT" -> COPYRIGHT
        "PUBLISHER" -> PUBLISHER
        "ENCODED_BY", "ENCODER" -> ENCODED_BY_SOFTWARE
        "GROUPING" -> GROUPING
        "DESCRIPTION" -> SUBTITLE
        "CONTENT_TYPE" -> CONTENT_TYPE
        "COMPILATION" -> COMPILATION
        "RATING" -> RATING
        "SUBTITLE" -> SUBTITLE
        "DISCNUMBER" -> DISC_NUMBER
        "ISRC" -> ISRC
        "ENCODEDBY" -> ENCODED_BY_PERSON
        else -> null
    }

    /**
     * 将 ID3v2 帧 ID 映射为内部元数据键。
     *
     * @return 对应的元数据键；`TXXX`、`APIC` 等特殊帧返回 `null`
     */
    fun toMetadataKey(frameId: String): String? = when (frameId) {
        TITLE -> "TITLE"
        ARTIST -> "ARTIST"
        ALBUM -> "ALBUM"
        TRACK_NUMBER -> "TRACKNUMBER"
        YEAR, DATE -> "DATE"
        GENRE -> "GENRE"
        COMMENT -> "COMMENT"
        LYRICS -> "LYRICS"
        ALBUM_ARTIST -> "ALBUM_ARTIST"
        COMPOSER -> "COMPOSER"
        BPM -> "BPM"
        COPYRIGHT -> "COPYRIGHT"
        PUBLISHER -> "PUBLISHER"
        // ---- 扩展字段 ----
        GROUPING -> "GROUPING"
        SUBTITLE -> "SUBTITLE"
        CONTENT_TYPE -> "CONTENT_TYPE"
        COMPILATION -> "COMPILATION"
        RATING -> "RATING"
        DISC_NUMBER -> "DISCNUMBER"
        ISRC -> "ISRC"
        ENCODED_BY_PERSON -> "ENCODEDBY"
        ENCODED_BY_SOFTWARE -> "ENCODED_BY"
        // ---- 特殊帧：不作为 Metadata 返回 ----
        USER_TEXT -> null
        PICTURE -> null
        else -> null
    }

    /**
     * 判断帧 ID 是否为普通文本帧（`T***` 前缀，排除 `TXXX` 和 `APIC`）。
     */
    fun isTextFrame(frameId: String): Boolean {
        return frameId.startsWith("T") && frameId != PICTURE && frameId != USER_TEXT
    }
}
