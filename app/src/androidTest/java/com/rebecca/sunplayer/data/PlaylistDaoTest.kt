package com.rebecca.sunplayer.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {
    private lateinit var database: SunPlayerDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SunPlayerDatabase::class.java).build()
        trackDao = database.trackDao()
        playlistDao = database.playlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun playlistCrudAndOrderingPersist() = runBlocking {
        val tracks = listOf(
            track(1L, "A"),
            track(2L, "B"),
            track(3L, "C")
        )
        trackDao.replaceAll(tracks.map(TrackEntity::fromAudioTrack))
        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(0L, "Road Trip", 1L, 1L)
        )

        assertTrue(playlistDao.addTrack(playlistId, 1L, 2L))
        assertTrue(playlistDao.addTrack(playlistId, 2L, 3L))
        assertTrue(playlistDao.addTrack(playlistId, 3L, 4L))
        assertEquals(listOf(1L, 2L, 3L), playlistDao.getTrackIds(playlistId))

        playlistDao.moveTrack(playlistId, 3L, 0, 5L)
        assertEquals(listOf(3L, 1L, 2L), playlistDao.getTrackIds(playlistId))

        playlistDao.renamePlaylist(playlistId, "Road Trip Updated", 6L)
        assertEquals("Road Trip Updated", playlistDao.observePlaylists().firstValue().single().name)
    }

    @Test
    fun duplicateMembershipIsIgnoredAndMissingTrackDoesNotDeletePlaylist() = runBlocking {
        val track = track(10L, "Available")
        trackDao.replaceAll(listOf(TrackEntity.fromAudioTrack(track)))
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(0L, "Keep", 1L, 1L))

        assertTrue(playlistDao.addTrack(playlistId, track.id, 2L))
        assertEquals(false, playlistDao.addTrack(playlistId, track.id, 3L))

        trackDao.clear()

        assertEquals(listOf(track.id), playlistDao.getTrackIds(playlistId))
        playlistDao.deletePlaylist(playlistId)
        assertEquals(emptyList<Long>(), playlistDao.getTrackIds(playlistId))
    }

    @Test
    fun reorderSupportsFirstLastAndSamePosition() = runBlocking {
        val tracks = listOf(track(20L, "A"), track(21L, "B"), track(22L, "C"))
        trackDao.replaceAll(tracks.map(TrackEntity::fromAudioTrack))
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(0L, "Order", 1L, 1L))
        tracks.forEachIndexed { index, track ->
            playlistDao.addTrack(playlistId, track.id, index.toLong())
        }

        playlistDao.moveTrack(playlistId, 1L, 2, 4L)
        assertEquals(listOf(21L, 22L, 20L), playlistDao.getTrackIds(playlistId))
        playlistDao.moveTrack(playlistId, 20L, 0, 5L)
        assertEquals(listOf(20L, 21L, 22L), playlistDao.getTrackIds(playlistId))
        playlistDao.moveTrack(playlistId, 21L, 1, 6L)
        assertEquals(listOf(20L, 21L, 22L), playlistDao.getTrackIds(playlistId))
    }

    @Test
    fun emptyAndSingleTrackPlaylistsAreValid() = runBlocking {
        val emptyId = playlistDao.insertPlaylist(PlaylistEntity(0L, "Empty", 1L, 1L))
        assertEquals(emptyList<Long>(), playlistDao.getTrackIds(emptyId))

        val track = track(30L, "Only")
        trackDao.replaceAll(listOf(TrackEntity.fromAudioTrack(track)))
        val singleId = playlistDao.insertPlaylist(PlaylistEntity(0L, "Single", 1L, 1L))
        playlistDao.addTrack(singleId, track.id, 2L)
        playlistDao.moveTrack(singleId, track.id, 0, 3L)
        assertEquals(listOf(track.id), playlistDao.getTrackIds(singleId))
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

private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.firstValue(): List<T> =
    runBlocking { first() }
