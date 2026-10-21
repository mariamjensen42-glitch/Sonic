package com.cycling.data.online.lx

import com.cycling.domain.online.model.KeyIssue
import com.cycling.domain.online.model.KeyIssueKind

/**
 * 把服务端/lxkey 返回的中文错误文案归类。
 *
 * 关键字表覆盖七类：额度、到期、停用、无效、无权限、平台、音质
 * 七类。归类之后 UI 能做「去续期」「换平台」「降档」这种可操作提示，而不是甩一条原文。
 */
object LxKeyIssues {

    private val QUOTA = listOf("额度已用完")
    private val EXPIRED = listOf("到期，请联系管理员续期")
    private val DISABLED = listOf("该 Key 已停用")
    private val INVALID = listOf("Key 无效或已被删除", "Key失效/鉴权失败")
    private val NOT_ALLOWED = listOf("该 Key 不允许")
    private val PLATFORM = listOf("未开放平台")
    private val QUALITY = listOf("该 Key 最高只允许")

    fun classify(message: String?): KeyIssue? {
        val text = message?.trim().orEmpty()
        if (text.isEmpty()) return null
        val kind = when {
            containsAny(text, QUOTA) -> KeyIssueKind.QUOTA
            containsAny(text, EXPIRED) -> KeyIssueKind.EXPIRED
            containsAny(text, DISABLED) -> KeyIssueKind.DISABLED
            containsAny(text, INVALID) -> KeyIssueKind.INVALID
            containsAny(text, NOT_ALLOWED) -> KeyIssueKind.NOT_ALLOWED
            containsAny(text, PLATFORM) -> KeyIssueKind.PLATFORM
            containsAny(text, QUALITY) -> KeyIssueKind.QUALITY
            else -> null
        } ?: return null
        return KeyIssue(kind, text)
    }

    /** 这类错误换平台/换档位也没用，应当直接停止重试。 */
    fun isFatal(issue: KeyIssue?): Boolean = when (issue?.kind) {
        KeyIssueKind.QUOTA, KeyIssueKind.EXPIRED, KeyIssueKind.DISABLED, KeyIssueKind.INVALID -> true
        else -> false
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it, ignoreCase = true) }
}
