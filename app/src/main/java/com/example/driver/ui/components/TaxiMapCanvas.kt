package com.example.driver.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver.model.GeoPoint

/**
 * Mapa interactivo vectorial desarrollado con Compose Canvas.
 * Permite visualizar el trazado de rutas, ubicaciones de origen/destino,
 * icono animado de taxi en movimiento, controles de zoom y toque para seleccionar ubicación.
 */
@Composable
fun TaxiMapCanvas(
    pickupLocation: GeoPoint,
    destinationLocation: GeoPoint,
    taxiLocation: GeoPoint?,
    tripProgressRatio: Float = 0f,
    etaMinutes: Int? = null,
    distanceKm: Double? = null,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Animación de pulso para marcadores
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = tripProgressRatio.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F4F7))
                .pointerInput(onMapClick) {
                    detectTapGestures { offset ->
                        if (onMapClick != null) {
                            // Convert canvas offset back to geographic point simulation
                            val width = size.width
                            val height = size.height
                            val normX = offset.x / width
                            val normY = offset.y / height
                            val lat = 4.72 - normY * 0.15
                            val lng = -74.15 + normX * 0.15
                            val addressName = "Punto seleccionado (${(normX * 100).toInt()}, ${(normY * 100).toInt()})"
                            onMapClick(GeoPoint(lat, lng, addressName))
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Dibujar plano urbano / Cuadrícula de calles
            drawCityMapBackground(canvasWidth, canvasHeight, zoomScale)

            // 2. Mapear coordenadas a píxeles
            val pickupOffset = geoToOffset(pickupLocation, canvasWidth, canvasHeight, zoomScale)
            val destOffset = geoToOffset(destinationLocation, canvasWidth, canvasHeight, zoomScale)

            // 3. Trazar ruta entre Origen y Destino
            drawRoutePath(pickupOffset, destOffset)

            // 4. Dibujar marcadores Origen y Destino
            drawLocationMarker(pickupOffset, isPickup = true, pulseRadius = pulseRadius)
            drawLocationMarker(destOffset, isPickup = false, pulseRadius = pulseRadius)

            // 5. Dibujar icono de Taxi en movimiento
            val currentVehicleOffset = if (taxiLocation != null) {
                geoToOffset(taxiLocation, canvasWidth, canvasHeight, zoomScale)
            } else {
                // Interpolar entre pickup y destination según el progreso
                Offset(
                    x = pickupOffset.x + (destOffset.x - pickupOffset.x) * animatedProgress,
                    y = pickupOffset.y + (destOffset.y - pickupOffset.y) * animatedProgress
                )
            }
            drawTaxiVehicleMarker(currentVehicleOffset)
        }

        // Overlay con Controles de Zoom y Leyenda
        MapControlsOverlay(
            zoomScale = zoomScale,
            onZoomIn = { if (zoomScale < 2.0f) zoomScale += 0.2f },
            onZoomOut = { if (zoomScale > 0.6f) zoomScale -= 0.2f }
        )

        // Banner de ETA y Distancia
        if (etaMinutes != null && distanceKm != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
                        text = "• ${String.format("%.1f", distanceKm)} km",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCityMapBackground(width: Float, height: Float, zoom: Float) {
    val blockColor = Color(0xFFE2E8F0)
    val mainRoadColor = Color.White
    val riverColor = Color(0xFFBAE6FD)

    // Río / Parque
    val riverPath = Path().apply {
        moveTo(width * 0.1f, 0f)
        cubicTo(width * 0.2f, height * 0.3f, width * 0.05f, height * 0.7f, width * 0.15f, height)
        lineTo(width * 0.25f, height)
        cubicTo(width * 0.15f, height * 0.7f, width * 0.3f, height * 0.3f, width * 0.2f, 0f)
        close()
    }
    drawPath(riverPath, color = riverColor)

    // Manzanas urbanas
    val cols = 8
    val rows = 12
    val stepX = width / cols
    val stepY = height / rows

    for (c in 0 until cols) {
        for (r in 0 until rows) {
            if ((c + r) % 3 != 0) {
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(c * stepX + 6f, r * stepY + 6f),
                    size = Size(stepX - 12f, stepY - 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }
    }

    // Avenidas principales
    val roadStroke = 18f * zoom
    drawLine(Color.White, Offset(0f, height * 0.35f), Offset(width, height * 0.35f), strokeWidth = roadStroke)
    drawLine(Color.White, Offset(0f, height * 0.65f), Offset(width, height * 0.65f), strokeWidth = roadStroke)
    drawLine(Color.White, Offset(width * 0.5f, 0f), Offset(width * 0.5f, height), strokeWidth = roadStroke)
    drawLine(Color.White, Offset(width * 0.8f, 0f), Offset(width * 0.8f, height), strokeWidth = roadStroke)

    // Líneas amarillas centrales
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
    drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(0f, height * 0.35f),
        end = Offset(width, height * 0.35f),
        strokeWidth = 3f,
        pathEffect = dashEffect
    )
    drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(width * 0.5f, 0f),
        end = Offset(width * 0.5f, height),
        strokeWidth = 3f,
        pathEffect = dashEffect
    )
}

private fun geoToOffset(geo: GeoPoint, width: Float, height: Float, zoom: Float): Offset {
    val minLat = 4.57
    val maxLat = 4.72
    val minLng = -74.15
    val maxLng = -74.00

    val normX = ((geo.longitude - minLng) / (maxLng - minLng)).coerceIn(0.1, 0.9).toFloat()
    val normY = (1.0 - (geo.latitude - minLat) / (maxLat - minLat)).coerceIn(0.1, 0.9).toFloat()

    val centerX = width / 2f
    val centerY = height / 2f

    val rawX = normX * width
    val rawY = normY * height

    val scaledX = centerX + (rawX - centerX) * zoom
    val scaledY = centerY + (rawY - centerY) * zoom

    return Offset(scaledX, scaledY)
}

private fun DrawScope.drawRoutePath(start: Offset, end: Offset) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        // Crear curva realista de avenida
        cubicTo(midX, start.y, midX, end.y, end.x, end.y)
    }

    // Sombreo de la ruta
    drawPath(
        path = path,
        color = Color(0x400284C7),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )

    // Ruta principal azul brillante
    drawPath(
        path = path,
        color = Color(0xFF0284C7),
        style = Stroke(
            width = 8f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), 0f)
        )
    )
}

private fun DrawScope.drawLocationMarker(center: Offset, isPickup: Boolean, pulseRadius: Float) {
    val baseColor = if (isPickup) Color(0xFF16A34A) else Color(0xFFDC2626)

    // Halo animado
    drawCircle(
        color = baseColor.copy(alpha = 0.25f),
        radius = pulseRadius,
        center = center
    )

    // Sombra del pin
    drawCircle(
        color = Color(0x40000000),
        radius = 12f,
        center = center + Offset(0f, 6f)
    )

    // Círculo base del pin
    drawCircle(
        color = baseColor,
        radius = 12f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = center
    )
}

private fun DrawScope.drawTaxiVehicleMarker(center: Offset) {
    // Sombra del taxi
    drawCircle(
        color = Color(0x50000000),
        radius = 18f,
        center = center + Offset(0f, 6f)
    )

    // Carrocería del taxi (Color amarillo taxi)
    drawRoundRect(
        color = Color(0xFFEAB308),
        topLeft = center - Offset(16f, 14f),
        size = Size(32f, 28f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    // Techo negro / Luces de taxi
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = center - Offset(10f, 8f),
        size = Size(20f, 16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )

    // Techo central blanco (Letrero TAXI)
    drawRoundRect(
        color = Color.White,
        topLeft = center - Offset(5f, 4f),
        size = Size(10f, 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
    )
}

@Composable
private fun MapControlsOverlay(
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                    IconButton(onClick = onZoomIn) {
                        Text(text = "+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Spacer(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(Color.LightGray)
                    )
                    IconButton(onClick = onZoomOut) {
                        Text(text = "-", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
