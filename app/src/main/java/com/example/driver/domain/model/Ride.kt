package com.example.driver.domain.model

enum class RideStatus(val label: String) {
    SEARCHING("Buscando conductor cercano..."),
    DRIVER_ASSIGNED("Conductor asignado"),
    DRIVER_EN_ROUTE("Conductor en camino"),
    ARRIVED("El conductor ha llegado al origen"),
    IN_PROGRESS("Viaje en curso"),
    COMPLETED("Viaje completado"),
    CANCELLED("Servicio cancelado")
}

enum class ServiceCategory(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    STANDARD("Taxi Estándar", "Económico y accesible", "local_taxi"),
    COMFORT("Taxi Confort", "Amplio con aire acondicionado", "directions_car"),
    VIP("Taxi Executive", "Vehículos premium y conductores top", "time_to_leave")
}

data class Ride(
    val id: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehiclePlate: String? = null,
    val vehicleModel: String? = null,
    val pickupLocation: GeoLocation = GeoLocation(),
    val destinationLocation: GeoLocation = GeoLocation(),
    val driverLocation: GeoLocation? = null,
    val category: ServiceCategory = ServiceCategory.STANDARD,
    val status: RideStatus = RideStatus.SEARCHING,
    val estimatedFare: Double = 0.0,
    val estimatedDistanceKm: Double = 0.0,
    val estimatedEtaSeconds: Long = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val cancellationReason: String? = null,
    val isRated: Boolean = false
)
