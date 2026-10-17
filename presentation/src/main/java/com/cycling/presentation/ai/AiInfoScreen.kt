@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.ai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
import com.cycling.core.ui.theme.SonLayout
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AiInfo
import com.cycling.presentation.R
import com.cycling.presentation.components.SonEmptyState
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun AiInfoScreen(
    type: String,
    name: String,
    artist: String,
    onNavigateBack: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit = {},
    viewModel: AiInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(type, name, artist) {
        when (type) {
            "artist" -> viewModel.handleIntent(AiInfoIntent.LoadArtistInfo(name))
            "album" -> viewModel.handleIntent(AiInfoIntent.LoadAlbumInfo(name, artist))
            "song" -> viewModel.handleIntent(AiInfoIntent.LoadSongInfo(name, artist))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // 阅读页的标题时刻：AI 解读是标题，被解读的对象是副标题。
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ai_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text(
                        text = if (artist.isBlank()) name else "$name · $artist",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        M3SymbolIcon(
                            name = SymbolIconName.ArrowBack,
                            filled = true,
                            contentDescription = stringResource(R.string.ai_back_desc)
                        )
                    }
                },
                actions = {
                    M3SymbolIcon(
                        name = SymbolIconName.AutoAwesome,
                        filled = true,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = SonSpacing.medium)
                            .size(SonSizes.iconSmall)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SonLayout.screenMargin)
        ) {
            when {
                !uiState.hasApiKey -> {
                    ApiKeyRequiredContent(
                        onNavigateToApiKeyConfig = onNavigateToApiKeyConfig
                    )
                }
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    val error = uiState.error
                    ErrorContent(
                        error = error!!,
                        onRetry = { viewModel.handleIntent(AiInfoIntent.Retry) }
                    )
                }
                uiState.info != null -> {
                    InfoContent(
                        info = uiState.info!!
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyRequiredContent(
    onNavigateToApiKeyConfig: () -> Unit
) {
    SonEmptyState(
        icon = SymbolIconName.AutoAwesome,
        title = stringResource(R.string.ai_need_api_key_title),
        subtitle = stringResource(R.string.ai_need_api_key_desc),
        actionLabel = stringResource(R.string.ai_go_to_settings),
        onAction = onNavigateToApiKeyConfig
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator()
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            Text(
                text = stringResource(R.string.ai_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    SonEmptyState(
        icon = SymbolIconName.Error,
        title = stringResource(R.string.ai_error),
        subtitle = error,
        action = {
            OutlinedButton(onClick = onRetry) {
                M3SymbolIcon(
                    name = SymbolIconName.Refresh,
                    filled = true,
                    contentDescription = null,
                    modifier = Modifier.size(SonSizes.buttonIcon)
                )
                Spacer(modifier = Modifier.width(SonSpacing.small))
                Text(text = stringResource(R.string.ai_retry))
            }
        }
    )
}

/**
 * 阅读面：不再用两层卡片把正文包起来。标题走 headline 强调变体，正文直接落在 surface 上，
 * 靠字阶与留白分层；这是本屏的 quiet surface，色面由顶栏的封面主题提供。
 */
@Composable
private fun InfoContent(
    info: AiInfo
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(SonSpacing.large))

        Text(
            text = info.title,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        MarkdownText(
            markdown = info.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(SonSpacing.extraExtraLarge))
    }
}
