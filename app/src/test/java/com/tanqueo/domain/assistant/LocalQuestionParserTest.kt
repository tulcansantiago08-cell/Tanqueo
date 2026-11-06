package com.tanqueo.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalQuestionParserTest {
    private fun parse(text: String) = LocalQuestionParser.parse(text)

    @Test fun `entiende la pregunta del viaje`() {
        assertEquals(
            AskIntent.Trip("Pasto", "Medellín", daysUntil = 1),
            parse("Mañana me voy de Pasto a Medellín, ¿cuánto debo tanquear?"),
        )
    }

    @Test fun `el orden de las ciudades puede cambiar`() {
        assertEquals(AskIntent.Trip("Pasto", "Medellín"), parse("voy a Medellín desde Pasto"))
    }

    @Test fun `sabe cuando falta un lugar`() {
        assertEquals(AskIntent.Trip(destination = "Cali"), parse("quiero ir a Cali"))
        assertEquals(AskIntent.Trip(origin = "Cali"), parse("salgo de Cali"))
    }

    @Test fun `un viaje puede venir en kilometros`() {
        assertEquals(AskIntent.Trip(distanceKm = 300, daysUntil = 2), parse("pasado mañana voy a manejar 300 km"))
    }

    @Test fun `conserva un lugar que no conoce para poder decirlo`() {
        assertEquals(AskIntent.Trip("sandona", "la union"), parse("viajo de Sandona a La Union"))
    }

    @Test fun `entiende cuanto rinde un valor`() {
        assertEquals(AskIntent.FuelRange(moneyCop = 80_000), parse("¿hasta dónde me alcanza con 80 mil pesos?"))
        assertEquals(AskIntent.FuelRange(moneyCop = 50_000), parse("cuanto me dura $ 50.000"))
    }

    @Test fun `eche tanta gasolina pregunta los km y no anota un tanqueo`() {
        assertEquals(AskIntent.FuelRange(gallons = 5.0), parse("Eché 5 galones, ¿cuántos kilómetros puedo recorrer?"))
        assertEquals(AskIntent.FuelRange(moneyCop = 50_000), parse("eche 50 mil, hasta donde llego"))
    }

    @Test fun `cuanto cuesta ir a un lugar es un viaje`() {
        assertEquals(AskIntent.Trip("Pasto", "Cali"), parse("¿cuánto gasto de Pasto a Cali?"))
    }

    @Test fun `entiende las preguntas de estado`() {
        assertEquals(AskIntent.Range, parse("¿cuánta gasolina me queda?"))
        assertEquals(AskIntent.Range, parse("¿cuándo tengo que tanquear?"))
        assertEquals(AskIntent.Efficiency, parse("¿cuál es mi rendimiento?"))
    }

    @Test fun `entiende el gasto de cada mes`() {
        assertEquals(AskIntent.Spending(), parse("¿cuánto he gastado este mes?"))
        assertEquals(AskIntent.Spending(lastMonth = true), parse("cuánto gasté el mes pasado"))
    }

    @Test fun `entiende los precios`() {
        assertEquals(AskIntent.Price("Pasto"), parse("¿cuánto cuesta el galón en Pasto?"))
        assertEquals(AskIntent.Price(), parse("¿dónde está más barato?"))
    }

    @Test fun `contar un tanqueo no es una pregunta`() {
        assertEquals(AskIntent.LogFillUp, parse("tanqueé 10 galones por 165 mil"))
    }

    @Test fun `los km con punto de miles se leen completos`() {
        assertEquals(AskIntent.Trip(distanceKm = 1500), parse("voy a manejar 1.500 km"))
    }

    @Test fun `una distancia absurda no rompe la app`() {
        assertEquals(AskIntent.Unknown, parse("voy a manejar 99999999999 km"))
    }

    @Test fun `cuanto cuesta ir a un lugar es un viaje y no un precio`() {
        assertEquals(AskIntent.Trip(destination = "Cali"), parse("¿cuánto cuesta ir a Cali?"))
        assertEquals(AskIntent.Trip(distanceKm = 200), parse("¿cuánto cuesta manejar 200 km?"))
    }

    @Test fun `si conoce solo una ciudad conserva la otra`() {
        assertEquals(AskIntent.Trip("pasto", "sandona"), parse("voy de Pasto a Sandona"))
    }

    @Test fun `un anio o un galon no se toman como gasolina echada`() {
        assertEquals(AskIntent.Spending(), parse("¿cuánto gasté en 2026?"))
        assertEquals(AskIntent.Price(), parse("¿cuánto cuesta 1 galón?"))
    }

    @Test fun `lo demas no lo entiende`() {
        assertEquals(AskIntent.Unknown, parse("¿va a llover?"))
    }
}
