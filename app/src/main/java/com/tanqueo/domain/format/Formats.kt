package com.tanqueo.domain.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Cómo se muestran los días restantes: nunca "unos 0 días". */
sealed interface DaysDisplay {
    data object LessThanOne : DaysDisplay
    data class About(val days: Int) : DaysDisplay
}

/** Formatos en español de Colombia: miles con punto y decimales con coma. */
object Formats {
    private val spanish = Locale.forLanguageTag("es-CO")
    private val symbols = DecimalFormatSymbols(spanish).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", spanish)
    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEE d", spanish)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", spanish)

    /** Pesos colombianos sin decimales: $ 164.700, con espacio que no se parte de línea. */
    fun cop(value: Long): String = "$\u00A0" + DecimalFormat("#,##0", symbols).format(value)

    fun cop(value: Double): String = cop(Math.round(value))

    /** Pesos de una estimación, redondeados a $ 100: más precisión sería falsa. */
    fun approxCop(value: Double): String = cop(Math.round(value / 100) * 100)

    /** Un decimal con coma: 10,5 */
    fun oneDecimal(value: Double): String = DecimalFormat("0.0", symbols).format(value)

    /** Enteros con punto de miles: 41.580 */
    fun integer(value: Int): String = DecimalFormat("#,##0", symbols).format(value)

    fun roundedKm(km: Double): String = integer(km.roundToInt())

    /** Hora de 24 horas: 19:07 */
    fun time(dateTime: LocalDateTime): String = timeFormatter.format(dateTime)

    /** Fecha corta: mié 4 nov (sin los puntos de abreviatura que agregan algunas versiones de CLDR) */
    fun shortDate(date: LocalDate): String = dateFormatter.format(date).replace(".", "").replace(",", "")

    /** Día de una fecha cercana, donde el mes no cabe ni hace falta: mié 4 */
    fun dayDate(date: LocalDate): String =
        headerDateFormatter.format(date).replace(".", "").replace(",", "")

    /** Fecha del encabezado: MIÉ 4 */
    fun headerDate(date: LocalDate): String = dayDate(date).uppercase(spanish)

    fun days(days: Double): DaysDisplay =
        if (days < 1.0) DaysDisplay.LessThanOne else DaysDisplay.About(days.roundToInt())
}
