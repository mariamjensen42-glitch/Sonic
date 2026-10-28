package com.cycling.presentation.scrape

import androidx.annotation.StringRes
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeField
import com.cycling.domain.model.Song

/** 一行的刮削状态。 */
enum class ScrapeStatus {
    /** 还没搜过。 */
    IDLE,

    /** 正在搜在线候选。 */
    SEARCHING,

    /** 搜到了，等用户挑（或等自动应用）。 */
    READY,

    /** 正在写入。 */
    APPLYING,

    /** 已经补全。 */
    DONE,

    /** 搜过了，没有可信的结果。 */
    NOT_FOUND,

    /** 出错（网络 / 写库）。 */
    FAILED
}

/**
 * 一行 = 一首本地歌曲。
 *
 * [candidates] 只在用户展开候选面板时有值；批量刮削不保留候选，省内存也省得界面跳动。
 */
data class ScrapeRow(
    val song: Song,
    val missing: Set<ScrapeField>,
    val status: ScrapeStatus = ScrapeStatus.IDLE,
    val candidates: List<CoverCandidate> = emptyList(),
    /** 自动应用时选中的那条，用于行尾显示「将是这一条」。 */
    val matched: CoverCandidate? = null,
    @StringRes val message: Int? = null
) {
    val id: Long get() = song.id
}

data class ScrapeUiState(
    val isLoading: Boolean = true,
    val rows: List<ScrapeRow> = emptyList(),
    /** 是否把信息完整的歌也列进来（只想补封面的场景）。 */
    val includeComplete: Boolean = false,
    /** 全自动刮削进行中。 */
    val isScrapingAll: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    /** 是否只保留还没补全的行（默认关，补全的行留在原位显示结果）。 */
    val hideDone: Boolean = true,
    /** 候选面板正在为哪一行展开。 */
    val openSheetFor: Long? = null,
    val isLoadingCandidates: Boolean = false,
    val sheetCandidates: List<CoverCandidate> = emptyList()
) {
    val pendingCount: Int get() = rows.count { it.status == ScrapeStatus.IDLE }
    val doneCount: Int get() = rows.count { it.status == ScrapeStatus.DONE }
    val visibleRows: List<ScrapeRow>
        get() = if (hideDone) rows.filter { it.status != ScrapeStatus.DONE } else rows
}

sealed interface ScrapeIntent {
    data object NavigateBack : ScrapeIntent
    data object Reload : ScrapeIntent
    data class SetIncludeComplete(val value: Boolean) : ScrapeIntent
    data class SetHideDone(val value: Boolean) : ScrapeIntent

    /** 自动刮削所有还没处理的行走一遍。 */
    data object ScrapeAll : ScrapeIntent
    data object CancelScrapeAll : ScrapeIntent

    /** 只刮这一首（走同一套自动匹配规则）。 */
    data class ScrapeOne(val songId: Long) : ScrapeIntent

    /** 展开候选面板，让用户自己挑。 */
    data class OpenCandidates(val songId: Long) : ScrapeIntent
    data object DismissCandidates : ScrapeIntent
    data class ApplyCandidate(val songId: Long, val candidateId: String) : ScrapeIntent
}

sealed interface ScrapeEffect {
    data object NavigateBack : ScrapeEffect
    data class ShowToast(@StringRes val message: Int) : ScrapeEffect
}
