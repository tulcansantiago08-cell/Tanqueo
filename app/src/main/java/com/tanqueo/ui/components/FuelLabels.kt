package com.tanqueo.ui.components

import androidx.annotation.StringRes
import com.tanqueo.R
import com.tanqueo.domain.model.FuelType

@get:StringRes
val FuelType.labelRes: Int
    get() = when (this) {
        FuelType.CORRIENTE -> R.string.fuel_corriente
        FuelType.EXTRA -> R.string.fuel_extra
        FuelType.DIESEL -> R.string.fuel_diesel
    }
