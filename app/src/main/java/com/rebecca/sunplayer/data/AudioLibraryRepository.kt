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
    ).build()

    private val trackDao = database.trackDao()

    val tracks: Flow<List<AudioTrack>> = trackDao.observeTracks().map { entities ->
        entities.map(TrackEntity::toAudioTrack)
    }

    suspend fun scanAndStore(scanner: AudioScanner): List<AudioTrack> {
        val scannedTracks = scanner.scanAudioTracks()
        trackDao.replaceAll(scannedTracks.map(TrackEntity::fromAudioTrack))
        return scannedTracks
    }

    fun close() {
        database.close()
    }
}
