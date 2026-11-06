package com.tanqueo.domain.usecase

import com.tanqueo.domain.assistant.AskIntent
import com.tanqueo.domain.assistant.ConsumptionReference
import com.tanqueo.domain.assistant.LocalQuestionParser
import com.tanqueo.domain.assistant.RoadNetwork
import com.tanqueo.domain.assistant.TripPlan
import com.tanqueo.domain.assistant.TripPlanner
import com.tanqueo.domain.assistant.Waypoint
import com.tanqueo.domain.budget.FuelBudgetCalculator
import com.tanqueo.domain.budget.ReferencePrice
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.calc.FillUpInput
import com.tanqueo.domain.calc.MonthSummaryCalculator
import com.tanqueo.domain.calc.RangeEstimate
import com.tanqueo.domain.calc.RangeEstimator
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.repository.FillUpRepository
import com.tanqueo.domain.repository.FuelAssistant
import com.tanqueo.domain.voice.SpanishNumbers
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** De dónde salió el rendimiento de un cálculo: el real del vehículo o el promedio de referencia. */
data class EfficiencyUsed(val kmPerGallon: Double, val isReference: Boolean)

/** Lo que el asistente responde: datos calculados en el reloj, listos para mostrar. */
sealed interface AskAnswer {
    data class Trip(
        val origin: String?,
        val destination: String?,
        val plan: TripPlan,
        val daysUntil: Int?,
        val priceReference: String?,
        val efficiency: EfficiencyUsed,
        /** False sin historial: no se sabe cuánto hay en el tanque, solo cuánto cuesta el viaje. */
        val tankKnown: Boolean,
    ) : AskAnswer

    data class Range(val km: Double, val days: Double?, val refuelOn: LocalDate?, val today: LocalDate) : AskAnswer
    /** Cuánto rinde una cantidad de gasolina, dicha en galones o en pesos. */
    data class FuelRange(
        val gallons: Double,
        val cop: Double?,
        val km: Double,
        val days: Double?,
        val refuelOn: LocalDate?,
        val exceedsTank: Boolean,
        val efficiency: EfficiencyUsed,
    ) : AskAnswer
    data class Spending(val month: YearMonth, val totalCop: Long, val count: Int) : AskAnswer
    data class Price(val place: String?, val price: ReferencePrice?, val cheapest: ReferencePrice?, val average: ReferencePrice?) : AskAnswer
    data class Efficiency(val kmPerGallon: Double, val copPerKm: Double?) : AskAnswer
    data class LogFillUp(val text: String) : AskAnswer

    /** Sabe qué viaje es, pero le falta de dónde sale o a dónde va. */
    data class TripMissingPlace(val origin: String?, val destination: String?) : AskAnswer
    data class UnknownPlace(val name: String) : AskAnswer

    /** Necesita más tanqueos con km para responder con datos del vehículo. */
    data object NeedsData : AskAnswer
    data object NotUnderstood : AskAnswer
}

/**
 * Responde una pregunta en dos pasos: entender (la IA, y si no responde el lector local) y calcular
 * (siempre aquí). Lo que entrega la IA se revisa: una cifra que la persona no dijo se descarta, para
 * que un número inventado nunca termine en un cálculo.
 */
/** Hoy, mañana o pasado mañana: más allá no se puede estimar cuánta gasolina habrá al salir. */
private const val MAX_DAYS_UNTIL_TRIP = 2

class AnswerQuestion @Inject constructor(
    private val assistant: FuelAssistant,
    private val observeDashboard: ObserveDashboard,
    private val fillUps: FillUpRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(question: String): AskAnswer =
        when (val intent = interpret(question)) {
            is AskIntent.Trip -> trip(intent)
            is AskIntent.FuelRange -> fuelRange(intent)
            AskIntent.Range -> range()
            is AskIntent.Spending -> spending(intent.lastMonth)
            is AskIntent.Price -> price(intent.place)
            AskIntent.Efficiency -> efficiency()
            AskIntent.LogFillUp -> AskAnswer.LogFillUp(question)
            AskIntent.Unknown -> AskAnswer.NotUnderstood
        }

    private suspend fun interpret(question: String): AskIntent {
        val local = LocalQuestionParser.parse(question)
        val ai = assistant.interpretQuestion(question)?.withoutInventedNumbers(question)
        return when {
            ai == null || ai == AskIntent.Unknown -> local
            ai is AskIntent.Trip && local is AskIntent.Trip -> ai.copy(
                // Un nombre de la IA que no se reconoce no le gana a uno válido del lector local.
                origin = bestPlace(ai.origin, local.origin),
                destination = bestPlace(ai.destination, local.destination),
                distanceKm = ai.distanceKm ?: local.distanceKm,
                daysUntil = ai.daysUntil ?: local.daysUntil,
            )
            else -> ai
        }
    }

    private fun bestPlace(ai: String?, local: String?): String? {
        val fromAi = ai?.takeIf { it.isNotBlank() }
        return when {
            fromAi != null && RoadNetwork.resolve(fromAi) != null -> fromAi
            local != null && RoadNetwork.resolve(local) != null -> local
            else -> fromAi ?: local
        }
    }

    private fun AskIntent.withoutInventedNumbers(question: String): AskIntent? = when (this) {
        is AskIntent.Trip -> copy(
            distanceKm = distanceKm?.takeIf { it > 0 && SpanishNumbers.mentions(question, it.toDouble()) },
            daysUntil = daysUntil?.takeIf { it in 0..MAX_DAYS_UNTIL_TRIP },
        )
        is AskIntent.FuelRange -> {
            val money = moneyCop?.takeIf { it in FuelBudgetCalculator.MIN_MONEY_COP..FuelBudgetCalculator.MAX_MONEY_COP && SpanishNumbers.mentions(question, it.toDouble()) }
            val amount = gallons?.takeIf { it in FillUpInput.MIN_GALLONS..FillUpInput.MAX_GALLONS && SpanishNumbers.mentions(question, it) }
            if (money == null && amount == null) null else copy(gallons = amount, moneyCop = money)
        }
        else -> this
    }

    private suspend fun trip(intent: AskIntent.Trip): AskAnswer {
        // Si dijo los km, un lugar desconocido no impide responder.
        if (intent.distanceKm == null) {
            listOfNotNull(intent.origin, intent.destination).firstOrNull { RoadNetwork.resolve(it) == null }
                ?.let { return AskAnswer.UnknownPlace(it) }
        }
        val origin = intent.origin?.let(RoadNetwork::resolve)
        val destination = intent.destination?.let(RoadNetwork::resolve)
        val route = if (origin != null && destination != null) RoadNetwork.route(origin, destination) else null
        val km = intent.distanceKm ?: route?.km ?: return AskAnswer.TripMissingPlace(origin, destination)
        val waypoints: List<Waypoint> = route?.waypoints.orEmpty()

        val dashboard = observeDashboard.current()
        val estimate = dashboard.estimate as? RangeEstimate.Available
        val efficiency = efficiencyOf(dashboard)
        val atDeparture = estimate?.let {
            (it.kmRemaining - (it.kmPerDay ?: 0.0) * (intent.daysUntil ?: 0)) / it.kmPerGallon
        }

        val fuel = dashboard.lastFillUp?.fuelType ?: FuelType.CORRIENTE
        val reference = origin?.takeIf { fuel != FuelType.EXTRA }?.let { place ->
            ReferencePrices.forFuel(fuel).firstOrNull { it.place == place }
        }
        val ownPrice = FillUpInput.lastPricePerGallon(fillUps.current(), fuel)
        val averagePrice = ReferencePrices.forFuel(fuel).firstOrNull { it.place == ReferencePrices.AVERAGE }?.pricePerGallon?.toDouble()
        return AskAnswer.Trip(
            origin = origin,
            destination = destination,
            plan = TripPlanner.plan(
                km = km,
                waypoints = waypoints,
                kmPerGallon = efficiency.kmPerGallon,
                capacityGallons = dashboard.settings.tankCapacityGallons,
                gallonsInTank = atDeparture ?: 0.0,
                pricePerGallon = reference?.pricePerGallon?.toDouble() ?: ownPrice ?: averagePrice,
            ),
            daysUntil = intent.daysUntil,
            priceReference = reference?.place ?: ReferencePrices.AVERAGE.takeIf { ownPrice == null },
            efficiency = efficiency,
            tankKnown = atDeparture != null,
        )
    }

    private suspend fun range(): AskAnswer {
        val estimate = observeDashboard.current().estimate as? RangeEstimate.Available ?: return AskAnswer.NeedsData
        val today = today()
        return AskAnswer.Range(
            km = estimate.kmRemaining,
            days = estimate.daysRemaining,
            refuelOn = estimate.daysRemaining?.let { RangeEstimator.refuelDate(today, it) },
            today = today,
        )
    }

    private suspend fun fuelRange(intent: AskIntent.FuelRange): AskAnswer {
        val dashboard = observeDashboard.current()
        val efficiency = efficiencyOf(dashboard)
        val price = priceFor(dashboard.lastFillUp?.fuelType ?: FuelType.CORRIENTE)
        val gallons = intent.gallons ?: price?.let { p -> intent.moneyCop?.let { it / p } } ?: return AskAnswer.NotUnderstood
        val capacity = dashboard.settings.tankCapacityGallons
        val km = minOf(gallons, capacity) * efficiency.kmPerGallon
        val days = RangeEstimator.kmPerDay(fillUps.current())?.let { km / it }
        return AskAnswer.FuelRange(
            gallons = gallons,
            cop = intent.moneyCop?.toDouble() ?: price?.let { it * gallons },
            km = km,
            days = days,
            refuelOn = days?.let { RangeEstimator.refuelDate(today(), it) },
            exceedsTank = gallons > capacity,
            efficiency = efficiency,
        )
    }

    private fun efficiencyOf(dashboard: Dashboard): EfficiencyUsed =
        dashboard.kmPerGallon?.let { EfficiencyUsed(it, isReference = false) }
            ?: EfficiencyUsed(ConsumptionReference.KM_PER_GALLON, isReference = true)

    /** Último precio pagado por ese combustible; sin historia, el promedio de referencia. */
    private suspend fun priceFor(fuel: FuelType): Double? =
        FillUpInput.lastPricePerGallon(fillUps.current(), fuel)
            ?: ReferencePrices.forFuel(fuel).firstOrNull { it.place == ReferencePrices.AVERAGE }?.pricePerGallon?.toDouble()

    private suspend fun spending(lastMonth: Boolean): AskAnswer {
        val month = YearMonth.from(today()).let { if (lastMonth) it.minusMonths(1) else it }
        val inMonth = fillUps.current().filter { YearMonth.from(it.at.atZone(clock.zone)) == month }
        return AskAnswer.Spending(month, inMonth.sumOf { it.paidCop }, inMonth.size)
    }

    private suspend fun price(place: String?): AskAnswer {
        val fuel = observeDashboard.current().lastFillUp?.fuelType ?: FuelType.CORRIENTE
        val prices = ReferencePrices.forFuel(fuel)
        val cities = prices.filter { it.place != ReferencePrices.AVERAGE }
        val name = place?.let { RoadNetwork.resolve(it) ?: it }
        return AskAnswer.Price(
            place = name,
            price = cities.firstOrNull { it.place == name },
            cheapest = cities.minByOrNull { it.pricePerGallon },
            average = prices.firstOrNull { it.place == ReferencePrices.AVERAGE },
        )
    }

    private suspend fun efficiency(): AskAnswer {
        val dashboard = observeDashboard.current()
        val kmPerGallon = dashboard.kmPerGallon ?: return AskAnswer.NeedsData
        val fuel = dashboard.lastFillUp?.fuelType ?: FuelType.CORRIENTE
        val price = FillUpInput.lastPricePerGallon(fillUps.current(), fuel)
        return AskAnswer.Efficiency(kmPerGallon, price?.let { it / kmPerGallon })
    }

    private fun today(): LocalDate = clock.instant().atZone(clock.zone).toLocalDate()
}

private suspend fun FillUpRepository.current() = observeAll().first()
