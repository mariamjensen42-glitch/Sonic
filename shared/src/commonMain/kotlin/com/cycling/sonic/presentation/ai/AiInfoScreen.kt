package com.cycling.sonic.presentation.ai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.cycling.sonic.core.ui.components.M3Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycling.sonic.core.ui.components.M3CircularProgressIndicator
import com.cycling.sonic.core.ui.components.M3DetailTopAppBar
import com.cycling.sonic.core.ui.theme.SpotifyColors

@Composable
fun AiInfoScreen(
    viewModel: AiInfoViewModel,
    type: String, name: String, artist: String,
    onNavigateBack: () -> Unit,
    onNavigateToApiKeyConfig: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AiInfoEffect.NavigateToApiKeyConfig -> onNavigateToApiKeyConfig()
            }
        }
    }

    M3Scaffold(
        topBar = {
            M3DetailTopAppBar(
                title = "AI 解读",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { M3CircularProgressIndicator() }
                uiState.error != null -> {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                        Text(uiState.error ?: "", color = SpotifyColors.TextSecondary, fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        if (uiState.error == "需要配置 API Key") {
                            Button(onClick = onNavigateToApiKeyConfig) { Text("前往配置") }
                        } else {
                            Button(onClick = { viewModel.handleIntent(AiInfoIntent.Retry) }) { Text("重试") }
                        }
                    }
                }
                else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(name, color = SpotifyColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(uiState.content, color = SpotifyColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
