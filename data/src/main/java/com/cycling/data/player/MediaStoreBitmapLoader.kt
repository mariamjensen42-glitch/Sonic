package com.cycling.data.player

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads cover art for the media notification and the system media control.
 *
 * media3's stock loader opens the artwork URI as a file, which the app's album-art URIs do not
 * survive: `Song.albumArt` is a `MediaStore.Audio.Albums` row
 * (`content://media/external/audio/albums/<albumId>`), and opening one of those throws
 * `SQLiteException: no such column: _data` out of MediaProvider on Android 10+. That failure is
 * what left the notification, and with it the system control bar, showing no cover — media3 logs
 * it as "Failed to load bitmap" from both `NotificationProvider` and `MediaSessionLegacyStub`.
 *
 * So this loader goes through [ContentResolver.loadThumbnail], which is the only supported way to
 * read album art on modern Android, using a track of the album as the handle. Anything that is not
 * an albums row (a user-supplied cover, say) falls back to reading the URI as an image directly.
 */
@Singleton
@OptIn(UnstableApi::class)
class MediaStoreBitmapLoader @Inject constructor(
    @ApplicationContext private val context: Context
) : BitmapLoader {

    // Single-threaded on purpose: artwork is decorative and low priority, and serialising it keeps
    // a queue of tracks from forking a load per item.
    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = executor.submit(
        Callable<Bitmap> {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IOException("Could not decode image data")
        }
    )

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit(
        Callable<Bitmap> {
            load(uri) ?: throw IOException("Could not load image from $uri")
        }
    )

    private fun load(uri: Uri): Bitmap? = try {
        context.contentResolver.loadThumbnail(thumbnailSource(uri), THUMBNAIL_SIZE, null)
    } catch (e: Exception) {
        Timber.w(e, "loadBitmap: thumbnail of %s failed, reading it as an image instead", uri)
        openStream(uri)
    }

    /**
     * Redirects a legacy albums row at a track that belongs to it, because only media rows can be
     * asked for a thumbnail. Falls back to the original URI so a caller that already passed
     * something loadable still gets its chance above.
     */
    private fun thumbnailSource(uri: Uri): Uri {
        if (!isAlbumArtRow(uri)) return uri
        return firstTrackOfAlbum(ContentUris.parseId(uri)) ?: uri
    }

    private fun isAlbumArtRow(uri: Uri): Boolean =
        uri.scheme == ContentResolver.SCHEME_CONTENT &&
            uri.toString().startsWith("${MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI}/")

    private fun firstTrackOfAlbum(albumId: Long): Uri? = runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.ALBUM_ID} = ?",
            arrayOf(albumId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(0)
                )
            } else {
                null
            }
        }
    }.onFailure { Timber.w(it, "firstTrackOfAlbum: lookup failed for album %d", albumId) }
        .getOrNull()

    private fun openStream(uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)

    private companion object {
        /** Album art is only ever shown small; decoding a full-resolution cover wastes memory. */
        val THUMBNAIL_SIZE = Size(512, 512)
    }
}
