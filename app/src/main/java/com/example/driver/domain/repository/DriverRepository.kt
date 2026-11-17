package com.example.driver.domain.repository

import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import kotlinx.coroutines.flow.Flow

interface DriverRepository {
    fun observeNearbyOnlineDrivers(): Flow<List<DriverProfile>>
    fun observeDriverProfile(driverId: String): Flow<DriverProfile?>
    fun observeAllDriversAdmin(): Flow<List<DriverProfile>>

    suspend fun updateOnlineStatus(driverId: String, isOnline: Boolean): Result<Unit>
    suspend fun updateLocation(driverId: String, location: GeoLocation): Result<Unit>
    suspend fun approveDriver(driverId: String): Result<Unit>
    suspend fun suspendDriver(driverId: String, isSuspended: Boolean): Result<Unit>
}
