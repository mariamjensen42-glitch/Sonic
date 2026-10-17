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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.cycling.core.ui.theme.SonSizes
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.model.AiInfo
import com.cycling.presentation.R
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ai_title),
                        style = MaterialTheme.typography.titleLarge,
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
                        modifier = Modifier.size(SonSizes.iconSmall)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SonSpacing.medium)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(SonSpacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            M3SymbolIcon(
                name = SymbolIconName.AutoAwesome,
                filled = true,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SonSizes.iconExtraLarge)
            )
            Spacer(modifier = Modifier.height(SonSpacing.medium))
            Text(
                text = stringResource(R.string.ai_need_api_key_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = stringResource(R.string.ai_need_api_key_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SonSpacing.large))
            Button(onClick = onNavigateToApiKeyConfig) {
                Text(text = stringResource(R.string.ai_go_to_settings))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(SonSpacing.extraLarge),
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(SonSpacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.ai_error),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(SonSpacing.small))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SonSpacing.large))
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
    }
}

@Composable
private fun InfoContent(
    info: AiInfo
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SonSpacing.mediumSmall)
            .verticalScroll(scrollState)
    ) {
        // 标题卡：抬升以与正文区分层级
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(SonSpacing.medium)) {
                M3SymbolIcon(
                    name = SymbolIconName.AutoAwesome,
                    filled = true,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SonSizes.iconSmall)
                )
                Spacer(modifier = Modifier.height(SonSpacing.small))
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(SonSpacing.medium))

        // 正文用填充卡承载；Markdown 自绘的引用/代码块样式由库决定，此处只统一文字色
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(SonSpacing.medium)) {
                MarkdownText(
                    markdown = info.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(SonSpacing.extraLarge))
    }
}
