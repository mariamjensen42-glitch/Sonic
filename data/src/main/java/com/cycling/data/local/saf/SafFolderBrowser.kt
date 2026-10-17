package com.cycling.data.local.saf

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.domain.model.FolderContent
import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.FolderRoot
import com.cycling.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val Context.browsedFoldersStore: DataStore<Preferences> by preferencesDataStore(name = "browsed_folders")

/**
 * 直接读文件系统的目录浏览：SAF（`ACTION_OPEN_DOCUMENT_TREE`）+ [DocumentsContract]，
 * 不经过媒体库（MediaStore 扫描 / Room），未入库的音频同样能列出、能播放。
 *
 * 用 SAF 而不是 `java.io.File`：Android 11 起没有 `MANAGE_EXTERNAL_STORAGE` 时，
 * 直接列举共享存储会被 scoped storage 拒绝；SAF 由用户逐目录授权，是平台给的正确通道。
 *
 * 已知边界（`ponytail:`）：曲目 id 由文档 uri 哈希出的负数合成，播放计数 / 收藏 /
 * 最近播放这些依赖 Room 行的行为对它们不生效；队列持久化后重启恢复也会丢弃它们
 * （[com.cycling.data.player.PlayerManager] 恢复时按 id 回查 Room）。要补齐就得给
 * 这些文件也建库，属于"引入媒体库"——与本类的目的相反，故不做。
 */
@Singleton
class SafFolderBrowser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** 已授权的目录树；授权失效（如被系统收回）的条目会自动跳过，不会崩。 */
    suspend fun roots(): List<FolderRoot> = withContext(Dispatchers.IO) {
        context.browsedFoldersStore.data.first()[ROOTS_KEY]?.mapNotNull { uriString ->
            runCatching { rootOf(Uri.parse(uriString)) }.getOrNull()
        } ?: emptyList()
    }

    /** 持久化一份目录树读授权，之后重启应用仍可浏览。 */
    suspend fun addRoot(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.browsedFoldersStore.edit { preferences ->
                preferences[ROOTS_KEY] = (preferences[ROOTS_KEY] ?: emptySet()) + treeUri.toString()
            }
        }.onFailure { Timber.w(it, "addRoot: cannot persist $treeUri") }
            .isSuccess
    }

    /**
     * 列出 [rootUri] 树下 [documentId] 目录的直接子目录与音频文件。
     *
     * [documentId] 为空串时取目录树自身的根。子目录的歌曲数只数直接子级，
     * 不再递归整棵子树 —— 那样每展开一级就把整棵树读一遍。
     */
    suspend fun children(rootUri: Uri, documentId: String): FolderContent = withContext(Dispatchers.IO) {
        val parentId = documentId.ifEmpty { DocumentsContract.getTreeDocumentId(rootUri) }
        val folders = mutableListOf<FolderItem>()
        val songs = mutableListOf<Song>()

        context.contentResolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, parentId),
            PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val size = cursor.getLong(2)
                val lastModified = cursor.getLong(3)
                val mimeType = cursor.getString(4)
                when {
                    mimeType == DocumentsContract.Document.MIME_TYPE_DIR ->
                        folders += FolderItem(
                            name = name,
                            path = docId,
                            songCount = countAudioFiles(rootUri, docId)
                        )

                    isAudioFileName(name) -> songs += toSong(rootUri, docId, name, size, lastModified)
                }
            }
        }

        FolderContent(
            folders = folders.sortedBy { it.name.lowercase() },
            songs = songs.sortedBy { it.title.lowercase() },
            currentPath = parentId,
            parentPath = parentDocumentId(parentId),
            rootUri = rootUri.toString()
        )
    }

    private fun countAudioFiles(rootUri: Uri, documentId: String): Int = runCatching {
        context.contentResolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, documentId),
            PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            var count = 0
            while (cursor.moveToNext()) {
                if (isAudioFileName(cursor.getString(1) ?: continue)) count++
            }
            count
        } ?: 0
    }.getOrDefault(0)

    private fun rootOf(treeUri: Uri): FolderRoot {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val name = context.contentResolver.query(
            DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId),
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        return FolderRoot(uri = treeUri.toString(), name = name ?: treeUri.lastPathSegment ?: "")
    }

    /**
     * 元数据走 [MediaMetadataRetriever] 而不是引新依赖：平台自带，容错差（标签缺失时
     * 返回 null），但这里只取标题 / 艺人 / 专辑 / 时长四项，缺了就回退到文件名。
     */
    private fun toSong(
        treeUri: Uri,
        documentId: String,
        displayName: String,
        size: Long,
        lastModified: Long
    ): Song {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val uriString = uri.toString()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            Song(
                // 合成 id：不能占用 Room 的正数 id 空间（播放会按 id 回查 Room），用负数哈希。
                id = -abs(uriString.hashCode().toLong()),
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: displayName.substringBeforeLast('.'),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: UNKNOWN,
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: UNKNOWN,
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                path = uriString,
                albumId = 0L,
                artistId = 0L,
                dateAdded = lastModified,
                dateModified = lastModified,
                size = size,
                mimeType = "audio/*",
                // 不抽内嵌封面：加载音频文件当图片靠解码器碰运气，反而不如直接给null落占位图
                albumArt = null
            )
        } catch (e: Exception) {
            Timber.w(e, "toSong: metadata read failed for $uriString")
            Song(
                id = -abs(uriString.hashCode().toLong()),
                title = displayName.substringBeforeLast('.'),
                artist = UNKNOWN,
                album = UNKNOWN,
                duration = 0L,
                path = uriString,
                albumId = 0L,
                artistId = 0L,
                dateAdded = lastModified,
                dateModified = lastModified,
                size = size,
                mimeType = "audio/*"
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private val ROOTS_KEY = stringSetPreferencesKey("folder_roots")
        private const val UNKNOWN = "<unknown>"

        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
    }
}

/**
 * SAF 文档 id 形如 `volume:dir/sub`，父目录就是去掉最后一段。
 * 没有 `/` 的是目录树根本身，返回 null —— 由调用方决定"再往上"去哪里（这里是根列表）。
 */
internal fun parentDocumentId(documentId: String): String? {
    val lastSlash = documentId.lastIndexOf('/')
    return if (lastSlash < 0) null else documentId.substring(0, lastSlash)
}

/** 按扩展名认音频；MediaMetadataRetriever 能解码的子集远大于此，够列即可。 */
internal fun isAudioFileName(fileName: String): Boolean =
    fileName.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "flac", "m4a", "aac", "ogg", "oga", "opus", "wav", "wma", "ape", "wv", "aiff", "aif"
)
