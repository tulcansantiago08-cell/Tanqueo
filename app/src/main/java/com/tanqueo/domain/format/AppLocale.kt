package com.tanqueo.domain.format

import java.util.Locale

/** Idiomas de la app. El español es el predeterminado, aunque el reloj esté en otro idioma. */
enum class AppLanguage(val tag: String) {
    ES("es-CO"),
    EN("en"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        val DEFAULT = ES

        /** El idioma de una etiqueta o de un Locale ("en-US" es inglés); cualquier otro, español. */
        fun of(tagOrLanguage: String?): AppLanguage =
            entries.firstOrNull { tagOrLanguage?.lowercase()?.startsWith(it.locale.language) == true } ?: DEFAULT
    }
}

/**
 * El idioma con el que se formatean fechas y números. Lo fija la app al arrancar y al cambiar de
 * idioma; las pruebas lo dejan en español.
 */
object AppLocale {
    @Volatile var language: AppLanguage = AppLanguage.DEFAULT

    val current: Locale get() = language.locale
}
