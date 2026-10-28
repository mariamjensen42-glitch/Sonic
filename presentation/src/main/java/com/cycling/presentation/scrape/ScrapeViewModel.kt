package com.cycling.presentation.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeOutcome
import com.cycling.domain.model.Song
import com.cycling.domain.repository.MetadataScraperRepository
import com.cycling.domain.scrape.ScrapeMatch
import com.cycling.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 在线刮削。
 *
 * 两条路都走同一套判定（[ScrapeMatch]）：
 * - **全自动**：只应用「可信」的候选，宁可留一条待确认，也不把整库标题改成同名翻唱；
 * - **手动**：点某一行展开候选，用户自己挑，挑谁应用谁。
 *
 * 应用是**逐条串行**的：封面要下载、标签可能还要重写文件，并发只会抢带宽并把进度弄得看不懂。
 */
@HiltViewModel
class ScrapeViewModel @Inject constructor(
    private val scraper: MetadataScraperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScrapeUiState())
    val uiState: StateFlow<ScrapeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ScrapeEffect>()
    val uiEffect: SharedFlow<ScrapeEffect> = _uiEffect.asSharedFlow()

    private var scrapeAllJob: Job? = null

    init {
        reload()
    }

    fun handleIntent(intent: ScrapeIntent) {
        when (intent) {
            ScrapeIntent.NavigateBack -> viewModelScope.launch {
                if (_uiState.value.isScrapingAll) {
                    scrapeAllJob?.cancel()
                }
                _uiEffect.emit(ScrapeEffect.NavigateBack)
            }

            ScrapeIntent.Reload -> reload()

            is ScrapeIntent.SetIncludeComplete -> {
                _uiState.update { it.copy(includeComplete = intent.value) }
                reload()
            }

            is ScrapeIntent.SetHideDone -> _uiState.update { it.copy(hideDone = intent.value) }

            ScrapeIntent.ScrapeAll -> startScrapeAll()

            ScrapeIntent.CancelScrapeAll -> {
                scrapeAllJob?.cancel()
                _uiState.update { it.copy(isScrapingAll = false) }
            }

            is ScrapeIntent.ScrapeOne -> viewModelScope.launch {
                val row = rowOf(intent.songId) ?: return@launch
                val applied = scrapeAndApply(row.song.id, autoApply = true)
                if (!applied) {
                    _uiEffect.emit(ScrapeEffect.ShowToast(R.string.scrape_toast_needs_confirm))
                }
            }

            is ScrapeIntent.OpenCandidates -> openCandidates(intent.songId)

            ScrapeIntent.DismissCandidates -> _uiState.update {
                it.copy(openSheetFor = null, sheetCandidates = emptyList(), isLoadingCandidates = false)
            }

            is ScrapeIntent.ApplyCandidate -> viewModelScope.launch {
                applyCandidate(intent.songId, intent.candidateId)
            }
        }
    }

    /** 重新列一遍待刮削的歌。已有的行状态不保留：列表可能整批换掉，保留反而对不上号。 */
    private fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val includeComplete = _uiState.value.includeComplete
            val targets = if (includeComplete) {
                scraper.findAllLocal()
            } else {
                scraper.findIncomplete()
            }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    rows = targets.map { ScrapeRow(song = it.song, missing = it.missing) },
                    processed = 0,
                    total = 0
                )
            }
        }
    }

    private fun startScrapeAll() {
        if (_uiState.value.isScrapingAll) return
        scrapeAllJob = viewModelScope.launch {
            val pending = _uiState.value.rows
                .filter { it.status == ScrapeStatus.IDLE || it.status == ScrapeStatus.FAILED }
                .map { it.id }
            if (pending.isEmpty()) {
                _uiEffect.emit(ScrapeEffect.ShowToast(R.string.scrape_toast_nothing_pending))
                return@launch
            }

            _uiState.update { it.copy(isScrapingAll = true, processed = 0, total = pending.size) }
            for ((index, songId) in pending.withIndex()) {
                if (!isActive) break
                scrapeAndApply(songId, autoApply = true)
                _uiState.update { it.copy(processed = index + 1) }
            }
            _uiState.update { it.copy(isScrapingAll = false) }
            if (isActive) {
                _uiEffect.emit(ScrapeEffect.ShowToast(R.string.scrape_toast_finished))
            }
        }
    }

    /**
     * 搜一次并尝试落库。
     *
     * @return true 表示真的应用了（含「应用了但文件写不进去」）；false 表示没找到可信结果。
     */
    private suspend fun scrapeAndApply(songId: Long, autoApply: Boolean): Boolean {
        val row = rowOf(songId) ?: return false
        updateRow(songId) { it.copy(status = ScrapeStatus.SEARCHING, message = null) }

        val candidates = scraper.candidates(row.song)
        if (candidates.isEmpty()) {
            updateRow(songId) { it.copy(status = ScrapeStatus.NOT_FOUND) }
            return false
        }

        val best = candidates.first()
        if (!autoApply || !ScrapeMatch.isConfident(row.song, best)) {
            // 找不到可信结果时把候选一起留下：用户点一下就能挑，不用再等一次网络。
            updateRow(songId) {
                it.copy(
                    status = ScrapeStatus.READY,
                    candidates = candidates,
                    matched = best,
                    message = R.string.scrape_row_needs_confirm
                )
            }
            return false
        }

        applyOutcome(songId, best, applySafe(row.song, best))
        return true
    }

    private fun openCandidates(songId: Long) {
        val row = rowOf(songId) ?: return
        _uiState.update {
            it.copy(
                openSheetFor = songId,
                sheetCandidates = row.candidates,
                isLoadingCandidates = row.candidates.isEmpty()
            )
        }
        if (row.candidates.isNotEmpty()) return

        viewModelScope.launch {
            val found = scraper.candidates(row.song)
            updateRow(songId) { it.copy(candidates = found) }
            _uiState.update { state ->
                if (state.openSheetFor != songId) return@update state
                state.copy(sheetCandidates = found, isLoadingCandidates = false)
            }
        }
    }

    private suspend fun applyCandidate(songId: Long, candidateId: String) {
        val row = rowOf(songId) ?: return
        val candidate = (row.candidates + _uiState.value.sheetCandidates)
            .firstOrNull { it.id == candidateId } ?: return
        _uiState.update { it.copy(openSheetFor = null, sheetCandidates = emptyList()) }
        updateRow(songId) { it.copy(status = ScrapeStatus.APPLYING) }
        applyOutcome(songId, candidate, applySafe(row.song, candidate))
        _uiEffect.emit(ScrapeEffect.ShowToast(R.string.scrape_toast_applied))
    }

    private fun applyOutcome(songId: Long, candidate: CoverCandidate, outcome: ScrapeOutcome?) {
        if (outcome == null || outcome.isEmpty) {
            Timber.w("Scrape: 应用失败 songId=%d", songId)
            updateRow(songId) { it.copy(status = ScrapeStatus.FAILED) }
            return
        }
        updateRow(songId) { row ->
            row.copy(
                song = row.song.copy(
                    title = candidate.title?.trim()?.takeIf { it.isNotEmpty() } ?: row.song.title,
                    artist = candidate.artist?.trim()?.takeIf { it.isNotEmpty() } ?: row.song.artist,
                    album = candidate.album?.trim()?.takeIf { it.isNotEmpty() } ?: row.song.album,
                    albumArt = outcome.newAlbumArt ?: row.song.albumArt,
                    hasCustomAlbumArt = row.song.hasCustomAlbumArt || outcome.coverApplied
                ),
                missing = emptySet(),
                status = ScrapeStatus.DONE,
                matched = candidate,
                // 文件写不进去时如实标注：元数据只在这个 App 里生效。
                message = if (outcome.fileTagsWritten) null else R.string.scrape_row_app_only
            )
        }
    }

    /** `runCatching` 会把协程取消也吞掉，批量刮削按「停止」时就停不下来，所以显式重抛。 */
    private suspend fun applySafe(song: Song, candidate: CoverCandidate): ScrapeOutcome? = try {
        scraper.apply(song, candidate)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        Timber.w(e, "Scrape: 应用失败 %s", song.title)
        null
    }

    private fun rowOf(songId: Long): ScrapeRow? =
        _uiState.value.rows.firstOrNull { it.id == songId }

    private fun updateRow(songId: Long, transform: (ScrapeRow) -> ScrapeRow) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row -> if (row.id == songId) transform(row) else row }
            )
        }
    }
}
