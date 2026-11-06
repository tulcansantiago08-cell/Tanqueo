package com.tanqueo.domain.voice

import java.text.Normalizer

/** Lee cantidades como se dicen en Colombia: "41.600", "10,5", "165 mil", "ciento diez" o "80 lucas". */
object SpanishNumbers {
    const val NUMBER = """\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?"""

    private val units = mapOf(
        "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6,
        "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18,
        "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26, "veintisiete" to 27,
        "veintiocho" to 28, "veintinueve" to 29, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
        "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90, "cien" to 100, "ciento" to 100,
        "doscientos" to 200, "trescientos" to 300, "cuatrocientos" to 400, "quinientos" to 500,
        "seiscientos" to 600, "setecientos" to 700, "ochocientos" to 800, "novecientos" to 900,
    )

    /** Minúsculas y sin tildes. */
    fun plain(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")

    /** Además de [plain], deja los números en cifras y convierte "165 mil" en 165000. */
    fun normalize(text: String): String = expandThousands(wordsToDigits(plain(text)))

    /** El único número de un texto escrito en un solo campo, o null si no hay exactamente uno. */
    fun parse(text: String): Double? {
        val numbers = Regex(NUMBER).findAll(normalize(text)).map { it.value }.toList()
        return numbers.singleOrNull()?.let(::toDouble)
    }

    /** Si el texto menciona esa cifra: sirve para rechazar números que la IA haya inventado. */
    fun mentions(text: String, value: Double): Boolean =
        Regex(NUMBER).findAll(normalize(text)).any { kotlin.math.abs(toDouble(it.value) - value) < 0.001 }

    fun toDouble(number: String): Double {
        val thousands = Regex("""\d{1,3}(?:[.,]\d{3})+""")
        return if (thousands.matches(number)) {
            number.replace(".", "").replace(",", "").toDouble()
        } else {
            number.replace(",", ".").toDouble()
        }
    }

    private fun expandThousands(text: String): String {
        // "41 mil 600 km" y "165 mil 500 pesos" son una sola cifra; en "165 mil 10 galones" son dos.
        val joined = Regex("""\b(\d{1,3}) mil (\d{1,3})(?=\s*(?:km|kms|kilometro|pesos|cop))""")
            .replace(text) { (it.groupValues[1].toLong() * 1000 + it.groupValues[2].toLong()).toString() }
        return Regex("""\b(\d{1,3}) (?:mil|lucas?)\b""").replace(joined) { (it.groupValues[1].toLong() * 1000).toString() }
    }

    /** Convierte los números en palabras: "ciento sesenta y cinco mil" → "165000". */
    private fun wordsToDigits(text: String): String {
        val tokens = text.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        val output = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val word = tokens[i].trim(',', '.', ';', ':', '?', '¿', '!', '¡')
            // Tras una cifra, "mil" se deja para expandThousands ("165 mil").
            val afterDigits = output.lastOrNull()?.trim(',', '.', ';', ':')?.all { it.isDigit() } == true
            val startsNumber = word in units || (word == "mil" && !afterDigits)
            // "un tanque" o "una vez" no son cantidades: "un" solo cuenta antes de mil, luca o galón.
            val isArticle = word in setOf("un", "una", "uno") &&
                tokens.getOrNull(i + 1)?.let { it.startsWith("mil") || it.startsWith("gal") || it.startsWith("luc") } != true
            if (!startsNumber || isArticle) {
                output += tokens[i]
                i++
                continue
            }
            var total = 0L
            var current = 0L
            while (i < tokens.size) {
                val token = tokens[i].trim(',', '.', ';', ':', '?', '¿', '!', '¡')
                val value = units[token]
                when {
                    value != null -> current += value
                    token == "mil" -> {
                        total += (if (current == 0L) 1 else current) * 1000
                        current = 0
                    }
                    token == "y" && tokens.getOrNull(i + 1)?.trim(',', '.', ';', ':') in units -> Unit
                    else -> break
                }
                i++
            }
            output += (total + current).toString()
        }
        return output.joinToString(" ")
    }
}
