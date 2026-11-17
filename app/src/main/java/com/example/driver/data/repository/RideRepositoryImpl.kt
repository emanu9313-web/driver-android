package com.example.driver.data.repository

import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.model.Rating
import com.example.driver.domain.model.Ride
import com.example.driver.domain.model.RidePricing
import com.example.driver.domain.model.RideStatus
import com.example.driver.domain.model.ServiceCategory
import com.example.driver.domain.repository.RideRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RideRepository {

    override fun observeActiveRideForPassenger(passengerId: String): Flow<Ride?> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("passengerId", passengerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val activeRide = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                }?.firstOrNull {
                    it.status != RideStatus.COMPLETED && it.status != RideStatus.CANCELLED
                }
                trySend(activeRide)
            }
        awaitClose { subscription.remove() }
    }

    override fun observePendingRidesForDriver(): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("status", RideStatus.SEARCHING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rides = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { subscription.remove() }
    }

    override fun observeActiveRideForDriver(driverId: String): Flow<Ride?> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val activeRide = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                }?.firstOrNull {
                    it.status != RideStatus.COMPLETED && it.status != RideStatus.CANCELLED
                }
                trySend(activeRide)
            }
        awaitClose { subscription.remove() }
    }

    override fun observePassengerHistory(passengerId: String): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("passengerId", passengerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rides = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(rides.sortedByDescending { it.createdAtMillis })
            }
        awaitClose { subscription.remove() }
    }

    override fun observeDriverHistory(driverId: String): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rides = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { subscription.remove() }
    }

    override fun observeAllRidesAdmin(): Flow<List<Ride>> = callbackFlow {
        val subscription = firestore.collection("rides")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rides = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RideDto::class.java)?.toDomain()
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun requestRide(
        passengerId: String,
        passengerName: String,
        passengerPhone: String,
        pickup: GeoLocation,
        destination: GeoLocation,
        category: ServiceCategory
    ): Result<Ride> = runCatching {
        val rideId = UUID.randomUUID().toString()
        val estimate = RidePricing.estimate(pickup, destination, category)
        val ride = Ride(
            id = rideId,
            passengerId = passengerId,
            passengerName = passengerName,
            passengerPhone = passengerPhone,
            pickupLocation = pickup,
            destinationLocation = destination,
            category = category,
            status = RideStatus.SEARCHING,
            estimatedFare = estimate.fareCop,
            estimatedDistanceKm = estimate.distanceKm,
            estimatedEtaSeconds = estimate.etaSeconds,
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )

        firestore.collection("rides").document(rideId)
            .set(RideDto.fromDomain(ride))
            .await()

        ride
    }

    override suspend fun acceptRide(rideId: String, driverId: String): Result<Unit> = runCatching {
        val driverDoc = firestore.collection("drivers").document(driverId).get().await()
        val driverProfile = driverDoc.toObject(DriverDto::class.java)?.toDomain()
            ?: throw IllegalStateException("Perfil de conductor no encontrado")

        val updates = mapOf(
            "driverId" to driverId,
            "driverName" to driverProfile.name,
            "driverPhone" to driverProfile.phone,
            "vehiclePlate" to driverProfile.vehiclePlate,
            "vehicleModel" to driverProfile.vehicleModel,
            "driverLocation" to GeoLocationDto.fromDomain(driverProfile.location),
            "status" to RideStatus.DRIVER_ASSIGNED.name,
            "updatedAtMillis" to System.currentTimeMillis()
        )

        firestore.collection("rides").document(rideId)
            .update(updates)
            .await()
    }

    override suspend fun updateRideStatus(rideId: String, nextStatus: RideStatus): Result<Unit> = runCatching {
        firestore.collection("rides").document(rideId)
            .update(
                "status", nextStatus.name,
                "updatedAtMillis", System.currentTimeMillis()
            )
            .await()
    }

    override suspend fun cancelRide(rideId: String, reason: String): Result<Unit> = runCatching {
        firestore.collection("rides").document(rideId)
            .update(
                "status", RideStatus.CANCELLED.name,
                "cancellationReason", reason,
                "updatedAtMillis", System.currentTimeMillis()
            )
            .await()
    }

    override suspend fun submitRating(rating: Rating): Result<Unit> = runCatching {
        val ratingId = UUID.randomUUID().toString()
        val ratingWithId = rating.copy(id = ratingId)

        firestore.collection("ratings").document(ratingId)
            .set(RatingDto.fromDomain(ratingWithId))
            .await()

        firestore.collection("rides").document(rating.rideId)
            .update("isRated", true)
            .await()
    }
}

data class RideDto(
    val id: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vehiclePlate: String? = null,
    val vehicleModel: String? = null,
    val pickupLocation: GeoLocationDto = GeoLocationDto(),
    val destinationLocation: GeoLocationDto = GeoLocationDto(),
    val driverLocation: GeoLocationDto? = null,
    val category: String = ServiceCategory.STANDARD.name,
    val status: String = RideStatus.SEARCHING.name,
    val estimatedFare: Double = 0.0,
    val estimatedDistanceKm: Double = 0.0,
    val estimatedEtaSeconds: Long = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val cancellationReason: String? = null,
    val isRated: Boolean = false
) {
    fun toDomain() = Ride(
        id = id,
        passengerId = passengerId,
        passengerName = passengerName,
        passengerPhone = passengerPhone,
        driverId = driverId,
        driverName = driverName,
        driverPhone = driverPhone,
        vehiclePlate = vehiclePlate,
        vehicleModel = vehicleModel,
        pickupLocation = pickupLocation.toDomain(),
        destinationLocation = destinationLocation.toDomain(),
        driverLocation = driverLocation?.toDomain(),
        category = try { ServiceCategory.valueOf(category) } catch (e: Exception) { ServiceCategory.STANDARD },
        status = try { RideStatus.valueOf(status) } catch (e: Exception) { RideStatus.SEARCHING },
        estimatedFare = estimatedFare,
        estimatedDistanceKm = estimatedDistanceKm,
        estimatedEtaSeconds = estimatedEtaSeconds,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        cancellationReason = cancellationReason,
        isRated = isRated
    )

    companion object {
        fun fromDomain(r: Ride) = RideDto(
            id = r.id,
            passengerId = r.passengerId,
            passengerName = r.passengerName,
            passengerPhone = r.passengerPhone,
            driverId = r.driverId,
            driverName = r.driverName,
            driverPhone = r.driverPhone,
            vehiclePlate = r.vehiclePlate,
            vehicleModel = r.vehicleModel,
            pickupLocation = GeoLocationDto.fromDomain(r.pickupLocation),
            destinationLocation = GeoLocationDto.fromDomain(r.destinationLocation),
            driverLocation = r.driverLocation?.let { GeoLocationDto.fromDomain(it) },
            category = r.category.name,
            status = r.status.name,
            estimatedFare = r.estimatedFare,
            estimatedDistanceKm = r.estimatedDistanceKm,
            estimatedEtaSeconds = r.estimatedEtaSeconds,
            createdAtMillis = r.createdAtMillis,
            updatedAtMillis = r.updatedAtMillis,
            cancellationReason = r.cancellationReason,
            isRated = r.isRated
        )
    }
}

data class RatingDto(
    val id: String = "",
    val rideId: String = "",
    val passengerId: String = "",
    val driverId: String = "",
    val score: Int = 5,
    val comment: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = Rating(id, rideId, passengerId, driverId, score, comment, createdAtMillis)

    companion object {
        fun fromDomain(r: Rating) = RatingDto(
            id = r.id,
            rideId = r.rideId,
            passengerId = r.passengerId,
            driverId = r.driverId,
            score = r.score,
            comment = r.comment,
            createdAtMillis = r.createdAtMillis
        )
    }
}
