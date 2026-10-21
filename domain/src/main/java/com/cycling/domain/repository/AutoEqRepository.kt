package com.cycling.domain.repository

import com.cycling.domain.model.AppliedAutoEq
import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import kotlinx.coroutines.flow.Flow

/**
 * 耳机校正档案（AutoEQ）。
 *
 * 与 `EqualizerRepository` 分开：目录是一次性只读资产，应用哪个档案是另一路状态，
 * 混在一起会让目录搜索触发整组均衡器设置的重组。
 *
 * 生效方式是**叠加**在图形均衡之上：最终滤波器链 = 档案滤波器 + 10 段增益，
 * 预放大 = 用户预放大 + 档案预放大。所以打开校正不必清掉用户手调的曲线。
 */
interface AutoEqRepository {

    /** 当前生效档案；`null` 表示未应用。 */
    val applied: Flow<AppliedAutoEq?>

    /** 目录条目总数，用于界面提示与空态判断。 */
    suspend fun totalCount(): Int

    /**
     * 按型号名模糊搜索，忽略大小写的子串匹配；[query] 为空时按名称顺序列出前 [limit] 条。
     *
     * [formFactor] 为 `null` 表示不筛形态。
     */
    suspend fun search(
        query: String,
        formFactor: AutoEqFormFactor? = null,
        limit: Int = DEFAULT_LIMIT
    ): List<AutoEqEntry>

    /** 应用档案并打开均衡器；目录里没有这个 id 时返回 `false`。 */
    suspend fun apply(id: String): Boolean

    /** 撤掉校正，只清档案这一层，不动用户自己的增益与开关。 */
    suspend fun clear()

    companion object {
        const val DEFAULT_LIMIT: Int = 60
    }
}
