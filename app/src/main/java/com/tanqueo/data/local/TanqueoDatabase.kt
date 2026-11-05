package com.tanqueo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FillUpEntity::class], version = 1, exportSchema = true)
abstract class TanqueoDatabase : RoomDatabase() {
    abstract fun fillUpDao(): FillUpDao
}
