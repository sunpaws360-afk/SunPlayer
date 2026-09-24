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
    ).addMigrations(SunPlayerDatabase.MIGRATION_1_2).build()

    private val trackDao = database.trackDao()

    val tracks: Flow<List<AudioTrack>> = trackDao.observeTracks().map { entities ->
        entities.map(TrackEntity::toAudioTrack)
    }

    val favoriteIds: Flow<Set<Long>> = trackDao.observeFavoriteIds().map { it.toSet() }

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

    fun close() {
        database.close()
    }
}
