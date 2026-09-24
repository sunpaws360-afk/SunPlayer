package com.rebecca.sunplayer.data

import androidx.room.Entity

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [
        androidx.room.Index(value = ["playlistId", "position"]),
        androidx.room.Index(value = ["trackId"])
    ]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long
)
