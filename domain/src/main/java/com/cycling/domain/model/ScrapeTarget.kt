package com.cycling.domain.model

/**
 * 一首本地歌曲缺失的元数据项。
 *
 * 只收「用户能一眼看出不对」的字段：标题、艺术家、专辑。年份 / 流派 / 曲号这些在列表里
 * 不显示，缺了也看不出来，不值得单独列成一个待刮削的理由。
 */
enum class ScrapeField { TITLE, ARTIST, ALBUM }

/**
 * 待刮削的对象：本地歌曲 + 它缺了什么。
 *
 * [missing] 空集合表示这首歌的文本字段是齐的（用户主动想补封面时仍可以刮，见刮削页
 * 的「包含信息完整的歌曲」开关）。
 */
data class ScrapeTarget(
    val song: Song,
    val missing: Set<ScrapeField>
)

/**
 * 一次刮削应用之后的结果。
 *
 * 三个布尔值互相独立，因为三件事的失败原因不同：
 * - [fileTagsWritten]：文件不可写（分区存储 / 容器没有 writer）时为 false，
 *   此时元数据只存在于 App 的覆盖表里 —— 界面仍然是对的，别的播放器看不到；
 * - [coverApplied]：封面存成 App 私有副本（不依赖写权限），失败一般是下载失败；
 * - [metadataApplied]：标题 / 艺术家 / 专辑至少有一项真的变了。
 */
data class ScrapeOutcome(
    val candidate: CoverCandidate,
    val metadataApplied: Boolean = false,
    val fileTagsWritten: Boolean = false,
    val coverApplied: Boolean = false,
    /**
     * 应用之后的封面地址（App 私有副本的 URI）。
     *
     * 带回来是为了让列表立刻换成新封面：歌曲行是快照，不去数据库重读就拿不到这个新值，
     * 而为了刷新一张图再查一次整行不划算。
     */
    val newAlbumArt: String? = null
) {
    /** 什么都没落下来 —— 界面上不该报「已补全」。 */
    val isEmpty: Boolean get() = !metadataApplied && !coverApplied
}
