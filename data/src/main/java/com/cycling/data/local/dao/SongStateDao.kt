package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cycling.data.local.entity.SongStateEntity

@Dao
interface SongStateDao {

    @Query("SELECT * FROM song_state WHERE songId = :songId")
    suspend fun get(songId: Long): SongStateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(state: SongStateEntity)

    /**
     * Single-statement upsert for the two hot columns.
     *
     * Written as raw `ON CONFLICT` rather than Room's `@Upsert` on purpose: an `@Upsert` from a
     * partially-populated entity would zero the column it does not carry, and resume positions
     * are written every few seconds — a read-modify-write here would be both slower and racy.
     *
     * SQLite UPSERT landed in 3.24; minSdk 30 ships 3.28+, so this is safe on every target.
     */
    @Query(
        """
        INSERT INTO song_state (songId, resumePositionMs, lyricsOffsetMs, updatedAt)
        VALUES (:songId, :resumePositionMs, 0, :updatedAt)
        ON CONFLICT(songId) DO UPDATE SET
            resumePositionMs = :resumePositionMs,
            updatedAt = :updatedAt
        """
    )
    suspend fun upsertResumePosition(songId: Long, resumePositionMs: Long, updatedAt: Long)

    @Query(
        """
        INSERT INTO song_state (songId, resumePositionMs, lyricsOffsetMs, updatedAt)
        VALUES (:songId, 0, :lyricsOffsetMs, :updatedAt)
        ON CONFLICT(songId) DO UPDATE SET
            lyricsOffsetMs = :lyricsOffsetMs,
            updatedAt = :updatedAt
        """
    )
    suspend fun upsertLyricsOffset(songId: Long, lyricsOffsetMs: Long, updatedAt: Long)

    @Query("DELETE FROM song_state WHERE songId = :songId")
    suspend fun delete(songId: Long)
}
