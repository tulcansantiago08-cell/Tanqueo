package com.tanqueo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FillUpEntity::class], version = 2, exportSchema = true)
abstract class TanqueoDatabase : RoomDatabase() {
    abstract fun fillUpDao(): FillUpDao

    companion object {
        /** Versión 2: cada tanqueo dice si fue del carro o de la moto. Los que ya había eran del carro. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `fill_ups` ADD COLUMN `vehicle_type` TEXT NOT NULL DEFAULT 'CARRO'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fill_ups_vehicle_type` ON `fill_ups` (`vehicle_type`)")
            }
        }
    }
}
