package com.sounddeck.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AutomationService : Service() {

    companion object {
        const val CHANNEL_ID = "sounddeck_automation_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.sounddeck.service.ACTION_START"
        const val ACTION_STOP = "com.sounddeck.service.ACTION_STOP"
        const val ACTION_UPDATE = "com.sounddeck.service.ACTION_UPDATE"

        const val EXTRA_OBS_STATUS = "extra_obs_status"
        const val EXTRA_POLLING_COUNT = "extra_polling_count"

        fun startService(context: Context, obsStatus: String = "DISCONNECTED", pollingCount: Int = 0) {
            val intent = Intent(context, AutomationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OBS_STATUS, obsStatus)
                putExtra(EXTRA_POLLING_COUNT, pollingCount)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateService(context: Context, obsStatus: String, pollingCount: Int) {
            val intent = Intent(context, AutomationService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_OBS_STATUS, obsStatus)
                putExtra(EXTRA_POLLING_COUNT, pollingCount)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutomationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentObsStatus = "DISCONNECTED"
    private var currentPollingCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                currentObsStatus = intent.getStringExtra(EXTRA_OBS_STATUS) ?: currentObsStatus
                currentPollingCount = intent.getIntExtra(EXTRA_POLLING_COUNT, currentPollingCount)
                val notification = buildNotification()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    }
                    startForeground(NOTIFICATION_ID, notification, serviceType)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE -> {
                currentObsStatus = intent.getStringExtra(EXTRA_OBS_STATUS) ?: currentObsStatus
                currentPollingCount = intent.getIntExtra(EXTRA_POLLING_COUNT, currentPollingCount)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "SoundDeck Automation Active"
        val text = "OBS: $currentObsStatus | Polling: $currentPollingCount active endpoints"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoundDeck Automation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps OBS connection and SoundDeck HTTP polling active in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
