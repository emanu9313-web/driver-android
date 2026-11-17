package com.example.driver.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Fuente de baldosas profesionales públicas ArcGIS Esri World Street Map
val ESRI_WORLD_STREET_TILE_SOURCE = object : OnlineTileSourceBase(
    "EsriWorldStreetMap",
    0, 19, 256, ".jpg",
    arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/",
        "https://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
    }
}

@Composable
fun RealOsmMapView(
    pickupLocation: GeoLocation,
    destinationLocation: GeoLocation,
    taxiLocation: GeoLocation?,
    nearbyDrivers: List<DriverProfile> = emptyList(),
    tripProgressRatio: Float = 0f,
    etaMinutes: Int? = null,
    distanceKm: Double? = null,
    onMapClick: ((GeoLocation) -> Unit)? = null,
    onMyLocationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val animatedProgress by animateFloatAsState(
        targetValue = tripProgressRatio.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress"
    )

    val mapView = remember {
        val prefs = context.getSharedPreferences("osmdroid_map_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        MapView(context).apply {
            setTileSource(ESRI_WORLD_STREET_TILE_SOURCE)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(OsmGeoPoint(pickupLocation.latitude, pickupLocation.longitude))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    LaunchedEffect(pickupLocation, destinationLocation, taxiLocation, nearbyDrivers, animatedProgress) {
        mapView.overlays.clear()

        val pickupOsm = OsmGeoPoint(pickupLocation.latitude, pickupLocation.longitude)
        val destOsm = OsmGeoPoint(destinationLocation.latitude, destinationLocation.longitude)

        // 1. Dibujar Taxis Cercanos Disponibles
        nearbyDrivers.forEach { driver ->
            val driverOsm = OsmGeoPoint(driver.location.latitude, driver.location.longitude)
            val driverMarker = Marker(mapView).apply {
                position = driverOsm
                title = "Taxi: ${driver.name} (${driver.vehiclePlate})"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createCustomCircleDrawable(AndroidColor.parseColor("#F59E0B"))
            }
            mapView.overlays.add(driverMarker)
        }

        // 2. Marcador de Origen (Verde)
        val pickupMarker = Marker(mapView).apply {
            position = pickupOsm
            title = "Origen: ${pickupLocation.addressName}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createCustomCircleDrawable(AndroidColor.parseColor("#16A34A"))
        }
        mapView.overlays.add(pickupMarker)

        // 3. Marcador de Destino (Rojo)
        val destMarker = Marker(mapView).apply {
            position = destOsm
            title = "Destino: ${destinationLocation.addressName}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createCustomCircleDrawable(AndroidColor.parseColor("#DC2626"))
        }
        mapView.overlays.add(destMarker)

        // 4. Trazar Ruta Polyline Azul sobre las Calles Reales
        val polyline = Polyline(mapView).apply {
            addPoint(pickupOsm)
            val midLat = (pickupLocation.latitude + destinationLocation.latitude) / 2.0
            val midLng = (pickupLocation.longitude + destinationLocation.longitude) / 2.0
            addPoint(OsmGeoPoint(midLat, midLng))
            addPoint(destOsm)
            outlinePaint.color = AndroidColor.parseColor("#0284C7")
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        mapView.overlays.add(polyline)

        // 5. Marcador del Taxi en Movimiento Asignado
        if (taxiLocation != null) {
            val taxiOsm = OsmGeoPoint(taxiLocation.latitude, taxiLocation.longitude)
            val taxiMarker = Marker(mapView).apply {
                position = taxiOsm
                title = "Taxi Asignado"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createCustomCircleDrawable(AndroidColor.parseColor("#EAB308"))
            }
            mapView.overlays.add(taxiMarker)
        }

        // 6. Captura de toque en el mapa
        if (onMapClick != null) {
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint?): Boolean {
                    if (p != null) {
                        onMapClick(GeoLocation(p.latitude, p.longitude, "Ubicación seleccionada"))
                    }
                    return true
                }
                override fun longPressHelper(p: OsmGeoPoint?): Boolean = false
            })
            mapView.overlays.add(eventsOverlay)
        }

        mapView.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Botones de zoom y GPS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onMyLocationClick != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    IconButton(onClick = onMyLocationClick) {
                        Text(text = "🎯", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column {
                    IconButton(onClick = { mapView.controller.zoomIn() }) {
                        Text(text = "+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Spacer(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(Color.LightGray)
                    )
                    IconButton(onClick = { mapView.controller.zoomOut() }) {
                        Text(text = "-", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }
            }
        }

        // Overlay superior de tiempo y distancia
        if (etaMinutes != null && distanceKm != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Llegada estimada: ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$etaMinutes min",
                        color = Color(0xFFFACC15),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "• ${String.format(java.util.Locale.US, "%.1f", distanceKm)} km",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun createCustomCircleDrawable(colorInt: Int): android.graphics.drawable.Drawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorInt)
        setStroke(4, AndroidColor.WHITE)
        setSize(65, 65)
    }
}
