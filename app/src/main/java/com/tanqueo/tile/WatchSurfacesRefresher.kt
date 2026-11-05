package com.tanqueo.tile

import android.content.Context
import androidx.wear.tiles.TileService
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Pide al sistema que vuelva a pedir la tile cuando cambian los datos. */
class TileRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SurfaceRefresher {
    override fun refresh() {
        TileService.getUpdater(context).requestUpdate(TanqueoTileService::class.java)
    }
}
