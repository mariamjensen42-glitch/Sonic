package com.cycling.sonic.presentation.lyrics.utils

fun Char.isCjk(): Boolean {
    val code = this.code
    return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0x20000..0x2A6DF ||
        code in 0x2A700..0x2B73F || code in 0x2B740..0x2B81F || code in 0x2B820..0x2CEAF ||
        code in 0xF900..0xFAFF || code in 0x2F800..0x2FA1F
}

fun Char.isArabic(): Boolean {
    val code = this.code
    return code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF ||
        code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF
}

fun Char.isDevanagari(): Boolean {
    val code = this.code
    return code in 0x0900..0x097F || code in 0xA8E0..0xA8FF || code in 0x1CD0..0x1CFF
}

fun String.isPureCjk(): Boolean {
    val cleaned = filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    return cleaned.isNotEmpty() && cleaned.all { it.isCjk() }
}

fun String.isRtl(): Boolean = any { it.isArabic() }

fun String.isPunctuation(): Boolean = isNotEmpty() && all {
    it.isWhitespace() || it in ".,!?;:\"'()[]{}…—–-、。，！？；：\"\"''（）【】《》～·" ||
    Character.getType(it) in setOf(Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.OTHER_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt())
}
