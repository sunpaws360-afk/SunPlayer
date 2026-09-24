package com.rebecca.sunplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query(
        "SELECT p.id, p.name, p.createdAt, p.updatedAt, COUNT(pt.trackId) AS trackCount " +
            "FROM playlists p LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.name COLLATE NOCASE ASC"
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)

    @Query(
        "SELECT pt.playlistId, pt.trackId, pt.position, pt.addedAt, " +
            "t.uri AS trackUri, t.title AS trackTitle, t.artist AS trackArtist, " +
            "t.album AS trackAlbum, t.durationMs AS trackDurationMs, " +
            "t.sizeBytes AS trackSizeBytes " +
            "FROM playlist_tracks pt LEFT JOIN tracks t ON pt.trackId = t.id " +
            "WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC"
    )
    fun observePlaylistEntries(playlistId: Long): Flow<List<PlaylistTrackRow>>

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTrackIds(playlistId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun observeTrackCount(playlistId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCount(playlistId: Long): Int

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updatePosition(playlistId: Long, trackId: Long, position: Int)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, updatedAt: Long)

    @Transaction
    suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistTracks(playlistId)
        deletePlaylistRow(playlistId)
    }

    @Transaction
    suspend fun addTrack(playlistId: Long, trackId: Long, now: Long): Boolean {
        val inserted = insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = getTrackCount(playlistId),
                addedAt = now
            )
        )
        if (inserted != -1L) touchPlaylist(playlistId, now)
        return inserted != -1L
    }

    @Transaction
    suspend fun moveTrack(playlistId: Long, trackId: Long, newPosition: Int, now: Long) {
        val ids = getTrackIds(playlistId).filter { it != trackId }.toMutableList()
        if (trackId !in getTrackIds(playlistId)) return
        ids.add(newPosition.coerceIn(0, ids.size), trackId)
        ids.forEachIndexed { index, id -> updatePosition(playlistId, id, index) }
        touchPlaylist(playlistId, now)
    }
}
