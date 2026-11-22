package com.tanqueo.ui.stations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.stations.GasStation
import com.tanqueo.domain.stations.GeoPoint
import com.tanqueo.domain.stations.MapMath
import com.tanqueo.domain.stations.TileKey
import com.tanqueo.ui.components.RoundIconButton
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.theme.TanqueoColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Las teselas son de 256 px; en la pantalla pequeña del reloj se agrandan para que las calles se lean. */
private const val TILE_SCALE = 1.5f

/** Zoom al tocar una gasolinera de la lista: se leen las calles alrededor. */
private const val FOCUS_ZOOM = 16
private val MapShape = RoundedCornerShape(26.dp)

/**
 * Mapa propio con OpenStreetMap: tu ubicación en el centro (punto azul) y las gasolineras más cercanas
 * numeradas igual que en la lista. Sin internet para las teselas, los marcadores se ven igual sobre un
 * fondo oscuro.
 */
@Composable
fun StationsMap(
    origin: GeoPoint,
    stations: List<GasStation>,
    modifier: Modifier = Modifier,
    /** Índice de la gasolinera tocada en la lista: el mapa se centra en ella y la resalta. */
    selected: Int? = null,
) {
    val screen = rememberScreenSize()
    val marked = stations.take(MapMath.MAP_STATIONS)
    val description = pluralStringResource(R.plurals.stations_map_description, marked.size, marked.size)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(screen.dp(0.62f))
            .clip(MapShape)
            .background(Color(0xFF1C1C22))
            .border(1.dp, TanqueoColors.GlassBorder, MapShape)
            .semantics { contentDescription = description },
    ) {
        val widthPx = constraints.maxWidth.toDouble()
        val heightPx = constraints.maxHeight.toDouble()
        val fitted = remember(origin, marked, widthPx, heightPx) {
            MapMath.fitZoom(origin, marked.map { it.location }, widthPx / TILE_SCALE, heightPx / TILE_SCALE)
        }
        var zoom by rememberSaveable(fitted) { mutableIntStateOf(fitted) }
        val focus = selected?.let { stations.getOrNull(it) }
        // Al tocar una gasolinera se acerca a ella; si no, se ve todo alrededor de la persona.
        LaunchedEffect(focus) { if (focus != null) zoom = maxOf(zoom, FOCUS_ZOOM) }
        val center = focus?.location ?: origin
        val visible = remember(center, zoom, widthPx, heightPx) {
            MapMath.visibleTiles(center, zoom, widthPx / TILE_SCALE, heightPx / TILE_SCALE)
        }
        val tiles = remember { mutableStateMapOf<TileKey, ImageBitmap>() }
        LaunchedEffect(visible) {
            visible.forEach { key ->
                OsmTiles.cached(key)?.let { tiles[key] = it } ?: launch { OsmTiles.load(key)?.let { tiles[key] = it } }
            }
        }
        val textMeasurer = rememberTextMeasurer()
        val numberStyle = TextStyle(color = Color.White, fontSize = screen.sp(10f), fontWeight = FontWeight.Bold)

        Canvas(Modifier.fillMaxSize()) {
            val middle = MapMath.toPixel(center, zoom)
            val left = middle.x * TILE_SCALE - size.width / 2
            val top = middle.y * TILE_SCALE - size.height / 2
            val tileSide = (MapMath.TILE_SIZE * TILE_SCALE).roundToInt()
            visible.forEach { key ->
                val image = tiles[key] ?: return@forEach
                // La columna puede venir ya ajustada al otro lado del mundo; se ubica respecto al centro.
                val columns = 1 shl zoom
                var x = key.x
                while (x * MapMath.TILE_SIZE - middle.x > columns * MapMath.TILE_SIZE / 2) x -= columns
                while (middle.x - x * MapMath.TILE_SIZE > columns * MapMath.TILE_SIZE / 2) x += columns
                drawImage(
                    image = image,
                    dstOffset = IntOffset((x * tileSide - left).roundToInt(), (key.y * tileSide - top).roundToInt()),
                    dstSize = IntSize(tileSide, tileSide),
                    filterQuality = FilterQuality.Medium,
                )
            }
            // Tu ubicación.
            val me = MapMath.toPixel(origin, zoom)
            val here = Offset((me.x * TILE_SCALE - left).toFloat(), (me.y * TILE_SCALE - top).toFloat())
            drawCircle(Color.White, radius = 7.dp.toPx(), center = here)
            drawCircle(TanqueoColors.RingEnd, radius = 5.dp.toPx(), center = here)
            // Gasolineras: la más cercana se dibuja de último para quedar encima.
            // La seleccionada se dibuja de último y más grande, para que se distinga.
            // Una de la lista que no estaba en el mapa también aparece si se toca.
            val shown = (marked.indices + listOfNotNull(selected?.takeIf { it in stations.indices })).distinct()
            val order = shown.reversed().sortedBy { it == selected }
            order.forEach { index ->
                val station = stations[index]
                val p = MapMath.toPixel(station.location, zoom)
                val at = Offset((p.x * TILE_SCALE - left).toFloat(), (p.y * TILE_SCALE - top).toFloat())
                val radius = (if (index == selected) 13 else 9).dp.toPx()
                drawCircle(TanqueoColors.Accent, radius = radius, center = at)
                drawCircle(Color.White, radius = radius, center = at, style = Stroke(width = 1.5.dp.toPx()))
                val label = textMeasurer.measure((index + 1).toString(), numberStyle)
                drawText(label, topLeft = Offset(at.x - label.size.width / 2f, at.y - label.size.height / 2f))
            }
        }

        // Acercar y alejar, al borde derecho, donde no tapan el centro.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoundIconButton(
                icon = R.drawable.ic_add,
                contentDescription = stringResource(R.string.stations_zoom_in),
                size = screen.dp(0.11f),
                onClick = { zoom = (zoom + 1).coerceAtMost(MapMath.MAX_ZOOM) },
                compact = true,
                // Violeta sólido: el vidrio translúcido no se ve sobre el mapa claro.
                accent = true,
            )
            RoundIconButton(
                icon = R.drawable.ic_remove,
                contentDescription = stringResource(R.string.stations_zoom_out),
                size = screen.dp(0.11f),
                onClick = { zoom = (zoom - 1).coerceAtLeast(MapMath.MIN_ZOOM) },
                compact = true,
                // Violeta sólido: el vidrio translúcido no se ve sobre el mapa claro.
                accent = true,
            )
        }
        // Atribución que exige OpenStreetMap.
        Text(
            text = stringResource(R.string.stations_map_attribution),
            color = Color.White,
            fontSize = screen.sp(8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .background(Color(0x99000000), RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
