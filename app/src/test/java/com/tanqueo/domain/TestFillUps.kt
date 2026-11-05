package com.tanqueo.domain

import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Duration
import java.time.Instant

/** Fecha fija de referencia; [dayN] cuenta los días a partir de ella. */
val START: Instant = Instant.parse("2026-10-01T15:00:00Z")

fun dayN(day: Double): Instant = START.plus(Duration.ofMinutes((day * 24 * 60).toLong()))

fun full(day: Int, km: Int?, gallons: Double = 10.0, id: Long = day.toLong()) =
    FillUp(id, dayN(day.toDouble()), gallons, (gallons * 16_000).toLong(), FuelType.CORRIENTE, true, km)

fun partial(day: Int, km: Int?, gallons: Double = 5.0, id: Long = day.toLong()) =
    FillUp(id, dayN(day.toDouble()), gallons, (gallons * 16_000).toLong(), FuelType.CORRIENTE, false, km)
