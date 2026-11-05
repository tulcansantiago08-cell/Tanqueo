package com.tanqueo.domain.usecase

import com.tanqueo.domain.repository.CachedTip
import com.tanqueo.domain.repository.FuelAssistant
import com.tanqueo.domain.repository.SettingsRepository
import com.tanqueo.domain.tip.MonthFacts
import com.tanqueo.domain.tip.MonthTips
import com.tanqueo.domain.voice.DictatedFillUp
import com.tanqueo.domain.voice.LocalFillUpParser
import java.time.LocalDate
import javax.inject.Inject

/**
 * Convierte una frase dictada en datos. Primero la IA, que entiende frases sueltas; el lector local
 * completa lo que ella no entregue y responde solo si no hay IA. El usuario siempre revisa antes de
 * guardar, y los valores imposibles se descartan.
 */
class InterpretDictation @Inject constructor(private val assistant: FuelAssistant) {
    suspend operator fun invoke(spokenText: String, minOdometerKm: Int): DictatedFillUp {
        val local = LocalFillUpParser.parse(spokenText).sanitized(minOdometerKm)
        val ai = assistant.interpretFillUp(spokenText)?.sanitized(minOdometerKm) ?: return local
        return ai.orElse(local)
    }
}

/**
 * Redacción del consejo por la IA, una vez al día mientras los datos no cambien. Devuelve null si no
 * hay IA o si su texto trae cifras que no están en los datos: entonces se muestra el consejo local.
 */
class WriteMonthTip @Inject constructor(
    private val assistant: FuelAssistant,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(facts: MonthFacts, today: LocalDate): String? {
        val prompt = facts.asPromptText()
        val key = "$today|${prompt.hashCode()}"
        settings.cachedTip()?.takeIf { it.key == key }?.let { return it.text }
        val text = assistant.writeTip(prompt)?.trim()?.takeIf { MonthTips.isTrustworthy(it, facts) } ?: return null
        settings.setCachedTip(CachedTip(key, text))
        return text
    }
}
