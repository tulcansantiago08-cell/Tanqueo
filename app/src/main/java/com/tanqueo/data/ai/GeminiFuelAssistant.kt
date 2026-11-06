package com.tanqueo.data.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.tanqueo.domain.assistant.AskIntent
import com.tanqueo.domain.model.FuelType
import com.tanqueo.domain.repository.FuelAssistant
import com.tanqueo.domain.tip.MonthTips
import com.tanqueo.domain.voice.DictatedFillUp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "TanqueoIA"

/** En orden de preferencia: si uno agota su cuota gratuita o se satura, se pasa al siguiente. */
private val MODELS = listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.5-flash")

/** En el reloj nadie espera mucho: pasado este tiempo sigue el lector local. */
private const val TOTAL_TIMEOUT_MS = 12_000L
private const val ATTEMPT_TIMEOUT_MS = 6_000L

private val DICTATION_INSTRUCTIONS = """
    Extraes los datos de un tanqueo de gasolina dictado en español de Colombia.
    Devuelve solo lo que la persona dijo. Si un dato no se dijo, omítelo: nunca lo adivines.
    gallons: galones cargados ("diez y medio" es 10.5).
    paidCop: valor pagado en pesos colombianos, entero ("165 mil" o "ciento sesenta y cinco" referido a dinero es 165000; "una luca" es 1000).
    fuelType: CORRIENTE, EXTRA o DIESEL ("ACPM" es DIESEL).
    isFull: true si llenó el tanque, false si fue parcial.
    odometerKm: kilometraje del odómetro, entero.
""".trimIndent()

private val TIP_INSTRUCTIONS = """
    Escribes un consejo breve y amable sobre el gasto en gasolina para la pantalla de un reloj.
    Español de Colombia, una sola frase, máximo 110 caracteres, sin emojis.
    Usa únicamente las cifras de los datos que recibes, escritas igual. No calcules ni inventes otras.
""".trimIndent()

private val QUESTION_INSTRUCTIONS = """
    Clasificas una pregunta en español de Colombia para una app de gasolina en un reloj. Solo clasificas: no respondas ni calcules.
    intent:
    TRIP: un viaje, cuánto tanquear o cuánto cuesta ir a algún lado ("mañana me voy de Pasto a Medellín", "cuánto gasto de Pasto a Cali").
    FUEL_RANGE: cuántos km da una cantidad de gasolina, en dinero o en galones ("hasta dónde llego con 80 mil", "eché 5 galones, ¿cuántos km puedo andar?").
    RANGE: cuánta gasolina queda, cuánto dura o cuándo tanquear.
    SPENDING: cuánto ha gastado.
    PRICE: cuánto cuesta el galón o dónde es más barato.
    EFFICIENCY: rendimiento o consumo del vehículo.
    LOG_FILL_UP: solo cuenta un tanqueo que ya hizo, sin preguntar nada ("tanqueé 10 galones por 165 mil").
    UNKNOWN: cualquier otra cosa.
    Devuelve solo lo que la persona dijo; lo que no dijo, omítelo. Nunca adivines distancias ni precios.
    origin, destination y place: el nombre del lugar, tal como se dijo.
    distanceKm: solo si dijo los kilómetros del viaje.
    daysUntilTrip: 0 si es hoy, 1 si es mañana, 2 si es pasado mañana.
    moneyCop: pesos colombianos, entero ("80 mil" o "80 lucas" es 80000).
    gallons: galones que dijo.
    lastMonth: true si pregunta por el mes pasado.
""".trimIndent()

private val questionSchema = Schema.obj(
    properties = mapOf(
        "intent" to Schema.enumeration(QuestionDto.INTENTS),
        "origin" to Schema.string(),
        "destination" to Schema.string(),
        "place" to Schema.string(),
        "distanceKm" to Schema.integer(),
        "daysUntilTrip" to Schema.integer(),
        "moneyCop" to Schema.long(),
        "gallons" to Schema.double(),
        "lastMonth" to Schema.boolean(),
    ),
    optionalProperties = listOf("origin", "destination", "place", "distanceKm", "daysUntilTrip", "moneyCop", "gallons", "lastMonth"),
)

private val dictationSchema = Schema.obj(
    properties = mapOf(
        "gallons" to Schema.double(),
        "paidCop" to Schema.long(),
        "fuelType" to Schema.enumeration(FuelType.entries.map { it.name }),
        "isFull" to Schema.boolean(),
        "odometerKm" to Schema.integer(),
    ),
    optionalProperties = listOf("gallons", "paidCop", "fuelType", "isFull", "odometerKm"),
)

/** Respuesta estructurada de la IA; coincide con [dictationSchema]. */
@Serializable
internal data class DictationDto(
    val gallons: Double? = null,
    val paidCop: Long? = null,
    val fuelType: String? = null,
    val isFull: Boolean? = null,
    val odometerKm: Int? = null,
) {
    fun toDomain() = DictatedFillUp(
        gallons = gallons,
        paidCop = paidCop,
        fuelType = FuelType.entries.firstOrNull { it.name == fuelType },
        isFull = isFull,
        odometerKm = odometerKm,
    )
}

/** Respuesta estructurada de la IA; coincide con [questionSchema]. */
@Serializable
internal data class QuestionDto(
    val intent: String,
    val origin: String? = null,
    val destination: String? = null,
    val place: String? = null,
    val distanceKm: Int? = null,
    val daysUntilTrip: Int? = null,
    val moneyCop: Long? = null,
    val gallons: Double? = null,
    val lastMonth: Boolean? = null,
) {
    /** Un viaje sin ningún dato o un valor sin cifra no sirven: se tratan como no entendidos. */
    fun toDomain(): AskIntent = when (intent) {
        "TRIP" -> AskIntent.Trip(origin, destination, distanceKm, daysUntilTrip)
        "FUEL_RANGE" -> if (gallons == null && moneyCop == null) AskIntent.Unknown else AskIntent.FuelRange(gallons, moneyCop)
        "RANGE" -> AskIntent.Range
        "SPENDING" -> AskIntent.Spending(lastMonth == true)
        "PRICE" -> AskIntent.Price(place)
        "EFFICIENCY" -> AskIntent.Efficiency
        "LOG_FILL_UP" -> AskIntent.LogFillUp
        else -> AskIntent.Unknown
    }

    companion object {
        val INTENTS = listOf("TRIP", "FUEL_RANGE", "RANGE", "SPENDING", "PRICE", "EFFICIENCY", "LOG_FILL_UP", "UNKNOWN")
    }
}

private val json = Json { ignoreUnknownKeys = true }

internal fun parseQuestion(text: String): AskIntent? =
    runCatching { json.decodeFromString<QuestionDto>(text).toDomain() }.getOrNull()

internal fun parseDictation(text: String): DictatedFillUp? =
    runCatching { json.decodeFromString<DictationDto>(text).toDomain() }.getOrNull()

/** Gemini por Firebase AI Logic. Sin google-services.json simplemente devuelve null. */
class GeminiFuelAssistant @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : FuelAssistant {

    private val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    override suspend fun interpretFillUp(spokenText: String): DictatedFillUp? =
        firstAnswer(spokenText) { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = dictationSchema
                    temperature = 0f
                },
                systemInstruction = content { text(DICTATION_INSTRUCTIONS) },
            )
        }?.let(::parseDictation)

    override suspend fun interpretQuestion(question: String): AskIntent? =
        firstAnswer(question) { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = questionSchema
                    temperature = 0f
                },
                systemInstruction = content { text(QUESTION_INSTRUCTIONS) },
            )
        }?.let(::parseQuestion)

    override suspend fun writeTip(monthFacts: String): String? =
        firstAnswer(monthFacts) { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    maxOutputTokens = MonthTips.MAX_TIP_LENGTH
                },
                systemInstruction = content { text(TIP_INSTRUCTIONS) },
            )
        }

    private suspend fun firstAnswer(prompt: String, model: (String) -> GenerativeModel): String? {
        if (!isConfigured) return null
        return withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            for (name in MODELS) {
                try {
                    val answer = withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) {
                        model(name).generateContent(prompt).text?.trim()
                    }
                    if (!answer.isNullOrBlank()) return@withTimeoutOrNull answer
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Sin el texto dictado en el registro: solo el modelo y el tipo de error.
                    Log.w(TAG, "Falló $name: ${e.javaClass.simpleName}")
                }
            }
            null
        }
    }
}
