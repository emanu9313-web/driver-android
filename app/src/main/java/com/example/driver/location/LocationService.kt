package com.example.driver.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.example.driver.domain.model.GeoLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationService(context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): GeoLocation? = withContext(Dispatchers.IO) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = suspendCancellableCoroutine { cont ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    if (cont.isActive) cont.resume(loc)
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
            }

            if (location != null) {
                val addressName = getAddressFromCoordinates(location.latitude, location.longitude)
                GeoLocation(location.latitude, location.longitude, addressName)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAddressFromCoordinates(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val featureName = addr.featureName ?: ""
                val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: ""
                val locality = addr.locality ?: addr.adminArea ?: ""
                if (thoroughfare.isNotEmpty()) {
                    "$thoroughfare $featureName, $locality".trim()
                } else if (addr.getAddressLine(0) != null) {
                    addr.getAddressLine(0)
                } else {
                    "Ubicación (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
                }
            } else {
                "Ubicación (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
            }
        } catch (e: Exception) {
            "Ubicación GPS (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
        }
    }

    suspend fun searchAddressByName(queryName: String): List<GeoLocation> = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(queryName, 5)
            if (!results.isNullOrEmpty()) {
                results.map { addr ->
                    val name = addr.getAddressLine(0) ?: "${addr.featureName ?: ""} ${addr.locality ?: ""}"
                    GeoLocation(addr.latitude, addr.longitude, name)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
