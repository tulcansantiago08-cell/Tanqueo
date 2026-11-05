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

private val json = Json { ignoreUnknownKeys = true }

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
