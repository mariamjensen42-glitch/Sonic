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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycling.core.ui.components.M3DetailTopAppBar
import com.cycling.core.ui.theme.SpotifyColors
import dev.jeziellago.compose.markdowntext.MarkdownText

private val AiAccent = androidx.compose.ui.graphics.Color(0xFFBB86FC)

@OptIn(ExperimentalMaterial3Api::class)
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
            M3DetailTopAppBar(
                title = "AI 解读",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack,
                actions = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AiAccent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyColors.Background,
                    titleContentColor = SpotifyColors.TextPrimary,
                    navigationIconContentColor = SpotifyColors.TextPrimary
                )
            )
        },
        containerColor = SpotifyColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AiAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "需要配置 API Key",
                color = SpotifyColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先在设置中配置 DeepSeek API Key 以使用 AI 功能",
                color = SpotifyColors.TextTertiary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateToApiKeyConfig) {
                Text("前往配置", color = AiAccent)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AiAccent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AI 正在分析...",
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "获取失败",
                color = Color(0xFFFF6B6B),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = SpotifyColors.TextTertiary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = AiAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试", color = AiAccent)
            }
        }
    }
}

@Composable
private fun InfoContent(
    info: com.cycling.domain.model.AiInfo
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // Title card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 16.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AiAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = info.title,
                    color = SpotifyColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Content
        MarkdownText(
            markdown = info.content,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = SpotifyColors.TextSecondary,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
