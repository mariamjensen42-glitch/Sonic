package com.cycling.data.online.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.online.model.OnlinePlugin
import com.cycling.domain.online.model.OnlinePluginSource
import com.cycling.domain.online.model.OnlineSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "online_music")

/** 插件列表持久化形态（domain 模型不依赖序列化插件，所以单独放一套 DTO）。 */
@Serializable
data class StoredPlugin(
    val id: String,
    val name: String,
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val homepage: String = "",
    val scriptFileName: String = "",
    val origin: String = "",
    val originUrl: String = "",
    val enabled: Boolean = true,
    val proOnly: Boolean = false,
    val configFields: List<String> = emptyList(),
    val sources: List<StoredSource> = emptyList(),
    val manualOrder: Int = 0,
    val addedAt: Long = 0L,
    val blockedPlatforms: List<String> = emptyList(),
    val autoBlockInvalid: Boolean = true,
    val autoBlockedPlatforms: List<String> = emptyList(),
    val opaque: Boolean = false,
    val error: String? = null,
    val status: String = ""
)

@Serializable
data class StoredSource(
    val key: String,
    val name: String,
    val actions: List<String> = emptyList(),
    val qualities: List<String> = emptyList(),
    val homeCategories: List<String> = emptyList()
)

fun StoredPlugin.toDomain(): OnlinePlugin = OnlinePlugin(
    id = id,
    name = name,
    version = version,
    author = author,
    description = description,
    homepage = homepage,
    scriptFileName = scriptFileName,
    origin = origin,
    originUrl = originUrl,
    enabled = enabled,
    proOnly = proOnly,
    configFields = configFields,
    sources = sources.mapNotNull { source ->
        OnlineSource.fromLxKey(source.key)?.let { key ->
            OnlinePluginSource(
                key = key,
                name = source.name,
                actions = source.actions,
                qualities = source.qualities,
                homeCategories = source.homeCategories
            )
        }
    },
    manualOrder = manualOrder,
    addedAt = addedAt,
    blockedPlatforms = blockedPlatforms.mapNotNull(OnlineSource::fromLxKey).toSet(),
    autoBlockInvalid = autoBlockInvalid,
    autoBlockedPlatforms = autoBlockedPlatforms.mapNotNull(OnlineSource::fromLxKey).toSet(),
    opaque = opaque,
    error = error,
    status = status
)

fun OnlinePlugin.toStored(): StoredPlugin = StoredPlugin(
    id = id,
    name = name,
    version = version,
    author = author,
    description = description,
    homepage = homepage,
    scriptFileName = scriptFileName,
    origin = origin,
    originUrl = originUrl,
    enabled = enabled,
    proOnly = proOnly,
    configFields = configFields,
    sources = sources.map {
        StoredSource(
            key = it.key.lxKey,
            name = it.name,
            actions = it.actions,
            qualities = it.qualities,
            homeCategories = it.homeCategories
        )
    },
    manualOrder = manualOrder,
    addedAt = addedAt,
    blockedPlatforms = blockedPlatforms.map { it.lxKey },
    autoBlockInvalid = autoBlockInvalid,
    autoBlockedPlatforms = autoBlockedPlatforms.map { it.lxKey },
    opaque = opaque,
    error = error,
    status = status
)

/**
 * 在线音源的持久化：插件清单（JSON）+ 排序模式 + 换源优先级，
 * 以及脚本正文（一个插件一个文件，放 `filesDir/online_scripts/`，不塞进 DataStore）。
 */
@Singleton
class OnlineStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val pluginsKey = stringPreferencesKey("plugins_json")
    private val sortModeKey = stringPreferencesKey("sort_mode")
    private val priorityKey = stringPreferencesKey("priority")

    private val scriptsDir: File
        get() = File(context.filesDir, "online_scripts").apply { if (!exists()) mkdirs() }

    val plugins: Flow<List<StoredPlugin>> = context.onlineDataStore.data.map { preferences ->
        decodePlugins(preferences[pluginsKey])
    }

    val sortMode: Flow<String?> = context.onlineDataStore.data.map { it[sortModeKey] }

    val priority: Flow<String?> = context.onlineDataStore.data.map { it[priorityKey] }

    suspend fun savePlugins(plugins: List<StoredPlugin>) {
        context.onlineDataStore.edit { it[pluginsKey] = json.encodeToString(plugins) }
    }

    suspend fun saveSortMode(storage: String) {
        context.onlineDataStore.edit { it[sortModeKey] = storage }
    }

    suspend fun savePriority(storage: String) {
        context.onlineDataStore.edit { it[priorityKey] = storage }
    }

    suspend fun writeScript(fileName: String, content: String) = withContext(Dispatchers.IO) {
        File(scriptsDir, fileName).writeText(content)
    }

    suspend fun readScript(fileName: String): String? = withContext(Dispatchers.IO) {
        val file = File(scriptsDir, fileName)
        if (file.isFile) runCatching { file.readText() }.getOrNull() else null
    }

    suspend fun deleteScript(fileName: String) = withContext(Dispatchers.IO) {
        if (fileName.isBlank()) return@withContext
        runCatching { File(scriptsDir, fileName).delete() }
            .onFailure { Timber.w(it, "OnlineStore: 删除脚本失败 %s", fileName) }
        Unit
    }

    suspend fun readSnapshot(): Snapshot = withContext(Dispatchers.IO) {
        val preferences = context.onlineDataStore.data.first()
        Snapshot(
            plugins = decodePlugins(preferences[pluginsKey]),
            sortMode = preferences[sortModeKey],
            priority = preferences[priorityKey]
        )
    }

    private fun decodePlugins(raw: String?): List<StoredPlugin> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<StoredPlugin>>(raw) }
            .getOrElse {
                Timber.w(it, "OnlineStore: 插件清单解析失败，按空清单处理")
                emptyList()
            }
    }

    data class Snapshot(
        val plugins: List<StoredPlugin>,
        val sortMode: String?,
        val priority: String?
    )
}
