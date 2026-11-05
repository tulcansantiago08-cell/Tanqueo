package com.tanqueo.domain.budget

import com.tanqueo.domain.model.FuelType

/** Precio de referencia de un galón en una ciudad. */
data class ReferencePrice(val place: String, val fuelType: FuelType, val pricePerGallon: Long)

/**
 * Precios de referencia oficiales de noviembre de 2026 publicados por la CREG (Circular 371 de 2026)
 * y recogidos por la prensa. El precio en cada estación puede variar. No hay una fuente automática:
 * se actualizan a mano junto con [VALID_FOR]. No hay un precio de referencia publicado para la Extra.
 */
object ReferencePrices {
    const val VALID_FOR = "nov 2026"

    val all: List<ReferencePrice> = listOf(
        ReferencePrice("Promedio", FuelType.CORRIENTE, 15_886),
        ReferencePrice("Bogotá", FuelType.CORRIENTE, 16_331),
        ReferencePrice("Medellín", FuelType.CORRIENTE, 16_251),
        ReferencePrice("Cali", FuelType.CORRIENTE, 16_339),
        ReferencePrice("Pasto", FuelType.CORRIENTE, 13_920),
        ReferencePrice("Villavicencio", FuelType.CORRIENTE, 16_431),
        ReferencePrice("Promedio", FuelType.DIESEL, 11_320),
        ReferencePrice("Bogotá", FuelType.DIESEL, 11_616),
        ReferencePrice("Medellín", FuelType.DIESEL, 11_641),
        ReferencePrice("Cali", FuelType.DIESEL, 11_763),
        ReferencePrice("Pasto", FuelType.DIESEL, 10_529),
    )

    /** Para Extra se ofrecen los de Corriente, que es lo más cercano con precio publicado. */
    fun forFuel(fuelType: FuelType): List<ReferencePrice> {
        val wanted = if (fuelType == FuelType.DIESEL) FuelType.DIESEL else FuelType.CORRIENTE
        return all.filter { it.fuelType == wanted }
    }
}
