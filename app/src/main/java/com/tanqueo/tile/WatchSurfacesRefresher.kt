package com.tanqueo.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.tanqueo.complication.RangeComplicationService
import com.tanqueo.domain.repository.SurfaceRefresher
import com.tanqueo.reminder.RefuelReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Al cambiar los datos: vuelve a pedir la tile y la complicación, y revisa si el aviso sigue vigente. */
class WatchSurfacesRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SurfaceRefresher {
    override fun refresh() {
        TileService.getUpdater(context).requestUpdate(TanqueoTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, RangeComplicationService::class.java))
            .requestUpdateAll()
        RefuelReminderWorker.clearIfNoLongerNeeded(context)
    }
}
