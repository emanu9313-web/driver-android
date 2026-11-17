package com.example.driver.domain.repository

import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.model.Rating
import com.example.driver.domain.model.Ride
import com.example.driver.domain.model.RideStatus
import com.example.driver.domain.model.ServiceCategory
import kotlinx.coroutines.flow.Flow

interface RideRepository {
    fun observeActiveRideForPassenger(passengerId: String): Flow<Ride?>
    fun observePendingRidesForDriver(): Flow<List<Ride>>
    fun observeActiveRideForDriver(driverId: String): Flow<Ride?>
    fun observePassengerHistory(passengerId: String): Flow<List<Ride>>
    fun observeDriverHistory(driverId: String): Flow<List<Ride>>
    fun observeAllRidesAdmin(): Flow<List<Ride>>

    suspend fun requestRide(
        passengerId: String,
        passengerName: String,
        passengerPhone: String,
        pickup: GeoLocation,
        destination: GeoLocation,
        category: ServiceCategory
    ): Result<Ride>

    suspend fun acceptRide(rideId: String, driverId: String): Result<Unit>
    suspend fun updateRideStatus(rideId: String, nextStatus: RideStatus): Result<Unit>
    suspend fun cancelRide(rideId: String, reason: String): Result<Unit>
    suspend fun submitRating(rating: Rating): Result<Unit>
}
