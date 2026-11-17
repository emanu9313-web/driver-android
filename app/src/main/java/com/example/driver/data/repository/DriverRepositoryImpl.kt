package com.example.driver.data.repository

import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.repository.DriverRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DriverRepository {

    override fun observeNearbyOnlineDrivers(): Flow<List<DriverProfile>> = callbackFlow {
        val subscription = firestore.collection("drivers")
            .whereEqualTo("isOnline", true)
            .whereEqualTo("isApproved", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val drivers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DriverDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(drivers)
            }
        awaitClose { subscription.remove() }
    }

    override fun observeDriverProfile(driverId: String): Flow<DriverProfile?> = callbackFlow {
        val subscription = firestore.collection("drivers").document(driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val driver = snapshot?.toObject(DriverDto::class.java)?.toDomain()
                trySend(driver)
            }
        awaitClose { subscription.remove() }
    }

    override fun observeAllDriversAdmin(): Flow<List<DriverProfile>> = callbackFlow {
        val subscription = firestore.collection("drivers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val drivers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DriverDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(drivers)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateOnlineStatus(driverId: String, isOnline: Boolean): Result<Unit> = runCatching {
        firestore.collection("drivers").document(driverId)
            .update("isOnline", isOnline)
            .await()
    }

    override suspend fun updateLocation(driverId: String, location: GeoLocation): Result<Unit> = runCatching {
        val locationMap = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "addressName" to location.addressName,
            "geohash" to location.geohash,
            "updatedAtMillis" to System.currentTimeMillis()
        )
        firestore.collection("drivers").document(driverId)
            .update("location", locationMap)
            .await()
    }

    override suspend fun approveDriver(driverId: String): Result<Unit> = runCatching {
        firestore.collection("drivers").document(driverId)
            .update("isApproved", true, "isSuspended", false)
            .await()
    }

    override suspend fun suspendDriver(driverId: String, isSuspended: Boolean): Result<Unit> = runCatching {
        firestore.collection("drivers").document(driverId)
            .update("isSuspended", isSuspended)
            .await()
    }
}

data class DriverDto(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val isOnline: Boolean = false,
    val isApproved: Boolean = true,
    val isSuspended: Boolean = false,
    val ratingAverage: Double = 5.0,
    val ratingCount: Int = 0,
    val location: GeoLocationDto = GeoLocationDto()
) {
    fun toDomain() = DriverProfile(
        uid = uid,
        name = name,
        phone = phone,
        vehicleModel = vehicleModel,
        vehiclePlate = vehiclePlate,
        vehicleColor = vehicleColor,
        isOnline = isOnline,
        isApproved = isApproved,
        isSuspended = isSuspended,
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
        location = location.toDomain()
    )

    companion object {
        fun fromDomain(d: DriverProfile) = DriverDto(
            uid = d.uid,
            name = d.name,
            phone = d.phone,
            vehicleModel = d.vehicleModel,
            vehiclePlate = d.vehiclePlate,
            vehicleColor = d.vehicleColor,
            isOnline = d.isOnline,
            isApproved = d.isApproved,
            isSuspended = d.isSuspended,
            ratingAverage = d.ratingAverage,
            ratingCount = d.ratingCount,
            location = GeoLocationDto.fromDomain(d.location)
        )
    }
}

data class GeoLocationDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressName: String = "",
    val geohash: String = ""
) {
    fun toDomain() = GeoLocation(latitude, longitude, addressName, geohash)

    companion object {
        fun fromDomain(g: GeoLocation) = GeoLocationDto(g.latitude, g.longitude, g.addressName, g.geohash)
    }
}
