package com.rebecca.sunplayer.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SunPlayerDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tracks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS playlists (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS playlist_tracks (" +
                        "playlistId INTEGER NOT NULL, " +
                        "trackId INTEGER NOT NULL, " +
                        "position INTEGER NOT NULL, " +
                        "addedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(playlistId, trackId))"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId_position " +
                        "ON playlist_tracks(playlistId, position)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_tracks_trackId " +
                        "ON playlist_tracks(trackId)"
                )
            }
        }
    }
}
