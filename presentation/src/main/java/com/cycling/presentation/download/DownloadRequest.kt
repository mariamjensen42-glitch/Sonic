package com.cycling.presentation.download

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.Song
import com.cycling.domain.online.model.MusicQuality
import com.cycling.core.ui.theme.SonSpacing
import com.cycling.domain.repository.DownloadRepository
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 全局的「下载这首歌」入口。
 *
 * 用 CompositionLocal 而不是给每个页面加参数：歌曲菜单在十几个页面里都有，
 * 逐个接线必然会漂移成「有的页面能下载、有的不能」。宿主在 AppNavGraph 根部装一次，
 * 菜单直接读这个 Local。
 */
val LocalDownloadRequester = staticCompositionLocalOf<((Song) -> Unit)?> { null }

/** 可下载的档位：插件专属档位（鲸云/全景声）不走离线，列出来只会误导。 */
private val DOWNLOADABLE_QUALITIES = listOf(
    MusicQuality.STANDARD,
    MusicQuality.EXHIGH,
    MusicQuality.LOSSLESS,
    MusicQuality.HIRES
)

data class DownloadRequestState(
    val pendingSong: Song? = null,
    val quality: MusicQuality = MusicQuality.EXHIGH
)

@HiltViewModel
class DownloadRequestViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadRequestState())
    val uiState: StateFlow<DownloadRequestState> = _uiState.asStateFlow()

    /** 正在入队；防止连点两次插两条。 */
    private var enqueueing = false

    fun request(song: Song) {
        if (!song.isOnline) return
        _uiState.update { it.copy(pendingSong = song) }
    }

    fun selectQuality(quality: MusicQuality) {
        _uiState.update { it.copy(quality = quality) }
    }

    fun dismiss() {
        _uiState.update { it.copy(pendingSong = null) }
    }

    /** @return 入队成功时为 true，宿主据此弹提示。 */
    fun confirm(onResult: (Boolean) -> Unit) {
        val state = _uiState.value
        val song = state.pendingSong ?: return
        if (enqueueing) return
        enqueueing = true
        viewModelScope.launch {
            val ok = runCatching { downloadRepository.enqueue(song, state.quality) }.isSuccess
            enqueueing = false
            _uiState.update { it.copy(pendingSong = null) }
            onResult(ok)
        }
    }
}

/**
 * 装在导航壳根部：提供 [LocalDownloadRequester]，并托管「选择音质」弹层。
 *
 * 弹层挂在根部而不是某个页面：用户可以在任何页面点下载，页面被 push/pop 时弹层不该跟着消失。
 */
@Composable
fun DownloadRequestProvider(content: @Composable () -> Unit) {
    val viewModel: DownloadRequestViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val request: (Song) -> Unit = remember(viewModel) { { viewModel.request(it) } }

    CompositionLocalProvider(LocalDownloadRequester provides request) {
        content()
    }

    val song = state.pendingSong
    if (song != null) {
        DownloadQualityDialog(
            song = song,
            selected = state.quality,
            onSelect = viewModel::selectQuality,
            onDismiss = viewModel::dismiss,
            onConfirm = {
                viewModel.confirm { ok ->
                    Toast.makeText(
                        context,
                        context.getString(
                            if (ok) R.string.download_enqueued else R.string.download_enqueue_failed
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
private fun DownloadQualityDialog(
    song: Song,
    selected: MusicQuality,
    onSelect: (MusicQuality) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_quality_title)) },
        text = {
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.download_quality_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SonSpacing.small)
                )
                DOWNLOADABLE_QUALITIES.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                            .padding(vertical = SonSpacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SonSpacing.small)
                    ) {
                        RadioButton(selected = quality == selected, onClick = { onSelect(quality) })
                        Text(text = quality.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.download_quality_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.download_quality_cancel))
            }
        }
    )
}
