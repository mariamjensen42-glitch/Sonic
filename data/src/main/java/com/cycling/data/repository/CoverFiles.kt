package com.cycling.data.repository

/**
 * Naming rules for the copies of cover art the app keeps in private storage.
 *
 * The file name carries a stamp on purpose. Coil keys its memory cache by the model's URI, so
 * reusing `1.jpg` for every replacement keeps serving the *previous* image after the file has
 * been overwritten — applying a network cover then picking a local picture (or the other way
 * round) looks like the second action did nothing. A fresh URI per update sidesteps the cache
 * without having to reach into Coil, and also busts the artwork cache in media3's notification
 * loader.
 *
 * The legacy un-stamped `1.jpg` name is still recognised so covers stored by an earlier build
 * get cleaned up instead of lingering forever.
 */
internal fun coverFileName(songId: Long, stamp: Long): String = "${coverFilePrefix(songId)}$stamp.jpg"

/** Every stamped name for [songId] starts with this; `1-` cannot match song `12`. */
internal fun coverFilePrefix(songId: Long): String = "$songId-"

/** True for the legacy `1.jpg` name and for every stamped `1-<stamp>.jpg`. */
internal fun isCoverFileForSong(fileName: String, songId: Long): Boolean =
    fileName == "$songId.jpg" || fileName.startsWith(coverFilePrefix(songId))
