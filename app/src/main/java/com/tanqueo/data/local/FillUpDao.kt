package com.tanqueo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FillUpDao {
    @Query("SELECT * FROM fill_ups ORDER BY at_epoch_millis DESC, id DESC")
    fun observeAll(): Flow<List<FillUpEntity>>

    @Query("SELECT * FROM fill_ups WHERE id = :id")
    fun observe(id: Long): Flow<FillUpEntity?>

    @Insert
    suspend fun insert(fillUp: FillUpEntity): Long

    @Query("DELETE FROM fill_ups WHERE id = :id")
    suspend fun delete(id: Long)
}
