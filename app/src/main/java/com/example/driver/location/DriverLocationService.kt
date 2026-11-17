package com.example.driver.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.driver.R
import com.example.driver.domain.model.GeoLocation
import com.example.driver.domain.repository.DriverRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DriverLocationService : Service() {

    @Inject
    lateinit var driverRepository: DriverRepository

    private lateinit var locationService: LocationService
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "driver_location_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_DRIVER_ID = "extra_driver_id"
    }

    override fun onCreate() {
        super.onCreate()
        locationService = LocationService(applicationContext)
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val driverId = intent?.getStringExtra(EXTRA_DRIVER_ID) ?: return START_NOT_STICKY

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        startLocationUpdates(driverId)

        return START_STICKY
    }

    private fun startLocationUpdates(driverId: String) {
        serviceScope.launch {
            while (isActive) {
                val gpsLocation = locationService.getCurrentGpsLocation()
                if (gpsLocation != null) {
                    driverRepository.updateLocation(driverId, gpsLocation)
                }
                delay(10000) // Publicar ubicación cada 10 segundos
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaxiGO Conductor En Línea")
            .setContentText("Publicando posición GPS en tiempo real")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Ubicación de Conductor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
