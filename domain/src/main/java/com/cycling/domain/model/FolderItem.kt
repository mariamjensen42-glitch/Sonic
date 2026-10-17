package com.cycling.domain.model

data class FolderItem(
    val name: String,
    val path: String,
    val songCount: Int = 0
)

/**
 * 用户通过 SAF 授权的一个浏览目录树。
 *
 * [uri] 是 `ACTION_OPEN_DOCUMENT_TREE` 返回并持久化的 tree uri，浏览全程以它定位；
 * 不再依赖 MediaStore 扫描，未入库的文件同样能看、能播。
 */
data class FolderRoot(
    val uri: String,
    val name: String
)

data class FolderContent(
    val folders: List<FolderItem>,
    val songs: List<Song>,
    val currentPath: String,
    val parentPath: String? = null,
    /** 本内容所属目录树的 tree uri；为空串表示当前在根列表（未进入任何目录树）。 */
    val rootUri: String = ""
)
