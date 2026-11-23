package com.tanqueo.domain

import com.tanqueo.domain.stations.FindGasStations
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.SavedStations
import com.tanqueo.domain.stations.StationCache
import com.tanqueo.domain.stations.StationPlace
import com.tanqueo.domain.stations.StationSearch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationCacheTest {
    private val pasto = GeoPoint(1.2136, -77.2811)
    private val now = Instant.parse("2026-11-20T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val terpel = StationPlace("Terpel Centro", "Terpel", GeoPoint(1.2140, -77.2811))

    private class MemoryCache(var saved: SavedStations? = null) : StationCache {
        override suspend fun load() = saved
        override suspend fun save(saved: SavedStations) {
            this.saved = saved
        }
    }

    @Test fun `una busqueda que funciona queda guardada`() = runTest {
        val cache = MemoryCache()
        FindGasStations({ pasto }, { _, _ -> listOf(terpel) }, cache, clock)()
        assertEquals(SavedStations(pasto, listOf(terpel), now), cache.saved)
    }

    @Test fun `una busqueda vacia no borra la guardada`() = runTest {
        val old = SavedStations(pasto, listOf(terpel), now.minus(Duration.ofDays(1)))
        val cache = MemoryCache(old)
        FindGasStations({ pasto }, { _, _ -> emptyList() }, cache, clock)()
        assertEquals(old, cache.saved)
    }

    @Test fun `sin internet muestra la guardada con su fecha`() = runTest {
        val savedAt = now.minus(Duration.ofDays(2))
        val cache = MemoryCache(SavedStations(pasto, listOf(terpel), savedAt))
        val result = FindGasStations({ pasto }, { _, _ -> null }, cache, clock)() as StationSearch.Found
        assertEquals(listOf("Terpel Centro"), result.stations.map { it.name })
        assertEquals(savedAt, result.savedAt)
    }

    @Test fun `con internet la lista es de ahora, sin fecha`() = runTest {
        val result = FindGasStations({ pasto }, { _, _ -> listOf(terpel) }, MemoryCache(), clock)() as StationSearch.Found
        assertNull(result.savedAt)
    }

    @Test fun `sin internet no usa una lista vieja ni de otra zona`() = runTest {
        val tooOld = MemoryCache(SavedStations(pasto, listOf(terpel), now.minus(Duration.ofDays(31))))
        assertEquals(StationSearch.Offline, FindGasStations({ pasto }, { _, _ -> null }, tooOld, clock)())

        val ipiales = GeoPoint(0.8302, -77.6444)
        val elsewhere = MemoryCache(SavedStations(ipiales, listOf(terpel), now))
        assertEquals(StationSearch.Offline, FindGasStations({ pasto }, { _, _ -> null }, elsewhere, clock)())
    }
}
