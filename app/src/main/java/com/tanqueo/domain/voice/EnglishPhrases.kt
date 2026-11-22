package com.tanqueo.domain.voice

/**
 * Lleva frases comunes en inglés a su forma en español, para que el lector local (sin IA) entienda
 * preguntas y dictados en inglés con las mismas reglas. Recibe texto ya en minúsculas y sin tildes.
 * El orden importa: las frases largas van antes que las palabras sueltas que contienen.
 */
object EnglishPhrases {
    private val phrases: List<Pair<Regex, String>> = listOf(
        "day after tomorrow" to "pasado manana",
        "tomorrow" to "manana",
        "today" to "hoy",
        "how long (?:will it take|does it take|would it take)?" to "cuanto me demoro",
        "how much time" to "cuanto tiempo",
        "how many hours" to "cuantas horas",
        "by (?:motorbike|motorcycle|scooter|bike)|on (?:my|the) (?:motorbike|motorcycle|scooter|bike)" to "en moto",
        "motorbike|motorcycle|scooter" to "moto",
        "by car|in (?:my|the) car" to "en carro",
        "car" to "carro",
        "(?:gas|petrol|fuel|filling) stations?" to "gasolinera",
        "where (?:can|do|should) i (?:refuel|fill up|get gas)" to "donde tanqueo",
        "when (?:do|should) i (?:need to )?(?:refuel|fill up|get gas)" to "cuando tanquear",
        "how much (?:did i spend|have i spent|i spent)" to "cuanto gaste",
        "last month" to "mes pasado",
        "this month" to "este mes",
        "how far" to "hasta donde",
        "how many (?:km|kilometers|kilometres)" to "cuantos km",
        "kilometers|kilometres" to "kilometros",
        "fuel left|gas left|range" to "autonomia",
        "efficiency|mileage|consumption" to "rendimiento",
        "how much (?:is|does|for) (?:a |the )?gallon" to "cuanto cuesta el galon",
        "price" to "precio",
        "costs?" to "cuesta",
        "cheapest|cheaper" to "barato",
        "i (?:put in|filled up with|filled|fueled|bought)" to "eche",
        "and a half" to "y medio",
        "half (?:a )?gallon" to "medio galon",
        "gallons" to "galones",
        "gallon" to "galon",
        "thousand" to "mil",
        "partial" to "parcial",
        "regular" to "corriente",
        "odometer|mileage reading" to "kilometraje",
        "i'?m (?:going|driving|traveling|travelling)|i will (?:go|drive)|trip" to "voy",
        "drive" to "manejar",
        "from" to "de",
        "to" to "a",
    ).map { (english, spanish) -> Regex("""\b(?:$english)\b""") to spanish }

    fun toSpanish(text: String): String = phrases.fold(text) { acc, (regex, spanish) -> regex.replace(acc, spanish) }
}
