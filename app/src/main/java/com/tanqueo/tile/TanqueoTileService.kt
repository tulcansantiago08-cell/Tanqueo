package com.tanqueo.tile

import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tanqueo.domain.usecase.ObserveDashboard
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

@AndroidEntryPoint
class TanqueoTileService : TileService() {
    @Inject lateinit var observeDashboard: ObserveDashboard

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = scope.future {
        val layout = tanqueoTileLayout(
            context = this@TanqueoTileService,
            deviceParameters = requestParams.deviceConfiguration,
            dashboard = observeDashboard.current(),
        )
        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            // La autonomía baja con el tiempo aunque no se registre nada.
            .setFreshnessIntervalMillis(TimeUnit.HOURS.toMillis(1))
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(Resources.Builder().setVersion(RESOURCES_VERSION).build())

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
