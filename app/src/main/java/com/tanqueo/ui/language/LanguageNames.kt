package com.tanqueo.ui.language

import com.tanqueo.R
import com.tanqueo.domain.format.AppLanguage

/** Nombre del idioma escrito en ese mismo idioma: "Español", "English". */
val AppLanguage.nameRes: Int
    get() = when (this) {
        AppLanguage.ES -> R.string.language_es
        AppLanguage.EN -> R.string.language_en
    }
