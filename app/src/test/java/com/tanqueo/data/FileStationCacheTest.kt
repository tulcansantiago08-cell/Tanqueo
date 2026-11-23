package com.tanqueo.data

import com.tanqueo.data.stations.FileStationCache
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.SavedStations
import com.tanqueo.domain.stations.StationPlace
import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class FileStationCacheTest {
    @get:Rule val folder = TemporaryFolder()

    private val saved = SavedStations(
        origin = GeoPoint(1.2136, -77.2811),
        places = listOf(
            StationPlace("Terpel Centro", "Terpel", GeoPoint(1.2140, -77.2811), street = "Calle 18"),
            StationPlace(null, null, GeoPoint(1.2150, -77.2800)),
        ),
        at = Instant.parse("2026-11-20T15:00:00Z"),
    )

    @Test fun `lo guardado se lee igual, tambien los campos vacios`() = runTest {
        val file = File(folder.root, "gasolineras.json")
        FileStationCache(file).save(saved)
        assertEquals(saved, FileStationCache(file).load())
    }

    @Test fun `sin archivo o con uno danado no hay nada guardado`() = runTest {
        val file = File(folder.root, "gasolineras.json")
        assertNull(FileStationCache(file).load())
        file.writeText("{no es json")
        assertNull(FileStationCache(file).load())
    }
}
