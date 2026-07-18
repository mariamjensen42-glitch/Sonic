package com.cycling.presentation.lyrics.utils

fun Char.isCjk(): Boolean {
    val codePoint = this.code
    return codePoint in 0x4E00..0x9FFF ||
            codePoint in 0x3400..0x4DBF ||
            codePoint in 0x20000..0x2A6DF ||
            codePoint in 0x2A700..0x2B73F ||
            codePoint in 0x2B740..0x2B81F ||
            codePoint in 0x2B820..0x2CEAF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x2F800..0x2FA1F
}

fun Char.isArabic(): Boolean {
    val codePoint = this.code
    return codePoint in 0x0600..0x06FF ||
            codePoint in 0x0750..0x077F ||
            codePoint in 0x08A0..0x08FF ||
            codePoint in 0xFB50..0xFDFF ||
            codePoint in 0xFE70..0xFEFF
}

fun Char.isDevanagari(): Boolean {
    val codePoint = this.code
    return codePoint in 0x0900..0x097F ||
            codePoint in 0xA8E0..0xA8FF
}

fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

fun String.isRtl(): Boolean {
    return any { it.isArabic() }
}

fun String.isPunctuation(): Boolean {
    return isNotEmpty() && all { char ->
        char.isWhitespace() ||
                char in ".,!?;:\"'()[]{}…—–-、。，！？；：\"\"''（）【】《》～·" ||
                Character.getType(char) in setOf(
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt()
        )
    }
}
