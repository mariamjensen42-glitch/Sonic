package com.cycling.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: user-owned song metadata, per-song state, and the online-lyrics cache.
 *
 * Everything here is additive, which matters more than it looks: the previous build shipped
 * `fallbackToDestructiveMigration()`, so this is the first upgrade path that has to *preserve*
 * data. The SQL below must match Room's generated schema byte for byte or the post-migration
 * validation check fails at open time — `schemas/…/2.json` is the source of truth, and
 * `AppDatabaseMigrationTest` locks it down.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Songs: star rating and a user-chosen cover. Both are preserved across rescans by
        // MusicScanner, which is why they live on `songs` rather than in `song_state`.
        db.execSQL("ALTER TABLE songs ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE songs ADD COLUMN customAlbumArt TEXT")

        db.execSQL("ALTER TABLE playlists ADD COLUMN customCover TEXT")

        // Per-song resume position and lyrics offset: written often, read for one song at a time.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `song_state` (
                `songId` INTEGER NOT NULL,
                `resumePositionMs` INTEGER NOT NULL DEFAULT 0,
                `lyricsOffsetMs` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`songId`),
                FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Downloaded lyrics, keyed by a portable `title|artist|duration` fingerprint.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lyrics_cache` (
                `cacheKey` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `format` TEXT,
                `rawContent` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`cacheKey`)
            )
            """.trimIndent()
        )
    }
}
