package com.cycling.domain.scanner

/**
 * 录音识别：把通话录音、录音笔、语音备忘这类非音乐音频从曲库里剔掉。
 *
 * MediaStore 只用 `IS_MUSIC` + 时长过滤，30 秒以上的通话录音照样会进来；而各家 ROM 的存放
 * 目录又不统一（MIUI `/MIUI/sound_recorder`、ColorOS / EMUI / 三星 `/Recordings`、
 * 通话录音 App 常见 `/Recordings/Call`、`/CallRecord`），所以只能按路径目录名 + 元数据关键词匹配。
 *
 * 原则是「宁可漏判，也不误杀」：只认足够特殊的片段，不匹配 `record` / `records` 这类
 * 完全可能是专辑名的词。
 */
object RecordingFilter {

    /** 目录名片段。命中任意一个就当作录音，比较时忽略大小写。 */
    private val DIRECTORY_TOKENS = listOf(
        "/recording",      // Recordings / Recording
        "/recorder",       // Recorder / SoundRecorder
        "/sound_recorder",
        "/soundrecorder",
        "/callrec",        // CallRecorder / CallRecordings
        "/call_rec",
        "/callrecord",
        "/call_record",
        "/voicememo",
        "/voice_memo",
        "/voicerecord",
        "/voice_record",
        "/录音",           // 录音 / 录音机
        "/通话",           // 通话录音
        "/语音备忘",
        "/语音记事",
        "/音频录制"
    )

    /** 文件名形如 `Call_20261023_1830.m4a`、`通话录音_20261023.amr`。 */
    private val CALL_FILE_PATTERN =
        Regex("""^(call|callrec|call_rec|通话录音|电话录音|录音)[ _-]?\d""")

    /** 落在标签里的录音特征；只认明确的中文/英文叫法。 */
    private val METADATA_TOKENS = listOf(
        "通话录音",
        "电话录音",
        "录音机",
        "语音备忘",
        "voice memo",
        "voicememo",
        "call recording"
    )

    /**
     * @param path 文件绝对路径
     * @param album 专辑名，只用来兜底识别录音 App 写入的标签
     * @param title 曲名
     */
    fun isRecording(path: String, album: String = "", title: String = ""): Boolean {
        if (path.isBlank()) return false

        val normalizedPath = path.lowercase()
        if (DIRECTORY_TOKENS.any { normalizedPath.contains(it) }) return true

        val fileName = normalizedPath.substringAfterLast('/')
        if (CALL_FILE_PATTERN.containsMatchIn(fileName)) return true

        val metadata = "${album.lowercase()}\u0000${title.lowercase()}"
        return METADATA_TOKENS.any { metadata.contains(it) }
    }
}
