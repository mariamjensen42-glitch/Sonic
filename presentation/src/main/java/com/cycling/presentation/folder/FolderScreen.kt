@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.folder

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.appleBottomBarClearance
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleChip
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.apple.AppleTopBar
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.FolderItem
import com.cycling.domain.model.FolderRoot
import com.cycling.domain.model.Song
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState
import com.cycling.presentation.components.SongListItem
import com.cycling.presentation.components.formatDuration

@Composable
fun FolderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long, List<Song>) -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SAF 选目录：权限由用户逐棵授予并持久化，浏览全程不需要任何存储权限
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleIntent(FolderIntent.AddRoot(it.toString())) }
    }

    // 树内/层级内先退回上一级，退出屏幕交给导航
    BackHandler(enabled = !uiState.atListRoot) {
        viewModel.handleIntent(FolderIntent.NavigateBack)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FolderEffect.NavigateToPlayer -> {
                    onNavigateToPlayer(effect.songId, effect.songs)
                }
                is FolderEffect.ShowToast -> {
                    Toast.makeText(context, context.getString(effect.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppleTheme.colors.background,
        topBar = {
            AppleTopBar(
                title = uiState.title,
                subtitle = stringResource(R.string.folder_song_count, uiState.songs.size),
                onNavigateBack = {
                    if (uiState.atListRoot) onNavigateBack()
                    else viewModel.handleIntent(FolderIntent.NavigateBack)
                },
                backContentDescription = stringResource(R.string.folder_back_desc),
                actions = {
                    // 「+ 选目录」只属于本机文件夹（SAF）来源
                    if (uiState.source == FolderSource.DEVICE_FILES) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            M3SymbolIcon(
                                name = SymbolIconName.Add,
                                filled = true,
                                contentDescription = stringResource(R.string.settings_add_folder_desc)
                            )
                    }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SourceSwitcher(
                source = uiState.source,
                onSelect = { viewModel.handleIntent(FolderIntent.SelectSource(it)) }
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> SonLoadingState()
                    uiState.atSafRootList -> {
                        if (uiState.roots.isEmpty()) {
                            SonEmptyState(
                                icon = SymbolIconName.Folder,
                                title = stringResource(R.string.folder_roots_empty_title),
                                subtitle = stringResource(R.string.folder_roots_empty_subtitle)
                            )
                        } else {
                            RootList(
                                roots = uiState.roots,
                                bottomPadding = bottomPadding,
                                onRootClick = { root ->
                                    viewModel.handleIntent(FolderIntent.NavigateToFolder(root.uri, ""))
                                }
                            )
                        }
                    }
                    uiState.folders.isEmpty() && uiState.songs.isEmpty() -> SonEmptyState(
                        icon = SymbolIconName.Folder,
                        title = stringResource(R.string.folder_empty_title),
                        subtitle = stringResource(R.string.folder_empty_subtitle)
                    )
                    else -> FolderContentList(
                        uiState = uiState,
                        viewModel = viewModel,
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

/** 两种浏览来源的分段切换：媒体库（扫描产物）/ 本机文件夹（SAF 直读）。 */
@Composable
private fun SourceSwitcher(
    source: FolderSource,
    onSelect: (FolderSource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppleChip(
            label = stringResource(R.string.folder_source_library),
            selected = source == FolderSource.LIBRARY,
            onClick = { onSelect(FolderSource.LIBRARY) }
        )
        AppleChip(
            label = stringResource(R.string.folder_source_device),
            selected = source == FolderSource.DEVICE_FILES,
            onClick = { onSelect(FolderSource.DEVICE_FILES) }
        )
    }
}

@Composable
private fun RootList(
    roots: List<FolderRoot>,
    bottomPadding: Dp,
    onRootClick: (FolderRoot) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = SonLayout.contentTopPadding,
            bottom = appleBottomBarClearance() + bottomPadding
        )
    ) {
        items(roots, key = { it.uri }) { root ->
            AppleSheetRow(
                title = root.name,
                onClick = { onRootClick(root) },
                leading = {
                    M3SymbolIcon(
                        name = SymbolIconName.Folder,
                        filled = true,
                        contentDescription = null,
                        tint = AppleTheme.colors.systemRed,
                        size = 20.dp
                    )
                }
            )
        }
    }
}

@Composable
private fun FolderContentList(
    uiState: FolderUiState,
    viewModel: FolderViewModel,
    bottomPadding: Dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = SonLayout.contentTopPadding,
            bottom = appleBottomBarClearance() + bottomPadding
        )
    ) {
        items(uiState.folders, key = { it.path }) { folder ->
            FolderListItem(
                folder = folder,
                onClick = {
                    // 两种来源的「路径」含义不同：媒体库是文件路径，SAF 是 document id
                    if (uiState.source == FolderSource.LIBRARY) {
                        viewModel.handleIntent(FolderIntent.NavigateToLibraryFolder(folder.path))
                    } else {
                        viewModel.handleIntent(
                            FolderIntent.NavigateToFolder(uiState.currentRootUri, folder.path)
                        )
                    }
                }
            )
        }

        items(uiState.songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                // quiet surface：文件夹列表同样不画分割线。
                showDivider = false,
                subtitle = "${song.artist} · ${formatDuration(song.duration)}",
                showDuration = false,
                showMoreButton = false,
                onClick = {
                    viewModel.handleIntent(FolderIntent.SongClick(song))
                }
            )
        }
    }
}

@Composable
private fun FolderListItem(
    folder: FolderItem,
    onClick: () -> Unit
) {
    AppleSheetRow(
        title = folder.name,
        onClick = onClick,
        leading = {
            M3SymbolIcon(
                name = SymbolIconName.Folder,
                filled = true,
                contentDescription = null,
                tint = AppleTheme.colors.systemRed,
                size = 20.dp
            )
        },
        trailing = {
            Text(
                text = stringResource(R.string.folder_song_count, folder.songCount),
                style = AppleType.caption1,
                color = AppleTheme.colors.secondaryLabel
            )
        }
    )
}
