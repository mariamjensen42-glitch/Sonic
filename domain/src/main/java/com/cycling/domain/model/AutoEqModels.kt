package com.cycling.domain.model

/**
 * 耳机形态，对应 AutoEQ 目录里的三组：入耳、平头塞、头戴，用于筛选。
 */
enum class AutoEqFormFactor {
    IN_EAR,
    EARBUD,
    OVER_EAR
}

/**
 * 目录条目。搜索列表只带这些字段 —— 8850 条全带滤波器会白占几 MB 堆。
 *
 * [source] 是测量来源（crinacle / oratory1990 等），[name] 是含品牌的型号名。
 */
data class AutoEqEntry(
    val id: String,
    val formFactor: AutoEqFormFactor,
    val source: String,
    val name: String
)

/**
 * 完整档案：型号 + 预放大 + 滤波器链。
 */
data class AutoEqProfile(
    val entry: AutoEqEntry,
    val preampDb: Float,
    val filters: List<EqFilterSpec>
)

/**
 * 已应用的档案。
 *
 * 滤波器与预放大随设置一起落盘，运行期不再回头读资产目录 —— 播放进程重启时
 * 不必先解析 8850 条目录才能把声音还原。
 */
data class AppliedAutoEq(
    val id: String,
    val name: String,
    val source: String,
    val preampDb: Float,
    val filters: List<EqFilterSpec>
)
