package com.tanqueo

/** Trabajo que corre al abrir la app. El set está vacío en release; en debug trae los datos de ejemplo. */
fun interface StartupTask {
    suspend fun run()
}
