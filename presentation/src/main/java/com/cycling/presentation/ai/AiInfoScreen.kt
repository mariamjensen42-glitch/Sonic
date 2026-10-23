@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cycling.presentation.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.apple.AppleActionButton
import com.cycling.core.ui.apple.AppleTheme
import com.cycling.core.ui.apple.AppleTopBar
import com.cycling.core.ui.apple.AppleType
import com.cycling.core.ui.components.M3SymbolIcon
import com.cycling.core.ui.components.SymbolIconName
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
        containerColor = AppleTheme.colors.background,
        topBar = {
            // 阅读页的标题时刻：AI 解读是标题，被解读的对象是副标题。
            AppleTopBar(
                title = stringResource(R.string.ai_title),
                subtitle = if (artist.isBlank()) name else "$name · $artist",
                onNavigateBack = onNavigateBack,
                backContentDescription = stringResource(R.string.ai_back_desc),
                actions = {
                    M3SymbolIcon(
                        name = SymbolIconName.AutoAwesome,
                        filled = true,
                        contentDescription = null,
                        tint = AppleTheme.colors.systemRed,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(22.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            when {
                !uiState.hasApiKey -> ApiKeyRequiredContent(onNavigateToApiKeyConfig)
                uiState.isLoading -> LoadingContent()
                uiState.error != null -> {
                    val error = uiState.error
                    ErrorContent(
                        error = error!!,
                        onRetry = { viewModel.handleIntent(AiInfoIntent.Retry) }
                    )
                }
                uiState.info != null -> InfoContent(info = uiState.info!!)
            }
        }
    }
}

@Composable
private fun ApiKeyRequiredContent(
    onNavigateToApiKeyConfig: () -> Unit
) {
    CenteredMessage(
        icon = SymbolIconName.Key,
        title = stringResource(R.string.ai_need_api_key_title),
        subtitle = stringResource(R.string.ai_need_api_key_desc),
        actionLabel = stringResource(R.string.ai_go_to_settings),
        onAction = onNavigateToApiKeyConfig
    )
}

@Composable
private fun LoadingContent() {
    CenteredMessage(
        icon = SymbolIconName.AutoAwesome,
        title = stringResource(R.string.ai_loading),
        subtitle = null,
        loading = true
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    CenteredMessage(
        icon = SymbolIconName.Error,
        title = stringResource(R.string.ai_error),
        subtitle = error,
        actionLabel = stringResource(R.string.ai_retry),
        onAction = onRetry
    )
}

/** 空态 / 加载态 / 错误态共用的居中块。 */
@Composable
private fun CenteredMessage(
    icon: SymbolIconName,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (loading) {
            LoadingIndicator()
        } else {
            M3SymbolIcon(
                name = icon,
                contentDescription = null,
                filled = true,
                tint = colors.tertiaryLabel,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = AppleType.headline,
            color = colors.label,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = AppleType.footnote,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            AppleActionButton(label = actionLabel, onClick = onAction, filled = false)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * 阅读面：正文直接落在 background 上，靠字阶与留白分层。
 */
@Composable
private fun InfoContent(info: AiInfo) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = info.title,
            style = AppleType.title2Emphasized,
            color = AppleTheme.colors.label
        )

        Spacer(modifier = Modifier.height(12.dp))

        MarkdownText(
            markdown = info.content,
            style = AppleType.body.copy(color = AppleTheme.colors.secondaryLabel)
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}
