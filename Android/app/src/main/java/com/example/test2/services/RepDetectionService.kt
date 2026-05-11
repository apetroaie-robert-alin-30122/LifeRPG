package com.example.test2.services

import android.app.*
import android.content.Intent
import android.hardware.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

abstract class RepDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var acceleration = 0f

    private var repCount = 0
    private var lastRepTime = 0L
    private val repCooldown = 800L  // ms between reps to avoid double counting

    abstract val targetReps: Int
    abstract val channelId: String
    abstract val questTitle: String
    abstract val notificationId: Int
    abstract val progressAction: String
    abstract val completeAction: String
    abstract val motionThreshold: Float  // different for situps vs pushups

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
        startForeground(notificationId, buildNotification(0))
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta

        val now = System.currentTimeMillis()
        if (acceleration > motionThreshold && now - lastRepTime > repCooldown) {
            lastRepTime = now
            repCount++
            updateNotification(repCount)
            sendBroadcast(Intent(progressAction).putExtra("reps", repCount))
            if (repCount >= targetReps) {
                sendBroadcast(Intent(completeAction))
                notifyComplete()
                stopSelf()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun buildNotification(reps: Int): Notification {
        val progress = ((reps.toFloat() / targetReps) * 100).toInt().coerceAtMost(100)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(questTitle)
            .setContentText("$reps / $targetReps reps")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(reps: Int) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, buildNotification(reps))
    }

    private fun notifyComplete() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Quest Complete!")
            .setContentText("You finished $questTitle!")
            .setSmallIcon(android.R.drawable.star_big_on)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId + 10, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelId, questTitle, NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}