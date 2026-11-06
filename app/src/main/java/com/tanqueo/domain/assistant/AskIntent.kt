package com.tanqueo.domain.assistant

import com.tanqueo.domain.voice.LocalFillUpParser
import com.tanqueo.domain.voice.SpanishNumbers

/** Qué quiere saber la persona. No lleva ningún resultado: esos los calcula el reloj. */
sealed interface AskIntent {
    /** [daysUntil]: 0 hoy, 1 mañana. Los lugares van como se dijeron; se validan contra [RoadNetwork]. */
    data class Trip(
        val origin: String? = null,
        val destination: String? = null,
        val distanceKm: Int? = null,
        val daysUntil: Int? = null,
    ) : AskIntent

    /** "Eché 5 galones" o "con 80 mil": cuántos km da esa gasolina. Trae uno de los dos datos. */
    data class FuelRange(val gallons: Double? = null, val moneyCop: Long? = null) : AskIntent
    data object Range : AskIntent
    data class Spending(val lastMonth: Boolean = false) : AskIntent
    data class Price(val place: String? = null) : AskIntent
    data object Efficiency : AskIntent

    /** Está contando un tanqueo, no preguntando: se abre el registro con lo que dijo. */
    data object LogFillUp : AskIntent
    data object Unknown : AskIntent
}

/** Entiende preguntas simples sin internet. Es el respaldo cuando la IA no responde. */
object LocalQuestionParser {
    private val tripWords = Regex("""\b(voy|vamos|viaje|viajo|viajar|viajamos|ir|irme|ruta|salgo|salimos|llegar|tanquear|manejar)\b""")
    private val logWords = Regex("""\b(tanquee|eche|le puse|le meti|cargue|llene)\b""")
    private val asksHowFar = Regex("""\b(cuantos km|cuantos kilometros|hasta donde|alcanza|rinde|dura|recorrer|llego|llegar)\b""")
    private val asksSomethingElse = Regex("""\b(gast\w*|precio|cuesta|vale)\b""")
    private const val MAX_TRIP_KM = 20_000.0
    private val origins = setOf("de", "desde")
    private val unknownRoute = Regex("""\b(?:de|desde) ([a-z ]{3,30}?) (?:a|hasta|para) ([a-z ]{3,30}?)(?:$|[,.?]| cuanto| manana| hoy| pasado)""")

    fun parse(text: String): AskIntent {
        val normalized = SpanishNumbers.normalize(text)
        val fillUp = LocalFillUpParser.parse(text)
        val hasFillUpData = fillUp.gallons != null || fillUp.paidCop != null
        val places = RoadNetwork.mentionedIn(normalized)
        if (hasFillUpData && asksHowFar.containsMatchIn(normalized) && places.isEmpty() && !asksSomethingElse.containsMatchIn(normalized)) {
            return fillUp.gallons?.let { AskIntent.FuelRange(gallons = it) } ?: AskIntent.FuelRange(moneyCop = fillUp.paidCop)
        }
        if (logWords.containsMatchIn(normalized) && hasFillUpData) return AskIntent.LogFillUp

        val days = when {
            "pasado manana" in normalized -> 2
            Regex("""\bmanana\b""").containsMatchIn(normalized) -> 1
            Regex("""\bhoy\b""").containsMatchIn(normalized) -> 0
            else -> null
        }
        val isTrip = tripWords.containsMatchIn(normalized)
        val isPrice = Regex("""\b(precio|cuesta|vale|barat[oa])\b""").containsMatchIn(normalized)
        if (places.size >= 2) return tripBetween(normalized, places, days)
        // "¿Cuánto cuesta ir a Cali?" es un viaje; "¿cuánto cuesta el galón en Cali?" es un precio.
        if (isPrice && !isTrip) return AskIntent.Price(places.firstOrNull()?.first)
        if (isTrip) {
            // Con un lugar desconocido se conservan los dos nombres, para poder decir cuál falta.
            unknownRoute.find(normalized)?.let {
                return AskIntent.Trip(it.groupValues[1].trim(), it.groupValues[2].trim(), daysUntil = days)
            }
        }
        if (places.size == 1 && isTrip) {
            val (place, position) = places.single()
            val saysOrigin = wordBefore(normalized, position) in origins
            return AskIntent.Trip(origin = place.takeIf { saysOrigin }, destination = place.takeUnless { saysOrigin }, daysUntil = days)
        }
        if (isTrip) {
            Regex("""(${SpanishNumbers.NUMBER})\s*(?:km|kms|kilometros?)\b""").find(normalized)?.let { match ->
                val km = SpanishNumbers.toDouble(match.groupValues[1]).takeIf { it in 1.0..MAX_TRIP_KM }
                if (km != null) return AskIntent.Trip(distanceKm = km.toInt(), daysUntil = days)
            }
        }
        return when {
            Regex("""\bgast""").containsMatchIn(normalized) ->
                AskIntent.Spending(lastMonth = "mes pasado" in normalized)
            Regex("""\b(rendimiento|km por galon|kilometros por galon|consumo)\b""").containsMatchIn(normalized) -> AskIntent.Efficiency
            Regex("""\b(queda|autonomia|cuando|alcanza|dura|gasolina|tanquear)\b""").containsMatchIn(normalized) -> AskIntent.Range
            else -> AskIntent.Unknown
        }
    }

    private fun tripBetween(text: String, places: List<Pair<String, Int>>, days: Int?): AskIntent.Trip {
        val (first, firstAt) = places[0]
        val (second, secondAt) = places[1]
        // "a Medellín desde Pasto": el segundo es el origen.
        val secondIsOrigin = wordBefore(text, secondAt) in origins && wordBefore(text, firstAt) !in origins
        return if (secondIsOrigin) AskIntent.Trip(second, first, daysUntil = days) else AskIntent.Trip(first, second, daysUntil = days)
    }

    private fun wordBefore(text: String, position: Int): String =
        text.substring(0, position).trim().substringAfterLast(' ')
}
