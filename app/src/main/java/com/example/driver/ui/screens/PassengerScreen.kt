package com.example.driver.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.model.RideStatus
import com.example.driver.domain.model.RidePricing
import com.example.driver.domain.model.ServiceCategory
import com.example.driver.ui.components.DriverDetailsCard
import com.example.driver.ui.components.RealOsmMapView
import com.example.driver.ui.components.ServiceTypeCard
import com.example.driver.ui.viewmodel.PassengerViewModel

@Composable
fun PassengerScreen(
    viewModel: PassengerViewModel,
    modifier: Modifier = Modifier
) {
    val pickup by viewModel.pickupLocation.collectAsState()
    val destination by viewModel.destinationLocation.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activeRide by viewModel.activeRide.collectAsState()
    val nearbyDrivers by viewModel.nearbyDrivers.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val rideRequestState by viewModel.rideRequestState.collectAsState()

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDestDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingScore by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // 1. Mapa Real OpenStreetMap (OsmDroid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
        ) {
            RealOsmMapView(
                pickupLocation = activeRide?.pickupLocation ?: pickup,
                destinationLocation = activeRide?.destinationLocation ?: destination,
                taxiLocation = activeRide?.driverLocation,
                nearbyDrivers = nearbyDrivers,
                etaMinutes = activeRide?.estimatedEtaSeconds?.let { (it / 60).toInt() },
                distanceKm = activeRide?.estimatedDistanceKm,
                onMapClick = { newPoint ->
                    if (activeRide == null) {
                        viewModel.setDestination(newPoint)
                    }
                },
                onMyLocationClick = {
                    viewModel.fetchCurrentGpsLocation()
                }
            )
        }

        // 2. Panel Inferior Moderno
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val currentRide = activeRide

                if (currentRide != null && currentRide.status != RideStatus.CANCELLED) {
                    if (currentRide.status == RideStatus.COMPLETED && !currentRide.isRated) {
                        showRatingDialog = true
                    }

                    // Seguimiento del servicio activo
                    Text(
                        text = if (currentRide.driverId == null) "Solicitud de viaje registrada" else "Seguimiento en Vivo del Servicio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentRide.status == RideStatus.DRIVER_EN_ROUTE || currentRide.status == RideStatus.IN_PROGRESS) {
                        LinearProgressIndicator(
                            color = Color(0xFFF59E0B),
                            trackColor = Color(0xFFFEF9C3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (currentRide.driverId == null && currentRide.status == RideStatus.SEARCHING) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Modo de simulación", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "La solicitud quedó guardada en Firestore. Aún no hay conductores conectados; el viaje sigue en búsqueda.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF78350F)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Distancia estimada: " + String.format("%.1f", currentRide.estimatedDistanceKm) + " km")
                                Text("Tarifa estimada: $${RidePricing.formatCop(currentRide.estimatedFare)} COP")
                                Text("Categoría: ${currentRide.category.displayName}")
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    DriverDetailsCard(
                        driver = DriverProfile(
                            uid = currentRide.driverId ?: "",
                            name = currentRide.driverName ?: "Buscando conductor...",
                            phone = currentRide.driverPhone ?: "---",
                            vehicleModel = currentRide.vehicleModel ?: "Asignando vehículo",
                            vehiclePlate = currentRide.vehiclePlate ?: "...",
                            location = currentRide.driverLocation ?: currentRide.pickupLocation
                        ),
                        rideStatus = currentRide.status,
                        onCancelTrip = { viewModel.cancelActiveRide() }
                    )
                } else {
                    // Formulario de Solicitud de Taxi
                    Text(
                        text = "¿A dónde vamos hoy?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Origen
                    LocationSelectorBox(
                        label = "Origen (GPS / Punto de partida)",
                        iconText = "🟢",
                        currentLocation = pickup,
                        onClick = { showPickupDropdown = true }
                    ) {
                        DropdownMenu(
                            expanded = showPickupDropdown,
                            onDismissRequest = { showPickupDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🎯 Mi ubicación GPS actual", fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) },
                                onClick = {
                                    viewModel.fetchCurrentGpsLocation()
                                    showPickupDropdown = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Búsqueda de Destino
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            viewModel.searchAddress(query)
                            showDestDropdown = true
                        },
                        placeholder = { Text("Buscar destino por dirección...") },
                        leadingIcon = { Text("🔴", fontSize = 16.sp) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showDestDropdown && searchResults.isNotEmpty()) {
                        DropdownMenu(
                            expanded = showDestDropdown,
                            onDismissRequest = { showDestDropdown = false }
                        ) {
                            searchResults.forEach { res ->
                                DropdownMenuItem(
                                    text = { Text("📍 ${res.addressName}") },
                                    onClick = {
                                        viewModel.setDestination(res)
                                        searchQuery = res.addressName
                                        showDestDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Elige la categoría de tu Taxi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ServiceCategory.entries.forEach { category ->
                        val estimate = viewModel.estimate(category)
                        ServiceTypeCard(
                            category = category,
                            estimatedFare = estimate.fareCop,
                            etaMinutes = (estimate.etaSeconds / 60).toInt(),
                            isSelected = selectedCategory == category,
                            onSelect = { viewModel.setCategory(category) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tarifa de simulación según distancia en línea recta; el recorrido real puede variar.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    rideRequestState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.requestRide() },
                        enabled = !rideRequestState.isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = if (rideRequestState.isSubmitting) "GUARDANDO SOLICITUD..." else "SOLICITAR TAXI AHORA",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Calificación al completar viaje
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("¡Viaje Finalizado!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Califica la atención del conductor:")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..5).forEach { star ->
                            Text(
                                text = if (star <= ratingScore) "⭐" else "☆",
                                fontSize = 28.sp,
                                modifier = Modifier.clickable { ratingScore = star }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        placeholder = { Text("Escribe un comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitRating(ratingScore, ratingComment)
                        showRatingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                ) {
                    Text("ENVIAR CALIFICACIÓN", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun LocationSelectorBox(
    label: String,
    iconText: String,
    currentLocation: GeoLocation,
    onClick: () -> Unit,
    dropdownMenu: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = iconText, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(
                        text = currentLocation.addressName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
            dropdownMenu()
        }
    }
}
