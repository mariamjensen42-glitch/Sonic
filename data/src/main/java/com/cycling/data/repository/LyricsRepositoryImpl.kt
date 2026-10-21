package com.cycling.data.repository

import com.cycling.data.local.dao.LyricsCacheDao
import com.cycling.data.local.dao.LyricsIndexDao
import com.cycling.data.local.dao.LyricsOverrideDao
import com.cycling.data.local.entity.LyricsCacheEntity
import com.cycling.data.local.entity.LyricsIndexEntity
import com.cycling.data.local.entity.LyricsOverrideEntity
import com.cycling.data.local.lyrics.EmbeddedLyricsHelper
import com.cycling.data.local.lyrics.LyricsFileHelper
import com.cycling.data.store.LyricsCacheGenerationStore
import com.cycling.domain.lyrics.model.ISyncedLine
import com.cycling.domain.lyrics.model.SyncedLyrics
import com.cycling.domain.lyrics.model.karaoke.KaraokeLine
import com.cycling.domain.lyrics.model.synced.SyncedLine
import com.cycling.domain.lyrics.model.synced.UncheckedSyncedLine
import com.cycling.domain.lyrics.parser.AutoParser
import com.cycling.domain.lyrics.provider.OnlineLyricsProvider
import com.cycling.domain.lyrics.utils.LyricsFormatGuesser
import com.cycling.domain.model.Lyrics
import com.cycling.domain.model.LyricsCandidate
import com.cycling.domain.model.LyricsProvider
import com.cycling.domain.model.LyricsQuery
import com.cycling.domain.model.LyricsSource
import com.cycling.domain.model.Song
import com.cycling.domain.repository.LyricsRepository
import com.cycling.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 在线来源集合的代号。**新增来源或改变默认顺序**时加一，触发一次歌词缓存清空
 * （见 `LyricsRepositoryImpl.ensureCacheGeneration`）。
 *
 * - 1 → 2：加入 AMLL；
 * - 2 → 3：加入 QQ 音乐 / 酷狗音乐的歌词来源。
 */
internal const val LYRICS_CACHE_GENERATION = 3

/**
 * Resolves lyrics for a track, cheapest and most authoritative source first.
 *
 * The rule is that the first source to yield something *the player can follow* wins: a sidecar
 * `.lrc` the user placed beside the file beats embedded tags, embedded tags beat a downloaded copy,
 * and the network is only consulted when none of them has timed lyrics. A source that is present
 * but untimed — an embedded USLT tag, or an `.lrc` with no timestamps — does not count as an
 * answer, because the player can only render a [SyncedLyrics].
 *
 * Lyrics the user wrote on the song detail page are the one exception, and they come first: they
 * are a statement about this recording rather than a guess, so they answer even when untimed, and
 * no automatic source is consulted on their behalf. The detail page can show text the player
 * cannot follow; nothing else may.
 */
@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val embeddedLyricsHelper: EmbeddedLyricsHelper,
    private val lyricsFileHelper: LyricsFileHelper,
    private val lyricsCacheDao: LyricsCacheDao,
    private val lyricsOverrideDao: LyricsOverrideDao,
    private val lyricsIndexDao: LyricsIndexDao,
    private val cacheGenerationStore: LyricsCacheGenerationStore,
    private val settingsRepository: SettingsRepository,
    providers: Set<@JvmSuppressWildcards OnlineLyricsProvider>
) : LyricsRepository {

    private val parser = AutoParser(LyricsFormatGuesser())

    private val providersByKind: Map<LyricsProvider, OnlineLyricsProvider> =
        providers.associateBy { it.provider }

    private val cacheGenerationLock = Mutex()
    private var cacheGenerationReady = false

    override suspend fun getLyrics(song: Song): Lyrics {
        val lyrics = resolveLyrics(song)
        // Every resolved document — sidecar, embedded, downloaded or user-written — becomes
        // searchable here. This is the single place all four sources converge.
        index(song.id, lyrics.rawContent)
        return lyrics
    }

    /**
     * 在线来源集合变化后的第一次取词：把旧来源留下的缓存整表清掉。
     *
     * 缓存行只记来源、不记「有没有比它更优先的来源还没问过」，所以新增一个 provider 之后，
     * 已经放过的歌会一直命中旧的网易云行直到 TTL 到期 —— 新来源对它们等于不存在。清一次表，
     * 下一次播放就会按当前顺序重新查。用户手写的歌词在另一张表，不受影响。
     *
     * 失败时保持旧缓存：宁可晚一点生效，也不能让取歌词本身失败。
     */
    private suspend fun ensureCacheGeneration() {
        if (cacheGenerationReady) return
        cacheGenerationLock.withLock {
            if (cacheGenerationReady) return@withLock
            try {
                if (cacheGenerationStore.get() < LYRICS_CACHE_GENERATION) {
                    lyricsCacheDao.clear()
                    cacheGenerationStore.set(LYRICS_CACHE_GENERATION)
                    Timber.d("getLyrics: cleared lyrics cache for provider generation $LYRICS_CACHE_GENERATION")
                }
                cacheGenerationReady = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "getLyrics: could not migrate the lyrics cache")
            }
        }
    }

    private suspend fun resolveLyrics(song: Song): Lyrics {
        ensureCacheGeneration()
        val songPath = song.path
        Timber.d("getLyrics: songPath=$songPath")

        userLyrics(song)?.let { return it }

        localLyrics(songPath)?.let { return it }

        val query = LyricsQuery.of(song)
        val cached = lyricsCacheDao.get(query.cacheKey) ?: return onlineLyrics(query)

        val parsed = usableLyrics(cached.rawContent)
        if (parsed != null && !cached.hasExpired(parsed)) {
            Timber.d("getLyrics: cache hit for ${query.cacheKey} (source=${cached.source})")
            return Lyrics(
                syncedLyrics = parsed,
                rawContent = cached.rawContent,
                source = LyricsSource.ONLINE,
                provider = providerOf(cached.source)
            )
        }

        // A blank body is a remembered "the providers had nothing", which must not cost two HTTP
        // round trips every time the track comes round again.
        if (parsed == null && cached.rawContent.isBlank() && !cached.hasExpired(parsed)) {
            Timber.d("getLyrics: remembered miss for ${query.cacheKey}")
            return NO_LYRICS
        }

        // Anything else is a stale, truncated or beatable row — fall through and replace it.
        return onlineLyrics(query)
    }

    override suspend fun refreshLyrics(song: Song): Lyrics {
        val query = LyricsQuery.of(song)
        try {
            lyricsCacheDao.delete(query.cacheKey)
        } catch (e: Exception) {
            // Left in place, the stale row is what the lookup below would serve, so the refresh
            // quietly turns into a no-op. Worth a log, not worth throwing: an answer the user can
            // see beats a crash.
            Timber.w(e, "refreshLyrics: could not drop the cached row for ${query.cacheKey}")
        }
        return getLyrics(song)
    }

    override suspend fun findLyricsCandidates(song: Song): List<LyricsCandidate> {
        if (!settingsRepository.onlineLyricsEnabled.first()) return emptyList()
        ensureCacheGeneration()

        val query = LyricsQuery.of(song)
        val inUse = lyricsCacheDao.get(query.cacheKey)?.rawContent

        val candidates = mutableListOf<LyricsCandidate>()
        for (kind in settingsRepository.lyricsProviderOrder.first()) {
            val provider = providersByKind[kind] ?: continue

            val found = try {
                provider.fetchCandidates(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "findLyricsCandidates: ${kind.name} failed for ${query.cacheKey}")
                continue
            }
            Timber.d("findLyricsCandidates: ${kind.name} returned ${found.size} for ${query.cacheKey}")

            found.forEachIndexed { index, lyrics ->
                val parsed = usableLyrics(lyrics.rawContent) ?: return@forEachIndexed
                // Two entries of the same recording that parse to the same lyrics are one version
                // as far as the user is concerned; listing both would only make the choice harder.
                if (candidates.any { it.rawContent == lyrics.rawContent }) return@forEachIndexed

                candidates += LyricsCandidate(
                    id = "${kind.name.lowercase()}#$index",
                    provider = kind,
                    wordByWord = parsed.isWordByWord,
                    label = lyrics.label,
                    preview = parsed.lines.firstNotNullOfOrNull { it.preview() },
                    isCurrent = lyrics.rawContent == inUse,
                    rawContent = lyrics.rawContent
                )
            }
        }
        return candidates
    }

    override suspend fun selectLyricsCandidate(song: Song, candidate: LyricsCandidate): Lyrics {
        val query = LyricsQuery.of(song)
        val parsed = usableLyrics(candidate.rawContent) ?: return NO_LYRICS

        // The cache row is the whole of the memory: the next lookup reads it back, which is why the
        // choice survives a restart without any schema of its own.
        cacheLyrics(query, candidate.provider.name.lowercase(), candidate.rawContent)
        index(song.id, candidate.rawContent)
        return Lyrics(
            syncedLyrics = parsed,
            rawContent = candidate.rawContent,
            source = LyricsSource.ONLINE,
            provider = candidate.provider
        )
    }

    override suspend fun saveLyrics(song: Song, rawContent: String): Lyrics {
        lyricsOverrideDao.put(
            LyricsOverrideEntity(
                songId = song.id,
                rawContent = rawContent,
                updatedAt = System.currentTimeMillis()
            )
        )
        index(song.id, rawContent)
        // Parsed here rather than on the next read so the caller can say whether the text is
        // followable: the detail page renders either way, the player only renders the timed form.
        return Lyrics(parseLyrics(rawContent)?.takeIf { it.lines.isNotEmpty() }, rawContent, LyricsSource.USER)
    }

    override suspend fun clearLyrics(song: Song) {
        lyricsOverrideDao.delete(song.id)
        // The index may still hold text copied from the deleted override; the next successful
        // resolution (sidecar, cache or online) will rebuild it.
        try {
            lyricsIndexDao.delete(song.id)
        } catch (e: Exception) {
            Timber.w(e, "clearLyrics: could not drop the search index for songId=${song.id}")
        }
    }

    /**
     * The user's own text, if they wrote any for [song].
     *
     * Returned even when it carries no timings — that is the one place the "must be followable"
     * rule does not apply, because the alternative is discarding something the user typed. A
     * failure to read the row is logged and treated as "no override": losing the edit for the
     * length of one screen is recoverable, crashing the detail page is not.
     */
    private suspend fun userLyrics(song: Song): Lyrics? {
        val stored = try {
            lyricsOverrideDao.get(song.id)
        } catch (e: Exception) {
            Timber.w(e, "getLyrics: could not read the lyrics override for songId=${song.id}")
            null
        } ?: return null

        if (stored.rawContent.isBlank()) return null

        Timber.d("getLyrics: user lyrics for songId=${song.id}")
        val parsed = parseLyrics(stored.rawContent)?.takeIf { it.lines.isNotEmpty() }
        return Lyrics(parsed, stored.rawContent, LyricsSource.USER)
    }

    /** A sidecar file, then embedded tags — both already on disk, so neither can fail slowly. */
    private suspend fun localLyrics(songPath: String): Lyrics? {
        val fileLyrics = lyricsFileHelper.getLyricsContent(songPath)
        if (!fileLyrics.isNullOrBlank()) {
            usableLyrics(fileLyrics)?.let {
                Timber.d("getLyrics: found file lyrics for songPath=$songPath")
                return Lyrics(it, fileLyrics, LyricsSource.FILE)
            }
        }

        val embeddedLyrics = embeddedLyricsHelper.extractLyrics(songPath)
        if (!embeddedLyrics.isNullOrBlank()) {
            usableLyrics(embeddedLyrics)?.let {
                Timber.d("getLyrics: found embedded lyrics for songPath=$songPath")
                return Lyrics(it, embeddedLyrics, LyricsSource.EMBEDDED)
            }
        }

        return null
    }

    /**
     * Each provider in the user's order, stopping at the first that yields something renderable.
     *
     * A provider that throws is logged and skipped rather than propagated: one unofficial endpoint
     * being down is exactly the case the ordered list exists to survive.
     */
    private suspend fun onlineLyrics(query: LyricsQuery): Lyrics {
        if (!settingsRepository.onlineLyricsEnabled.first()) {
            Timber.d("getLyrics: online lookup disabled, no lyrics for ${query.cacheKey}")
            return NO_LYRICS
        }

        for (kind in settingsRepository.lyricsProviderOrder.first()) {
            val provider = providersByKind[kind] ?: continue

            val found = try {
                provider.fetch(query)
            } catch (e: CancellationException) {
                // The song changed or the screen went away; stopping is the point, not a failure
                // to degrade from — falling through would keep hitting the network regardless.
                throw e
            } catch (e: Exception) {
                Timber.w(e, "getLyrics: ${kind.name} failed for ${query.cacheKey}")
                null
            }
            if (found == null) {
                // Debug-level on purpose: "this source had nothing" is the normal outcome for a
                // library outside its catalogue, and it is what tells a support log apart from a
                // provider that never got asked.
                Timber.d("getLyrics: ${kind.name} had nothing for ${query.cacheKey}")
                continue
            }

            val syncedLyrics = usableLyrics(found.rawContent)
            if (syncedLyrics == null) {
                Timber.w("getLyrics: ${kind.name} returned unusable lyrics for ${query.cacheKey}")
                continue
            }

            cacheLyrics(query, kind.name.lowercase(), found.rawContent)
            return Lyrics(
                syncedLyrics = syncedLyrics,
                rawContent = found.rawContent,
                source = LyricsSource.ONLINE,
                provider = kind
            )
        }

        Timber.d("getLyrics: no online lyrics found for ${query.cacheKey}")
        cacheLyrics(query, MISS_SOURCE, rawContent = "")
        return NO_LYRICS
    }

    private suspend fun cacheLyrics(query: LyricsQuery, source: String, rawContent: String) {
        try {
            lyricsCacheDao.put(
                LyricsCacheEntity(
                    cacheKey = query.cacheKey,
                    source = source,
                    // Left null so the format is guessed on load: pinning the format a provider
                    // claims to send would downgrade word-by-word lyrics to line-level ones.
                    format = null,
                    rawContent = rawContent,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // The lyrics are already in hand; failing to cache them must not fail the lookup.
            Timber.w(e, "getLyrics: could not cache lyrics for ${query.cacheKey}")
        }
    }

    /** Timed lyrics, or `null` when the content carries no timings the player can follow. */
    private fun usableLyrics(content: String): SyncedLyrics? =
        parseLyrics(content)?.takeIf { it.lines.isNotEmpty() }

    /**
     * The provider a cached row came from.
     *
     * `lyrics_cache.source` stores the enum name lowercased (and `"none"` for a remembered miss),
     * so the round trip is a case-insensitive lookup. An unknown value — an old row written by a
     * provider that has since been removed — is reported as `null` rather than guessed.
     */
    private fun providerOf(source: String): LyricsProvider? =
        LyricsProvider.entries.firstOrNull { it.name.equals(source, ignoreCase = true) }

    /** The first line's text, used to tell two versions apart when the source gave no label. */
    private fun ISyncedLine.preview(): String? {
        val text = when (this) {
            is KaraokeLine -> syllables.joinToString("") { it.content }
            is SyncedLine -> content
            is UncheckedSyncedLine -> content
            else -> null
        } ?: return null
        return text.trim().takeIf { it.isNotEmpty() }
    }

    private fun parseLyrics(content: String): SyncedLyrics? {
        return try {
            parser.parse(content)
        } catch (e: Exception) {
            Timber.e(e, "parseLyrics: failed to parse lyrics")
            null
        }
    }

    /**
     * Stores [rawContent] in the search index as plain, tag-free text.
     *
     * Failure is logged, never propagated: indexing is a convenience and must not turn a working
     * lyrics lookup into an error. A blank document is not indexed at all, so a remembered miss
     * cannot make a song match every query.
     */
    private suspend fun index(songId: Long, rawContent: String?) {
        if (rawContent.isNullOrBlank()) return
        val plain = plainText(rawContent)
        if (plain.isBlank()) return
        try {
            lyricsIndexDao.put(
                LyricsIndexEntity(
                    songId = songId,
                    content = plain,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "index: could not index lyrics for songId=$songId")
        }
    }

    /**
     * Strips timings but keeps the words.
     *
     * `[00:12.34]`, `[ar:…]` and yrc's `(start,duration,0)` tags all go; line breaks are folded to
     * spaces so a phrase that spans a line boundary still matches, and so the row's snippet reads
     * as one sentence.
     */
    private fun plainText(rawContent: String): String = rawContent
        .replace(TIMING_TAG, "")
        .replace(YRC_TAG, "")
        .replace(XML_TAG, " ")
        .replace(WHITESPACE, " ")
        .trim()

    /**
     * How long a cached row is trusted, given what it holds.
     *
     * A remembered miss is kept for a week so that a track the providers later learn about — or one
     * the user re-tags — is looked up again rather than being empty for the life of the install.
     * Lyrics the player can follow word by word are kept just as long, because they are the best
     * any provider has.
     *
     * A copy that is only line-level is the one case worth re-checking sooner. Providers list the
     * same recording many times over and the entry closest in length does not always carry the
     * word-by-word document, so a fresh lookup has a real chance of doing better — and a row that
     * was written before the provider knew to look has no other way of ever being replaced.
     */
    private fun LyricsCacheEntity.hasExpired(parsed: SyncedLyrics?): Boolean {
        val ttl = when {
            rawContent.isBlank() -> SETTLED_TTL_MS
            parsed?.isWordByWord == true -> SETTLED_TTL_MS
            else -> LINE_LEVEL_TTL_MS
        }
        return System.currentTimeMillis() - createdAt > ttl
    }

    private companion object {
        val NO_LYRICS = Lyrics(syncedLyrics = null, rawContent = null, source = LyricsSource.NONE)

        /** Marks a cache row that records failure rather than lyrics; see [hasExpired]. */
        const val MISS_SOURCE = "none"

        /** How long an answer that cannot be improved on — lyrics, or a remembered miss — is kept. */
        const val SETTLED_TTL_MS = 7L * 24 * 60 * 60 * 1000

        /** How long a line-level copy is kept before it is worth asking the providers again. */
        const val LINE_LEVEL_TTL_MS = 24L * 60 * 60 * 1000

        /** `[00:12.34]`, `[01:02]`, `[ar:…]`, `[offset:…]` — everything bracketed is metadata. */
        val TIMING_TAG = Regex("\\[[^\\]]*]")

        /** yrc / Lyricify syllable tags, e.g. `(1200,480,0)`. */
        val YRC_TAG = Regex("\\(\\d+,\\d+,\\d+\\)")

        /** TTML and other XML lyric formats. */
        val XML_TAG = Regex("<[^>]+>")

        val WHITESPACE = Regex("\\s+")
    }
}
