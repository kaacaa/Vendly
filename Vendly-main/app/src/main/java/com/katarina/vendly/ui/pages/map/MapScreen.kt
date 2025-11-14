package com.katarina.vendly.ui.pages.map

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.elevatedCardColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap as GmsMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.katarina.vendly.domain.model.vm.VendingStatus
import com.katarina.vendly.ui.filters.FilterDialog
import com.katarina.vendly.ui.filters.FilterViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel()
) {
    LaunchedEffect(Unit) { mapViewModel.start() }

    val ui by mapViewModel.uiState.collectAsState()
    val userLatLng = ui.latitude?.let { lat ->
        ui.longitude?.let { lng -> LatLng(lat, lng) }
    }

    var mapLoaded by rememberSaveable { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val selectedVending = ui.selectedVending
    var followMe by rememberSaveable { mutableStateOf(true) }

    val filtersVm: FilterViewModel = viewModel()
    val fUi by filtersVm.ui.collectAsState()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showList by rememberSaveable { mutableStateOf(false) }

    var showRadiusDialog by rememberSaveable { mutableStateOf(false) }
    var radiusMeters by rememberSaveable { mutableIntStateOf(0) }

    // ✅ pass required parameter
    LaunchedEffect(Unit) { filtersVm.loadAll(updatedAfter = 0L) }

    LaunchedEffect(userLatLng, followMe) {
        if (userLatLng != null && followMe) {
            if (cameraPositionState.position.zoom == 0f) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
        }
    }

    fun distanceMeters(from: LatLng, to: LatLng): Int {
        val out = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, out)
        return out[0].roundToInt()
    }

    val baseList = if (fUi.filterActive) fUi.filtered else fUi.all

    val visibleList = remember(baseList, radiusMeters, userLatLng) {
        if (radiusMeters <= 0 || userLatLng == null) baseList
        else baseList.filter {
            distanceMeters(userLatLng, LatLng(it.latitude, it.longitude)) <= radiusMeters
        }
    }

    fun radiusText(m: Int): String =
        if (m <= 0) "Radius" else "≤ " + if (m >= 1000) "${m / 1000} km" else "$m m"

    Box(Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true
            ),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(),
            onMapLoaded = { mapLoaded = true }
        ) {
            MapEffect(Unit) { map ->
                map.setOnCameraMoveStartedListener { reason ->
                    if (reason == GmsMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        followMe = false
                    }
                }
            }

            userLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "You are here",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                if (radiusMeters > 0) {
                    Circle(
                        center = it,
                        radius = radiusMeters.toDouble(),
                        fillColor = Color(0x332196F3),
                        strokeColor = Color(0xFF2196F3),
                        strokeWidth = 2f
                    )
                }
            }

            visibleList.forEach { vending ->
                Marker(
                    state = MarkerState(LatLng(vending.latitude, vending.longitude)),
                    title = vending.name,
                    snippet = vending.productType,
                    onClick = {
                        mapViewModel.onVendingMarkerClicked(vending)
                        true
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { if (fUi.filterActive) filtersVm.clear() else showFilter = true }
            ) {
                Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(if (fUi.filterActive) "Clear" else "Filter")
            }

            FilledTonalButton(onClick = { showRadiusDialog = true }) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(radiusText(radiusMeters))
            }

            FilledTonalButton(onClick = { showList = !showList }) {
                Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(if (showList) "Hide" else "List")
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("addVending") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add vending machine")
        }

        if (showList) {
            ElevatedCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f),
                colors = elevatedCardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("Name",  modifier = Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Type",  modifier = Modifier.weight(0.25f), fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Status", modifier = Modifier.weight(0.25f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, color = Color.Black)
                }

                LazyColumn {
                    items(visibleList, key = { it.id }) { v ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("vendingDetails/${v.id}") { launchSingleTop = true }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(v.name, modifier = Modifier.weight(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Black)
                            Text(v.productType, modifier = Modifier.weight(0.25f), color = Color.Black)
                            Text(VendingStatus.fromCode(v.status).label, modifier = Modifier.weight(0.25f), textAlign = TextAlign.End, color = Color.Black)
                        }
                        Divider()
                    }
                }
            }
        }

        val detailsSheetState = rememberModalBottomSheetState()
        if (selectedVending != null) {
            ModalBottomSheet(
                onDismissRequest = { mapViewModel.onVendingMarkerClicked(null) },
                sheetState = detailsSheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = selectedVending.imageUrl,
                        contentDescription = selectedVending.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(selectedVending.name, style = MaterialTheme.typography.titleLarge)
                    Text(selectedVending.productType, color = Color.Gray)
                    Text("Status: ${VendingStatus.fromCode(selectedVending.status).label}")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("vendingDetails/${selectedVending.id}") { launchSingleTop = true }
                            mapViewModel.onVendingMarkerClicked(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Update status") }
                }
            }
        }

        if (showFilter) {
            FilterDialog(
                productType = fUi.productType,
                status = fUi.status,
                onProductTypeChange = filtersVm::setProductType,
                onStatusChange = filtersVm::setStatus,
                isFiltering = fUi.isFiltering,
                onClear = {
                    filtersVm.clear()
                    showFilter = false
                },
                onApply = {
                    // ✅ pass required parameter
                    filtersVm.refresh(updatedAfter = 0L)
                    showFilter = false
                },
                onDismiss = { showFilter = false }
            )
        }

        // ✅ Radius dialog — now actually rendered
        if (showRadiusDialog) {
            RadiusDialog(
                value = radiusMeters,
                onChange = { radiusMeters = it },
                onDismiss = { showRadiusDialog = false }
            )
        }

        if (userLatLng == null || !mapLoaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading map or location…")
                }
            }
        }
    }
}

/** Small helper for the radius picker */
@Composable
private fun RadiusDialog(
    value: Int,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Show only within radius") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (value <= 0) "All distances" else "≤ ${if (value >= 1000) "${value/1000} km" else "$value m"}")
                Slider(
                    value = value.coerceIn(0, 3000).toFloat(),
                    onValueChange = { onChange(it.toInt()) },
                    valueRange = 0f..3000f,
                    steps = 5 // stops at 0,500,1000,1500,2000,2500,3000
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 250, 500, 1000, 2000).forEach { preset ->
                        AssistChip(
                            onClick = { onChange(preset) },
                            label = { Text(if (preset == 0) "All" else if (preset >= 1000) "${preset/1000} km" else "${preset} m") }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = { onChange(0); onDismiss() }) { Text("Clear") } }
    )
}