package com.example.driver.model

import org.json.JSONObject
import java.util.UUID

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val addressName: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("addressName", addressName)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GeoPoint {
            return GeoPoint(
                latitude = json.optDouble("latitude", 4.6097),
                longitude = json.optDouble("longitude", -74.0817),
                addressName = json.optString("addressName", "Ubicación actual")
            )
        }
    }
}

enum class ServiceType(
    val displayName: String,
    val baseFare: Double,
    val ratePerKm: Double,
    val description: String
) {
    STANDARD("Taxi Estándar", 3500.0, 1200.0, "Servicio cómodo y accesible"),
    COMFORT("Taxi Confort", 5500.0, 1600.0, "Vehículo amplio con aire acondicionado"),
    VIP("Taxi Executive", 8500.0, 2200.0, "Servicio premium con conductores top")
}

enum class TripStatus(val label: String) {
    SEARCHING("Buscando taxi cercano..."),
    DRIVER_ASSIGNED("Conductor asignado"),
    DRIVER_EN_ROUTE("Conductor en camino al origen"),
    IN_PROGRESS("Viaje en curso al destino"),
    COMPLETED("Viaje completado"),
    CANCELLED("Servicio cancelado")
}

data class Driver(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val vehicleModel: String,
    val licensePlate: String,
    val rating: Double,
    val phone: String,
    val currentLocation: GeoPoint,
    val isOnline: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("vehicleModel", vehicleModel)
            put("licensePlate", licensePlate)
            put("rating", rating)
            put("phone", phone)
            put("currentLocation", currentLocation.toJsonObject())
            put("isOnline", isOnline)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Driver {
            return Driver(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Carlos Mendoza"),
                vehicleModel = json.optString("vehicleModel", "Kia Rio Yellow"),
                licensePlate = json.optString("licensePlate", "TXI-492"),
                rating = json.optDouble("rating", 4.9),
                phone = json.optString("phone", "+57 310 4829102"),
                currentLocation = GeoPoint.fromJsonObject(json.optJSONObject("currentLocation") ?: JSONObject()),
                isOnline = json.optBoolean("isOnline", true)
            )
        }
    }
}

data class TaxiRequest(
    val id: String = UUID.randomUUID().toString(),
    val passengerName: String,
    val passengerPhone: String,
    val pickupLocation: GeoPoint,
    val destinationLocation: GeoPoint,
    val serviceType: ServiceType,
    val status: TripStatus,
    val estimatedDistanceKm: Double,
    val estimatedEtaMinutes: Int,
    val totalFare: Double,
    val driver: Driver? = null,
    val driverLocation: GeoPoint? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("passengerName", passengerName)
            put("passengerPhone", passengerPhone)
            put("pickupLocation", pickupLocation.toJsonObject())
            put("destinationLocation", destinationLocation.toJsonObject())
            put("serviceType", serviceType.name)
            put("status", status.name)
            put("estimatedDistanceKm", estimatedDistanceKm)
            put("estimatedEtaMinutes", estimatedEtaMinutes)
            put("totalFare", totalFare)
            driver?.let { put("driver", it.toJsonObject()) }
            driverLocation?.let { put("driverLocation", it.toJsonObject()) }
            put("createdAtMillis", createdAtMillis)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): TaxiRequest {
            val driverJson = json.optJSONObject("driver")
            val driverLocJson = json.optJSONObject("driverLocation")
            return TaxiRequest(
                id = json.optString("id", UUID.randomUUID().toString()),
                passengerName = json.optString("passengerName", "Usuario"),
                passengerPhone = json.optString("passengerPhone", "+57 300 1234567"),
                pickupLocation = GeoPoint.fromJsonObject(json.optJSONObject("pickupLocation") ?: JSONObject()),
                destinationLocation = GeoPoint.fromJsonObject(json.optJSONObject("destinationLocation") ?: JSONObject()),
                serviceType = try {
                    ServiceType.valueOf(json.optString("serviceType", ServiceType.STANDARD.name))
                } catch (e: Exception) { ServiceType.STANDARD },
                status = try {
                    TripStatus.valueOf(json.optString("status", TripStatus.SEARCHING.name))
                } catch (e: Exception) { TripStatus.SEARCHING },
                estimatedDistanceKm = json.optDouble("estimatedDistanceKm", 3.5),
                estimatedEtaMinutes = json.optInt("estimatedEtaMinutes", 6),
                totalFare = json.optDouble("totalFare", 7700.0),
                driver = if (driverJson != null) Driver.fromJsonObject(driverJson) else null,
                driverLocation = if (driverLocJson != null) GeoPoint.fromJsonObject(driverLocJson) else null,
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }
    }
}
