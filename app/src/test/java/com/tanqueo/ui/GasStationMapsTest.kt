package com.tanqueo.ui

import com.tanqueo.ui.maps.GasStationMaps
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GasStationMapsTest {
    @Test fun `busca gasolineras alrededor de la ubicacion actual`() {
        assertEquals("geo:0,0?q=gasolineras", GasStationMaps.watchUri().toString())
    }

    @Test fun `una gasolinera de la lista se abre con su punto y su nombre`() {
        val point = com.tanqueo.domain.stations.GeoPoint(1.2136, -77.2811)
        assertEquals("geo:1.213600,-77.281100?q=1.213600%2C-77.281100(Terpel)", GasStationMaps.placeWatchUri(point, "Terpel").toString())
    }

    @Test fun `sin app de mapas lo sabe y no intenta abrir nada`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        org.junit.Assert.assertFalse(GasStationMaps.isAvailable(context))
    }
}
