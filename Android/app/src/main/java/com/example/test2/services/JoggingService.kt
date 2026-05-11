package com.example.test2.services

import android.R
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class JoggingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var distanceJogged = 0f
    private val targetDistance = 500f
    private val CHANNEL_ID = "jogging_quest_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(3, buildNotification(0f))
        startTracking()
    }

    private fun startTracking() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateDistanceMeters(2f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLocation?.let {
                    distanceJogged += it.distanceTo(location)
                    updateNotification(distanceJogged)
                    sendBroadcast(Intent("JOG_PROGRESS").putExtra("distance", distanceJogged))
                    if (distanceJogged >= targetDistance) {
                        sendBroadcast(Intent("JOG_COMPLETE"))
                        notifyComplete()
                        stopSelf()
                    }
                }
                lastLocation = location
            }
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun buildNotification(distance: Float): Notification {
        val progress = ((distance / targetDistance) * 100).toInt().coerceAtMost(100)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jogging Quest")
            .setContentText("${distance.toInt()}m / 500m")
            .setSmallIcon(R.drawable.ic_dialog_map)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(distance: Float) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(3, buildNotification(distance))
    }

    private fun notifyComplete() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quest Complete!")
            .setContentText("You jogged 500 meters!")
            .setSmallIcon(R.drawable.star_big_on)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(4, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Jogging Quest", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}