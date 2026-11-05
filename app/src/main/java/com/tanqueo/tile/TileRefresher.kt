package com.tanqueo.tile

import android.content.Context
import com.tanqueo.domain.repository.SurfaceRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Por ahora no hay superficies externas; la tile se conecta aquí cuando exista. */
class TileRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SurfaceRefresher {
    override fun refresh() = Unit
}
