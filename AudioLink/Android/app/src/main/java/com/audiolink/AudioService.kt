package com.audiolink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log

class AudioService : Service() {

    private val binder = LocalBinder()
    private val audioStreamer = AudioStreamer()
    private val CHANNEL_ID = "AudioLinkChannel"

    val isStreaming: Boolean
        get() = audioStreamer.isStreaming


    // Proxy listener to Activity through Service
    var streamListener: AudioStreamer.StreamListener? = null
        set(value) {
            field = value
            audioStreamer.listener = value
        }

    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    fun startStreaming(ip: String) {
        startForegroundServiceNotification()
        audioStreamer.startStreaming(ip)
    }

    fun stopStreaming() {
        audioStreamer.stopStreaming()
        stopForeground(true)
        stopSelf()
    }

    fun setVolume(volume: Float) {
        audioStreamer.setVolume(volume)
    }

    fun setMute(mute: Boolean) {
        audioStreamer.setMute(mute)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AudioLink Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AudioLink Active")
            .setContentText("Microphone is streaming to PC")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
             startForeground(1, notification)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioStreamer.stopStreaming()
    }
}
