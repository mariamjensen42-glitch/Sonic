package com.cycling.data.local.autoeq

import android.content.Context
import com.cycling.domain.audio.AutoEqFilterCodec
import com.cycling.domain.model.AutoEqEntry
import com.cycling.domain.model.AutoEqFormFactor
import com.cycling.domain.model.AutoEqProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AutoEQ 目录（assets/autoeq，8850 条，MIT）。
 *
 * 首次访问时解压解析一次并常驻内存：目录本身 2 MB 文本，解析后约 8850 条，
 * 但**不缓存滤波器链** —— 只有被选中的那一条会被解析，避免几万个 biquad 对象长住堆里。
 *
 * 搜索在内存里做线性过滤，8850 条的子串匹配在几十微秒量级，不值得建索引。
 */
@Singleton
class AutoEqCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mutex = Mutex()

    @Volatile
    private var profiles: List<AutoEqProfile>? = null

    @Volatile
    private var indexById: Map<String, Int> = emptyMap()

    suspend fun totalCount(): Int = ensureLoaded().size

    suspend fun find(id: String): AutoEqProfile? {
        val loaded = ensureLoaded()
        val index = indexById[id] ?: return null
        return loaded.getOrNull(index)
    }

    suspend fun search(
        query: String,
        formFactor: AutoEqFormFactor?,
        limit: Int
    ): List<AutoEqEntry> {
        val loaded = ensureLoaded()
        val keyword = query.trim().lowercase()
        val candidates = if (formFactor == null) {
            loaded
        } else {
            loaded.filter { it.entry.formFactor == formFactor }
        }
        if (keyword.isEmpty()) return candidates.take(limit).map { it.entry }

        // 小写只算一次：8850 条 × 每键一路比较，重复 lowercase() 会白烧十几万次字符串分配。
        val matched = candidates
            .map { it to it.entry.name.lowercase() }
            .filter { (_, lower) -> lower.contains(keyword) }
        // 前缀命中的排前面，其余按名称顺序，避免"搜 Sony 时 WH-1000XM5 排在 Sony 自家型号后"。
        return matched
            .sortedWith(
                compareBy<Pair<AutoEqProfile, String>>(
                    { !it.second.startsWith(keyword) },
                    { it.second }
                )
            )
            .take(limit)
            .map { (profile, _) -> profile.entry }
    }

    private suspend fun ensureLoaded(): List<AutoEqProfile> {
        profiles?.let { return it }
        return mutex.withLock {
            profiles?.let { return it }
            val parsed = withContext(Dispatchers.IO) { loadFromAssets() }
            indexById = parsed.withIndex().associate { (index, profile) -> profile.entry.id to index }
            profiles = parsed
            parsed
        }
    }

    private fun loadFromAssets(): List<AutoEqProfile> {
        val text = runCatching {
            context.assets.open(ASSET_PATH).use { input ->
                GZIPInputStream(input).use { gzip -> gzip.readBytes() }
            }.toString(Charsets.UTF_8)
        }.getOrNull() ?: return emptyList()

        return AutoEqFilterCodec.parseCatalog(text)
            .sortedBy { it.entry.name.lowercase() }
    }

    private companion object {
        const val ASSET_PATH = "autoeq/catalog-v1.tsv.gz"
    }
}
