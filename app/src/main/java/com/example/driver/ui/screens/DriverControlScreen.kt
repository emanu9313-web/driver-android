package com.example.driver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver.domain.model.Ride
import com.example.driver.domain.model.RidePricing
import com.example.driver.domain.model.RideStatus
import com.example.driver.ui.components.TripStatusBadge
import com.example.driver.ui.viewmodel.DriverViewModel

@Composable
fun DriverControlScreen(
    viewModel: DriverViewModel,
    modifier: Modifier = Modifier
) {
    val driverProfile by viewModel.driverProfile.collectAsState()
    val activeRide by viewModel.activeRide.collectAsState()
    val pendingRides by viewModel.pendingRides.collectAsState()
    val driverHistory by viewModel.driverHistory.collectAsState()

    val isOnline = driverProfile?.isOnline ?: false
    val completedCount = driverHistory.count { it.status == RideStatus.COMPLETED }
    val totalEarnings = driverHistory.filter { it.status == RideStatus.COMPLETED }.sumOf { it.estimatedFare }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Banner de Control
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Central de Control Conductor",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Conductor: ${driverProfile?.name ?: "---"} (${driverProfile?.vehiclePlate ?: "..."})",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.toggleOnlineStatus(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF065F46)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = completedCount.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFFFACC15))
                        Text(text = "Servicios hoy", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$${RidePricing.formatCop(totalEarnings)} COP",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF10B981),
                            maxLines = 1
                        )
                        Text(text = "Ganancias estimadas", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (activeRide != null) {
            Text(
                text = "Viaje Asignado en Curso",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(10.dp))
            DriverRideCard(
                ride = activeRide!!,
                onAccept = { },
                onAdvanceState = { viewModel.advanceRideStatus(activeRide!!) }
            )
        } else {
            Text(
                text = "Solicitudes Entrantes Disponibles",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (pendingRides.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🚕", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isOnline) "Esperando solicitudes de pasajeros..." else "Ponte en línea para recibir servicios",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = pendingRides, key = { ride -> ride.id }) { ride ->
                        DriverRideCard(
                            ride = ride,
                            onAccept = { viewModel.acceptRide(ride.id) },
                            onAdvanceState = { viewModel.advanceRideStatus(ride) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverRideCard(
    ride: Ride,
    onAccept: () -> Unit,
    onAdvanceState: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ride.category.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "$${RidePricing.formatCop(ride.estimatedFare)} COP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF10B981),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TripStatusBadge(status = ride.status)

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "🟢 Origen: ${ride.pickupLocation.addressName}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "🔴 Destino: ${ride.destinationLocation.addressName}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(14.dp))

            when (ride.status) {
                RideStatus.SEARCHING -> {
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "ACEPTAR SERVICIO", fontWeight = FontWeight.Bold)
                    }
                }
                RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_EN_ROUTE, RideStatus.ARRIVED, RideStatus.IN_PROGRESS -> {
                    Button(
                        onClick = onAdvanceState,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = when (ride.status) {
                            RideStatus.DRIVER_ASSIGNED -> "INICIAR RUTA AL CLIENTE"
                            RideStatus.DRIVER_EN_ROUTE -> "LLEGUÉ AL ORIGEN"
                            RideStatus.ARRIVED -> "INICIAR VIAJE"
                            RideStatus.IN_PROGRESS -> "FINALIZAR VIAJE"
                            else -> "AVANZAR ESTADO"
                        }
                        Text(text = label, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
    }
}
