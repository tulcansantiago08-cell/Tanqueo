package com.tanqueo.domain.model

/**
 * Qué se tanquea. Solo cambia los valores de partida: en cuanto hay historial, mandan los datos reales.
 *
 * - [defaultCapacityGallons]: un carro particular ronda los 12 galones; una moto de 100 a 200 cc,
 *   unos 3 (10 a 12 litros).
 * - [defaultFillUpGallons]: lo que se sugiere al registrar el primer tanqueo.
 * - [referenceKmPerGallon]: el consumo que se usa sin historial. No hay una cifra oficial actual en
 *   Colombia: lo publicado para un carro particular va de 25 a 40 km/galón, y para una moto de
 *   trabajo de 80 a 130. Se usa un valor intermedio y la app siempre dice que es de referencia.
 * - [averageRoadSpeedKmh]: promedio en carretera colombiana, con curvas, cordillera y pueblos, sin
 *   contar paradas. Pasto–Cali (unos 400 km) toma cerca de 8 horas en carro; una moto de 125 a 160 cc
 *   va algo más despacio en subidas y con viento.
 */
enum class VehicleType(
    val defaultCapacityGallons: Double,
    val defaultFillUpGallons: Double,
    val referenceKmPerGallon: Double,
    val averageRoadSpeedKmh: Double,
) {
    CARRO(defaultCapacityGallons = 12.0, defaultFillUpGallons = 10.0, referenceKmPerGallon = 35.0, averageRoadSpeedKmh = 50.0),
    MOTO(defaultCapacityGallons = 3.0, defaultFillUpGallons = 2.0, referenceKmPerGallon = 100.0, averageRoadSpeedKmh = 45.0),
    ;

    companion object {
        /** Quien ya usaba la app antes de poder elegir tenía un carro. */
        val DEFAULT = CARRO
    }
}
