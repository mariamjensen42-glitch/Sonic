package com.cycling.sonic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.data.player.PlayerManager
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 应用级的一次性引导：接上播放器，并在首次拿到媒体权限后把音乐库灌满。
 *
 * 扫描放在这里而不是某个页面里，是因为"第一次打开"没有一个确定的落脚页面 —— 用户可能停在
 * 首页、歌曲页或者别处，无论哪儿都得先把库扫出来给他看。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val songRepository: SongRepository,
    private val scanMusicUseCase: ScanMusicUseCase
) : ViewModel() {

    init {
        viewModelScope.launch {
            playerManager.connect()
        }
    }

    /**
     * 首次启动授权之后跑一次。库里已经有歌就不再扫：升级上来的用户库早就是满的，每次启动都
     * 重扫一遍只是白白读一遍 MediaStore。
     */
    fun scanIfLibraryEmpty() {
        viewModelScope.launch {
            if (songRepository.getSongCount() == 0) scan()
        }
    }

    private suspend fun scan() {
        // 扫不出来不是致命错误 —— 目录是空的、文件全被排除都会走到这里，界面上的空态会说明情况。
        scanMusicUseCase.scan()
            .exceptionOrNull()
            ?.let { Timber.w(it, "MainViewModel: library scan failed") }
    }
}
