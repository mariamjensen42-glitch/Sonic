package com.cycling.sonic.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.data.player.PlayerManager
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 应用级的一次性引导：接上播放器，并在首次拿到媒体权限后把音乐库灌满。
 *
 * 扫描放在这里而不是某个页面里，是因为"第一次打开"没有一个确定的落脚页面 —— 用户可能停在
 * 首页、歌曲页或者别处，无论哪儿都得先把库扫出来给他看。
 *
 * 之后每次回到前台还会做一次**增量刷新**（[refreshLibraryOnForeground]），外部拷进来的歌、
 * 别处删掉的歌都不需要用户自己去设置里再按一次扫描。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val songRepository: SongRepository,
    private val scanMusicUseCase: ScanMusicUseCase
) : ViewModel() {

    /** 正在跑的那次扫描；同一时刻只允许一次，两次扫描互相剪枝会把库扫乱。 */
    private var scanJob: Job? = null

    /** 上次真正开始扫描的时刻，用来做前台刷新的节流。 */
    private var lastScanAtMs = 0L

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
        startScan(onlyWhenEmpty = true)
    }

    /**
     * 回到前台时增量刷新。
     *
     * 走的是和手动扫描同一条路径（`SongLibraryWriter`：幂等 upsert + 只剪掉真正消失的文件），
     * 所以频繁调用是安全的；节流只是为了不必每次切回前台都全量读一遍 MediaStore。
     * 没有媒体权限时**不能**进来 —— 那时 MediaStore 查不到东西，剪枝会把整个库清空。
     */
    fun refreshLibraryOnForeground() {
        startScan(onlyWhenEmpty = false)
    }

    private fun startScan(onlyWhenEmpty: Boolean) {
        if (scanJob?.isActive == true) return
        if (SystemClock.elapsedRealtime() - lastScanAtMs < MIN_REFRESH_INTERVAL_MS) return

        scanJob = viewModelScope.launch {
            if (onlyWhenEmpty && songRepository.getSongCount() != 0) return@launch
            lastScanAtMs = SystemClock.elapsedRealtime()
            // 扫不出来不是致命错误 —— 目录是空的、文件全被排除都会走到这里，界面上的空态会说明情况。
            scanMusicUseCase.scan()
                .exceptionOrNull()
                ?.let { Timber.w(it, "MainViewModel: library scan failed") }
        }
    }

    private companion object {
        /**
         * 两次自动扫描之间的最短间隔。
         *
         * 冷启动那一次通常不会被这条挡住（`lastScanAtMs` 还是 0），挡住的是"切出去接个通知
         * 再切回来"这种几分钟内反复回前台的情形。
         */
        const val MIN_REFRESH_INTERVAL_MS = 60_000L
    }
}
