package com.example.driver.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.model.Ride
import com.example.driver.domain.model.RideStatus
import com.example.driver.domain.repository.AuthRepository
import com.example.driver.domain.repository.DriverRepository
import com.example.driver.domain.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DriverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val driverRepository: DriverRepository,
    private val rideRepository: RideRepository
) : ViewModel() {

    val currentDriverId: String? get() = authRepository.currentUserId

    val driverProfile: StateFlow<DriverProfile?> = authRepository.observeAuthState()
        .flatMapLatest { user ->
            if (user != null) {
                driverRepository.observeDriverProfile(user.uid)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRide: StateFlow<Ride?> = authRepository.observeAuthState()
        .flatMapLatest { user ->
            if (user != null) {
                rideRepository.observeActiveRideForDriver(user.uid)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingRides: StateFlow<List<Ride>> = rideRepository.observePendingRidesForDriver()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driverHistory: StateFlow<List<Ride>> = authRepository.observeAuthState()
        .flatMapLatest { user ->
            if (user != null) {
                rideRepository.observeDriverHistory(user.uid)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleOnlineStatus(isOnline: Boolean) {
        val driverId = currentDriverId ?: return
        viewModelScope.launch {
            driverRepository.updateOnlineStatus(driverId, isOnline)
        }
    }

    fun updateLocation(location: GeoLocation) {
        val driverId = currentDriverId ?: return
        viewModelScope.launch {
            driverRepository.updateLocation(driverId, location)
        }
    }

    fun acceptRide(rideId: String) {
        val driverId = currentDriverId ?: return
        viewModelScope.launch {
            rideRepository.acceptRide(rideId, driverId)
        }
    }

    fun advanceRideStatus(ride: Ride) {
        val nextStatus = when (ride.status) {
            RideStatus.SEARCHING -> RideStatus.DRIVER_ASSIGNED
            RideStatus.DRIVER_ASSIGNED -> RideStatus.DRIVER_EN_ROUTE
            RideStatus.DRIVER_EN_ROUTE -> RideStatus.ARRIVED
            RideStatus.ARRIVED -> RideStatus.IN_PROGRESS
            RideStatus.IN_PROGRESS -> RideStatus.COMPLETED
            else -> ride.status
        }
        viewModelScope.launch {
            rideRepository.updateRideStatus(ride.id, nextStatus)
        }
    }
}
