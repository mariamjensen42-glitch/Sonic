package com.cycling.data.online.lx

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 脚本侧 `lx.utils.*` 的实现。
 *
 * 约定如下：
 * 二进制类工具（zlib / aes / randomBytes）统一返回 base64（NO_WRAP），
 * md5 返回小写十六进制；入参既可以是 base64 字符串也可以是字节数组。
 *
 * `qrc.*` 是 QQ 音乐 KRC/QRC 加密歌词的解码入口，实现在 [QrcCodec]；
 * 脚本也可以只用 `crypto.aesDecrypt` / `zlib.inflateRaw` 这类原语自己解。
 */
object LxJsUtils {

    private val hex = "0123456789abcdef".toCharArray()

    fun execute(name: String, args: List<Any?>): Any? = when (name) {
        "crypto.md5" -> {
            val text = args.getOrNull(0)?.toString().orEmpty()
            val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(StandardCharsets.UTF_8))
            toHex(digest)
        }

        "crypto.randomBytes" -> {
            val size = (args.getOrNull(0) as? Number)?.toInt() ?: 16
            val bytes = ByteArray(size.coerceAtLeast(0))
            SecureRandom().nextBytes(bytes)
            toHex(bytes)
        }

        "text.gb18030" -> decodeGb18030(decodeBytes(args.getOrNull(0)) ?: ByteArray(0))

        "buffer.from" -> bufferFrom(args)

        "buffer.bufToString" -> {
            val value = args.getOrNull(0)?.toString().orEmpty()
            val encoding = args.getOrNull(1)?.toString() ?: "hex"
            if (encoding == "base64") {
                Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            } else {
                value
            }
        }

        "zlib.inflate" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val inflater = Inflater()
            inflater.setInput(input)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val read = inflater.inflate(buffer)
                if (read > 0) output.write(buffer, 0, read) else break
            }
            inflater.end()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }

        "zlib.deflate" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val deflater = Deflater()
            deflater.setInput(input)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer))
            }
            deflater.end()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }

        "zlib.gzip" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val output = ByteArrayOutputStream()
            GZIPOutputStream(output).use { it.write(input) }
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }

        "zlib.gunzip" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val output = GZIPInputStream(ByteArrayInputStream(input)).use { it.readBytes() }
            Base64.encodeToString(output, Base64.NO_WRAP)
        }

        "zlib.inflateRaw" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val inflater = Inflater(true)
            inflater.setInput(input)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val read = inflater.inflate(buffer)
                if (read > 0) output.write(buffer, 0, read) else break
            }
            inflater.end()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }

        "zlib.deflateRaw" -> {
            val input = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
            deflater.setInput(input)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer))
            }
            deflater.end()
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }

        "crypto.aesEncrypt" -> aesEncrypt(args)

        "crypto.aesDecrypt" -> aesDecrypt(args)

        "crypto.aesEcbEncryptB64Key" -> {
            val text = args.getOrNull(0)?.toString().orEmpty()
            val key = args.getOrNull(1)?.toString().orEmpty()
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey(key), "AES"))
            Base64.encodeToString(
                cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8)),
                Base64.NO_WRAP
            )
        }

        "crypto.aesEcbDecryptB64Key" -> {
            val data = args.getOrNull(0)?.toString().orEmpty()
            val key = args.getOrNull(1)?.toString().orEmpty()
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey(key), "AES"))
            val plain = cipher.doFinal(Base64.decode(data, Base64.NO_WRAP))
            String(plain, StandardCharsets.UTF_8)
        }

        "crypto.rsaEncrypt" -> rsaEncrypt(args)

        // QRC（QQ 音乐逐字歌词）不再是缺口：解码器在 QrcCodec，脚本可直接拿到明文 / LRC / YRC。
        "qrc.decrypt" -> QrcCodec.decrypt(args.firstOrNull()?.toString().orEmpty())
        "qrc.toLrc" -> QrcCodec.toLrc(args.firstOrNull()?.toString().orEmpty())
        "qrc.toYrc" -> QrcCodec.toYrc(args.firstOrNull()?.toString().orEmpty())

        else -> throw IllegalStateException("utils.$name 未实现")
    }

    /** `buffer.from(data, encoding)`：字节数组 -> 字符串；字符串按 encoding 描述原样返回或 base64。 */
    private fun bufferFrom(args: List<Any?>): Any {
        val value = args.getOrNull(0) ?: return ""
        if (value is String) {
            val encoding = args.getOrNull(1)?.toString() ?: "utf8"
            return if (encoding == "base64") {
                Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            } else {
                value
            }
        }
        if (value is List<*>) {
            val bytes = value.mapNotNull { (it as? Number)?.toByte() }.toByteArray()
            return String(bytes, StandardCharsets.UTF_8)
        }
        return value.toString()
    }

    private fun aesEncrypt(args: List<Any?>): String {
        val text = args.getOrNull(0)?.toString().orEmpty()
        val mode = args.getOrNull(1)?.toString() ?: "CBC"
        val key = aesKey(args.getOrNull(2)?.toString().orEmpty())
        val data = text.toByteArray(StandardCharsets.UTF_8)
        return if (mode.equals("ECB", ignoreCase = true)) {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
        } else {
            val iv = aesKey(args.getOrNull(3)?.toString() ?: "0000000000000000")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            Base64.encodeToString(cipher.doFinal(data), Base64.NO_WRAP)
        }
    }

    /** 与 [aesEncrypt] 对称：参数 (密文, 模式, key[, iv])，返回 UTF-8 明文。 */
    private fun aesDecrypt(args: List<Any?>): String {
        val data = decodeBytes(args.getOrNull(0)) ?: ByteArray(0)
        val mode = args.getOrNull(1)?.toString() ?: "CBC"
        val key = aesKey(args.getOrNull(2)?.toString().orEmpty())
        val plain = if (mode.equals("ECB", ignoreCase = true)) {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
            cipher.doFinal(data)
        } else {
            val iv = aesKey(args.getOrNull(3)?.toString() ?: "0000000000000000")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            cipher.doFinal(data)
        }
        return String(plain, StandardCharsets.UTF_8)
    }

    private fun rsaEncrypt(args: List<Any?>): String {        val text = args.getOrNull(0)?.toString().orEmpty()
        val keyText = args.getOrNull(1)?.toString().orEmpty()
        val encoded = keyText
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(Base64.decode(encoded, Base64.NO_WRAP)))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeToString(cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    /** 密钥按原实现处理：UTF-8 取字节后截断/补齐到 16 字节。 */
    private fun aesKey(text: String): ByteArray {
        val raw = text.toByteArray(StandardCharsets.UTF_8)
        return raw.copyOf(16)
    }

    private fun decodeBytes(value: Any?): ByteArray? = when (value) {
        null -> null
        is String -> runCatching { Base64.decode(value, Base64.NO_WRAP) }
            .getOrElse { value.toByteArray(StandardCharsets.UTF_8) }

        is List<*> -> value.mapNotNull { (it as? Number)?.toByte() }.toByteArray()
        else -> value.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun decodeGb18030(bytes: ByteArray): String {
        val gb = runCatching { String(bytes, Charset.forName("GB18030")) }.getOrNull()
        if (gb != null && !gb.contains('\uFFFD')) return gb
        val utf8 = String(bytes, StandardCharsets.UTF_8)
        if (!utf8.contains('\uFFFD')) return utf8
        return gb ?: runCatching { String(bytes, Charset.forName("GBK")) }.getOrDefault(utf8)
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            out[index * 2] = hex[value ushr 4]
            out[index * 2 + 1] = hex[value and 0x0F]
        }
        return String(out)
    }
}
