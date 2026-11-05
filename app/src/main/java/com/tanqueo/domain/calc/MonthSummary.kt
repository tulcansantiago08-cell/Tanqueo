package com.tanqueo.domain.calc

import com.tanqueo.domain.model.FillUp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class MonthSummary(
    val totalSpentCop: Long,
    val fillUpCount: Int,
    /** Null si en el mes no hay tanqueos. */
    val averagePricePerGallon: Double?,
    /** Días de calendario desde el último tanqueo, sea de este mes o no. Null si no hay ninguno. */
    val daysSinceLastFillUp: Long?,
)

object MonthSummaryCalculator {
    /** El precio promedio se pondera: total pagado en el mes ÷ total de galones del mes. */
    fun calculate(fillUps: List<FillUp>, now: Instant, zone: ZoneId): MonthSummary {
        val today = now.atZone(zone).toLocalDate()
        val thisMonth = YearMonth.from(today)
        val inMonth = fillUps.filter { YearMonth.from(it.at.atZone(zone)) == thisMonth }
        val totalCop = inMonth.sumOf { it.paidCop }
        val totalGallons = inMonth.sumOf { it.gallons }
        val lastDate = fillUps.maxOfOrNull { it.at }?.atZone(zone)?.toLocalDate()
        return MonthSummary(
            totalSpentCop = totalCop,
            fillUpCount = inMonth.size,
            averagePricePerGallon = if (totalGallons > 0) totalCop / totalGallons else null,
            daysSinceLastFillUp = lastDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) },
        )
    }
}
