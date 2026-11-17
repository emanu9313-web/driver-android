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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
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
import com.example.driver.ui.components.TripStatusBadge
import com.example.driver.ui.viewmodel.PassengerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: PassengerViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.passengerHistory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de Servicios",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0F172A)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7)
            ) {
                Text(
                    text = "${history.size} registros",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFD97706)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📜", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aún no tienes servicios registrados",
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
                items(items = history, key = { ride -> ride.id }) { ride ->
                    HistoryItemCard(ride = ride)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(ride: Ride) {
    val dateFormat = rememberDateFormat(ride.createdAtMillis)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "$${RidePricing.formatCop(ride.estimatedFare)} COP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            TripStatusBadge(status = ride.status)

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "🟢 ${ride.pickupLocation.addressName}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "🔴 ${ride.destinationLocation.addressName}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)

            ride.driverName?.let { name ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Conductor: $name • ${ride.vehicleModel ?: ""} (${ride.vehiclePlate ?: ""})",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun rememberDateFormat(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
