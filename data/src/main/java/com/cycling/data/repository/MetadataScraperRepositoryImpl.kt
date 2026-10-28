package com.cycling.data.repository

import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.ScrapeField
import com.cycling.domain.model.ScrapeOutcome
import com.cycling.domain.model.ScrapeTarget
import com.cycling.domain.model.Song
import com.cycling.domain.model.TagUpdate
import com.cycling.domain.repository.AudioMetadataRepository
import com.cycling.domain.repository.CoverRepository
import com.cycling.domain.repository.MetadataScraperRepository
import com.cycling.domain.repository.SongRepository
import com.cycling.domain.scrape.ScrapeMatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 在线刮削的落地实现。
 *
 * 搜索沿用 [CoverRepository.findCandidates]：那一次搜索本来就同时返回封面和文本字段，
 * 这里不再单独打一套接口，也不会因为两条链路各自的排序不同而给出互相矛盾的候选。
 *
 * 写回按「界面先对、文件尽力」的顺序：
 * 1. `updateSongInfo` —— 改歌曲行，列表 / 详情页立刻正确；
 * 2. `saveUserTagOverrides` —— 年份 / 流派 / 曲号这些只存在 App 侧的字段；
 * 3. `updateAudioTags` —— 尽力写进文件，分区存储下通常失败（失败不影响 1、2）；
 * 4. `applyCandidate` —— 封面存成 App 私有副本，只改歌曲行的封面指针，不依赖写权限。
 */
@Singleton
class MetadataScraperRepositoryImpl @Inject constructor(
    private val songRepository: SongRepository,
    private val coverRepository: CoverRepository,
    private val audioMetadataRepository: AudioMetadataRepository
) : MetadataScraperRepository {

    override suspend fun findIncomplete(limit: Int): List<ScrapeTarget> =
        localTargets(limit) { missing -> missing.isNotEmpty() }

    override suspend fun findAllLocal(limit: Int): List<ScrapeTarget> =
        localTargets(limit) { true }

    private suspend fun localTargets(
        limit: Int,
        keep: (Set<ScrapeField>) -> Boolean
    ): List<ScrapeTarget> {
        val songs = try {
            songRepository.getAllSongs().first()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.w(e, "MetadataScraper: 读取曲库失败")
            return emptyList()
        }
        return songs.asSequence()
            .filter { !it.isOnline }
            .map { song -> ScrapeTarget(song, ScrapeMatch.missingFields(song)) }
            .filter { keep(it.missing) }
            .sortedWith(
                compareByDescending<ScrapeTarget> { it.missing.size }
                    .thenBy { it.song.title.lowercase() }
            )
            .take(limit)
            .toList()
    }

    override suspend fun candidates(song: Song): List<CoverCandidate> {
        val found = try {
            coverRepository.findCandidates(song.forScrapeSearch())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.w(e, "MetadataScraper: 搜索失败 ${song.title}")
            return emptyList()
        }
        // 排序仍按**原始**歌曲算：占位值只影响搜索关键词，不该影响「这条结果像不像它」。
        return ScrapeMatch.rank(song, found)
    }

    /**
     * 把占位值抹掉再拿去搜。
     *
     * 各平台的封面/元数据提供者拼关键词的方式都是 `"艺术家 标题"`，艺术家是 `<unknown>` 时
     * 关键词会变成 `<unknown> 稻香` —— 一条都搜不到。空串会被 `trim()` 吃掉，正好退化成按标题搜。
     */
    private fun Song.forScrapeSearch(): Song = copy(
        artist = if (ScrapeMatch.isMissingArtist(this)) "" else artist,
        album = if (ScrapeMatch.isMissingAlbum(this)) "" else album
    )

    override suspend fun apply(song: Song, candidate: CoverCandidate): ScrapeOutcome {
        val title = candidate.title.cleaned()
        val artist = candidate.artist.cleaned()
        val album = candidate.album.cleaned()
        val metadataApplied = title != null || artist != null || album != null

        if (metadataApplied) {
            // `null` = 这一项音源没给，不要拿空值覆盖用户已有的正确字段。
            try {
                songRepository.updateSongInfo(song.id, title, artist, album)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Timber.w(e, "MetadataScraper: 更新歌曲行失败 ${song.id}")
            }
        }

        val overrides = TagUpdate(
            albumArtist = candidate.albumArtist.cleaned(),
            year = candidate.year.cleanedYear(),
            genre = candidate.genre.cleaned(),
            trackNumber = candidate.trackNumber.cleaned(),
            discNumber = candidate.discNumber.cleaned()
        )
        if (overrides != TagUpdate()) {
            try {
                audioMetadataRepository.saveUserTagOverrides(song.id, overrides)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Timber.w(e, "MetadataScraper: 覆盖表写入失败 ${song.id}")
            }
        }

        val fileTagsWritten = writeFileTags(song, title, artist, album, overrides)

        val coverResult = try {
            coverRepository.applyCandidate(song, candidate)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.w(e, "MetadataScraper: 封面写入失败 ${song.id}")
            null
        }
        val coverApplied = coverResult?.isSuccess == true

        Timber.i(
            "MetadataScraper: applied %s → metadata=%s file=%s cover=%s",
            song.title, metadataApplied, fileTagsWritten, coverApplied
        )
        return ScrapeOutcome(
            candidate = candidate,
            metadataApplied = metadataApplied,
            fileTagsWritten = fileTagsWritten,
            coverApplied = coverApplied,
            newAlbumArt = coverResult?.getOrNull()
        )
    }

    /** 文件标签：能写就写，写不了只记日志 —— 上层用 [ScrapeOutcome.fileTagsWritten] 决定提示。 */
    private suspend fun writeFileTags(
        song: Song,
        title: String?,
        artist: String?,
        album: String?,
        overrides: TagUpdate
    ): Boolean {
        if (song.path.isBlank()) return false
        if (!audioMetadataRepository.isTagWriteSupported(song.path)) return false
        return try {
            audioMetadataRepository.updateAudioTags(
                filePath = song.path,
                tagUpdate = TagUpdate(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = overrides.albumArtist,
                    year = overrides.year,
                    genre = overrides.genre,
                    trackNumber = overrides.trackNumber,
                    discNumber = overrides.discNumber
                )
            ).getOrDefault(false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.w(e, "MetadataScraper: 文件标签写入失败 ${song.path}")
            false
        }
    }

    private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** 年份只留四位数字：音源给的常是 `1999-05-06` 或 `1999年`。 */
    private fun String?.cleanedYear(): String? {
        val digits = this?.filter { it.isDigit() } ?: return null
        return digits.take(4).takeIf { it.length == 4 }
    }
}
