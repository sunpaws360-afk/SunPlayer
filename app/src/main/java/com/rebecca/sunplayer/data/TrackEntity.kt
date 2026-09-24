package com.rebecca.sunplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rebecca.sunplayer.model.AudioTrack

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val isFavorite: Boolean = false
) {
    fun toAudioTrack(): AudioTrack = AudioTrack(
        id = id,
        uri = android.net.Uri.parse(uri),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        sizeBytes = sizeBytes
    )

    companion object {
        fun fromAudioTrack(track: AudioTrack, isFavorite: Boolean = false): TrackEntity = TrackEntity(
            id = track.id,
            uri = track.uri.toString(),
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            sizeBytes = track.sizeBytes,
            isFavorite = isFavorite
        )
    }
}
