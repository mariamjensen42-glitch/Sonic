package com.cycling.domain.repository

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeOutcome
import com.cycling.domain.model.ScrapeTarget
import com.cycling.domain.model.Song

/**
 * 在线刮削：用在线目录补齐本地文件缺失的元数据。
 *
 * 与 [CoverRepository] 的关系是「用它的搜索、做更大的事」：候选本来就同时带封面和文本字段
 * （一次搜索两条都用得上），这里负责把候选**落地** —— 标题 / 艺术家 / 专辑写进歌曲行与文件标签，
 * 封面存成 App 私有副本（不依赖写权限）。
 *
 * 落地的三条路故意分开记结果，见 [ScrapeOutcome]：分区存储下文件标签大概率写不进去，
 * 但「界面显示对了、封面有了」对用户仍然是有价值的，不能因为写不了文件就整条判失败。
 */
interface MetadataScraperRepository {

    /**
     * 找出元数据不全的本地歌曲，缺得多的排前面。
     *
     * 只列本地文件：在线曲目的标题 / 艺术家来自平台接口，本来就不缺。
     */
    suspend fun findIncomplete(limit: Int = DEFAULT_LIMIT): List<ScrapeTarget>

    /** 全部本地歌曲（含信息完整的），给刮削页的「包含完整歌曲」开关用。 */
    suspend fun findAllLocal(limit: Int = DEFAULT_LIMIT): List<ScrapeTarget>

    /** 在线候选，已经按与本地文件的匹配度排好序。 */
    suspend fun candidates(song: Song): List<CoverCandidate>

    /**
     * 把 [candidate] 应用到这个文件上。
     *
     * 幂等：同一份候选重复应用不会写出重复的覆盖记录或封面。
     */
    suspend fun apply(song: Song, candidate: CoverCandidate): ScrapeOutcome

    companion object {
        /** 一次最多列 200 首：再多用户也不会一屏屏翻，全自动刮削也扛不住几百次网络请求。 */
        const val DEFAULT_LIMIT = 200
    }
}
