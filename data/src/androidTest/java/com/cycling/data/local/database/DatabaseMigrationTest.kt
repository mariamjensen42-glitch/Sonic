package com.cycling.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [MIGRATION_1_2] against a database built with the real v1 DDL.
 *
 * Scope note: this drives the migration directly rather than going through
 * `MigrationTestHelper`. That is deliberate — `exportSchema` was off in v1, so there is no
 * `1.json` to feed the helper, and fabricating one would mean testing a reconstruction of v1
 * rather than v1 itself. What this covers is exactly what the migration must not break: existing
 * rows keep their values and their playlist links. What it does *not* cover is Room's own
 * post-migration schema validation, which happens on open and is locked down instead by the
 * checked-in `schemas/…/2.json`.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var databaseName: String
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseName = "migration-test-${System.nanoTime()}.db"
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createV1(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
    }

    @After
    fun tearDown() {
        helper.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun migratingToV2KeepsSongsFavouritesAndPlaylistLinks() {
        val db = helper.writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON")

        db.execSQL(
            """
            INSERT INTO songs (id, title, artist, album, duration, path, albumId, artistId,
                dateAdded, dateModified, size, mimeType, albumArt, isFavorite, playCount,
                lastPlayedAt, bitrate)
            VALUES (1, 'Song', 'Artist', 'Album', 180000, '/music/1.mp3', 1, 1,
                100, 200, 5000, 'audio/mpeg', NULL, 1, 7, 555, 320)
            """.trimIndent()
        )
        db.execSQL("INSERT INTO playlists (id, name, dateAdded, dateModified) VALUES (10, 'My List', 1, 1)")
        db.execSQL("INSERT INTO playlist_songs (id, playlistId, songId, position) VALUES (100, 10, 1, 0)")

        MIGRATION_1_2.migrate(db)

        db.query("SELECT isFavorite, playCount, lastPlayedAt, rating, customAlbumArt FROM songs WHERE id = 1").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("favourite lost in migration", 1, c.getInt(0))
            assertEquals("play count lost in migration", 7, c.getInt(1))
            assertEquals(555L, c.getLong(2))
            assertEquals("new rating column should default to 0", 0, c.getInt(3))
            assertNull("new cover column should default to null", c.getString(4))
        }

        db.query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = 10").use { c ->
            c.moveToFirst()
            assertEquals("playlist link lost in migration", 1, c.getInt(0))
        }
    }

    @Test
    fun migratingToV2CreatesNewTablesAndColumnsAsWritable() {
        val db = helper.writableDatabase

        MIGRATION_1_2.migrate(db)

        // New columns accept writes.
        db.execSQL("INSERT INTO songs (id, title, artist, album, duration, path, albumId, artistId, dateAdded, dateModified, size, mimeType, isFavorite, playCount, bitrate) VALUES (1, 'S', 'A', 'B', 1, '/p', 1, 1, 0, 0, 0, 'audio/mpeg', 0, 0, 0)")
        db.execSQL("UPDATE songs SET rating = 4, customAlbumArt = 'file:///c.jpg' WHERE id = 1")
        db.execSQL("INSERT INTO playlists (id, name, dateAdded, dateModified) VALUES (1, 'P', 0, 0)")
        db.execSQL("UPDATE playlists SET customCover = 'file:///p.jpg' WHERE id = 1")

        // New tables accept writes and the song_state foreign key resolves.
        db.execSQL("INSERT INTO song_state (songId, resumePositionMs, lyricsOffsetMs, updatedAt) VALUES (1, 12345, -250, 99)")
        db.execSQL("INSERT INTO lyrics_cache (cacheKey, source, format, rawContent, createdAt) VALUES ('k', 'lrclib', 'LRC', '[00:01.00]hi', 5)")

        db.query("SELECT resumePositionMs, lyricsOffsetMs FROM song_state WHERE songId = 1").use { c ->
            c.moveToFirst()
            assertEquals(12_345L, c.getLong(0))
            assertEquals(-250L, c.getLong(1))
        }
        db.query("SELECT source, rawContent FROM lyrics_cache WHERE cacheKey = 'k'").use { c ->
            c.moveToFirst()
            assertEquals("lrclib", c.getString(0))
            assertEquals("[00:01.00]hi", c.getString(1))
        }
    }

    /**
     * `song_state` cascades with its song, which is how per-song state cleans itself up when a
     * file is genuinely removed. Note this is the *delete* path — the rescan path must never
     * take it, which is what [com.cycling.data.local.scanner.SongLibraryWriterTest] pins down.
     */
    @Test
    fun songStateRowsDisappearWithTheirSong() {
        val db = helper.writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO songs (id, title, artist, album, duration, path, albumId, artistId, dateAdded, dateModified, size, mimeType, isFavorite, playCount, bitrate) VALUES (1, 'S', 'A', 'B', 1, '/p', 1, 1, 0, 0, 0, 'audio/mpeg', 0, 0, 0)")

        MIGRATION_1_2.migrate(db)

        db.execSQL("INSERT INTO song_state (songId, resumePositionMs, lyricsOffsetMs, updatedAt) VALUES (1, 5000, 0, 1)")
        db.execSQL("DELETE FROM songs WHERE id = 1")

        db.query("SELECT COUNT(*) FROM song_state").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }

    private fun createV1(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `path` TEXT NOT NULL, `albumId` INTEGER NOT NULL, `artistId` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL, `size` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `albumArt` TEXT, `isFavorite` INTEGER NOT NULL, `playCount` INTEGER NOT NULL, `lastPlayedAt` INTEGER, `bitrate` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `dateModified` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_info_cache` (`cacheKey` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
    }
}
