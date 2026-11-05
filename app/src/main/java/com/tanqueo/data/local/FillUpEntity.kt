package com.tanqueo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Instant

@Entity(tableName = "fill_ups")
data class FillUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "at_epoch_millis", index = true) val atEpochMillis: Long,
    val gallons: Double,
    @ColumnInfo(name = "paid_cop") val paidCop: Long,
    @ColumnInfo(name = "fuel_type") val fuelType: String,
    @ColumnInfo(name = "is_full") val isFull: Boolean,
    @ColumnInfo(name = "odometer_km") val odometerKm: Int?,
)

fun FillUpEntity.toDomain() = FillUp(
    id = id,
    at = Instant.ofEpochMilli(atEpochMillis),
    gallons = gallons,
    paidCop = paidCop,
    fuelType = FuelType.entries.firstOrNull { it.name == fuelType } ?: FuelType.CORRIENTE,
    isFull = isFull,
    odometerKm = odometerKm,
)

fun FillUp.toEntity() = FillUpEntity(
    id = id,
    atEpochMillis = at.toEpochMilli(),
    gallons = gallons,
    paidCop = paidCop,
    fuelType = fuelType.name,
    isFull = isFull,
    odometerKm = odometerKm,
)
