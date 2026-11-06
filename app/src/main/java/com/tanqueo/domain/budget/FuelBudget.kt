package com.tanqueo.domain.budget

import com.tanqueo.domain.voice.LocalFillUpParser
import kotlin.math.min

/** Cuánto alcanza un monto de dinero. */
data class FuelBudget(
    val gallons: Double,
    /** Null si todavía no se conoce el rendimiento del vehículo: nunca se inventa. */
    val km: Double?,
    /** Días que dura esa gasolina según lo que se maneja a diario; null si aún no hay promedio. */
    val days: Double?,
    /** True si los galones no caben en el tanque; los km se limitan a lo que cabe. */
    val exceedsTank: Boolean,
)

object FuelBudgetCalculator {
    const val MONEY_STEP_COP = 1_000L
    const val MIN_MONEY_COP = 1_000L
    const val MAX_MONEY_COP = 2_000_000L
    const val DEFAULT_MONEY_COP = 50_000L

    fun calculate(
        moneyCop: Long,
        pricePerGallon: Double,
        kmPerGallon: Double?,
        tankCapacityGallons: Double,
        kmPerDay: Double? = null,
    ): FuelBudget? {
        if (pricePerGallon <= 0 || moneyCop <= 0) return null
        val gallons = moneyCop / pricePerGallon
        val km = kmPerGallon?.let { min(gallons, tankCapacityGallons) * it }
        return FuelBudget(
            gallons = gallons,
            km = km,
            days = if (km != null && kmPerDay != null && kmPerDay > 0) km / kmPerDay else null,
            exceedsTank = gallons > tankCapacityGallons,
        )
    }

    /** Lo escrito o dictado ("50000", "50.000", "50 mil"); null si no es un valor posible. */
    fun parseMoney(text: String): Long? =
        LocalFillUpParser.parse(text).paidCop?.takeIf { it in MIN_MONEY_COP..MAX_MONEY_COP }

    fun adjustMoney(moneyCop: Long, steps: Int): Long =
        (moneyCop + steps * MONEY_STEP_COP).coerceIn(MIN_MONEY_COP, MAX_MONEY_COP)
}
