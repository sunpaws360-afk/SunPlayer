package com.rebecca.sunplayer.data

import com.rebecca.sunplayer.model.AudioTrack

data class PlaylistTrackRow(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long,
    val trackUri: String?,
    val trackTitle: String?,
    val trackArtist: String?,
    val trackAlbum: String?,
    val trackDurationMs: Long?,
    val trackSizeBytes: Long?
) {
    val isAvailable: Boolean
        get() = trackUri != null

    fun toAudioTrackOrNull(): AudioTrack? {
        val uri = trackUri ?: return null
        return AudioTrack(
            id = trackId,
            uri = android.net.Uri.parse(uri),
            title = trackTitle ?: "Unknown Track",
            artist = trackArtist ?: "Unknown Artist",
            album = trackAlbum ?: "Unknown Album",
            durationMs = trackDurationMs ?: 0L,
            sizeBytes = trackSizeBytes ?: 0L
        )
    }
}
