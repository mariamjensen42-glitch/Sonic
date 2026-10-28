package com.cycling.presentation.songdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.domain.lyrics.model.Attributes
import com.cycling.domain.lyrics.model.ISyncedLine
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.domain.lyrics.model.synced.UncheckedSyncedLine
import com.cycling.domain.lyrics.parser.AutoParser
import com.cycling.domain.lyrics.utils.LrcMetadataHelper
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import com.cycling.domain.model.AudioMetadata
import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.Song
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.CoverRepository
import com.cycling.domain.repository.PlayerRepository
import com.cycling.domain.repository.PlaylistRepository
import com.cycling.domain.repository.SettingsRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.usecase.GetLyricsUseCase
import com.cycling.domain.usecase.SaveLyricsUseCase
import com.cycling.presentation.R
import com.cycling.presentation.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel(assistedFactory = SongDetailViewModel.Factory::class)
class SongDetailViewModel @AssistedInject constructor(
    private val songRepository: SongRepository,
    private val playerRepository: PlayerRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioMetadataRepository: AudioMetadataRepository,
    private val coverRepository: CoverRepository,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val saveLyricsUseCase: SaveLyricsUseCase,
    private val settingsRepository: SettingsRepository,
    @Assisted private val route: Screen.SongDetail
) : ViewModel() {

    private val songId: Long = route.songId

    /** Only used to describe the lyrics in the info dialog, never to render them on the page. */
    private val lyricsParser = AutoParser(LyricsFormatGuesser())
    private val lyricsFormatGuesser = LyricsFormatGuesser()

    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SongDetailEffect>()
    val uiEffect: SharedFlow<SongDetailEffect> = _uiEffect.asSharedFlow()

    init {
        loadSong()
        loadPlaylists()
    }

    fun handleIntent(intent: SongDetailIntent) {
        when (intent) {
            is SongDetailIntent.LoadSong -> loadSong()
            is SongDetailIntent.ToggleFavorite -> toggleFavorite()
            is SongDetailIntent.PlaySong -> playSong()
            is SongDetailIntent.AddToPlaylist -> addToPlaylist()
            is SongDetailIntent.DismissAddToPlaylistDialog -> dismissAddToPlaylistDialog()
            is SongDetailIntent.AddToPlaylistTo -> addToPlaylist(intent.playlistId)
            is SongDetailIntent.CreatePlaylistAndAddSong -> createPlaylistAndAddSong(intent.name)
            is SongDetailIntent.CopyPath -> copyPath(intent.path)
            is SongDetailIntent.EnterEditMode -> enterEditMode()
            is SongDetailIntent.ExitEditMode -> exitEditMode()
            is SongDetailIntent.UpdateTitle -> updateTitle(intent.title)
            is SongDetailIntent.UpdateArtist -> updateArtist(intent.artist)
            is SongDetailIntent.UpdateAlbum -> updateAlbum(intent.album)
            is SongDetailIntent.UpdateAlbumArtist -> updateAlbumArtist(intent.albumArtist)
            is SongDetailIntent.UpdateYear -> updateYear(intent.year)
            is SongDetailIntent.UpdateGenre -> updateGenre(intent.genre)
            is SongDetailIntent.UpdateTrackNumber -> updateTrackNumber(intent.trackNumber)
            is SongDetailIntent.UpdateDiscNumber -> updateDiscNumber(intent.discNumber)
            is SongDetailIntent.UpdateComposer -> updateComposer(intent.composer)
            is SongDetailIntent.UpdatePublisher -> updatePublisher(intent.publisher)
            is SongDetailIntent.UpdateCopyright -> updateCopyright(intent.copyright)
            is SongDetailIntent.UpdateComment -> updateComment(intent.comment)
            is SongDetailIntent.UpdateLyrics -> updateLyrics(intent.lyrics)
            is SongDetailIntent.SaveChanges -> saveChanges()
            is SongDetailIntent.DiscardChanges -> discardChanges()
            is SongDetailIntent.KeepEditing -> keepEditing()
            is SongDetailIntent.OpenLyricsCandidates -> openLyricsCandidates()
            is SongDetailIntent.DismissLyricsCandidates -> dismissLyricsCandidates()
            is SongDetailIntent.SelectLyricsCandidate -> selectLyricsCandidate(intent.candidateId)
            is SongDetailIntent.OpenLyricsInfo -> openLyricsInfo()
            is SongDetailIntent.DismissLyricsInfo -> dismissLyricsInfo()
            is SongDetailIntent.ApplyLyricsMetadata -> applyLyricsMetadata(intent.metadata)
            is SongDetailIntent.FillSongInfoFromLyricsMetadata -> fillSongInfoFromLyricsMetadata(intent.metadata)
            is SongDetailIntent.OpenCoverPicker -> openCoverPicker()
            is SongDetailIntent.DismissCoverPicker -> dismissCoverPicker()
            is SongDetailIntent.RetryOnlineSearch -> retryOnlineSearch()
            is SongDetailIntent.SelectCoverCandidate -> selectCoverCandidate(intent.candidateId)
            is SongDetailIntent.ApplyLocalCover -> applyLocalCover(intent.imageUri)
            is SongDetailIntent.ClearCustomCover -> clearCustomCover()
            is SongDetailIntent.OpenMetadataCandidates -> openMetadataCandidates()
            is SongDetailIntent.DismissMetadataCandidates -> dismissMetadataCandidates()
            is SongDetailIntent.SelectMetadataCandidate -> selectMetadataCandidate(intent.candidateId)
        }
    }

    private fun loadSong() {
        Timber.d("loadSong: starting songId=$songId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val song = songRepository.getSongById(songId)
                if (song == null) {
                    Timber.d("loadSong: song not found")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                Timber.d("loadSong: loaded song ${song.title}")
                val metadata = loadMergedMetadata(song)
                _uiState.update {
                    it.copy(
                        song = song,
                        isLoading = false,
                        isFavorite = song.isFavorite,
                        audioMetadata = metadata,
                        editedTitle = song.title,
                        editedArtist = song.artist,
                        editedAlbum = song.album,
                        editedAlbumArtist = metadata?.albumArtist ?: "",
                        editedYear = metadata?.year ?: "",
                        editedGenre = metadata?.genre ?: "",
                        editedTrackNumber = metadata?.trackNumber ?: "",
                        editedDiscNumber = metadata?.discNumber ?: "",
                        editedComposer = metadata?.composer ?: "",
                        editedPublisher = metadata?.publisher ?: "",
                        editedCopyright = metadata?.copyright ?: "",
                        editedComment = metadata?.comment ?: ""
                    )
                }
                // Separate coroutine: a cache miss goes to the network, and the page should be
                // readable while that happens rather than waiting on it.
                loadLyrics(song)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // 不兜这一下，isLoading 会停在 true：整页永远转圈，连"歌曲不存在"的出口都没有。
                Timber.e(error, "loadSong: failed for songId=$songId")
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_load_info_failed))
            }
        }
    }

    /**
     * Resolves the song's lyrics for the page, and seeds the editor with the same text.
     *
     * The editor gets the raw content, not the display lines: saving it back has to preserve LRC
     * timings, and stripping them for the sake of a prettier text field would quietly turn a timed
     * lyric into one the player can no longer follow.
     */
    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLyrics = true) }

            val lyrics = try {
                getLyricsUseCase(song)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "loadLyrics: failed for songId=${song.id}")
                _uiState.update { it.copy(isLoadingLyrics = false) }
                return@launch
            }

            _uiState.update { state ->
                val original = lyrics.rawContent.orEmpty()
                // An answer that arrives while the user is typing must not overwrite the draft;
                // only the comparison baseline moves.
                val draft = if (state.isEditMode && state.editedLyrics != state.originalLyrics) {
                    state.editedLyrics
                } else {
                    original
                }
                val next = state.copy(
                    lyrics = displayLines(lyrics),
                    lyricsSource = lyrics.source,
                    lyricsProvider = lyrics.provider,
                    isLoadingLyrics = false,
                    originalLyrics = original,
                    editedLyrics = draft
                )
                next.copy(hasChanges = checkForChanges(next))
            }
        }
    }

    private fun updateLyrics(lyrics: String) {
        _uiState.update { state ->
            state.copy(
                editedLyrics = lyrics,
                hasChanges = checkForChanges(state.copy(editedLyrics = lyrics))
            )
        }
    }

    // ── 歌词获取 / 版本选择 ─────────────────────────────────────────

    /**
     * 拉一份在线版本列表给「获取歌词」用。
     *
     * 与播放页的版本选择器是同一份数据（`GetLyricsUseCase.candidates`），区别只在选中之后做什么：
     * 播放页立刻换掉当前歌词，这里是填进编辑器，等用户保存才落库 —— 编辑页上的东西在点保存之前
     * 都不该生效。
     *
     * 每次都重新查而不是复用上次的结果：用户点这个按钮，多半就是因为自动挑的那份不对。
     */
    private fun openLyricsCandidates() {
        val song = _uiState.value.song ?: return

        viewModelScope.launch {
            if (!settingsRepository.onlineLyricsEnabled.first()) {
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_lyrics_online_disabled))
                return@launch
            }

            _uiState.update {
                it.copy(
                    showLyricsCandidates = true,
                    isLoadingLyricsCandidates = true,
                    lyricsCandidates = emptyList()
                )
            }

            val found = mutableListOf<LyricsCandidate>()
            try {
                getLyricsUseCase.candidates(song).collect { batch ->
                    if (batch.candidates.isNotEmpty()) {
                        found += batch.candidates
                        // 逐家填进列表：这里是挑一份文本填进编辑器，没必要等最慢的那家；
                        // 稳定排序只是为了平台分组别跳来跳去（同一家内部的顺序保持提供方给的优先级）。
                        val merged = found.sortedBy { it.provider.ordinal }
                        _uiState.update { it.copy(lyricsCandidates = merged) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "openLyricsCandidates: failed for songId=$songId")
                _uiState.update {
                    it.copy(showLyricsCandidates = false, isLoadingLyricsCandidates = false)
                }
                _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_lyrics_fetch_failed))
                return@launch
            }

            _uiState.update { it.copy(isLoadingLyricsCandidates = false) }

            if (found.isEmpty()) {
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.lyrics_candidates_empty))
            }
        }
    }

    private fun dismissLyricsCandidates() {
        _uiState.update { it.copy(showLyricsCandidates = false) }
    }

    /**
     * 把选中的版本填进歌词编辑器。
     *
     * 只填不存：保存仍然走手写歌词那条路（数据库里的一份 USER 覆盖），这样它压得过内嵌标签和
     * 旁车文件，也符合「编辑页的东西保存才生效」这条规则。用户在编辑器里继续改，改的就是这一份。
     */
    private fun selectLyricsCandidate(candidateId: String) {
        val candidate = _uiState.value.lyricsCandidates.firstOrNull { it.id == candidateId } ?: return

        _uiState.update { state ->
            val next = state.copy(
                editedLyrics = candidate.rawContent,
                showLyricsCandidates = false,
                isLoadingLyricsCandidates = false,
                lyricsCandidates = emptyList()
            )
            next.copy(hasChanges = checkForChanges(next))
        }

        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_lyrics_candidate_filled))
        }
    }

    // ── 歌词信息 ────────────────────────────────────────────────────

    private fun openLyricsInfo() {
        _uiState.update {
            it.copy(showLyricsInfoDialog = true, lyricsInfo = buildLyricsInfo(it))
        }
    }

    private fun dismissLyricsInfo() {
        _uiState.update { it.copy(showLyricsInfoDialog = false) }
    }

    /**
     * 把改过的头部标签写回歌词文本。
     *
     * 只改文本，不落库：写回之后 [checkForChanges] 会把这当成「歌词被改过」，页面底部的保存按钮
     * 才是唯一写入数据库的动作。没有歌词可写时什么也不做 —— 不能让元数据编辑把空文档变成一段
     * 只有 `[ti:]` 的歌词。
     */
    private fun applyLyricsMetadata(metadata: Attributes) {
        if (_uiState.value.editedLyrics.isBlank()) {
            _uiState.update { it.copy(showLyricsInfoDialog = false) }
            return
        }

        _uiState.update { state ->
            val updated = LrcMetadataHelper.applyAttributes(state.editedLyrics, metadata)
            val next = state.copy(editedLyrics = updated, showLyricsInfoDialog = false)
            next.copy(hasChanges = checkForChanges(next))
        }

        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_lyrics_metadata_applied))
        }
    }

    /**
     * 用歌词里的 `[ti:]` / `[ar:]` / `[al:]` 填歌曲信息表单。
     *
     * 空值不覆盖已有输入：歌词里常常只写了标题，把用户已经改好的歌手清空是最糟的结果。
     */
    private fun fillSongInfoFromLyricsMetadata(metadata: Attributes) {
        val title = metadata.title?.take(MAX_METADATA_LENGTH)
        val artist = metadata.artist?.take(MAX_METADATA_LENGTH)
        val album = metadata.album?.take(MAX_METADATA_LENGTH)

        if (title == null && artist == null && album == null) {
            viewModelScope.launch {
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_lyrics_metadata_no_song_info))
            }
            return
        }

        _uiState.update { state ->
            val next = state.copy(
                editedTitle = title ?: state.editedTitle,
                editedArtist = artist ?: state.editedArtist,
                editedAlbum = album ?: state.editedAlbum,
                showLyricsInfoDialog = false
            )
            next.copy(hasChanges = checkForChanges(next))
        }

        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_lyrics_metadata_filled))
        }
    }

    /**
     * 从当前草稿（而不是上次解析结果）描述歌词，所以用户在编辑器里改了什么，信息面板就说什么。
     *
     * 来源与提供方无法从文本推出来，只能取页面加载时解析出的那一份；其余全是文本的函数。
     */
    private fun buildLyricsInfo(
        state: SongDetailUiState,
        text: String = state.editedLyrics
    ): LyricsInfo {
        val synced = if (text.isBlank()) {
            null
        } else {
            try {
                lyricsParser.parse(text)
            } catch (e: Exception) {
                Timber.e(e, "buildLyricsInfo: could not parse lyrics for songId=$songId")
                null
            }
        }
        val lines = synced?.lines.orEmpty()

        return LyricsInfo(
            source = state.lyricsSource,
            provider = state.lyricsProvider,
            format = if (text.isBlank()) null else lyricsFormatGuesser.guessFormat(text)?.name,
            isWordByWord = synced?.isWordByWord == true,
            hasTimings = lines.isNotEmpty(),
            lineCount = if (lines.isNotEmpty()) lines.size else plainLines(text).size,
            hasTranslation = lines.any { it.translationOrNull()?.isNotBlank() == true },
            metadata = if (text.isBlank()) Attributes() else LrcMetadataHelper.parse(text)
        )
    }

    private fun ISyncedLine.translationOrNull(): String? = when (this) {
        is SyncedLine -> translation
        is KaraokeLine -> translation
        is UncheckedSyncedLine -> translation
        else -> null
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val currentSong = _uiState.value.song ?: return@launch
            val newFavoriteState = songRepository.toggleFavorite(currentSong.id)
            _uiState.update { it.copy(isFavorite = newFavoriteState) }
            val message = if (newFavoriteState) R.string.player_added_to_favorites else R.string.player_removed_from_favorites
            _uiEffect.emit(SongDetailEffect.ShowCopiedMessage(message))
        }
    }

    private fun playSong() {
        viewModelScope.launch {
            val currentSong = _uiState.value.song ?: return@launch
            playerRepository.playSong(currentSong, listOf(currentSong))
            _uiEffect.emit(SongDetailEffect.NavigateToPlayer(currentSong.id))
        }
    }

    private fun addToPlaylist() {
        _uiState.update { it.copy(showAddToPlaylistDialog = true) }
    }

    private fun dismissAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = false) }
    }

    private fun addToPlaylist(playlistId: Long) {
        val songId = _uiState.value.song?.id ?: return
        viewModelScope.launch {
            val added = playlistRepository.addSongToPlaylist(playlistId, songId)
            _uiEffect.emit(
                SongDetailEffect.ShowToast(
                    if (added > 0) R.string.playlist_added_to else R.string.playlist_song_already_added
                )
            )
            dismissAddToPlaylistDialog()
        }
    }

    private fun createPlaylistAndAddSong(name: String) {
        val songId = _uiState.value.song?.id ?: return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongToPlaylist(playlistId, songId)
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.playlist_created_and_added))
            dismissAddToPlaylistDialog()
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun copyPath(path: String) {
        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowCopiedMessage(R.string.song_detail_vm_path_copied))
        }
    }

    private fun enterEditMode() {
        val song = _uiState.value.song ?: return
        val metadata = _uiState.value.audioMetadata
        _uiState.update { 
            it.copy(
                isEditMode = true,
                editedTitle = song.title,
                editedArtist = song.artist,
                editedAlbum = song.album,
                editedAlbumArtist = metadata?.albumArtist ?: "",
                editedYear = metadata?.year ?: "",
                editedGenre = metadata?.genre ?: "",
                editedTrackNumber = metadata?.trackNumber ?: "",
                editedDiscNumber = metadata?.discNumber ?: "",
                editedComposer = metadata?.composer ?: "",
                editedPublisher = metadata?.publisher ?: "",
                editedCopyright = metadata?.copyright ?: "",
                editedComment = metadata?.comment ?: "",
                editedLyrics = it.originalLyrics,
                showLyricsCandidates = false,
                isLoadingLyricsCandidates = false,
                lyricsCandidates = emptyList(),
                showLyricsInfoDialog = false,
                hasChanges = false
            )
        }
    }

    private fun exitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _uiState.update { it.copy(isEditMode = false) }
        }
    }

    private fun updateTitle(title: String) {
        val maxLength = 200
        val trimmedTitle = title.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedTitle = trimmedTitle,
                hasChanges = checkForChanges(state.copy(editedTitle = trimmedTitle))
            )
        }
    }

    private fun updateArtist(artist: String) {
        val maxLength = 200
        val trimmedArtist = artist.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedArtist = trimmedArtist,
                hasChanges = checkForChanges(state.copy(editedArtist = trimmedArtist))
            )
        }
    }

    private fun updateAlbum(album: String) {
        val maxLength = 200
        val trimmedAlbum = album.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedAlbum = trimmedAlbum,
                hasChanges = checkForChanges(state.copy(editedAlbum = trimmedAlbum))
            )
        }
    }

    private fun updateYear(year: String) {
        val filteredYear = year.filter { it.isDigit() }.take(4)
        _uiState.update { state ->
            state.copy(
                editedYear = filteredYear,
                hasChanges = checkForChanges(state.copy(editedYear = filteredYear))
            )
        }
    }

    private fun updateGenre(genre: String) {
        val maxLength = 100
        val trimmedGenre = genre.take(maxLength)
        _uiState.update { state ->
            state.copy(
                editedGenre = trimmedGenre,
                hasChanges = checkForChanges(state.copy(editedGenre = trimmedGenre))
            )
        }
    }

    private fun updateAlbumArtist(albumArtist: String) {
        val trimmed = albumArtist.take(MAX_TEXT_LENGTH)
        _uiState.update { state ->
            state.copy(
                editedAlbumArtist = trimmed,
                hasChanges = checkForChanges(state.copy(editedAlbumArtist = trimmed))
            )
        }
    }

    private fun updateTrackNumber(trackNumber: String) {
        val filtered = trackNumber.filter { it.isDigit() || it == '/' }.take(7)
        _uiState.update { state ->
            state.copy(
                editedTrackNumber = filtered,
                hasChanges = checkForChanges(state.copy(editedTrackNumber = filtered))
            )
        }
    }

    private fun updateDiscNumber(discNumber: String) {
        val filtered = discNumber.filter { it.isDigit() || it == '/' }.take(7)
        _uiState.update { state ->
            state.copy(
                editedDiscNumber = filtered,
                hasChanges = checkForChanges(state.copy(editedDiscNumber = filtered))
            )
        }
    }

    private fun updateComposer(composer: String) {
        val trimmed = composer.take(MAX_TEXT_LENGTH)
        _uiState.update { state ->
            state.copy(
                editedComposer = trimmed,
                hasChanges = checkForChanges(state.copy(editedComposer = trimmed))
            )
        }
    }

    private fun updatePublisher(publisher: String) {
        val trimmed = publisher.take(MAX_TEXT_LENGTH)
        _uiState.update { state ->
            state.copy(
                editedPublisher = trimmed,
                hasChanges = checkForChanges(state.copy(editedPublisher = trimmed))
            )
        }
    }

    private fun updateCopyright(copyright: String) {
        val trimmed = copyright.take(MAX_TEXT_LENGTH)
        _uiState.update { state ->
            state.copy(
                editedCopyright = trimmed,
                hasChanges = checkForChanges(state.copy(editedCopyright = trimmed))
            )
        }
    }

    private fun updateComment(comment: String) {
        val trimmed = comment.take(MAX_COMMENT_LENGTH)
        _uiState.update { state ->
            state.copy(
                editedComment = trimmed,
                hasChanges = checkForChanges(state.copy(editedComment = trimmed))
            )
        }
    }

    private fun checkForChanges(state: SongDetailUiState): Boolean {
        val song = state.song ?: return false
        val metadata = state.audioMetadata

        return state.editedTitle != song.title ||
                state.editedArtist != song.artist ||
                state.editedAlbum != song.album ||
                state.editedAlbumArtist != (metadata?.albumArtist ?: "") ||
                state.editedYear != (metadata?.year ?: "") ||
                state.editedGenre != (metadata?.genre ?: "") ||
                state.editedTrackNumber != (metadata?.trackNumber ?: "") ||
                state.editedDiscNumber != (metadata?.discNumber ?: "") ||
                state.editedComposer != (metadata?.composer ?: "") ||
                state.editedPublisher != (metadata?.publisher ?: "") ||
                state.editedCopyright != (metadata?.copyright ?: "") ||
                state.editedComment != (metadata?.comment ?: "") ||
                state.editedLyrics != state.originalLyrics
    }

    /**
     * Turns resolved [lyrics] into what the page draws.
     *
     * Timed lyrics go through the parser's own lines, so karaoke documents lose their per-syllable
     * timing but keep their text and translation. Anything the parser could not read — plain text,
     * or a format it does not know — falls back to the raw text with its tags stripped, which is
     * the whole point of the detail page: it can show a lyric the player cannot follow.
     */
    private fun displayLines(lyrics: Lyrics): List<LyricsDisplayLine> {
        val synced = lyrics.syncedLyrics
        if (synced != null && synced.lines.isNotEmpty()) {
            return synced.lines.mapNotNull { line ->
                when (line) {
                    is SyncedLine -> LyricsDisplayLine(line.content, line.translation)
                    is KaraokeLine -> LyricsDisplayLine(
                        text = line.syllables.joinToString("") { it.content }.trim(),
                        translation = line.translation
                    )
                    else -> null
                }
            }.filter { it.text.isNotBlank() }
        }
        return plainLines(lyrics.rawContent)
    }

    /** Raw text as display lines: blank lines, `[ti:]`-style tags and timestamps removed. */
    private fun plainLines(rawContent: String?): List<LyricsDisplayLine> {
        if (rawContent.isNullOrBlank()) return emptyList()
        return rawContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { MetadataTag.matches(it) }
            .map { TimestampTag.replace(it, "").trim() }
            .filter { it.isNotEmpty() }
            .map { LyricsDisplayLine(it) }
            .toList()
    }

    private fun saveChanges() {
        val song = _uiState.value.song ?: return
        val snapshot = _uiState.value
        val metadata = snapshot.audioMetadata

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val titleChanged = snapshot.editedTitle != song.title
            val artistChanged = snapshot.editedArtist != song.artist
            val albumChanged = snapshot.editedAlbum != song.album
            val albumArtistChanged = snapshot.editedAlbumArtist != (metadata?.albumArtist ?: "")
            val yearChanged = snapshot.editedYear != (metadata?.year ?: "")
            val genreChanged = snapshot.editedGenre != (metadata?.genre ?: "")
            val trackNumberChanged = snapshot.editedTrackNumber != (metadata?.trackNumber ?: "")
            val discNumberChanged = snapshot.editedDiscNumber != (metadata?.discNumber ?: "")
            val composerChanged = snapshot.editedComposer != (metadata?.composer ?: "")
            val publisherChanged = snapshot.editedPublisher != (metadata?.publisher ?: "")
            val copyrightChanged = snapshot.editedCopyright != (metadata?.copyright ?: "")
            val commentChanged = snapshot.editedComment != (metadata?.comment ?: "")
            val lyricsChanged = snapshot.editedLyrics != snapshot.originalLyrics

            if (titleChanged || artistChanged || albumChanged) {
                val updateSuccess = songRepository.updateSongInfo(
                    songId = song.id,
                    title = if (titleChanged) snapshot.editedTitle else null,
                    artist = if (artistChanged) snapshot.editedArtist else null,
                    album = if (albumChanged) snapshot.editedAlbum else null
                )
                if (!updateSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_update_failed))
                    return@launch
                }
            }

            // Tag fields are stored twice on purpose: the database override keeps the edit even
            // when the file cannot be written (unsupported container, too large, or no write
            // access to the media library), the overrides above are the only copy of the edit.
            // and the file tag keeps it meaningful outside this app. `null` means "unchanged", so
            // a save only touches the fields the user actually edited.
            val tagUpdate = TagUpdate(
                title = if (titleChanged) snapshot.editedTitle else null,
                artist = if (artistChanged) snapshot.editedArtist else null,
                album = if (albumChanged) snapshot.editedAlbum else null,
                albumArtist = snapshot.editedAlbumArtist.orNullIfUnchanged(albumArtistChanged),
                year = snapshot.editedYear.orNullIfUnchanged(yearChanged),
                genre = snapshot.editedGenre.orNullIfUnchanged(genreChanged),
                trackNumber = snapshot.editedTrackNumber.orNullIfUnchanged(trackNumberChanged),
                discNumber = snapshot.editedDiscNumber.orNullIfUnchanged(discNumberChanged),
                composer = snapshot.editedComposer.orNullIfUnchanged(composerChanged),
                publisher = snapshot.editedPublisher.orNullIfUnchanged(publisherChanged),
                copyright = snapshot.editedCopyright.orNullIfUnchanged(copyrightChanged),
                comment = snapshot.editedComment.orNullIfUnchanged(commentChanged)
            )

            var fileTagsWritten = true
            if (tagUpdate != TagUpdate()) {
                val overrideUpdate = TagUpdate(
                    // Raw edited values, not `orNullIfUnchanged`: a blank here means the user
                    // cleared the field, which has to remove the stored override.
                    albumArtist = if (albumArtistChanged) snapshot.editedAlbumArtist else null,
                    year = if (yearChanged) snapshot.editedYear else null,
                    genre = if (genreChanged) snapshot.editedGenre else null,
                    trackNumber = if (trackNumberChanged) snapshot.editedTrackNumber else null,
                    discNumber = if (discNumberChanged) snapshot.editedDiscNumber else null,
                    composer = if (composerChanged) snapshot.editedComposer else null,
                    publisher = if (publisherChanged) snapshot.editedPublisher else null,
                    copyright = if (copyrightChanged) snapshot.editedCopyright else null,
                    comment = if (commentChanged) snapshot.editedComment else null
                )
                try {
                    if (overrideUpdate != TagUpdate()) {
                        audioMetadataRepository.saveUserTagOverrides(song.id, overrideUpdate)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to store tag overrides for songId=${song.id}")
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_save_failed))
                    return@launch
                }

                if (audioMetadataRepository.isTagWriteSupported(song.path)) {
                    val result = audioMetadataRepository.updateAudioTags(song.path, tagUpdate)
                    result.exceptionOrNull()?.let { error ->
                        Timber.e(error, "Failed to update audio tags")
                        _uiState.update { it.copy(isSaving = false) }
                        _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_save_failed))
                        return@launch
                    }
                    fileTagsWritten = result.getOrNull() == true
                } else {
                    // No writer for this container (or the extension is unknown); the database
                    // override above is then the only copy of these fields.
                    fileTagsWritten = false
                }
            }

            // Lyrics are stored in the database, not written into the file: the app holds a read
            // grant on the media library, and an edit that silently failed on a modern device
            // would be worse than one that is honestly kept on this side.
            var savedLyrics: Lyrics? = null
            if (lyricsChanged) {
                try {
                    savedLyrics = saveLyricsUseCase(song, snapshot.editedLyrics)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save lyrics for songId=${song.id}")
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_lyrics_save_failed))
                    return@launch
                }
            }

            // Re-read the merged metadata so the view mode reflects what was actually stored —
            // the file tag when it could be written, the override when it could not.
            val refreshedMetadata = loadMergedMetadata(song)

            _uiState.update { state ->
                val updatedSong = if (titleChanged || artistChanged || albumChanged) {
                    song.copy(
                        title = snapshot.editedTitle,
                        artist = snapshot.editedArtist,
                        album = snapshot.editedAlbum
                    )
                } else {
                    state.song
                }
                val saved = savedLyrics
                val base = state.copy(
                    isSaving = false,
                    hasChanges = false,
                    isEditMode = false,
                    song = updatedSong,
                    audioMetadata = refreshedMetadata
                )
                if (saved == null) {
                    base
                } else {
                    // The save may have resolved to something other than what was typed — a
                    // cleared editor falls back to the automatic sources — so both the baseline
                    // and the draft are taken from the result, not from the input.
                    val raw = saved.rawContent.orEmpty()
                    base.copy(
                        lyrics = displayLines(saved),
                        lyricsSource = saved.source,
                        lyricsProvider = saved.provider,
                        originalLyrics = raw,
                        editedLyrics = raw,
                        showLyricsCandidates = false,
                        isLoadingLyricsCandidates = false,
                        lyricsCandidates = emptyList(),
                        showLyricsInfoDialog = false
                    )
                }
            }
            _uiEffect.emit(
                SongDetailEffect.ShowToast(
                    if (fileTagsWritten) R.string.song_detail_vm_save_success
                    else R.string.song_detail_vm_save_partial
                )
            )
        }
    }

    // ── 在线目录：封面 / 元数据 ─────────────────────────────────────

    private fun openCoverPicker() {
        val song = _uiState.value.song ?: return
        _uiState.update { it.copy(showCoverPicker = true) }
        searchOnlineCandidates(song)
    }

    private fun dismissCoverPicker() {
        _uiState.update {
            it.copy(
                showCoverPicker = false,
                onlineCandidates = emptyList(),
                isLoadingOnlineCandidates = false
            )
        }
    }

    private fun retryOnlineSearch() {
        val song = _uiState.value.song ?: return
        searchOnlineCandidates(song)
    }

    /**
     * 打开面板就搜一次：用户点「更换封面」或「获取在线信息」时想要的正是候选，而不是一个还要再点
     * 一下的空面板。两个面板共用这一份结果（同一条命中同时带封面图和文本字段）；搜不到时面板留在
     * 空态，带「重新搜索」这条退路，封面面板另外还有「从相册选择」。
     */
    private fun searchOnlineCandidates(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOnlineCandidates = true, onlineCandidates = emptyList()) }

            val found = try {
                coverRepository.findCandidates(song)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "searchOnlineCandidates: failed for songId=${song.id}")
                emptyList()
            }

            _uiState.update { it.copy(onlineCandidates = found, isLoadingOnlineCandidates = false) }
            if (found.isEmpty()) {
                Timber.d("searchOnlineCandidates: nothing found for ${song.title}")
            }
        }
    }

    private fun selectCoverCandidate(candidateId: String) {
        val song = _uiState.value.song ?: return
        val candidate = _uiState.value.onlineCandidates.firstOrNull { it.id == candidateId } ?: return
        applyCover { coverRepository.applyCandidate(song, candidate) }
    }

    private fun applyLocalCover(imageUri: String) {
        val song = _uiState.value.song ?: return
        applyCover { coverRepository.applyLocalImage(song, imageUri) }
    }

    private fun clearCustomCover() {
        val song = _uiState.value.song ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingCover = true) }
            val result = coverRepository.clearCustomCover(song)
            if (result.isSuccess) {
                refreshSong()
                _uiState.update {
                    it.copy(
                        isApplyingCover = false,
                        showCoverPicker = false,
                        onlineCandidates = emptyList(),
                        isLoadingOnlineCandidates = false
                    )
                }
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_vm_cover_cleared))
            } else {
                Timber.e(result.exceptionOrNull(), "clearCustomCover: failed for songId=${song.id}")
                _uiState.update { it.copy(isApplyingCover = false) }
                _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_cover_failed))
            }
        }
    }

    /**
     * Downloads/copies the chosen image and, on success, reloads the song row so the header shows
     * the effective artwork. The sheet stays open while the image is being applied, with a spinner,
     * because downloading a cover over a slow connection is not instant.
     */
    private fun applyCover(action: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingCover = true) }

            val result = action()
            result.exceptionOrNull()?.let { Timber.e(it, "applyCover: failed for songId=$songId") }

            if (result.isSuccess) {
                refreshSong()
                _uiState.update {
                    it.copy(
                        isApplyingCover = false,
                        showCoverPicker = false,
                        onlineCandidates = emptyList(),
                        isLoadingOnlineCandidates = false
                    )
                }
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_vm_cover_updated))
            } else {
                _uiState.update { it.copy(isApplyingCover = false) }
                _uiEffect.emit(SongDetailEffect.ShowError(R.string.song_detail_vm_cover_failed))
            }
        }
    }

    // ── 在线元数据 ──────────────────────────────────────────────────

    private fun openMetadataCandidates() {
        val song = _uiState.value.song ?: return
        _uiState.update { it.copy(showMetadataCandidates = true) }
        searchOnlineCandidates(song)
    }

    private fun dismissMetadataCandidates() {
        _uiState.update {
            it.copy(showMetadataCandidates = false, isLoadingOnlineCandidates = false)
        }
    }

    /**
     * Fills the edit form from one catalogue match.
     *
     * Only the fields the source actually reported are replaced: a source that knows the title but
     * not the genre must not blank a genre the user already typed. Everything lands in the draft,
     * so the page's Save button stays the only thing that persists it — exactly like the lyrics
     * version picker.
     */
    private fun selectMetadataCandidate(candidateId: String) {
        val candidate = _uiState.value.onlineCandidates.firstOrNull { it.id == candidateId } ?: return

        val year = candidate.year?.filter { it.isDigit() }?.take(4)?.takeIf { it.isNotEmpty() }
        val trackNumber = candidate.trackNumber
            ?.filter { it.isDigit() || it == '/' }
            ?.take(MAX_ORDINAL_LENGTH)
            ?.takeIf { it.isNotEmpty() }
        val discNumber = candidate.discNumber
            ?.filter { it.isDigit() || it == '/' }
            ?.take(MAX_ORDINAL_LENGTH)
            ?.takeIf { it.isNotEmpty() }

        val hasAnything = listOfNotNull(
            candidate.title,
            candidate.artist,
            candidate.album,
            candidate.albumArtist,
            year,
            candidate.genre,
            trackNumber,
            discNumber
        ).isNotEmpty()

        if (!hasAnything) {
            _uiState.update { it.copy(showMetadataCandidates = false) }
            viewModelScope.launch {
                _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_vm_metadata_empty))
            }
            return
        }

        _uiState.update { state ->
            val next = state.copy(
                editedTitle = candidate.title?.take(MAX_TEXT_LENGTH) ?: state.editedTitle,
                editedArtist = candidate.artist?.take(MAX_TEXT_LENGTH) ?: state.editedArtist,
                editedAlbum = candidate.album?.take(MAX_TEXT_LENGTH) ?: state.editedAlbum,
                editedAlbumArtist = candidate.albumArtist?.take(MAX_TEXT_LENGTH) ?: state.editedAlbumArtist,
                editedYear = year ?: state.editedYear,
                editedGenre = candidate.genre?.take(MAX_GENRE_LENGTH) ?: state.editedGenre,
                editedTrackNumber = trackNumber ?: state.editedTrackNumber,
                editedDiscNumber = discNumber ?: state.editedDiscNumber,
                showMetadataCandidates = false,
                isLoadingOnlineCandidates = false
            )
            next.copy(hasChanges = checkForChanges(next))
        }

        viewModelScope.launch {
            _uiEffect.emit(SongDetailEffect.ShowToast(R.string.song_detail_vm_metadata_filled))
        }
    }

    /** Reloads the song row so the page shows the effective artwork (custom or MediaStore). */
    private suspend fun refreshSong() {
        val updated = try {
            songRepository.getSongById(songId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "refreshSong: failed for songId=$songId")
            null
        } ?: return

        _uiState.update { it.copy(song = updated, isFavorite = updated.isFavorite) }
    }

    /** A changed file tag only makes sense with content; blank means "leave the tag alone". */
    private fun String.orNullIfUnchanged(changed: Boolean): String? =
        if (changed) takeIf { it.isNotBlank() } else null

    /** File tags, overlaid with the tags the user edited in the app. */
    private suspend fun loadMergedMetadata(song: Song): AudioMetadata? {
        val fileMetadata = try {
            audioMetadataRepository.getAudioMetadata(song.path)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "loadMergedMetadata: could not read file tags for songId=${song.id}")
            null
        }
        val overrides = audioMetadataRepository.getUserTagOverrides(song.id)
        return mergeMetadata(fileMetadata, overrides)
    }

    private fun mergeMetadata(file: AudioMetadata?, overrides: TagUpdate?): AudioMetadata? {
        if (file == null && overrides == null) return null

        val base = file ?: AudioMetadata()
        return base.copy(
            albumArtist = overrides?.albumArtist ?: base.albumArtist,
            year = overrides?.year ?: base.year,
            genre = overrides?.genre ?: base.genre,
            trackNumber = overrides?.trackNumber ?: base.trackNumber,
            discNumber = overrides?.discNumber ?: base.discNumber,
            composer = overrides?.composer ?: base.composer,
            publisher = overrides?.publisher ?: base.publisher,
            copyright = overrides?.copyright ?: base.copyright,
            comment = overrides?.comment ?: base.comment
        )
    }

    private fun discardChanges() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showDiscardDialog = false,
                    hasChanges = false,
                    isEditMode = false,
                    editedLyrics = it.originalLyrics,
                    showLyricsCandidates = false,
                    isLoadingLyricsCandidates = false,
                    lyricsCandidates = emptyList(),
                    showLyricsInfoDialog = false
                )
            }
        }
    }

    private fun keepEditing() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    private companion object {
        /** `[ti:]` / `[ar:]` / `[al:]` 的上限，与歌曲信息输入框保持一致。 */
        const val MAX_METADATA_LENGTH = 200

        /** 标签文本字段的上限；与 `[ti:]` 一样取 200，避免 ATOM 帧被无限长的输入撑大。 */
        const val MAX_TEXT_LENGTH = 200

        /** 备注允许长一些，它本来就是放说明性文字的地方。 */
        const val MAX_COMMENT_LENGTH = 500

        /** `3/12` 这类位置字符串的上限。 */
        const val MAX_ORDINAL_LENGTH = 7

        /** 流派字段的上限。 */
        const val MAX_GENRE_LENGTH = 100

        /** `[00:12.34]` / `[1:02.5]` at the start of a line, stripped for display only. */
        val TimestampTag = Regex("\\[\\d{1,2}:\\d{1,2}(?:[.:]\\d{1,3})?]")

        /** `[ti:…]` / `[ar:…]` / `[offset:…]` — LRC header lines, never lyric text. */
        val MetadataTag = Regex("^\\[[A-Za-z#]+:[^]]*]$")
    }

    @AssistedFactory
    interface Factory {
        fun create(route: Screen.SongDetail): SongDetailViewModel
    }
}
