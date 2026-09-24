package com.rebecca.sunplayer.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "migration-v2-v3-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "CREATE TABLE tracks (" +
                            "id INTEGER NOT NULL PRIMARY KEY, " +
                            "uri TEXT NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, " +
                            "album TEXT NOT NULL, durationMs INTEGER NOT NULL, " +
                            "sizeBytes INTEGER NOT NULL, isFavorite INTEGER NOT NULL)"
                    )
                    database.execSQL(
                        "INSERT INTO tracks VALUES (7, 'content://track/7', 'Saved', 'Artist', 'Album', 1000, 10, 1)"
                    )
                }

                override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration2To3_preservesTracksFavoritesAndAddsPlaylistTables() {
        val database = helper.writableDatabase

        SunPlayerDatabase.MIGRATION_2_3.migrate(database)

        database.query("SELECT isFavorite FROM tracks WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        assertTrue(tableExists(database, "playlists"))
        assertTrue(tableExists(database, "playlist_tracks"))

        database.execSQL("INSERT INTO playlists(name, createdAt, updatedAt) VALUES ('Saved List', 1, 1)")
        database.execSQL("INSERT INTO playlist_tracks VALUES (1, 7, 0, 1)")
        database.query("SELECT trackId FROM playlist_tracks WHERE playlistId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7L, cursor.getLong(0))
        }
    }

    private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }
}
