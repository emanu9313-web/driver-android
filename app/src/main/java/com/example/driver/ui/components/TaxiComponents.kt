package com.example.driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.RidePricing
import com.example.driver.domain.model.RideStatus
import com.example.driver.domain.model.ServiceCategory

@Composable
fun ServiceTypeCard(
    category: ServiceCategory,
    estimatedFare: Double,
    etaMinutes: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
    val bgColor = if (isSelected) Color(0xFFFEFCE8) else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFDE047) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (category) {
                            ServiceCategory.STANDARD -> "🚖"
                            ServiceCategory.COMFORT -> "🚘"
                            ServiceCategory.VIP -> "🏎️"
                        },
                        fontSize = 22.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = category.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Duración aprox. $etaMinutes min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "$${RidePricing.formatCop(estimatedFare)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "COP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 2.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun DriverDetailsCard(
    driver: DriverProfile,
    rideStatus: RideStatus,
    onCancelTrip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            TripStatusBadge(status = rideStatus)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF08A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👨‍✈️", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = driver.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐ ${driver.ratingAverage}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "• ${driver.phone}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = driver.vehiclePlate,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = driver.vehicleModel,
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            maxLines = 1
                        )
                    }
                }
            }

            if (onCancelTrip != null && (rideStatus == RideStatus.SEARCHING || rideStatus == RideStatus.DRIVER_ASSIGNED || rideStatus == RideStatus.DRIVER_EN_ROUTE)) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCancelTrip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancelar servicio", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TripStatusBadge(status: RideStatus) {
    val (bgColor, textColor, icon) = when (status) {
        RideStatus.SEARCHING -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "🔍")
        RideStatus.DRIVER_ASSIGNED -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "✅")
        RideStatus.DRIVER_EN_ROUTE -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "🚘")
        RideStatus.ARRIVED -> Triple(Color(0xFFFEF08A), Color(0xFFB45309), "📍")
        RideStatus.IN_PROGRESS -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), "🏁")
        RideStatus.COMPLETED -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "🎉")
        RideStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "❌")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
