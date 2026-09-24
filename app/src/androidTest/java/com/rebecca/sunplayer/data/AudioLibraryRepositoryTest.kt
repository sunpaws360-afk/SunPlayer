package com.rebecca.sunplayer.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rebecca.sunplayer.model.AudioTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioLibraryRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: AudioLibraryRepository
    private val databaseName = "scan-result-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = AudioLibraryRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun failedScanPreservesPreviouslyIndexedTracks() = runBlocking {
        val track = AudioTrack(
            id = 99L,
            uri = android.net.Uri.EMPTY,
            title = "Keep me",
            artist = "Artist",
            album = "Album",
            durationMs = 120_000L,
            sizeBytes = 1_024L
        )

        repository.scanAndStore { ScanResult.Success(listOf(track)) }
        val result = repository.scanAndStore {
            ScanResult.Failure(IllegalStateException("MediaStore unavailable"))
        }

        assertEquals(true, result is ScanResult.Failure)
        assertEquals(listOf(track), repository.tracks.first())
    }
}
