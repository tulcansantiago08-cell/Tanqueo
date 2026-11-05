package com.tanqueo.domain.calc

/** Qué tan urgente es la autonomía. Cambia el color del anillo y de la cifra. */
enum class RangeLevel {
    NORMAL,
    LOW,
    CRITICAL,
    ;

    companion object {
        const val LOW_KM = 50.0
        const val CRITICAL_KM = 20.0

        fun of(kmRemaining: Double): RangeLevel = when {
            kmRemaining < CRITICAL_KM -> CRITICAL
            kmRemaining < LOW_KM -> LOW
            else -> NORMAL
        }
    }
}
