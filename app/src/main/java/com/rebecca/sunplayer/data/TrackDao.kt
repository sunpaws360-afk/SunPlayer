package com.rebecca.sunplayer.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getTracks(): List<TrackEntity>

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) {
            clear()
        } else {
            upsertAll(tracks)
            deleteMissing(tracks.map { it.id })
        }
    }
}
