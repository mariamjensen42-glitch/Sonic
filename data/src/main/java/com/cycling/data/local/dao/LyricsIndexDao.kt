package com.cycling.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cycling.data.local.entity.LyricsIndexEntity
import com.cycling.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/** A lyrics hit: the whole song row plus the indexed text it matched. */
data class LyricsSearchRow(
    @Embedded val song: SongEntity,
    val content: String
)

@Dao
interface LyricsIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsIndexEntity)

    @Query("SELECT * FROM lyrics_index WHERE songId = :songId")
    suspend fun get(songId: Long): LyricsIndexEntity?

    @Query("DELETE FROM lyrics_index WHERE songId = :songId")
    suspend fun delete(songId: Long)

    /**
     * Full-text-ish search over the indexed lyrics.
     *
     * `LIKE` rather than FTS: the project has no `@TypeConverter`s and no FTS table, and the
     * library is small enough that a scan over one text column is not the bottleneck. Only rows
     * whose lyrics were actually resolved are searchable — see [LyricsIndexEntity].
     */
    @Transaction
    @Query(
        """
        SELECT s.*, li.content AS content FROM songs s
        INNER JOIN lyrics_index li ON s.id = li.songId
        WHERE li.content LIKE '%' || :query || '%'
        ORDER BY s.title ASC
        LIMIT 50
        """
    )
    fun search(query: String): Flow<List<LyricsSearchRow>>
}
