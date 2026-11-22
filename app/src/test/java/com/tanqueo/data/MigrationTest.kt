package com.tanqueo.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tanqueo.data.local.TanqueoDatabase
import com.tanqueo.domain.model.VehicleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Quien actualiza la app no pierde sus tanqueos: los de antes quedan como del carro. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MigrationTest {
    @Test fun `de la version 1 a la 2 los tanqueos quedan del carro`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migracion.db").apply { parentFile?.mkdirs(); delete() }
        // Base tal como la dejaba la versión 1 (app/schemas/.../1.json).
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `fill_ups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `at_epoch_millis` INTEGER NOT NULL, `gallons` REAL NOT NULL, `paid_cop` INTEGER NOT NULL, `fuel_type` TEXT NOT NULL, `is_full` INTEGER NOT NULL, `odometer_km` INTEGER)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_fill_ups_at_epoch_millis` ON `fill_ups` (`at_epoch_millis`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '77357379286f2947405debd14f3b4484')")
            db.execSQL("INSERT INTO fill_ups (at_epoch_millis, gallons, paid_cop, fuel_type, is_full, odometer_km) VALUES (1764000000000, 10.5, 170000, 'CORRIENTE', 1, 41000)")
            db.version = 1
        }

        val database = Room.databaseBuilder(context, TanqueoDatabase::class.java, file.absolutePath)
            .addMigrations(TanqueoDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val rows = database.fillUpDao().observeAll(VehicleType.CARRO.name).first()
            assertEquals(1, rows.size)
            assertEquals(41000, rows.single().odometerKm)
            assertEquals(0, database.fillUpDao().observeAll(VehicleType.MOTO.name).first().size)
        } finally {
            database.close()
        }
    }
}
