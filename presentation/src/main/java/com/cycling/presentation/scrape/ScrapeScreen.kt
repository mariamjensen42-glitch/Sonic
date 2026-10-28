@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.scrape

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleDims
import com.cycling.core.ui.apple.AppleSheet
import com.cycling.core.ui.apple.AppleSheetRow
import com.cycling.core.ui.apple.AppleSheetTitle
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3Artwork
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeField
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import com.cycling.presentation.components.SonLoadingState

/**
 * 元数据刮削：列出元数据不全的本地歌曲，用在线目录把它们补齐。
 *
 * 两种用法共用一套判定：
 * - 顶部「全部刮削」= 自动模式，只应用可信匹配，遇到近似结果标成待确认；
 * - 点某一行 = 手动模式，在候选面板里自己挑。
 *
 * 一眼能看到的三个事实必须区分：**没找到**（换关键词也没用，音源里没有）、
 * **待确认**（有结果但不敢自动写）、**只在本 App 生效**（文件不可写，见 `scrape_row_app_only`）。
 */
@Composable
fun ScrapeScreen(
    viewModel: ScrapeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ScrapeEffect.NavigateBack -> onNavigateBack()
                is ScrapeEffect.ShowToast -> Toast.makeText(
                    context,
                    context.getString(effect.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.scrape_title))
                        if (uiState.isScrapingAll) {
                            Text(
                                text = stringResource(
                                    R.string.scrape_progress,
                                    uiState.processed,
                                    uiState.total
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.handleIntent(ScrapeIntent.NavigateBack) }
                    ) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            contentDescription = stringResource(R.string.scrape_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isScrapingAll) {
                        TextButton(
                            onClick = { viewModel.handleIntent(ScrapeIntent.CancelScrapeAll) }
                        ) {
                            Text(stringResource(R.string.scrape_stop))
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.handleIntent(ScrapeIntent.ScrapeAll) }
                        ) {
                            Text(stringResource(R.string.scrape_all))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + SonLayout.bottomBarClearance
        )
        when {
            uiState.isLoading -> SonLoadingState()

            uiState.rows.isEmpty() -> SonEmptyState(
                icon = SymbolIconName.MusicNote,
                title = stringResource(R.string.scrape_empty_title),
                subtitle = stringResource(R.string.scrape_empty_subtitle)
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                ScrapeFilters(
                    uiState = uiState,
                    onIncludeComplete = {
                        viewModel.handleIntent(ScrapeIntent.SetIncludeComplete(it))
                    },
                    onHideDone = { viewModel.handleIntent(ScrapeIntent.SetHideDone(it)) }
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    items(uiState.visibleRows, key = { it.id }) { row ->
                        ScrapeRowItem(
                            row = row,
                            onClick = {
                                viewModel.handleIntent(ScrapeIntent.OpenCandidates(row.id))
                            },
                            onRetry = { viewModel.handleIntent(ScrapeIntent.ScrapeOne(row.id)) }
                        )
                    }
                }
            }
        }
    }

    val sheetSongId = uiState.openSheetFor
    if (sheetSongId != null) {
        ScrapeCandidatesSheet(
            candidates = uiState.sheetCandidates,
            isLoading = uiState.isLoadingCandidates,
            onSelect = { candidateId ->
                viewModel.handleIntent(ScrapeIntent.ApplyCandidate(sheetSongId, candidateId))
            },
            onDismiss = { viewModel.handleIntent(ScrapeIntent.DismissCandidates) }
        )
    }
}

@Composable
private fun ScrapeFilters(
    uiState: ScrapeUiState,
    onIncludeComplete: (Boolean) -> Unit,
    onHideDone: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SonSizes.listItemHorizontalPadding,
                end = SonSizes.listItemHorizontalPadding,
                top = SonSpacing.small
            )
    ) {
        Text(
            text = stringResource(
                R.string.scrape_summary,
                uiState.pendingCount,
                uiState.doneCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)) {
            FilterChip(
                selected = uiState.includeComplete,
                onClick = { onIncludeComplete(!uiState.includeComplete) },
                label = { Text(stringResource(R.string.scrape_include_complete)) }
            )
            FilterChip(
                selected = uiState.hideDone,
                onClick = { onHideDone(!uiState.hideDone) },
                label = { Text(stringResource(R.string.scrape_hide_done)) }
            )
        }
    }
}

@Composable
private fun ScrapeRowItem(
    row: ScrapeRow,
    onClick: () -> Unit,
    onRetry: () -> Unit
) {
    ListItem(
        // 整行可点：即使音源没给出可信结果，用户也总能展开候选自己挑一个。
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            M3Artwork(
                artwork = row.song.albumArt,
                shape = MaterialTheme.shapes.small,
                contentDescription = null,
                modifier = Modifier.size(SonSizes.thumbnail)
            )
        },
        headlineContent = {
            Text(text = row.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = supportingText(row),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (row.status) {
                    ScrapeStatus.SEARCHING, ScrapeStatus.APPLYING -> {
                        LoadingIndicator(modifier = Modifier.size(SonSizes.iconSmall))
                    }

                    ScrapeStatus.DONE -> M3SymbolIcon(
                        name = SymbolIconName.Check,
                        contentDescription = stringResource(R.string.scrape_row_done),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    ScrapeStatus.READY -> TextButton(onClick = onClick) {
                        Text(stringResource(R.string.scrape_action_confirm))
                    }

                    ScrapeStatus.IDLE -> IconButton(onClick = onRetry) {
                        M3SymbolIcon(
                            name = SymbolIconName.Search,
                            contentDescription = stringResource(R.string.scrape_action_scrape),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    ScrapeStatus.NOT_FOUND, ScrapeStatus.FAILED -> IconButton(onClick = onRetry) {
                        M3SymbolIcon(
                            name = SymbolIconName.Refresh,
                            contentDescription = stringResource(R.string.scrape_action_retry),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

/** 行副标题：缺什么 + 当前状态。两者都有信息量，合成一行省高度。 */
@Composable
private fun supportingText(row: ScrapeRow): String {
    // 缺什么：`map` 是 inline 的，可以在这里读 stringResource。
    val missingLabels = row.missing.map { stringResource(it.labelRes()) }
    val missingText = if (missingLabels.isEmpty()) {
        "${row.song.artist} · ${row.song.album}"
    } else {
        stringResource(R.string.scrape_row_missing, missingLabels.joinToString(" · "))
    }

    val statusText = when (row.status) {
        ScrapeStatus.SEARCHING -> stringResource(R.string.scrape_status_searching)
        ScrapeStatus.APPLYING -> stringResource(R.string.scrape_status_applying)
        ScrapeStatus.NOT_FOUND -> stringResource(R.string.scrape_row_not_found)
        ScrapeStatus.DONE -> row.matched?.title
            ?.let { stringResource(R.string.scrape_row_matched, it) }
            .orEmpty()

        ScrapeStatus.READY, ScrapeStatus.FAILED -> row.message?.let { stringResource(it) }.orEmpty()
        ScrapeStatus.IDLE -> ""
    }

    // 「已补全」后面可能还要接一句「文件不可写」，所以 message 单独成一节。
    val note = row.message
        ?.takeIf { row.status == ScrapeStatus.DONE }
        ?.let { stringResource(it) }
        .orEmpty()

    return listOf(missingText, statusText, note)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}

private fun ScrapeField.labelRes(): Int = when (this) {
    ScrapeField.TITLE -> R.string.scrape_field_title
    ScrapeField.ARTIST -> R.string.scrape_field_artist
    ScrapeField.ALBUM -> R.string.scrape_field_album
}

@Composable
private fun ScrapeCandidatesSheet(
    candidates: List<CoverCandidate>,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppleSheet(onDismiss = onDismiss) {
        AppleSheetTitle(
            title = stringResource(R.string.scrape_sheet_title),
            subtitle = stringResource(R.string.scrape_sheet_hint)
        )

        when {
            isLoading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SonSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
            ) {
                LoadingIndicator(modifier = Modifier.size(SonSizes.iconSmall))
                Text(
                    text = stringResource(R.string.scrape_sheet_loading),
                    style = AppleType.footnote,
                    color = AppleTheme.colors.secondaryLabel
                )
            }

            candidates.isEmpty() -> Text(
                text = stringResource(R.string.scrape_sheet_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SonSpacing.large),
                style = AppleType.footnote,
                color = AppleTheme.colors.secondaryLabel
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                items(candidates, key = { it.id }) { candidate ->
                    AppleSheetRow(
                        title = candidate.title ?: candidate.album.orEmpty(),
                        subtitle = listOfNotNull(candidate.artist, candidate.album, candidate.year)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        onClick = { onSelect(candidate.id) },
                        leading = {
                            M3Artwork(
                                artwork = candidate.imageUrl,
                                placeholderIcon = SymbolIconName.Album,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(AppleDims.listItemHeight)
                            )
                        },
                        trailing = {
                            if (candidate.isBestMatch) {
                                Text(
                                    text = stringResource(R.string.scrape_sheet_best),
                                    style = AppleType.caption1Emphasized,
                                    color = AppleTheme.colors.key
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
