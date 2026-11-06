package com.tanqueo.ui.ask

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import com.tanqueo.R
import com.tanqueo.domain.assistant.TripPlan
import com.tanqueo.domain.assistant.TripStop
import com.tanqueo.domain.budget.ReferencePrice
import com.tanqueo.domain.budget.ReferencePrices
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.usecase.AskAnswer
import com.tanqueo.domain.usecase.EfficiencyUsed
import com.tanqueo.ui.components.edgeAware
import com.tanqueo.ui.components.rangeDaysText
import com.tanqueo.ui.components.rangeKmText
import com.tanqueo.ui.components.refuelDateText
import com.tanqueo.ui.theme.TanqueoColors
import java.time.format.TextStyle
import java.util.Locale

internal fun TransformingLazyColumnScope.answerItems(answer: AskAnswer) {
    when (answer) {
        is AskAnswer.Trip -> tripItems(answer)
        is AskAnswer.Range -> item {
            AnswerCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.ask_range_label),
                value = rangeKmText(answer.km),
                detail = answer.days?.let { "${rangeDaysText(it)} · ${refuelDateText(it)}" },
            )
        }
        is AskAnswer.FuelRange -> fuelRangeItems(answer)
        is AskAnswer.Spending -> item {
            val month = answer.month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("es-CO"))
            AnswerCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.ask_spending_label, month),
                value = Formats.cop(answer.totalCop),
                detail = stringResource(R.string.ask_spending_detail, answer.count),
            )
        }
        is AskAnswer.Price -> priceItems(answer)
        is AskAnswer.Efficiency -> item {
            AnswerCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.summary_efficiency),
                value = "${Formats.oneDecimal(answer.kmPerGallon)} ${stringResource(R.string.unit_km_per_gallon)}",
                detail = answer.copPerKm?.let { stringResource(R.string.ask_cost_per_km, Formats.cop(it)) },
            )
        }
        is AskAnswer.TripMissingPlace -> item {
            AnswerMessage(modifier = Modifier.edgeAware(this), text = stringResource(R.string.ask_trip_missing))
        }
        is AskAnswer.UnknownPlace -> item {
            AnswerMessage(modifier = Modifier.edgeAware(this), text = stringResource(R.string.ask_unknown_place, answer.name))
        }
        AskAnswer.NeedsData -> item {
            AnswerMessage(modifier = Modifier.edgeAware(this), text = stringResource(R.string.ask_needs_data))
        }
        AskAnswer.NotUnderstood -> item {
            AnswerMessage(modifier = Modifier.edgeAware(this), text = stringResource(R.string.ask_not_understood))
        }
        // La pantalla la lleva directo al registro; aquí nunca se dibuja.
        is AskAnswer.LogFillUp -> Unit
    }
}

private fun TransformingLazyColumnScope.fuelRangeItems(answer: AskAnswer.FuelRange) {
    item {
        AnswerCard(
            modifier = Modifier.edgeAware(this),
            label = listOfNotNull(
                stringResource(R.string.ask_gallons, Formats.oneDecimal(answer.gallons)),
                answer.cop?.let { stringResource(R.string.ask_approx_cop, Formats.approxCop(it)) },
            ).joinToString(" · "),
            value = rangeKmText(answer.km),
            valueColor = TanqueoColors.Accent,
            detail = answer.days?.let { days ->
                // Con espacios que no se parten: si no cabe, baja completa a la línea siguiente.
                val until = answer.refuelOn?.let { stringResource(R.string.ask_until, Formats.dayDate(it)).replace(' ', '\u00A0') }
                listOfNotNull(rangeDaysText(days), until).joinToString(" · ")
            },
        )
    }
    item {
        AnswerFootnote(
            modifier = Modifier.edgeAware(this),
            text = listOfNotNull(
                stringResource(R.string.ask_tank_limit).takeIf { answer.exceedsTank },
                efficiencyNote(answer.efficiency),
            ).joinToString(" "),
        )
    }
}

private fun TransformingLazyColumnScope.tripItems(answer: AskAnswer.Trip) {
    val plan = answer.plan
    item {
        AnswerCard(
            modifier = Modifier.edgeAware(this),
            label = tripTitle(answer),
            value = stringResource(R.string.range_km, Formats.integer(plan.km)),
            detail = departureText(answer.daysUntil),
        )
    }
    item {
        AnswerCard(
            modifier = Modifier.edgeAware(this),
            label = stringResource(R.string.ask_trip_cost),
            value = plan.copOfTrip?.let { stringResource(R.string.ask_approx_cop, Formats.approxCop(it)) }
                ?: stringResource(R.string.ask_gallons, Formats.oneDecimal(plan.gallonsNeeded)),
            valueColor = TanqueoColors.Accent,
            detail = stringResource(R.string.ask_trip_cost_gallons, Formats.oneDecimal(plan.gallonsNeeded)).takeIf { plan.copOfTrip != null },
        )
    }
    if (answer.tankKnown) {
        item {
            AnswerCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(if (plan.alreadyEnough) R.string.ask_trip_enough else R.string.ask_trip_before),
                value = if (plan.alreadyEnough) {
                    stringResource(R.string.ask_trip_no_refuel)
                } else {
                    stringResource(R.string.ask_gallons, Formats.oneDecimal(plan.gallonsBeforeLeaving))
                },
                detail = if (plan.alreadyEnough) {
                    stringResource(R.string.ask_trip_in_tank, Formats.oneDecimal(plan.gallonsInTank))
                } else {
                    plan.copBeforeLeaving?.let { stringResource(R.string.ask_approx_cop, Formats.approxCop(it)) }
                },
            )
        }
    }
    if (answer.tankKnown) {
        plan.stops.forEach { stop ->
            item { TripStopCard(stop, Modifier.edgeAware(this)) }
        }
        if (plan.stops.isNotEmpty()) {
            item {
                AnswerCard(
                    modifier = Modifier.edgeAware(this),
                    label = stringResource(R.string.ask_trip_total),
                    value = stringResource(R.string.ask_gallons, Formats.oneDecimal(plan.gallonsTotal)),
                    detail = plan.copTotal?.let { stringResource(R.string.ask_approx_cop, Formats.approxCop(it)) },
                )
            }
        }
    }
    item {
        AnswerFootnote(modifier = Modifier.edgeAware(this), text = tripNote(answer, plan))
    }
}

@Composable
private fun TripStopCard(stop: TripStop, modifier: Modifier) {
    AnswerCard(
        modifier = modifier,
        label = stop.place?.let { stringResource(R.string.ask_stop_in, it) } ?: stringResource(R.string.ask_stop_at_km, Formats.integer(stop.atKm)),
        value = stringResource(R.string.ask_gallons, Formats.oneDecimal(stop.gallons)),
        detail = stringResource(R.string.ask_stop_detail, Formats.integer(stop.atKm)),
    )
}

@Composable
private fun tripTitle(answer: AskAnswer.Trip): String {
    val origin = answer.origin
    val destination = answer.destination
    return if (origin != null && destination != null) {
        stringResource(R.string.ask_trip_route, origin, destination)
    } else {
        stringResource(R.string.ask_trip_distance)
    }
}

@Composable
private fun departureText(days: Int?): String? = when (days) {
    0 -> stringResource(R.string.ask_leaving_today)
    1 -> stringResource(R.string.ask_leaving_tomorrow)
    2 -> stringResource(R.string.ask_leaving_day_after)
    else -> null
}

@Composable
private fun tripNote(answer: AskAnswer.Trip, plan: TripPlan): String {
    val price = plan.pricePerGallon
    val priceNote = when {
        price == null -> null
        answer.priceReference == ReferencePrices.AVERAGE -> stringResource(R.string.ask_note_price_average, Formats.cop(price))
        answer.priceReference != null -> stringResource(R.string.ask_note_price_reference, Formats.cop(price), answer.priceReference)
        else -> stringResource(R.string.ask_note_price_own, Formats.cop(price))
    }
    return listOfNotNull(efficiencyNote(answer.efficiency), priceNote, stringResource(R.string.ask_trip_note)).joinToString(" ")
}

/** De dónde sale el consumo usado: siempre se dice, para saber qué tanto confiar en el resultado. */
@Composable
private fun efficiencyNote(efficiency: EfficiencyUsed): String {
    val value = Formats.oneDecimal(efficiency.kmPerGallon)
    return stringResource(if (efficiency.isReference) R.string.ask_note_reference else R.string.ask_note_own, value)
}

private fun TransformingLazyColumnScope.priceItems(answer: AskAnswer.Price) {
    answer.price?.let { price ->
        item {
            AnswerCard(
                modifier = Modifier.edgeAware(this),
                label = stringResource(R.string.ask_price_in, price.place),
                value = Formats.cop(price.pricePerGallon),
            )
        }
    } ?: answer.place?.let { place ->
        item { AnswerMessage(modifier = Modifier.edgeAware(this), text = stringResource(R.string.ask_price_unknown, place)) }
    }
    answer.cheapest?.takeIf { it.place != answer.price?.place }?.let { cheapest ->
        item { ReferenceCard(R.string.ask_price_cheapest, cheapest, Modifier.edgeAware(this)) }
    }
    answer.average?.let { average ->
        item { ReferenceCard(R.string.ask_price_average, average, Modifier.edgeAware(this)) }
    }
}

@Composable
private fun ReferenceCard(label: Int, price: ReferencePrice, modifier: Modifier) {
    AnswerCard(
        modifier = modifier,
        label = stringResource(label, price.place),
        value = Formats.cop(price.pricePerGallon),
    )
}
