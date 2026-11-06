package com.tanqueo.domain.voice

import com.tanqueo.domain.model.FuelType

/**
 * Entiende en el propio reloj frases simples de un tanqueo, sin internet:
 * "tanqueé 10 galones y medio, 165 mil, corriente, lleno, 41.600 km".
 * Es el respaldo cuando la IA no está disponible.
 */
object LocalFillUpParser {
    private const val NUMBER = SpanishNumbers.NUMBER

    fun parse(text: String): DictatedFillUp {
        var rest = SpanishNumbers.normalize(text)

        fun take(regex: Regex): MatchResult? = regex.find(rest)?.also {
            rest = rest.replaceRange(it.range, " ")
        }

        val odometer = (take(Regex("""($NUMBER)\s*(?:km|kms|kilometros?)\b"""))?.groupValues?.get(1)
            ?: take(Regex("""(?:kilometraje|odometro)\s*(?:de|en|es|:)?\s*($NUMBER)"""))?.groupValues?.get(1))
            ?.let(SpanishNumbers::toDouble)?.toInt()

        val gallons = take(Regex("""($NUMBER)\s+y\s+medio\s+gal(?:on|ones)?\b"""))?.let { SpanishNumbers.toDouble(it.groupValues[1]) + 0.5 }
            ?: take(Regex("""\bmedio\s+gal(?:on|ones)?\b"""))?.let { 0.5 }
            ?: take(Regex("""($NUMBER)\s*gal(?:on|ones)?\b(\s+y\s+medio)?"""))?.let {
                SpanishNumbers.toDouble(it.groupValues[1]) + if (it.groupValues[2].isNotEmpty()) 0.5 else 0.0
            }

        val paid = (take(Regex("""\$\s*($NUMBER)"""))?.groupValues?.get(1)
            ?: take(Regex("""($NUMBER)\s*(?:pesos|cop)\b"""))?.groupValues?.get(1)
            ?: take(Regex("""(?:valor|pague|costo|cuesta|por)\s*(?:de)?\s*($NUMBER)"""))?.groupValues?.get(1)
            ?: Regex(NUMBER).findAll(rest).map { it.value }.firstOrNull { SpanishNumbers.toDouble(it) >= MIN_BARE_COP })
            ?.let(SpanishNumbers::toDouble)?.toLong()

        return DictatedFillUp(
            gallons = gallons,
            paidCop = paid,
            fuelType = when {
                Regex("""\b(?:diesel|acpm|biodiesel)\b""").containsMatchIn(rest) -> FuelType.DIESEL
                Regex("""\b(?:extra|premium)\b""").containsMatchIn(rest) -> FuelType.EXTRA
                Regex("""\b(?:corriente|regular)\b""").containsMatchIn(rest) -> FuelType.CORRIENTE
                else -> null
            },
            isFull = when {
                Regex("""\b(?:parcial|no lleno|sin llenar|medio tanque)\b""").containsMatchIn(rest) -> false
                Regex("""\b(?:lleno|llene|llenar|full|tanqueo completo)\b""").containsMatchIn(rest) -> true
                else -> null
            },
            odometerKm = odometer,
        )
    }

    /** Un número suelto solo se toma como dinero si es muy grande para ser otra cosa. */
    private const val MIN_BARE_COP = 1_000.0
}
