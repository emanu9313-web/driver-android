package com.example.driver.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.Ride
import com.example.driver.domain.repository.DriverRepository
import com.example.driver.domain.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val rideRepository: RideRepository
) : ViewModel() {

    val allDrivers: StateFlow<List<DriverProfile>> = driverRepository.observeAllDriversAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRides: StateFlow<List<Ride>> = rideRepository.observeAllRidesAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveDriver(driverId: String) {
        viewModelScope.launch {
            driverRepository.approveDriver(driverId)
        }
    }

    fun suspendDriver(driverId: String, isSuspended: Boolean) {
        viewModelScope.launch {
            driverRepository.suspendDriver(driverId, isSuspended)
        }
    }

    fun assignDriverToRide(rideId: String, driverId: String) {
        viewModelScope.launch {
            rideRepository.acceptRide(rideId, driverId)
        }
    }
}
