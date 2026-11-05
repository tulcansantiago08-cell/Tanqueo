package com.tanqueo.domain.tip

import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.model.FillUp
import com.tanqueo.domain.model.FuelType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** Consejo del mes calculado con los datos reales. Funciona sin IA. */
sealed interface MonthTip {
    data class PremiumCostsMore(val extraPerGallon: Long) : MonthTip
    data class SpendingUp(val percent: Int) : MonthTip
    data class SpendingDown(val percent: Int) : MonthTip
    data class EfficiencyDrop(val percent: Int) : MonthTip
    data object KeepLogging : MonthTip
}

/** Datos con los que se arma el consejo y lo único que se le entrega a la IA. */
data class MonthFacts(
    val spentCop: Long,
    val fillUps: Int,
    val spentLastMonthSamePeriodCop: Long?,
    val averagePriceByFuel: Map<FuelType, Long>,
    val kmPerGallon: Double?,
    val lastSegmentKmPerGallon: Double?,
) {
    /** Texto para la IA. Solo trae agregados: ni fechas exactas ni kilometraje. */
    fun asPromptText(): String = buildString {
        appendLine("Gasto en lo que va del mes: ${Formats.cop(spentCop)} en $fillUps tanqueos.")
        spentLastMonthSamePeriodCop?.let { appendLine("Mismo periodo del mes pasado: ${Formats.cop(it)}.") }
        averagePriceByFuel.forEach { (fuel, price) ->
            appendLine("Precio promedio por galón de ${fuel.name.lowercase()}: ${Formats.cop(price)}.")
        }
        kmPerGallon?.let { appendLine("Rendimiento promedio: ${Formats.oneDecimal(it)} km por galón.") }
        lastSegmentKmPerGallon?.let { appendLine("Rendimiento del último tramo: ${Formats.oneDecimal(it)} km por galón.") }
    }.trim()
}

object MonthTips {
    private const val MIN_CHANGE_PERCENT = 10
    private const val MIN_PREMIUM_DIFFERENCE_COP = 500L

    fun facts(fillUps: List<FillUp>, now: Instant, zone: ZoneId): MonthFacts {
        val today = now.atZone(zone).toLocalDate()
        val thisMonth = YearMonth.from(today)
        val previousMonth = thisMonth.minusMonths(1)
        val inMonth = fillUps.filter { YearMonth.from(it.at.atZone(zone)) == thisMonth }
        val inPrevious = fillUps.filter { YearMonth.from(it.at.atZone(zone)) == previousMonth }
        val samePeriod = inPrevious.filter { it.at.atZone(zone).dayOfMonth <= today.dayOfMonth }
        val chronological = fillUps.sortedBy { it.at }
        return MonthFacts(
            spentCop = inMonth.sumOf { it.paidCop },
            fillUps = inMonth.size,
            // Sin tanqueos el mes pasado no hay contra qué comparar.
            spentLastMonthSamePeriodCop = if (inPrevious.isEmpty()) null else samePeriod.sumOf { it.paidCop },
            averagePriceByFuel = fillUps.groupBy { it.fuelType }.mapValues { (_, list) ->
                (list.sumOf { it.paidCop } / list.sumOf { it.gallons }).roundToInt().toLong()
            },
            kmPerGallon = RangeEstimator.kmPerGallon(fillUps),
            lastSegmentKmPerGallon = lastSegment(chronological),
        )
    }

    /** Elige el dato más útil. Sin nada que decir, invita a seguir registrando. */
    fun localTip(facts: MonthFacts): MonthTip {
        val extra = facts.averagePriceByFuel[FuelType.EXTRA]
        val regular = facts.averagePriceByFuel[FuelType.CORRIENTE]
        val previous = facts.spentLastMonthSamePeriodCop
        val average = facts.kmPerGallon
        val last = facts.lastSegmentKmPerGallon
        val change = previous?.takeIf { it > 0 }?.let { ((facts.spentCop - it) * 100.0 / it).roundToInt() }
        val efficiencyChange = if (average != null && last != null && average > 0) {
            ((last - average) * 100 / average).roundToInt()
        } else {
            null
        }
        return when {
            efficiencyChange != null && efficiencyChange <= -MIN_CHANGE_PERCENT -> MonthTip.EfficiencyDrop(abs(efficiencyChange))
            change != null && change >= MIN_CHANGE_PERCENT -> MonthTip.SpendingUp(change)
            change != null && change <= -MIN_CHANGE_PERCENT -> MonthTip.SpendingDown(abs(change))
            extra != null && regular != null && extra - regular >= MIN_PREMIUM_DIFFERENCE_COP -> MonthTip.PremiumCostsMore(extra - regular)
            else -> MonthTip.KeepLogging
        }
    }

    /**
     * La IA solo redacta: se rechaza su texto si trae una cifra que no esté en los datos, si está
     * vacío o si es demasiado largo para la pantalla del reloj.
     */
    fun isTrustworthy(text: String, facts: MonthFacts): Boolean {
        if (text.isBlank() || text.length > MAX_TIP_LENGTH) return false
        val digitsIn = { value: String -> Regex("""\d+(?:[.,]\d+)*""").findAll(value).map { it.value.replace(".", "").replace(",", "") }.toSet() }
        return digitsIn(facts.asPromptText()).containsAll(digitsIn(text))
    }

    const val MAX_TIP_LENGTH = 140

    private fun lastSegment(chronological: List<FillUp>): Double? {
        val anchors = chronological.indices.filter { chronological[it].isFull && chronological[it].odometerKm != null }
        if (anchors.size < 3) return null
        val from = anchors[anchors.size - 2]
        val to = anchors.last()
        val km = chronological[to].odometerKm!! - chronological[from].odometerKm!!
        val gallons = chronological.subList(from + 1, to + 1).sumOf { it.gallons }
        return if (km > 0 && gallons > 0) km / gallons else null
    }
}
