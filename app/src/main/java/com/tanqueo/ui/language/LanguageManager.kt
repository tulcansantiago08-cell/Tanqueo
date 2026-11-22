package com.tanqueo.ui.language

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.tanqueo.domain.format.AppLanguage
import com.tanqueo.domain.format.AppLocale
import androidx.core.content.edit
import java.util.Locale

/**
 * Idioma de la app, español por defecto aunque el reloj esté en otro idioma.
 *
 * Desde Android 13 (Wear OS 4) se usa el idioma por app del sistema: cambia también la tile, la
 * complicación y los avisos, y aparece en los ajustes del reloj. En versiones anteriores se guarda
 * aquí y se aplica a la app al crear cada pantalla.
 */
object LanguageManager {
    private const val PREFS = "idioma"
    private const val KEY = "idioma_app"

    /** Al arrancar la app: si nadie eligió idioma, queda en español. */
    fun applyAtStartup(context: Context) {
        val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java)
            val chosen = manager.applicationLocales
            if (chosen.isEmpty) {
                manager.applicationLocales = LocaleList.forLanguageTags(AppLanguage.DEFAULT.tag)
                AppLanguage.DEFAULT
            } else {
                AppLanguage.of(chosen[0].toLanguageTag())
            }
        } else {
            saved(context)
        }
        use(language)
    }

    fun current(context: Context): AppLanguage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val chosen = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (chosen.isEmpty) AppLanguage.DEFAULT else AppLanguage.of(chosen[0].toLanguageTag())
        } else {
            saved(context)
        }

    /** Cambia el idioma y vuelve a dibujar la pantalla con él. */
    fun set(activity: Activity, language: AppLanguage) {
        if (language == current(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, language.tag) }
        use(language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // El sistema vuelve a crear las pantallas abiertas con el idioma nuevo.
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
        } else {
            activity.recreate()
        }
    }

    /**
     * Envuelve el contexto con el idioma elegido. Antes de Android 13 es la única forma; desde 13 evita
     * que la primera pantalla salga en el idioma del reloj mientras el sistema aplica el de la app.
     */
    fun wrap(base: Context): Context {
        val locale = current(base).locale
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    /** Fechas y números con el idioma elegido, también en la tile y los avisos. */
    private fun use(language: AppLanguage) {
        AppLocale.language = language
        Locale.setDefault(language.locale)
    }

    private fun saved(context: Context): AppLanguage =
        AppLanguage.of(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
}
