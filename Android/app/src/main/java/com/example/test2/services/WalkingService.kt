package com.example.test2.services

import android.R
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class WalkingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var distanceWalked = 0f
    private val targetDistance = 250f
    val CHANNEL_ID = "walking_quest_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(1, buildNotification(0f))
        startTracking()
    }

    private fun startTracking() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000L
        ).setMinUpdateDistanceMeters(2f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLocation?.let {
                    distanceWalked += it.distanceTo(location)
                    updateNotification(distanceWalked)
                    sendProgressBroadcast(distanceWalked)
                    if (distanceWalked >= targetDistance) {
                        sendCompletionBroadcast()
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
            .setContentTitle("Walking Quest")
            .setContentText("${distance.toInt()}m / 250m")
            .setSmallIcon(R.drawable.ic_dialog_map)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(distance: Float) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, buildNotification(distance))
    }

    private fun sendProgressBroadcast(distance: Float) {
        val intent = Intent("WALK_PROGRESS").putExtra("distance", distance)
        sendBroadcast(intent)
    }

    private fun sendCompletionBroadcast() {
        val intent = Intent("WALK_COMPLETE")
        sendBroadcast(intent)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quest Complete!")
            .setContentText("You walked 250 meters!")
            .setSmallIcon(R.drawable.star_big_on)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Walking Quest", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}