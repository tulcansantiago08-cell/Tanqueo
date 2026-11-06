package com.tanqueo.domain.assistant

import com.tanqueo.domain.voice.SpanishNumbers
import java.util.PriorityQueue

/** Una ciudad de la ruta y a cuántos km del origen queda. */
data class Waypoint(val place: String, val atKm: Int)

/** Recorrido por carretera; [waypoints] no incluye el origen ni el destino. */
data class Route(val origin: String, val destination: String, val km: Int, val waypoints: List<Waypoint>)

/**
 * Tramos principales por carretera de Colombia y sus distancias aproximadas. Es la fuente de los km de
 * un viaje: la IA solo dice qué ciudades nombró la persona y nunca aporta distancias. Las rutas salen
 * sumando tramos, no en línea recta: un viaje real por la cordillera es mucho más largo que el mapa.
 */
object RoadNetwork {
    private val legs = listOf(
        Triple("Bogotá", "Tunja", 140),
        Triple("Tunja", "Bucaramanga", 280),
        Triple("Bucaramanga", "Cúcuta", 200),
        Triple("Bucaramanga", "Bosconia", 420),
        Triple("Bucaramanga", "Medellín", 400),
        Triple("Bosconia", "Valledupar", 85),
        Triple("Bosconia", "Santa Marta", 175),
        Triple("Bosconia", "Barranquilla", 220),
        Triple("Valledupar", "Riohacha", 170),
        Triple("Santa Marta", "Riohacha", 170),
        Triple("Santa Marta", "Barranquilla", 95),
        Triple("Barranquilla", "Cartagena", 120),
        Triple("Cartagena", "Sincelejo", 185),
        Triple("Sincelejo", "Montería", 115),
        Triple("Montería", "Medellín", 400),
        Triple("Medellín", "Turbo", 340),
        Triple("Medellín", "Quibdó", 230),
        Triple("Medellín", "Honda", 265),
        Triple("Honda", "Bogotá", 150),
        Triple("Medellín", "Manizales", 195),
        Triple("Medellín", "Pereira", 211),
        Triple("Manizales", "Pereira", 50),
        Triple("Pereira", "Armenia", 45),
        Triple("Pereira", "Cali", 210),
        Triple("Armenia", "Cali", 180),
        Triple("Armenia", "Ibagué", 90),
        Triple("Ibagué", "Girardot", 75),
        Triple("Girardot", "Bogotá", 125),
        Triple("Girardot", "Neiva", 165),
        Triple("Bogotá", "Villavicencio", 90),
        Triple("Bogotá", "Yopal", 375),
        Triple("Neiva", "Florencia", 245),
        Triple("Neiva", "Popayán", 270),
        Triple("Cali", "Buenaventura", 125),
        Triple("Cali", "Popayán", 139),
        Triple("Popayán", "Pasto", 240),
        Triple("Pasto", "Ipiales", 80),
        Triple("Pasto", "Tumaco", 285),
        Triple("Pasto", "Mocoa", 140),
    )

    private val graph: Map<String, List<Pair<String, Int>>> = buildMap<String, MutableList<Pair<String, Int>>> {
        for ((a, b, km) in legs) {
            getOrPut(a) { mutableListOf() } += b to km
            getOrPut(b) { mutableListOf() } += a to km
        }
    }

    private val aliases: Map<String, String> =
        graph.keys.associateBy { SpanishNumbers.plain(it) } + mapOf(
            "medallo" to "Medellín",
            "barranquilla" to "Barranquilla",
            "bogota dc" to "Bogotá",
        )

    /** El nombre oficial de un lugar conocido, o null. */
    fun resolve(name: String): String? = aliases[SpanishNumbers.plain(name).trim()]

    /** Lugares conocidos que menciona un texto ya sin tildes, con la posición donde aparece cada uno. */
    fun mentionedIn(plainText: String): List<Pair<String, Int>> =
        aliases.mapNotNull { (alias, place) ->
            Regex("""\b${Regex.escape(alias)}\b""").find(plainText)?.let { place to it.range.first }
        }.distinctBy { it.first }.sortedBy { it.second }

    /** La ruta más corta entre dos lugares conocidos, o null si alguno no está en la red. */
    fun route(origin: String, destination: String): Route? {
        if (origin !in graph || destination !in graph || origin == destination) return null
        val distance = mutableMapOf(origin to 0)
        val previous = mutableMapOf<String, String>()
        val queue = PriorityQueue<Pair<String, Int>>(compareBy { it.second })
        queue += origin to 0
        while (queue.isNotEmpty()) {
            val (place, km) = queue.poll() ?: break
            if (km > (distance[place] ?: Int.MAX_VALUE)) continue
            for ((next, leg) in graph.getValue(place)) {
                val total = km + leg
                if (total < (distance[next] ?: Int.MAX_VALUE)) {
                    distance[next] = total
                    previous[next] = place
                    queue += next to total
                }
            }
        }
        val total = distance[destination] ?: return null
        val path = generateSequence(destination) { previous[it] }.toList().reversed()
        return Route(
            origin = origin,
            destination = destination,
            km = total,
            waypoints = path.drop(1).dropLast(1).map { Waypoint(it, distance.getValue(it)) },
        )
    }
}
