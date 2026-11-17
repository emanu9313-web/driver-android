package com.example.driver.domain.model

data class DriverProfile(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val isOnline: Boolean = false,
    val isApproved: Boolean = false,
    val isSuspended: Boolean = false,
    val ratingAverage: Double = 5.0,
    val ratingCount: Int = 0,
    val location: GeoLocation = GeoLocation()
)
