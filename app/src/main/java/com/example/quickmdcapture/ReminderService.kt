package com.example.quickmdcapture

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class ReminderService : Service() {

    companion object {
        private const val REMINDER_CHANNEL_ID = "ReminderChannel"
        private const val REMINDER_NOTIFICATION_ID = 100
    }

    private lateinit var settingsViewModel: SettingsViewModel
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var reminderJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsViewModel = ViewModelProvider.AndroidViewModelFactory(application).create(SettingsViewModel::class.java)
        createNotificationChannel()
        startForeground(REMINDER_NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startReminderJob()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        reminderJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.reminder_channel_description)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.reminder_service_running))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun startReminderJob() {
        reminderJob?.cancel()
        reminderJob = serviceScope.launch {
            while (isActive) {
                if (shouldShowReminder()) {
                    showReminderNotification()
                }
                
                // Calculate next exact minute
                val now = System.currentTimeMillis()
                val nextMinute = (now / 60000 + 1) * 60000
                val delay = nextMinute - now
                
                delay(delay)
            }
        }
    }

    private fun shouldShowReminder(): Boolean {
        if (!settingsViewModel.isReminderEnabled.value) return false

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startTime = settingsViewModel.reminderStartTime.value.split(":")
        val endTime = settingsViewModel.reminderEndTime.value.split(":")
        
        val startTimeInMinutes = startTime[0].toInt() * 60 + startTime[1].toInt()
        val endTimeInMinutes = endTime[0].toInt() * 60 + endTime[1].toInt()

        // Handle case when end time is on the next day
        val isInTimeRange = if (endTimeInMinutes < startTimeInMinutes) {
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        } else {
            currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        }

        if (!isInTimeRange) return false

        // Check if current minute matches the reminder schedule
        val interval = settingsViewModel.reminderInterval.value
        
        // Calculate which minute in the interval we should be at
        val minutesSinceStart = (currentTimeInMinutes - startTimeInMinutes) % interval
        return minutesSinceStart == 0
    }

    private fun showReminderNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, TransparentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(settingsViewModel.reminderText.value)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
    }
} 