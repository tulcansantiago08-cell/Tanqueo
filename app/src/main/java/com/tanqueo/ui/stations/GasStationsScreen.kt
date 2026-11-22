package com.tanqueo.ui.stations

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.tanqueo.R
import com.tanqueo.domain.format.Formats
import com.tanqueo.domain.stations.GasStation
import com.tanqueo.domain.stations.GasStationRanking
import com.tanqueo.domain.stations.MapMath
import com.tanqueo.domain.stations.StationSearch
import com.tanqueo.ui.components.GlassCard
import com.tanqueo.ui.components.GlassPillButton
import com.tanqueo.ui.components.ListScreen
import com.tanqueo.ui.components.PrimaryPillButton
import com.tanqueo.ui.components.edgeAware
import com.tanqueo.ui.components.rememberScreenSize
import com.tanqueo.ui.maps.GasStationMaps
import com.tanqueo.ui.maps.rememberMapsAvailable
import com.tanqueo.ui.theme.TanqueoColors

/** El mapa va justo debajo del título. */
private const val MAP_ITEM_INDEX = 1

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

/**
 * Gasolineras cerca: el mapa y la lista salen de OpenStreetMap y funcionan sin Google Maps (también en
 * el emulador). Si el reloj tiene una app de mapas, "Ver en Mapas" la abre y tocar una gasolinera la
 * abre allí; si no, tocarla la marca en el mapa propio. Nunca manda a la tienda ni pide una cuenta.
 */
@Composable
fun GasStationsScreen(viewModel: GasStationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mapsAvailable = rememberMapsAvailable()
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberTransformingLazyColumnState()
    val scope = rememberCoroutineScope()
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) viewModel.search() else viewModel.onPermissionDenied()
    }
    LaunchedEffect(Unit) {
        if (state != StationsUiState.Starting) return@LaunchedEffect
        val hasPermission = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) viewModel.search() else askPermission.launch(LOCATION_PERMISSIONS)
    }

    val found = ((state as? StationsUiState.Done)?.search as? StationSearch.Found)
    ListScreen(listState) {
        item {
            Text(
                text = stringResource(R.string.stations_title),
                color = TanqueoColors.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.edgeAware(this).fillMaxWidth(),
            )
        }
        if (found != null) {
            item { StationsMap(found.origin, found.stations, Modifier.edgeAware(this), selected = selected) }
        }
        if (mapsAvailable) {
            item {
                GlassPillButton(
                    text = stringResource(R.string.stations_open_maps),
                    icon = R.drawable.ic_map,
                    onClick = { GasStationMaps.open(context) },
                    modifier = Modifier.edgeAware(this).fillMaxWidth(),
                )
            }
        }
        when (val current = state) {
            StationsUiState.Starting, StationsUiState.Searching -> item {
                StationMessage(stringResource(R.string.stations_searching), Modifier.edgeAware(this))
            }
            StationsUiState.PermissionDenied -> {
                item { StationMessage(stringResource(R.string.stations_permission), Modifier.edgeAware(this)) }
                item {
                    PrimaryPillButton(
                        text = stringResource(R.string.stations_allow),
                        onClick = { askPermission.launch(LOCATION_PERMISSIONS) },
                        modifier = Modifier.edgeAware(this),
                    )
                }
            }
            is StationsUiState.Done -> when (val search = current.search) {
                is StationSearch.Found -> {
                    search.stations.forEachIndexed { index, station ->
                        item {
                            val label = station.title()
                            // Las que salen en el mapa llevan su mismo número.
                            val number = (index + 1).takeIf { index < MapMath.MAP_STATIONS }
                            StationCard(
                                station = station,
                                label = number?.let { "$it. $label" } ?: label,
                                onClick = {
                                    val opened = mapsAvailable && GasStationMaps.openPlace(context, station.location, label)
                                    if (!opened) {
                                        // Sin app de mapas: se marca en el mapa propio, que está arriba.
                                        selected = index
                                        scope.launch { listState.animateScrollToItem(MAP_ITEM_INDEX) }
                                    }
                                },
                                modifier = Modifier.edgeAware(this),
                            )
                        }
                    }
                    item {
                        Text(
                            text = stringResource(if (mapsAvailable) R.string.stations_source else R.string.stations_source_no_maps),
                            color = TanqueoColors.Secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.edgeAware(this).fillMaxWidth(),
                        )
                    }
                }
                StationSearch.NoneNearby -> item {
                    StationMessage(
                        stringResource(R.string.stations_none, GasStationRanking.RADIUS_METERS / 1000),
                        Modifier.edgeAware(this),
                    )
                }
                StationSearch.NoLocation -> retryItems(R.string.stations_no_location, viewModel::search)
                StationSearch.Offline -> retryItems(R.string.stations_offline, viewModel::search)
            }
        }
    }
}

private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope.retryItems(message: Int, onRetry: () -> Unit) {
    item { StationMessage(stringResource(message), Modifier.edgeAware(this)) }
    item {
        PrimaryPillButton(
            text = stringResource(R.string.stations_retry),
            onClick = onRetry,
            modifier = Modifier.edgeAware(this),
        )
    }
}

/** Nombre para mostrar: el de la estación, si no la marca, y si no nada inventado. */
@Composable
private fun GasStation.title(): String = name ?: brand ?: stringResource(R.string.stations_unnamed)

@Composable
private fun StationCard(station: GasStation, label: String, onClick: () -> Unit, modifier: Modifier) {
    val screen = rememberScreenSize()
    GlassCard(modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = TanqueoColors.White,
                fontSize = screen.sp(13f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    stringResource(R.string.stations_distance, Formats.distance(station.distanceKm)),
                    // Sin nombre, la marca ya es el título: no se repite.
                    station.brand?.takeIf { station.name != null && it != station.name },
                ).joinToString(" · "),
                color = TanqueoColors.Accent,
                fontSize = screen.sp(11f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            station.street?.let { street ->
                Text(
                    text = street,
                    color = TanqueoColors.Secondary,
                    fontSize = screen.sp(10f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StationMessage(text: String, modifier: Modifier) {
    val screen = rememberScreenSize()
    GlassCard(modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TanqueoColors.White,
            fontSize = screen.sp(12f),
            lineHeight = screen.sp(15f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
