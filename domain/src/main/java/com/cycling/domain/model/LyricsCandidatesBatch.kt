package com.cycling.domain.model

/**
 * 一个来源对「这首歌有哪些版本」的回答。
 *
 * 按来源分批给出而不是攒成一个列表：面板打开后每查到一家就画一家，用户不必等最慢的那个站点
 * 才看到结果，也不会因为某一家超时就把整份列表一起拖住。
 *
 * @property provider 回答的是哪一家。
 * @property candidates 它给出的版本，最好的在前；没有就是空列表。
 * @property failed 这一家是**查询失败**而不是「没有这首歌」。两者都要能从界面上分开 ——
 *   前者值得重试，后者不必。
 */
data class LyricsCandidatesBatch(
    val provider: LyricsProvider,
    val candidates: List<LyricsCandidate>,
    val failed: Boolean = false
)
