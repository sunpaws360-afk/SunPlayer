package com.rebecca.sunplayer.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rebecca.sunplayer.model.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackEntityTest {
    @Test
    fun fromAudioTrack_preservesLibraryFields() {
        val track = AudioTrack(
            id = 42L,
            uri = Uri.EMPTY,
            title = "Solar Wind",
            artist = "Sun Player",
            album = "Daylight",
            durationMs = 185_000L,
            sizeBytes = 4_096L
        )

        val entity = TrackEntity.fromAudioTrack(track)

        assertEquals(track.id, entity.id)
        assertEquals(track.uri.toString(), entity.uri)
        assertEquals(track.title, entity.title)
        assertEquals(track.artist, entity.artist)
        assertEquals(track.album, entity.album)
        assertEquals(track.durationMs, entity.durationMs)
        assertEquals(track.sizeBytes, entity.sizeBytes)
    }
}
