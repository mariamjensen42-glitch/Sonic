package com.cycling.domain.online.model

/**
 * 取链档位。
 *
 * [lxKey] 是 LX 脚本 `musicUrl` 请求里的 `type` 字段（`128k` / `320k` / `flac` …），
 * [rank] 越大音质越高，用于 [LADDER] 的「档位降级」：先要最高档，拿不到就沿 rank 递减再试。
 *
 * `flac24bit` / `atmos` / `atmos_plus` / `master` / `jyeffect` / `sky` / `jymaster`
 * 这些是最激进的档位，只有部分插件会提供。
 */
enum class MusicQuality(
    val lxKey: String,
    val displayName: String,
    val rank: Int,
    /** true 表示只有插件可能提供（客户端合成/空间音频类档位）。 */
    val pluginOnly: Boolean = false
) {
    STANDARD("128k", "标准", 0),
    EXHIGH("320k", "极高", 1),
    LOSSLESS("flac", "无损", 2),
    HIRES("hires", "Hi-Res", 3),
    FLAC24BIT("flac24bit", "24bit 无损", 4, pluginOnly = true),
    JY_EFFECT("jyeffect", "鲸云臻音 AI", 5, pluginOnly = true),
    JY_SKY("sky", "鲸云臻音 2.0", 6, pluginOnly = true),
    JY_MASTER("jymaster", "鲸云母带", 7, pluginOnly = true),
    ATMOS("atmos", "全景声", 8, pluginOnly = true),
    ATMOS_PLUS("atmos_plus", "全景声+", 9, pluginOnly = true),
    MASTER("master", "母带", 10, pluginOnly = true);

    companion object {
        /** 从高到低的完整档位序列，降级就沿着它走。 */
        val LADDER: List<MusicQuality> = entries.sortedByDescending { it.rank }

        fun fromLxKey(key: String): MusicQuality? = entries.firstOrNull { it.lxKey == key }

        /**
         * 在插件声明的可用档位里，挑一个不超过 [preferred] 且插件确实支持的档位。
         *
         * 与 `LxMaps.pickLxQuality` 一致：偏好档不可用时向下找，都找不到就退回插件声明的最低档。
         * 返回值仍然是 LX 档位键，因为它的用途就是填进脚本请求。
         */
        fun pickLxQuality(available: List<String>, preferred: MusicQuality): String {
            if (available.isEmpty()) return preferred.lxKey
            if (preferred.lxKey in available) return preferred.lxKey
            val index = LADDER.indexOf(preferred).let { if (it < 0) LADDER.indexOf(EXHIGH) else it }
            for (i in index until LADDER.size) {
                val candidate = LADDER[i]
                if (candidate.lxKey in available) return candidate.lxKey
            }
            for (i in index - 1 downTo 0) {
                val candidate = LADDER[i]
                if (candidate.lxKey in available) return candidate.lxKey
            }
            return available.last()
        }

        /**
         * 生成「档位降级」序列：从 [preferred] 起向下，只保留插件声明支持的档位。
         *
         * [pluginOnlyPreferred] 为 true（偏好本身就是插件专属档位）时只试这一档——
         * 这是 `LxMaps.qualityLadder` 的规则：插件专属档位混进普通档位降级没有意义。
         */
        fun ladder(
            available: List<String>,
            preferred: MusicQuality,
            limit: Int = 4,
            pluginOnlyPreferred: Boolean = false
        ): List<String> {
            val start = pickLxQuality(available, preferred)
            if (pluginOnlyPreferred && preferred.pluginOnly) return listOf(start)
            if (available.isEmpty()) return listOf(start)
            val from = LADDER.indexOfFirst { it.lxKey == start }.takeIf { it >= 0 } ?: (LADDER.size - 1)
            val result = ArrayList<String>(limit)
            for (i in from until LADDER.size) {
                val key = LADDER[i].lxKey
                if (key in available) {
                    result += key
                    if (result.size >= limit) break
                }
            }
            return result.ifEmpty { listOf(start) }
        }
    }
}
