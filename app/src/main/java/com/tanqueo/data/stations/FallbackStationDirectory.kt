package com.tanqueo.data.stations

import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.StationDirectory
import com.tanqueo.domain.stations.StationPlace
import javax.inject.Inject

/**
 * Prueba un directorio y, si no responde, el siguiente: los servicios gratuitos de OpenStreetMap a
 * veces están saturados. Null solo si ninguno respondió.
 */
class FallbackStationDirectory(private val directories: List<StationDirectory>) : StationDirectory {
    @Inject constructor(nominatim: NominatimStationDirectory, overpass: OverpassStationDirectory) :
        this(listOf(nominatim, overpass))

    override suspend fun around(point: GeoPoint, radiusMeters: Int): List<StationPlace>? {
        for (directory in directories) {
            directory.around(point, radiusMeters)?.let { return it }
        }
        return null
    }
}
