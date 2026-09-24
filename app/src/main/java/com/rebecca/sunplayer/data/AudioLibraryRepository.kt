package com.rebecca.sunplayer.data

import android.content.Context
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioLibraryRepository(context: Context) {
    private val database = androidx.room.Room.databaseBuilder(
        context.applicationContext,
        SunPlayerDatabase::class.java,
        "sunplayer.db"
    ).addMigrations(
        SunPlayerDatabase.MIGRATION_1_2,
        SunPlayerDatabase.MIGRATION_2_3
    ).build()

    private val trackDao = database.trackDao()

    val tracks: Flow<List<AudioTrack>> = trackDao.observeTracks().map { entities ->
        entities.map(TrackEntity::toAudioTrack)
    }

    val favoriteIds: Flow<Set<Long>> = trackDao.observeFavoriteIds().map { it.toSet() }

    val playlists: Flow<List<PlaylistSummary>> = database.playlistDao().observePlaylists()

    private val playlistDao = database.playlistDao()

    suspend fun scanAndStore(scanner: AudioScanner): List<AudioTrack> {
        val scannedTracks = scanner.scanAudioTracks()
        val favoriteIds = trackDao.getFavoriteIds()
        trackDao.replaceAll(scannedTracks.map { track ->
            TrackEntity.fromAudioTrack(track, isFavorite = track.id in favoriteIds)
        })
        return scannedTracks
    }

    suspend fun setFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.setFavorite(trackId, isFavorite)
    }

    fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> =
        playlistDao.observePlaylistTracks(playlistId).map { tracks ->
            tracks.map(TrackEntity::toAudioTrack)
        }

    fun observePlaylistTrackCount(playlistId: Long): Flow<Int> =
        playlistDao.observeTrackCount(playlistId)

    suspend fun createPlaylist(name: String): Long {
        val now = System.currentTimeMillis()
        return playlistDao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now)
        )
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        playlistDao.renamePlaylist(playlistId, name.trim(), System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Boolean =
        playlistDao.addTrack(playlistId, trackId, System.currentTimeMillis())

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrack(playlistId, trackId)
        playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
    }

    suspend fun moveTrackInPlaylist(playlistId: Long, trackId: Long, position: Int) {
        playlistDao.moveTrack(playlistId, trackId, position, System.currentTimeMillis())
    }

    fun close() {
        database.close()
    }
}
