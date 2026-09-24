package com.rebecca.sunplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
abstract class SunPlayerDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
