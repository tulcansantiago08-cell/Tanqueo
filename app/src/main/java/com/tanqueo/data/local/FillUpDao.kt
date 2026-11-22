package com.tanqueo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FillUpDao {
    @Query("SELECT * FROM fill_ups WHERE vehicle_type = :vehicleType ORDER BY at_epoch_millis DESC, id DESC")
    fun observeAll(vehicleType: String): Flow<List<FillUpEntity>>

    @Query("SELECT * FROM fill_ups WHERE id = :id")
    fun observe(id: Long): Flow<FillUpEntity?>

    @Insert
    suspend fun insert(fillUp: FillUpEntity): Long

    @Query("DELETE FROM fill_ups WHERE id = :id")
    suspend fun delete(id: Long)
}
