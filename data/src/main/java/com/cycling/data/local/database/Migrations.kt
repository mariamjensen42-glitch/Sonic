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

/**
 * v2 → v3: the playlist description written by the AI playlist generator.
 *
 * Additive like every migration here — existing playlists keep a NULL description and the
 * detail page simply hides the section for them.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE playlists ADD COLUMN description TEXT")
    }
}

/**
 * v3 → v4: lyrics the user wrote themselves.
 *
 * Its own table rather than a column on `songs` or a row in `lyrics_cache`: the cache is keyed by
 * a `title|artist|duration` fingerprint that moves when the user re-tags the file, and a lyrics
 * blob on `songs` would be rewritten by every scan's upsert. Cascade with `songs` so a row cannot
 * outlive the track and be served for a different file that later reuses the id.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lyrics_override` (
                `songId` INTEGER NOT NULL,
                `rawContent` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`songId`),
                FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

/**
 * v4 → v5: searchable lyrics text.
 *
 * A per-song index rather than searching `lyrics_cache`: the cache only holds downloads and is
 * keyed by a fingerprint, while this covers sidecar files, embedded tags and user text too.
 * Existing rows stay unindexed until their lyrics are resolved once (played, opened, or edited).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lyrics_index` (
                `songId` INTEGER NOT NULL,
                `content` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`songId`),
                FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

/**
 * v5 → v6: 同一首歌在一张歌单里只能有一行。
 *
 * 之前插入从不查重，"添加到歌单"可以反复把同一首塞进去。歌单详情页拿 `song.id` 当
 * LazyColumn 的 key，重复行会让 Compose 抛 "Key ... was already used" 直接闪退 ——
 * 这就是本次要修的问题。唯一索引把这条不变量落到库里；先 DELETE 再建索引，是因为
 * 已有的重复行会让建索引失败。
 *
 * 每组只留 `MIN(id)`，也就是用户第一次把它加进去的那一行：位置保持不变，歌单看起来
 * 还是他当初排的顺序。
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM playlist_songs
            WHERE id NOT IN (SELECT MIN(id) FROM playlist_songs GROUP BY playlistId, songId)
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_songs_playlistId_songId` " +
                "ON `playlist_songs` (`playlistId`, `songId`)"
        )
    }
}

/**
 * v6 → v7: the tag fields the user edited in the app.
 *
 * A separate table rather than columns on `songs`: these are user-owned overrides of file tags,
 * they must survive rescans (which rewrite the scan-owned columns), and they apply to formats
 * whose tags the app cannot write. A `null` column means "not overridden", so the file's value is
 * still what the detail page shows. Cascades with `songs` for the usual reason.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `song_metadata` (
                `songId` INTEGER NOT NULL,
                `albumArtist` TEXT,
                `year` TEXT,
                `genre` TEXT,
                `trackNumber` TEXT,
                `discNumber` TEXT,
                `composer` TEXT,
                `publisher` TEXT,
                `copyright` TEXT,
                `comment` TEXT,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`songId`),
                FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

/**
 * v7 → v8: 播放事件表，听歌统计与排行榜的数据源。
 *
 * 新增一张表而不是在 `songs` 上加列：时长榜、时间脉络、小时分布都要按**事件**聚合
 * （"这段时间里听了多久""每天听了多少"），累计列回答不了这些问题。字段按"一次播放"
 * 组织（曲目快照 + 实际时长 + 是否听完），便于以后做云端同步时直接映射。
 *
 * `playMs` 与 `playedAt` 各建一个索引：按天/按小时/按窗口的查询都以 `playedAt` 为条件，
 * 排行榜按 `playMs` 排序。
 *
 * 这是纯新增表，没有改动既有列 —— 老用户的播放次数、收藏、歌单都不受影响。
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `play_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `platform` TEXT NOT NULL,
                `songId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `artist` TEXT NOT NULL,
                `artistId` INTEGER NOT NULL,
                `album` TEXT NOT NULL,
                `coverUrl` TEXT,
                `durationMs` INTEGER NOT NULL,
                `playedAt` INTEGER NOT NULL,
                `playMs` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `source` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_play_events_playedAt` " +
                "ON `play_events` (`playedAt`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_play_events_songId` " +
                "ON `play_events` (`songId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_play_events_platform` " +
                "ON `play_events` (`platform`)"
        )
    }
}

/**
 * 在线曲目变成一等公民：给 `songs` 加平台键与平台 id。
 *
 * 两列都可空，本地行保持 null，不需要回填。之所以不建新表：收藏、歌单、播放次数、
 * 统计都挂在 `songs.id` 上，共用一张表才能让在线曲目直接复用这些能力。
 *
 * 在线行的 `path` 存空串 —— 它是"文件的路径"，而在线曲目没有文件。
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `onlineSourceKey` TEXT")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `onlineSongId` TEXT")
    }
}
