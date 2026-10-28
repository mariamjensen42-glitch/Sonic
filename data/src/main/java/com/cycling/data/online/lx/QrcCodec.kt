package com.cycling.data.online.lx

import java.util.Base64
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * QQ 音乐 QRC（EQRC）逐字歌词的解码器。
 *
 * 算法不是标准 3DES：QQ 音乐自己改过 DES 的字节序与 S 盒，社区称之为「buggy DES」。
 * 这里按 `amll-dev/applemusic-like-lyrics` 的 `formats/eqrc`（MIT，迁自
 * `apoint123/qrc-decoder`）1:1 移植 —— 常量、密钥扩展、位表全部照抄，不做「优化」，
 * 因为任何一处「修正」都会让结果解不出来。
 *
 * 明文结构：`zlib 流(UTF-8 XML，可能带 BOM)`；密文以 8 字节分组，
 * 解密次序 D(K3) → E(K2) → D(K1)，填充是零填充。
 */
object QrcCodec {

    private const val DES_BLOCK_SIZE = 8

    private val KEY_1 = "!@#)(*$%".toByteArray(Charsets.UTF_8)
    private val KEY_2 = "123ZXC!@".toByteArray(Charsets.UTF_8)
    private val KEY_3 = "!@#)(NHL".toByteArray(Charsets.UTF_8)

    private val SBOX1 = intArrayOf(
        14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7, 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12,
        11, 9, 5, 3, 8, 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0, 15, 12, 8, 2, 4, 9,
        1, 7, 5, 11, 3, 14, 10, 0, 6, 13,
    )

    private val SBOX2 = intArrayOf(
        15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10, 3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1,
        10, 6, 9, 11, 5, 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15, 13, 8, 10, 1, 3, 15,
        4, 2, 11, 6, 7, 12, 0, 5, 14, 9,
    )

    private val SBOX3 = intArrayOf(
        10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8, 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5,
        14, 12, 11, 15, 1, 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7, 1, 10, 13, 0, 6,
        9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12,
    )

    private val SBOX4 = intArrayOf(
        7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15, 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2,
        12, 1, 10, 14, 9, 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4, 3, 15, 0, 6, 10,
        10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14,
    )

    private val SBOX5 = intArrayOf(
        2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9, 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15,
        10, 3, 9, 8, 6, 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14, 11, 8, 12, 7, 1, 14,
        2, 13, 6, 15, 0, 9, 10, 4, 5, 3,
    )

    private val SBOX6 = intArrayOf(
        12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11, 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13,
        14, 0, 11, 3, 8, 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6, 4, 3, 2, 12, 9, 5,
        15, 10, 11, 14, 1, 7, 6, 0, 8, 13,
    )

    private val SBOX7 = intArrayOf(
        4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1, 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5,
        12, 2, 15, 8, 6, 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2, 6, 11, 13, 8, 1, 4,
        10, 7, 9, 5, 0, 15, 14, 2, 3, 12,
    )

    private val SBOX8 = intArrayOf(
        13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7, 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6,
        11, 0, 14, 9, 2, 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8, 2, 1, 14, 7, 4, 10,
        8, 13, 15, 12, 9, 0, 3, 5, 6, 11,
    )

    private val P_BOX = intArrayOf(
        16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31, 10, 2, 8, 24, 14, 32, 27, 3, 9,
        19, 13, 30, 6, 22, 11, 4, 25,
    )

    private val E_BOX_TABLE = intArrayOf(
        32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 8, 9, 10, 11, 12, 13, 12, 13, 14, 15, 16, 17, 16, 17,
        18, 19, 20, 21, 20, 21, 22, 23, 24, 25, 24, 25, 26, 27, 28, 29, 28, 29, 30, 31, 32, 1,
    )

    private val KEY_RND_SHIFT = intArrayOf(
        1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1,
    )

    private val KEY_PERM_C = intArrayOf(
        56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2,
        59, 51, 43, 35,
    )

    private val KEY_PERM_D = intArrayOf(
        62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12,
        4, 27, 19, 11, 3,
    )

    private val KEY_COMPRESSION = intArrayOf(
        13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1, 40,
        51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31,
    )

    private val IP_RULE = intArrayOf(
        34, 42, 50, 58, 2, 10, 18, 26, 36, 44, 52, 60, 4, 12, 20, 28, 38, 46, 54, 62, 6, 14, 22,
        30, 40, 48, 56, 64, 8, 16, 24, 32, 33, 41, 49, 57, 1, 9, 17, 25, 35, 43, 51, 59, 3, 11, 19,
        27, 37, 45, 53, 61, 5, 13, 21, 29, 39, 47, 55, 63, 7, 15, 23, 31,
    )

    private val INV_IP_RULE = intArrayOf(
        37, 5, 45, 13, 53, 21, 61, 29, 38, 6, 46, 14, 54, 22, 62, 30, 39, 7, 47, 15, 55, 23, 63,
        31, 40, 8, 48, 16, 56, 24, 64, 32, 33, 1, 41, 9, 49, 17, 57, 25, 34, 2, 42, 10, 50, 18, 58,
        26, 35, 3, 43, 11, 51, 19, 59, 27, 36, 4, 44, 12, 52, 20, 60, 28,
    )

    private val S_BOXES = arrayOf(SBOX1, SBOX2, SBOX3, SBOX4, SBOX5, SBOX6, SBOX7, SBOX8)

    private val IP_LEFT_TABLE = IntArray(2048)
    private val IP_RIGHT_TABLE = IntArray(2048)
    private val INV_IP_LEFT_TABLE = IntArray(2048)
    private val INV_IP_RIGHT_TABLE = IntArray(2048)
    private val SP_TABLE = IntArray(512)
    private val EBOX_HIGH_TABLE = IntArray(1024)
    private val EBOX_LOW_TABLE = IntArray(1024)

    private val DECRYPT_SCHEDULES = arrayOf(
        keySchedule(KEY_3, decrypt = true),
        keySchedule(KEY_2, decrypt = false),
        keySchedule(KEY_1, decrypt = true)
    )

    private val ENCRYPT_SCHEDULES = arrayOf(
        keySchedule(KEY_1, decrypt = false),
        keySchedule(KEY_2, decrypt = true),
        keySchedule(KEY_3, decrypt = false)
    )

    init {
        buildPermutationTables()
        buildSpTables()
        buildEBoxTables()
    }

    // region 对外

    /** 密文（hex 或 base64）→ QRC 明文（XML）。 */
    fun decrypt(source: String): String =
        String(decryptBytes(decodePayload(source)), Charsets.UTF_8)

    /** 密文（hex 或 base64）→ LRC：每行 `[mm:ss.xx]文本`，逐字时间被抹掉。 */
    fun toLrc(source: String): String = convert(source, wordByWord = false)

    /** 密文（hex 或 base64）→ YRC：每行 `[start,dur](s,d,0)字…`。 */
    fun toYrc(source: String): String = convert(source, wordByWord = true)

    fun decryptBytes(encrypted: ByteArray): ByteArray {
        require(encrypted.size % DES_BLOCK_SIZE == 0) {
            "QRC 密文长度不是 $DES_BLOCK_SIZE 的倍数（${encrypted.size}）"
        }
        val out = ByteArray(encrypted.size)
        val first = ByteArray(DES_BLOCK_SIZE)
        val second = ByteArray(DES_BLOCK_SIZE)
        var offset = 0
        while (offset < encrypted.size) {
            desCrypt(encrypted, offset, first, 0, DECRYPT_SCHEDULES[0])
            desCrypt(first, 0, second, 0, DECRYPT_SCHEDULES[1])
            desCrypt(second, 0, out, offset, DECRYPT_SCHEDULES[2])
            offset += DES_BLOCK_SIZE
        }
        return stripBom(inflate(out))
    }

    /** 明文 → 密文。宿主自己不用，留着做往返自检。 */
    fun encryptBytes(plain: ByteArray): ByteArray {
        val compressed = deflate(plain)
        val padded = compressed.copyOf(padToBlock(compressed.size))
        val out = ByteArray(padded.size)
        val first = ByteArray(DES_BLOCK_SIZE)
        val second = ByteArray(DES_BLOCK_SIZE)
        var offset = 0
        while (offset < padded.size) {
            desCrypt(padded, offset, first, 0, ENCRYPT_SCHEDULES[0])
            desCrypt(first, 0, second, 0, ENCRYPT_SCHEDULES[1])
            desCrypt(second, 0, out, offset, ENCRYPT_SCHEDULES[2])
            offset += DES_BLOCK_SIZE
        }
        return out
    }

    // endregion

    // region 文本转换

    private fun convert(source: String, wordByWord: Boolean): String {
        val plain = if (looksPlain(source)) source else decrypt(source)
        val content = xmlUnescape(extractLyricContent(plain) ?: plain)
        val out = StringBuilder()
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val match = QRC_LINE_TAG.find(line)
            if (match == null) {
                // `[ti:…]` / `[ar:…]` 这类头部标签原样保留，其它噪声丢掉。
                if (HEADER_TAG.matches(line)) out.append(line).append('\n')
                return@forEach
            }
            val start = match.groupValues[1].toLongOrNull() ?: return@forEach
            val duration = match.groupValues[2].toLongOrNull() ?: 0L
            val body = line.substring(match.range.last + 1)
            if (wordByWord) {
                out.append('[').append(start).append(',').append(duration).append(']')
                words(body).forEach { (wordStart, wordDuration, text) ->
                    out.append('(')
                        .append(if (wordStart < 0) start else wordStart)
                        .append(',').append(wordDuration).append(",0)")
                        .append(text)
                }
            } else {
                out.append(lrcTime(start)).append(words(body).joinToString("") { it.third })
            }
            out.append('\n')
        }
        return out.toString().trimEnd()
    }

    /** QRC 的逐字段：`词(0,500)语(500,500)` → (start, dur, "词") … */
    private fun words(body: String): List<Triple<Long, Long, String>> {
        val result = ArrayList<Triple<Long, Long, String>>()
        var cursor = 0
        for (match in QRC_WORD_TAG.findAll(body)) {
            if (match.range.first > cursor) {
                result.add(
                    Triple(
                        match.groupValues[1].toLongOrNull() ?: 0L,
                        match.groupValues[2].toLongOrNull() ?: 0L,
                        body.substring(cursor, match.range.first)
                    )
                )
            }
            cursor = match.range.last + 1
        }
        if (cursor < body.length) {
            val tail = body.substring(cursor)
            val last = result.lastOrNull()
            result.add(Triple(-1L, last?.second ?: 0L, tail))
        }
        return result
    }

    private fun lrcTime(ms: Long): String = String.format(
        Locale.ROOT,
        "[%02d:%02d.%02d]",
        ms / 60_000,
        (ms / 1_000) % 60,
        (ms % 1_000) / 10
    )

    private fun extractLyricContent(text: String): String? =
        LYRIC_CONTENT.find(text)?.groupValues?.get(1)

    private fun looksPlain(source: String): Boolean =
        source.contains("LyricContent") ||
            source.contains("<?xml") ||
            QRC_LINE_TAG.containsMatchIn(source)

    private fun xmlUnescape(text: String): String {
        if (!text.contains('&')) return text
        var value = text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
        NUMERIC_ENTITY.findAll(value).forEach { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@forEach
            value = value.replace(match.value, code.toChar().toString())
        }
        return value
    }

    // endregion

    // region 编解码底座

    private fun decodePayload(source: String): ByteArray {
        val text = source.trim().replace(WHITESPACE, "")
        if (text.isEmpty()) throw IllegalArgumentException("QRC 密文为空")
        if (text.length % 2 == 0 && (text.length / 2) % DES_BLOCK_SIZE == 0 && HEX.matches(text)) {
            return ByteArray(text.length / 2) { index ->
                text.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
        return runCatching { Base64.getDecoder().decode(text) }
            .getOrElse { throw IllegalArgumentException("QRC 密文既不是 hex 也不是 base64") }
    }

    private fun padToBlock(size: Int): Int {
        val remainder = size % DES_BLOCK_SIZE
        return if (remainder == 0) size else size + (DES_BLOCK_SIZE - remainder)
    }

    private fun stripBom(bytes: ByteArray): ByteArray =
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(input)
        val output = java.io.ByteArrayOutputStream(input.size * 4)
        val buffer = ByteArray(8 * 1024)
        try {
            while (!inflater.finished()) {
                val read = inflater.inflate(buffer)
                // 0 字节只可能是「流结束了」或「数据被截断」，两种情况都该停 —— 不能空转。
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        } catch (e: Exception) {
            throw IllegalStateException("QRC 解压失败（不是有效的 zlib 流）", e)
        } finally {
            inflater.end()
        }
        if (output.size() == 0) throw IllegalStateException("QRC 解压结果为空")
        return output.toByteArray()
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val output = java.io.ByteArrayOutputStream(input.size + 64)
        val buffer = ByteArray(8 * 1024)
        try {
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer))
            }
        } finally {
            deflater.end()
        }
        return output.toByteArray()
    }

    // endregion

    // region 密钥调度与分组运算

    /** 从 8 字节密钥按下标表取位；把密钥当两个小端 32 位整数，这是 QQ 的怪癖。 */
    private fun permuteFromKeyBytes(key: ByteArray, table: IntArray): Long {
        var output = 0L
        var mask = 1L shl (table.size - 1)
        for (pos in table) {
            val wordIndex = pos shr 5
            val bitInWord = pos and 31
            val byteInWord = bitInWord shr 3
            val bitInByte = bitInWord and 7
            val byteIndex = wordIndex * 4 + 3 - byteInWord
            val bit = (key[byteIndex].toInt() shr (7 - bitInByte)) and 1
            if (bit != 0) output = output or mask
            mask = mask ushr 1
        }
        return output
    }

    private fun rotateLeft28Bit(value: Long, amount: Int): Long {
        val masked = value and BITS_28_MASK
        val shifted = (masked shl amount) or (masked ushr (28 - amount))
        return shifted and BITS_28_MASK
    }

    private fun keySchedule(key: ByteArray, decrypt: Boolean): IntArray {
        val schedule = IntArray(32)
        var c = permuteFromKeyBytes(key, KEY_PERM_C) shl 4
        var d = permuteFromKeyBytes(key, KEY_PERM_D) shl 4
        for (round in 0 until 16) {
            val shift = KEY_RND_SHIFT[round]
            c = rotateLeft28Bit(c, shift)
            d = rotateLeft28Bit(d, shift)

            val target = if (decrypt) 15 - round else round
            var subkey = 0L
            for (index in KEY_COMPRESSION.indices) {
                val pos = KEY_COMPRESSION[index]
                val bit = if (pos < 28) {
                    (c ushr (31 - pos)) and 1L
                } else {
                    // QQ 的怪癖：D 部分的偏移是 pos - 27，不是 pos - 28。
                    (d ushr (31 - (pos - 27))) and 1L
                }
                if (bit == 1L) subkey = subkey or (1L shl (47 - index))
            }
            val b5 = ((subkey ushr 40) and 0xFF).toInt()
            val b4 = ((subkey ushr 32) and 0xFF).toInt()
            val b3 = ((subkey ushr 24) and 0xFF).toInt()
            val b2 = ((subkey ushr 16) and 0xFF).toInt()
            val b1 = ((subkey ushr 8) and 0xFF).toInt()
            val b0 = (subkey and 0xFF).toInt()
            schedule[target * 2] = (b5 shl 16) or (b4 shl 8) or b3
            schedule[target * 2 + 1] = (b2 shl 16) or (b1 shl 8) or b0
        }
        return schedule
    }

    private fun buildPermutationTables() {
        for (bytePos in 0 until 8) {
            for (byteValue in 0 until 256) {
                val input = byteValue.toLong() shl (56 - bytePos * 8)
                val index = (bytePos shl 8) or byteValue

                val permutedIp = applyPermutation(input, IP_RULE)
                IP_LEFT_TABLE[index] = (permutedIp ushr 32).toInt()
                IP_RIGHT_TABLE[index] = permutedIp.toInt()

                val permutedInvIp = applyPermutation(input, INV_IP_RULE)
                INV_IP_LEFT_TABLE[index] = (permutedInvIp ushr 32).toInt()
                INV_IP_RIGHT_TABLE[index] = permutedInvIp.toInt()
            }
        }
    }

    private fun applyPermutation(input: Long, rule: IntArray): Long {
        var output = 0L
        for (index in 0 until 64) {
            val sourceBit = rule[index]
            if ((input ushr (64 - sourceBit)) and 1L != 0L) {
                output = output or (1L shl (63 - index))
            }
        }
        return output
    }

    private fun buildSpTables() {
        for (boxIndex in 0 until 8) {
            for (input in 0 until 64) {
                val sBoxIndex = (input and 0x20) or ((input and 0x1F) shr 1) or ((input and 1) shl 4)
                val value = S_BOXES[boxIndex][sBoxIndex] shl (28 - boxIndex * 4)
                SP_TABLE[(boxIndex shl 6) or input] = applyQqPBoxPermutation(value)
            }
        }
    }

    private fun applyQqPBoxPermutation(input: Int): Int {
        var output = 0
        for (index in 0 until 32) {
            val sourceBit = P_BOX[index]
            if ((input and (1 shl (32 - sourceBit))) != 0) {
                output = output or (1 shl (31 - index))
            }
        }
        return output
    }

    private fun buildEBoxTables() {
        for (chunk in 0 until 4) {
            val shift = (3 - chunk) * 8
            for (byteValue in 0 until 256) {
                var high24 = 0
                var low24 = 0
                val input = byteValue shl shift
                for (index in 0 until 24) {
                    if ((input ushr (32 - E_BOX_TABLE[index])) and 1 != 0) {
                        high24 = high24 or (1 shl (23 - index))
                    }
                }
                for (index in 24 until 48) {
                    if ((input ushr (32 - E_BOX_TABLE[index])) and 1 != 0) {
                        low24 = low24 or (1 shl (47 - index))
                    }
                }
                val tableIndex = (chunk shl 8) or byteValue
                EBOX_HIGH_TABLE[tableIndex] = high24
                EBOX_LOW_TABLE[tableIndex] = low24
            }
        }
    }

    private fun fFunction(state: Int, keyHigh24: Int, keyLow24: Int): Int {
        val b0 = (state ushr 24) and 0xFF
        val b1 = (state ushr 16) and 0xFF
        val b2 = (state ushr 8) and 0xFF
        val b3 = state and 0xFF

        val eboxHigh = EBOX_HIGH_TABLE[b0] or EBOX_HIGH_TABLE[256 or b1] or
            EBOX_HIGH_TABLE[512 or b2] or EBOX_HIGH_TABLE[768 or b3]
        val eboxLow = EBOX_LOW_TABLE[b0] or EBOX_LOW_TABLE[256 or b1] or
            EBOX_LOW_TABLE[512 or b2] or EBOX_LOW_TABLE[768 or b3]

        val xorHigh = eboxHigh xor keyHigh24
        val xorLow = eboxLow xor keyLow24

        var output = SP_TABLE[(xorHigh ushr 18) and 0x3F]
        output = output or SP_TABLE[64 or ((xorHigh ushr 12) and 0x3F)]
        output = output or SP_TABLE[128 or ((xorHigh ushr 6) and 0x3F)]
        output = output or SP_TABLE[192 or (xorHigh and 0x3F)]
        output = output or SP_TABLE[256 or ((xorLow ushr 18) and 0x3F)]
        output = output or SP_TABLE[320 or ((xorLow ushr 12) and 0x3F)]
        output = output or SP_TABLE[384 or ((xorLow ushr 6) and 0x3F)]
        output = output or SP_TABLE[448 or (xorLow and 0x3F)]
        return output
    }

    private fun desCrypt(
        input: ByteArray,
        inputOffset: Int,
        output: ByteArray,
        outputOffset: Int,
        schedule: IntArray
    ) {
        var left = 0
        var right = 0
        for (index in 0 until 8) {
            val tableIndex = (index shl 8) or (input[inputOffset + index].toInt() and 0xFF)
            left = left or IP_LEFT_TABLE[tableIndex]
            right = right or IP_RIGHT_TABLE[tableIndex]
        }

        for (round in 0 until 15) {
            val previous = right
            right = left xor fFunction(right, schedule[round * 2], schedule[round * 2 + 1])
            left = previous
        }
        left = left xor fFunction(right, schedule[30], schedule[31])

        var outLeft = 0
        var outRight = 0
        for (index in 0 until 4) {
            val leftIndex = (index shl 8) or ((left ushr (24 - index * 8)) and 0xFF)
            outLeft = outLeft or INV_IP_LEFT_TABLE[leftIndex]
            outRight = outRight or INV_IP_RIGHT_TABLE[leftIndex]

            val rightIndex = ((index + 4) shl 8) or ((right ushr (24 - index * 8)) and 0xFF)
            outLeft = outLeft or INV_IP_LEFT_TABLE[rightIndex]
            outRight = outRight or INV_IP_RIGHT_TABLE[rightIndex]
        }

        output[outputOffset] = ((outLeft ushr 24) and 0xFF).toByte()
        output[outputOffset + 1] = ((outLeft ushr 16) and 0xFF).toByte()
        output[outputOffset + 2] = ((outLeft ushr 8) and 0xFF).toByte()
        output[outputOffset + 3] = (outLeft and 0xFF).toByte()
        output[outputOffset + 4] = ((outRight ushr 24) and 0xFF).toByte()
        output[outputOffset + 5] = ((outRight ushr 16) and 0xFF).toByte()
        output[outputOffset + 6] = ((outRight ushr 8) and 0xFF).toByte()
        output[outputOffset + 7] = (outRight and 0xFF).toByte()
    }

    // endregion

    private const val BITS_28_MASK = 0xFFFFFFF0L

    private val QRC_LINE_TAG = Regex("^\\[(\\d+),(\\d+)]")
    private val QRC_WORD_TAG = Regex("\\((\\d+),(\\d+)(?:,\\d+)?\\)")
    private val HEADER_TAG = Regex("^\\[[a-zA-Z#]+:.*]$")
    private val LYRIC_CONTENT = Regex("LyricContent\\s*=\\s*\"([\\s\\S]*?)\"")
    private val NUMERIC_ENTITY = Regex("&#(\\d+);")
    private val HEX = Regex("^[0-9a-fA-F]+$")
    private val WHITESPACE = Regex("\\s+")
}
