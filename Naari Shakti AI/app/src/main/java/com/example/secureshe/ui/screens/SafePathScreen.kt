package com.example.secureshe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.secureshe.ui.viewmodels.SafePathViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.google.android.gms.location.LocationServices
// Removed unused ActivityResult contracts and Activity imports
// Removed Google Places; using OSM search instead
// LaunchedEffect already available from runtime.* import
import android.Manifest
// Removed unused Log import
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.maps.CameraUpdateFactory
import android.annotation.SuppressLint
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.secureshe.R
import com.example.secureshe.ui.components.LocalDrawerController

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun SafePathScreen(navController: NavController, vm: SafePathViewModel = hiltViewModel()) {
    var destText by remember { mutableStateOf("") }
    var destLabel by remember { mutableStateOf("") }
    val safest by vm.safest.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // UI toggles need to be accessible across the whole screen scope
    var showHeatmap by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    // Permissions
    val fineLocationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Request permission on first launch if not granted
    LaunchedEffect(Unit) {
        if (fineLocationPermission.status is PermissionStatus.Denied) {
            fineLocationPermission.launchPermissionRequest()
        }
    }

    // Get last known location as origin when screen opens (only if granted)
    LaunchedEffect(fineLocationPermission.status) {
        if (fineLocationPermission.status is PermissionStatus.Granted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    vm.setOrigin(LatLng(loc.latitude, loc.longitude))
                }
            }
        }
    }

    val headerColor = colorResource(id = R.color.header_color)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) {
                        listOf(Color(0xFF1A2A3D), Color(0xFF2A3D57))
                    } else {
                        listOf(Color(0xFFF8D7E8), Color(0xFFE3C7FF))
                    }
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(headerColor)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val drawer = LocalDrawerController.current
            IconButton(onClick = { drawer.open() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.safe_path_navigation),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            // Info icon to the left of logo
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Filled.Info, contentDescription = "Safety info", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        val controlScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEF1FF))
                .padding(12.dp)
                .heightIn(max = 300.dp)
                .verticalScroll(controlScroll)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = destLabel.ifBlank { destText },
                onValueChange = { destText = it; destLabel = "" },
                label = { Text("Destination (type lat,lng or use Search)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color(0xFF1A1A1A),
                    cursorColor = Color(0xFF1A1A1A),
                    focusedLabelColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF475569),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(12.dp))

            // Local OSM search state
            val suggestions by vm.searchResults.collectAsState()
            val searchLoading by vm.searchLoading.collectAsState()
            val searchError by vm.searchError.collectAsState()
            var autoRouteFromSearch by remember { mutableStateOf(false) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val normalized = destText.replace("\u00A0", " ").trim()
                    val parts = if (normalized.contains(",")) normalized.split(",") else normalized.split(" ")
                    if (parts.size == 2) {
                        val lat = parts[0].trim().toDoubleOrNull()
                        val lng = parts[1].trim().toDoubleOrNull()
                        if (lat != null && lng != null) {
                            if (vm.origin.value == null) {
                                vm.setOrigin(LatLng(lat - 0.005, lng - 0.005))
                            }
                            vm.setDestination(LatLng(lat, lng))
                            destLabel = "${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
                            vm.computeSafestRoute()
                        } else {
                            vm.error.value = "Invalid coordinates. Use format: lat,lng"
                        }
                    } else {
                        vm.error.value = "Enter coordinates as lat,lng"
                    }
                }, modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor),
                    border = BorderStroke(1.dp, headerColor)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use Coordinates", color = headerColor)
                }
                // Move Show Heatmap next to Use Coordinates, linked to same toggle
                OutlinedButton(
                    onClick = { showHeatmap = !showHeatmap },
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor),
                    border = BorderStroke(1.dp, headerColor)
                ) {
                    Text(if (showHeatmap) "Hide Heatmap" else "Show Heatmap", color = headerColor)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    autoRouteFromSearch = true
                    // try Google geocode first; fallback to OSM
                    val query = destText.ifBlank { destLabel }
                    if (query.isNotBlank()) {
                        scope.launch {
                            val gg = vm.geocodeGoogle(query)
                            if (gg.isNotEmpty()) {
                                val s = gg.first()
                                destLabel = s.label
                                vm.setDestination(s.location)
                                if (vm.origin.value == null) {
                                    vm.setOrigin(LatLng(s.location.latitude - 0.005, s.location.longitude - 0.005))
                                }
                                vm.computeSafestRoute()
                                autoRouteFromSearch = false
                            } else {
                                vm.searchPlacesOSM(query)
                            }
                        }
                    } else {
                        vm.searchPlacesOSM(query)
                    }
                }, modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = headerColor, contentColor = Color.White)
                ) { Text("Search Destination") }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Show OSM search suggestions
            if (searchLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            searchError?.let { msg ->
                AssistChip(onClick = { vm.searchError.value = null }, label = { Text(msg) })
                Spacer(Modifier.height(8.dp))
            }
            if (suggestions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        suggestions.forEach { s ->
                            TextButton(onClick = {
                                destLabel = s.label
                                vm.setDestination(s.location)
                                if (vm.origin.value == null) {
                                    vm.setOrigin(LatLng(s.location.latitude - 0.005, s.location.longitude - 0.005))
                                }
                                vm.searchResults.value = emptyList()
                                vm.computeSafestRoute()
                            }) {
                                Text(s.label)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Auto-pick the first result when user pressed Search Destination
            LaunchedEffect(suggestions, autoRouteFromSearch, searchLoading) {
                if (autoRouteFromSearch && !searchLoading) {
                    if (suggestions.isNotEmpty()) {
                        val s = suggestions.first()
                        destLabel = s.label
                        vm.setDestination(s.location)
                        if (vm.origin.value == null) {
                            vm.setOrigin(LatLng(s.location.latitude - 0.005, s.location.longitude - 0.005))
                        }
                        vm.searchResults.value = emptyList()
                        vm.computeSafestRoute()
                    } else if (vm.searchError.value == null) {
                        vm.searchError.value = "No results"
                    }
                    autoRouteFromSearch = false
                }
            }

            // Removed separate heatmap chip (moved next to Use Coordinates)
            Spacer(Modifier.height(8.dp))
        }

        val origin = vm.origin.collectAsState().value
        val camPosState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(origin ?: LatLng(17.3850, 78.4867), 13f)
        }

        // Map takes the rest of the screen below the scrolled controls
        Box(Modifier.fillMaxWidth().weight(1f, fill = true)) {
            val locGranted = fineLocationPermission.status is PermissionStatus.Granted
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camPosState,
                properties = MapProperties(isMyLocationEnabled = locGranted),
                uiSettings = MapUiSettings(myLocationButtonEnabled = locGranted, zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    // Single-tap to set destination and compute route
                    destLabel = "${"%.5f".format(latLng.latitude)}, ${"%.5f".format(latLng.longitude)}"
                    vm.setDestination(latLng)
                    if (vm.origin.value == null) {
                        // If origin not yet set, approximate one near destination to avoid error
                        vm.setOrigin(LatLng(latLng.latitude - 0.005, latLng.longitude - 0.005))
                    }
                    vm.computeSafestRoute()
                }
            ) {
                origin?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "Start",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
                vm.destination.collectAsState().value?.let {
                    Marker(
                        state = MarkerState(it),
                        title = "Destination",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
                vm.routes.collectAsState().value.forEach { route ->
                    Polyline(points = route.points, color = androidx.compose.ui.graphics.Color.Gray, width = 8f)
                }
                safest?.let { best ->
                    Polyline(points = best.points, color = androidx.compose.ui.graphics.Color(0xFF00E676), width = 12f)
                }
                // Heatmap overlay from risk tiles as translucent grid
                if (showHeatmap) {
                    val tiles = vm.tiles.collectAsState().value
                    tiles.forEach { t ->
                        val rect = listOf(
                            LatLng(t.latMin, t.lngMin),
                            LatLng(t.latMin, t.lngMax),
                            LatLng(t.latMax, t.lngMax),
                            LatLng(t.latMax, t.lngMin),
                        )
                        // Map risk [0..1] to red alpha
                        val alpha = (t.risk.coerceIn(0.0, 1.0) * 0.35f).toFloat()
                        val color = androidx.compose.ui.graphics.Color(1f, 0f, 0f, alpha)
                        Polygon(points = rect, fillColor = color, strokeColor = color, strokeWidth = 0f)
                    }
                }
            }

            // Adjust camera when safest route updates
            LaunchedEffect(safest) {
                val best = safest ?: return@LaunchedEffect
                if (best.points.size >= 2) {
                    var minLat = Double.MAX_VALUE
                    var minLng = Double.MAX_VALUE
                    var maxLat = -Double.MAX_VALUE
                    var maxLng = -Double.MAX_VALUE
                    best.points.forEach { p ->
                        if (p.latitude < minLat) minLat = p.latitude
                        if (p.latitude > maxLat) maxLat = p.latitude
                        if (p.longitude < minLng) minLng = p.longitude
                        if (p.longitude > maxLng) maxLng = p.longitude
                    }
                    val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
                    camPosState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 72))
                } else if (best.points.isNotEmpty()) {
                    camPosState.animate(CameraUpdateFactory.newLatLngZoom(best.points.first(), 14f))
                }
            }

            // Loading and error status overlays
            val loading by vm.loading.collectAsState()
            val error by vm.error.collectAsState()
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                )
            }
            error?.let { msg ->
                AssistChip(
                    onClick = { vm.error.value = null },
                    label = { Text(msg, color = Color.Black) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFEFF3FF),
                        labelColor = Color.Black
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }

            if (showInfo) {
                AlertDialog(
                    onDismissRequest = { showInfo = false },
                    title = { Text("About Safe Path", color = Color(0xFF1A1A1A)) },
                    text = { Text(
                        "Routes balance time, distance, and estimated risk.\n" +
                        "Risk is estimated from recent incidents by area and decays over time.\n\n" +
                        "Steps:\n" +
                        "1) Enter your destination in the box\n" +
                        "2) Click on Search Destination\n" +
                        "3) A list of similar places is shown — select the required location and wait\n" +
                        "4) Do not press the Search Destination button continuously\n" +
                        "5) The possible safe and fast route will be displayed.\n\n" +
                        "Note:\n" +
                        "1) This feature is still in beta version, so route calculation may take time\n" +
                        "2) Keep the location search to local places\n" +
                        "3) This is a guidance tool and not a guarantee of safety.",
                        color = Color(0xFF1A1A1A)
                    ) },
                    confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
                    containerColor = Color(0xFFF4F6FF),
                    tonalElevation = 0.dp
                )
            }

            // Bottom-left status: only off-route alert (removed ETA/Δdist/risk chip per request)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val offRoute by vm.offRoute.collectAsState()
                if (offRoute) {
                    AssistChip(
                        onClick = { vm.computeSafestRoute() },
                        label = { Text("Off route – Recalculate", color = Color.Black) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFEFF3FF),
                            labelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Periodic location polling for off-route detection (simple MVP)
        LaunchedEffect(safest, fineLocationPermission.status) {
            if (safest == null) return@LaunchedEffect
            if (fineLocationPermission.status !is PermissionStatus.Granted) return@LaunchedEffect
            val client = LocationServices.getFusedLocationProviderClient(context)
            while (true) {
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        vm.setCurrentLocation(LatLng(loc.latitude, loc.longitude))
                        vm.evaluateDeviation()
                    }
                }
                kotlinx.coroutines.delay(5000)
            }
        }

        // Footer band
        Text(
            text = "\"Safety That Moves With You\"",
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(vertical = 48.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        // White strip under footer to avoid collision with system back/home
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.White)
        )
    }
}
