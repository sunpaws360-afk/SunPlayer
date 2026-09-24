package com.rebecca.sunplayer.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDaoTest {
    private lateinit var database: SunPlayerDatabase
    private lateinit var dao: TrackDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SunPlayerDatabase::class.java).build()
        dao = database.trackDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceAll_removesTracksMissingFromLatestScan() = runBlocking {
        val firstTrack = track(id = 1L, title = "First")
        val secondTrack = track(id = 2L, title = "Second")

        dao.replaceAll(listOf(firstTrack, secondTrack).map(TrackEntity::fromAudioTrack))
        dao.replaceAll(listOf(secondTrack).map(TrackEntity::fromAudioTrack))

        val storedTracks = dao.getTracks()

        assertEquals(listOf(2L), storedTracks.map { it.id })
    }

    @Test
    fun favoriteSurvivesReplacingScanSnapshot() = runBlocking {
        val track = track(id = 3L, title = "Favorite")

        dao.replaceAll(listOf(TrackEntity.fromAudioTrack(track)))
        dao.setFavorite(track.id, true)
        dao.replaceAll(listOf(TrackEntity.fromAudioTrack(track.copy(title = "Updated Favorite"))))

        assertEquals(listOf(3L), dao.getFavoriteIds())
        assertEquals("Updated Favorite", dao.getTracks().single().title)
    }

    private fun track(id: Long, title: String): AudioTrack = AudioTrack(
        id = id,
        uri = android.net.Uri.EMPTY,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 120_000L,
        sizeBytes = 1_024L
    )
}
