package com.cycling.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cycling.data.local.dao.SongDao
import com.cycling.domain.model.CoverCandidate
import com.cycling.domain.model.CoverSource
import com.cycling.domain.model.Song
import com.cycling.domain.repository.CoverArtProvider
import com.cycling.domain.repository.CoverRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Stores the user's chosen cover art in app-private storage.
 *
 * The image is copied out of the provider's CDN (or the photo picker) into `files/covers`, and
 * only the app-private `file://` URI is recorded on the song. That is what makes the choice
 * durable: MediaStore resets its own album-art column on every scan, a provider's CDN URL can
 * expire, and a photo-picker URI's read grant ends with the process.
 *
 * Images are decoded and re-encoded to a bounded square-ish JPEG so a 4K scan of a record sleeve
 * cannot cost the app tens of megabytes of storage.
 */
@Singleton
class CoverRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val providers: Set<@JvmSuppressWildcards CoverArtProvider>,
    sharedClient: OkHttpClient
) : CoverRepository {

    /**
     * Fixed precedence: the Chinese catalogues first (the app's audience and its lyric sources are
     * Chinese-music-heavy), then iTunes for Western releases.
     *
     * Sources are asked one at a time and the first that answers wins. Merging all four would
     * show the same album several times over and cost four round trips for a five-item choice.
     */
    private val orderedProviders: List<CoverArtProvider> = providers.sortedBy { orderOf(it.source) }

    private val client = sharedClient.newBuilder()
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun findCandidates(song: Song): List<CoverCandidate> = withContext(Dispatchers.IO) {
        for (provider in orderedProviders) {
            val found = try {
                provider.search(song)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "findCandidates: ${provider.source} failed for ${song.title}")
                continue
            }
            if (found.isNotEmpty()) {
                Timber.d("findCandidates: ${provider.source} returned ${found.size} for ${song.title}")
                return@withContext found
            }
        }
        emptyList()
    }

    override suspend fun applyCandidate(song: Song, candidate: CoverCandidate): Result<String> =
        store(song) {
            download(candidate)
        }

    override suspend fun applyLocalImage(song: Song, imageUri: String): Result<String> =
        store(song) {
            context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { it.readBytes() }
                ?: throw IOException("cannot open picked image: $imageUri")
        }

    override suspend fun clearCustomCover(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            songDao.updateCustomAlbumArt(song.id, null)
            deleteAllCoverFiles(song.id)
            Timber.d("clearCustomCover: removed the cover for songId=${song.id}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "clearCustomCover: failed for songId=${song.id}")
            Result.failure(e)
        }
    }

    /**
     * Reads the image through [read], normalises it, writes it next to the app's other files and
     * points the song row at it. The DB write is the last step so a failed download or a corrupt
     * image never leaves the row pointing at a file that does not exist.
     *
     * The file name carries a timestamp (see [coverFileName]) so the URI changes on every update.
     * That is what makes a replacement visible: reusing one name would leave Coil serving the
     * previously cached image, which is exactly the "picked a local picture but the downloaded
     * cover stayed" bug this naming avoids.
     */
    private suspend fun store(song: Song, read: suspend () -> ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val normalized = normalizeCover(read())
                val file = newCoverFile(song.id)
                file.writeBytes(normalized)
                val artwork = Uri.fromFile(file).toString()
                songDao.updateCustomAlbumArt(song.id, artwork)
                pruneOldCoverFiles(song.id, keep = file)
                Timber.d("store: saved cover for songId=${song.id} (${normalized.size} bytes)")
                Result.success(artwork)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "store: failed to store the cover for songId=${song.id}")
                Result.failure(e)
            }
        }

    /**
     * Downloads the image behind [candidate].
     *
     * Chinese catalogues serve their covers from CDNs that sometimes check the referer, so one is
     * sent for those sources; iTunes does not care. Without it a perfectly good candidate URL can
     * answer 403 and the user sees "应用封面失败" for the only cover they wanted.
     */
    private fun download(candidate: CoverCandidate): ByteArray {
        val request = Request.Builder()
            .url(candidate.imageUrl)
            .apply {
                refererOf(candidate.source)?.let { header("Referer", it) }
            }
            .build()

        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("cover download failed: HTTP ${it.code}")
            val bytes = it.body?.bytes() ?: throw IOException("empty cover response")
            if (bytes.size > MAX_DOWNLOAD_BYTES) {
                throw IOException("cover image too large: ${bytes.size} bytes")
            }
            return bytes
        }
    }

    /** The referer a source's image CDN expects, or `null` when it does not check one. */
    private fun refererOf(source: CoverSource): String? = when (source) {
        CoverSource.NETEASE -> "https://music.163.com/"
        CoverSource.QQ_MUSIC -> "https://y.qq.com/"
        CoverSource.KUGOU -> "https://www.kugou.com/"
        CoverSource.ITUNES -> null
    }

    private fun coverDirectory(): File = File(context.filesDir, COVER_DIR)

    /**
     * A fresh, unique file for [songId].
     *
     * `System.currentTimeMillis()` is enough for human-paced taps; the existence loop only guards
     * against two updates landing in the same millisecond (which would bring the stale-cache bug
     * straight back).
     */
    private fun newCoverFile(songId: Long): File {
        val dir = coverDirectory()
        dir.mkdirs()
        var stamp = System.currentTimeMillis()
        var file = File(dir, coverFileName(songId, stamp))
        while (file.exists()) {
            stamp += 1
            file = File(dir, coverFileName(songId, stamp))
        }
        return file
    }

    /**
     * Deletes this song's other covers once they are too old for any live UI to be showing them.
     *
     * The just-replaced file is kept for a week on purpose: media3's `MediaItem` holds the artwork
     * URI it was created with, so deleting it immediately can blank the notification's cover while
     * the track is still playing. A week is far longer than any notification lives.
     */
    private fun pruneOldCoverFiles(songId: Long, keep: File) {
        val cutoff = System.currentTimeMillis() - COVER_RETENTION_MS
        coverDirectory().listFiles()?.forEach { file ->
            if (file.name != keep.name && isCoverFileForSong(file.name, songId) && file.lastModified() < cutoff) {
                if (file.delete()) {
                    Timber.d("pruneOldCoverFiles: deleted stale cover %s", file.name)
                }
            }
        }
    }

    private fun deleteAllCoverFiles(songId: Long) {
        coverDirectory().listFiles()?.forEach { file ->
            if (isCoverFileForSong(file.name, songId)) {
                file.delete()
            }
        }
    }

    /**
     * Decodes [bytes], scales it down to at most [MAX_COVER_PX] on the long side and re-encodes it
     * as JPEG.
     *
     * A non-image (an error page served with `200`) has to be rejected here rather than written to
     * disk: Coil would then show a permanently broken cover with no way to tell why.
     */
    private fun normalizeCover(bytes: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("cover is not a decodable image")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_COVER_PX) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: throw IOException("cover image could not be decoded")

        val longSide = maxOf(decoded.width, decoded.height)
        val scaled = if (longSide > MAX_COVER_PX) {
            val ratio = MAX_COVER_PX.toFloat() / longSide
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val compressed = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        return compressed
    }

    private companion object {
        const val COVER_DIR = "covers"
        const val CALL_TIMEOUT_SECONDS = 20L

        /** How long a replaced cover file is kept for consumers that still hold its URI. */
        const val COVER_RETENTION_MS = 7L * 24 * 60 * 60 * 1000

        /** Enough for a 500-600px album cover; larger than this and the cover is downscaled. */
        const val MAX_COVER_PX = 640

        /** An album cover does not need more than this. */
        const val JPEG_QUALITY = 90

        /** Guards against an endpoint answering with something enormous under an image URL. */
        const val MAX_DOWNLOAD_BYTES = 12 * 1024 * 1024

        fun orderOf(source: CoverSource): Int = when (source) {
            CoverSource.NETEASE -> 0
            CoverSource.QQ_MUSIC -> 1
            CoverSource.KUGOU -> 2
            CoverSource.ITUNES -> 3
        }
    }
}
