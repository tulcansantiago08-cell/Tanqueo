package com.tanqueo.domain

import com.tanqueo.domain.assistant.AskIntent
import com.tanqueo.domain.assistant.LocalQuestionParser
import com.tanqueo.domain.format.AppLanguage
import com.tanqueo.domain.format.AppLocale
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.model.VehicleType
import com.tanqueo.domain.voice.LocalFillUpParser
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** En inglés el lector local entiende lo mismo, y los números y fechas se escriben a la inglesa. */
class EnglishTest {
    @After fun spanishAgain() {
        AppLocale.language = AppLanguage.ES
    }

    @Test fun `entiende preguntas en ingles sin IA`() {
        assertEquals(
            AskIntent.Trip("Pasto", "Cali", vehicle = VehicleType.MOTO, asksTime = true),
            LocalQuestionParser.parse("How long from Pasto to Cali by motorbike?"),
        )
        assertEquals(AskIntent.Trip("Pasto", "Medellín", daysUntil = 1), LocalQuestionParser.parse("Tomorrow I'm driving from Pasto to Medellín"))
        assertEquals(AskIntent.GasStations, LocalQuestionParser.parse("Where can I refuel?"))
        assertEquals(AskIntent.GasStations, LocalQuestionParser.parse("Find a gas station"))
        assertEquals(AskIntent.Range, LocalQuestionParser.parse("When do I need to refuel?"))
        assertEquals(AskIntent.Spending(lastMonth = true), LocalQuestionParser.parse("How much did I spend last month?"))
        assertEquals(AskIntent.FuelRange(gallons = 5.0), LocalQuestionParser.parse("I put in 5 gallons, how far can I go?"))
    }

    @Test fun `entiende un dictado en ingles`() {
        val fillUp = LocalFillUpParser.parse("10 gallons, 165 thousand, regular, full")
        assertEquals(10.0, fillUp.gallons!!, 0.0)
        assertEquals(165_000L, fillUp.paidCop)
        assertEquals(FuelType.CORRIENTE, fillUp.fuelType)
        assertEquals(true, fillUp.isFull)
    }

    @Test fun `el espanol sigue igual`() {
        assertEquals(AskIntent.GasStations, LocalQuestionParser.parse("¿Dónde hay una gasolinera cerca?"))
        assertEquals("10,5", Formats.oneDecimal(10.5))
        assertEquals("41.580", Formats.integer(41_580))
    }

    @Test fun `en ingles los numeros y fechas cambian`() {
        AppLocale.language = AppLanguage.EN
        assertEquals("10.5", Formats.oneDecimal(10.5))
        assertEquals("41,580", Formats.integer(41_580))
        assertEquals("$ 164,700", Formats.cop(164_700L))
        assertEquals("SUN 22", Formats.headerDate(LocalDate.of(2026, 11, 22)))
    }

    @Test fun `cualquier otro idioma cae en espanol`() {
        assertEquals(AppLanguage.EN, AppLanguage.of("en-US"))
        assertEquals(AppLanguage.ES, AppLanguage.of("es-CO"))
        assertEquals(AppLanguage.ES, AppLanguage.of("fr"))
        assertEquals(AppLanguage.ES, AppLanguage.of(null))
    }
}
