package com.example.driver.domain.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

data class RideEstimate(
    val distanceKm: Double,
    val fareCop: Double,
    val etaSeconds: Long
)

object RidePricing {
    fun estimate(
        pickup: GeoLocation,
        destination: GeoLocation,
        category: ServiceCategory
    ): RideEstimate {
        val distanceKm = calculateDistanceKm(pickup, destination)
        val (baseFare, ratePerKm) = when (category) {
            ServiceCategory.STANDARD -> 3500.0 to 1200.0
            ServiceCategory.COMFORT -> 5500.0 to 1600.0
            ServiceCategory.VIP -> 8500.0 to 2200.0
        }
        val fare = round((baseFare + distanceKm * ratePerKm) / 100.0) * 100.0
        val etaSeconds = max(180L, round(distanceKm / 25.0 * 3600.0).toLong())

        return RideEstimate(distanceKm, fare, etaSeconds)
    }

    fun calculateDistanceKm(pickup: GeoLocation, destination: GeoLocation): Double {
        val earthRadiusKm = 6371.0
        val lat1 = Math.toRadians(pickup.latitude)
        val lat2 = Math.toRadians(destination.latitude)
        val deltaLat = Math.toRadians(destination.latitude - pickup.latitude)
        val deltaLng = Math.toRadians(destination.longitude - pickup.longitude)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val distance = earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
        return max(1.0, round(distance * 10.0) / 10.0)
    }

    fun formatCop(amount: Double): String =
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(amount)
}
