package com.tanqueo.domain.budget

import kotlin.math.min

/** Cuánto alcanza un monto de dinero. */
data class FuelBudget(
    val gallons: Double,
    /** Null si todavía no se conoce el rendimiento del vehículo: nunca se inventa. */
    val km: Double?,
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
    ): FuelBudget? {
        if (pricePerGallon <= 0 || moneyCop <= 0) return null
        val gallons = moneyCop / pricePerGallon
        return FuelBudget(
            gallons = gallons,
            km = kmPerGallon?.let { min(gallons, tankCapacityGallons) * it },
            exceedsTank = gallons > tankCapacityGallons,
        )
    }

    fun adjustMoney(moneyCop: Long, steps: Int): Long =
        (moneyCop + steps * MONEY_STEP_COP).coerceIn(MIN_MONEY_COP, MAX_MONEY_COP)
}
