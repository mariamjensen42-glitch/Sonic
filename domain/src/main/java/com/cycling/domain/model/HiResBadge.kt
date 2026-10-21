package com.cycling.domain.model

/**
 * 播放页封面左下角的「小金标」样式。
 *
 * 纯装饰：Salt Player 的做法也是让用户自己挑一个标志，而不去判断文件的位深/采样率。
 * 这里同样不读音频格式 —— 标上去的只是用户的选择，不是音质结论。
 */
enum class HiResBadge {
    OFF,
    HI_RES,
    HI_RES_WIRELESS,
    GOLD_SEAL,
    CUSTOM;

    companion object {
        fun fromId(id: String?): HiResBadge = entries.firstOrNull { it.name == id } ?: OFF
    }
}
