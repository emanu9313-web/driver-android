package com.example.driver.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.model.Rating
import com.example.driver.domain.model.Ride
import com.example.driver.domain.model.RideEstimate
import com.example.driver.domain.model.RidePricing
import com.example.driver.domain.model.ServiceCategory
import com.example.driver.domain.model.User
import com.example.driver.domain.repository.AuthRepository
import com.example.driver.domain.repository.DriverRepository
import com.example.driver.domain.repository.RideRepository
import com.example.driver.location.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PassengerViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val rideRepository: RideRepository,
    private val driverRepository: DriverRepository
) : AndroidViewModel(application) {

    data class RideRequestState(
        val isSubmitting: Boolean = false,
        val error: String? = null
    )

    private val _rideRequestState = MutableStateFlow(RideRequestState())
    val rideRequestState: StateFlow<RideRequestState> = _rideRequestState.asStateFlow()

    private val locationService = LocationService(application)

    private val _pickupLocation = MutableStateFlow(
        GeoLocation(4.6097, -74.0817, "Plaza Central - Centro")
    )
    val pickupLocation: StateFlow<GeoLocation> = _pickupLocation.asStateFlow()

    private val _destinationLocation = MutableStateFlow(
        GeoLocation(4.6560, -74.0595, "Zona Rosa - Chapinero")
    )
    val destinationLocation: StateFlow<GeoLocation> = _destinationLocation.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ServiceCategory.STANDARD)
    val selectedCategory: StateFlow<ServiceCategory> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeoLocation>>(emptyList())
    val searchResults: StateFlow<List<GeoLocation>> = _searchResults.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeRide: StateFlow<Ride?> = authRepository.observeAuthState()
        .flatMapLatest { user ->
            if (user != null) {
                rideRepository.observeActiveRideForPassenger(user.uid)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nearbyDrivers: StateFlow<List<DriverProfile>> = driverRepository.observeNearbyOnlineDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passengerHistory: StateFlow<List<Ride>> = authRepository.observeAuthState()
        .flatMapLatest { user ->
            if (user != null) {
                rideRepository.observePassengerHistory(user.uid)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchCurrentGpsLocation() {
        viewModelScope.launch {
            val gps = locationService.getCurrentGpsLocation()
            if (gps != null) {
                _pickupLocation.value = gps
            }
        }
    }

    fun searchAddress(query: String) {
        if (query.trim().length < 3) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = locationService.searchAddressByName(query)
        }
    }

    fun setPickup(loc: GeoLocation) { _pickupLocation.value = loc }
    fun setDestination(loc: GeoLocation) { _destinationLocation.value = loc }
    fun setCategory(category: ServiceCategory) { _selectedCategory.value = category }

    fun estimate(category: ServiceCategory): RideEstimate =
        RidePricing.estimate(_pickupLocation.value, _destinationLocation.value, category)

    fun requestRide() {
        if (_rideRequestState.value.isSubmitting || activeRide.value != null) return
        val user = currentUser.value
        if (user == null) {
            _rideRequestState.value = RideRequestState(error = "Inicia sesión para solicitar un viaje.")
            return
        }
        val pickup = _pickupLocation.value
        val destination = _destinationLocation.value
        if (pickup.latitude == destination.latitude && pickup.longitude == destination.longitude) {
            _rideRequestState.value = RideRequestState(error = "El origen y el destino deben ser distintos.")
            return
        }

        viewModelScope.launch {
            _rideRequestState.value = RideRequestState(isSubmitting = true)
            val result = rideRepository.requestRide(
                passengerId = user.uid,
                passengerName = user.name,
                passengerPhone = user.phone,
                pickup = pickup,
                destination = destination,
                category = _selectedCategory.value
            )
            _rideRequestState.value = result.fold(
                onSuccess = { RideRequestState() },
                onFailure = { error ->
                    RideRequestState(error = error.localizedMessage ?: "No se pudo guardar el viaje.")
                }
            )
        }
    }

    fun cancelActiveRide(reason: String = "Cancelado por el usuario") {
        val currentRide = activeRide.value ?: return
        viewModelScope.launch {
            rideRepository.cancelRide(currentRide.id, reason)
        }
    }

    fun submitRating(score: Int, comment: String) {
        val currentRide = activeRide.value ?: return
        val driverId = currentRide.driverId ?: return
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val rating = Rating(
                rideId = currentRide.id,
                passengerId = userId,
                driverId = driverId,
                score = score,
                comment = comment
            )
            rideRepository.submitRating(rating)
        }
    }
}
