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
        private const val CHANNEL_ID = "ReminderChannel"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var settingsViewModel: SettingsViewModel
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var reminderJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsViewModel = ViewModelProvider.AndroidViewModelFactory(application).create(SettingsViewModel::class.java)
        createNotificationChannel()
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
            val name = getString(R.string.reminder_channel_name)
            val descriptionText = getString(R.string.reminder_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, TransparentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(settingsViewModel.reminderText.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
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
                delay(TimeUnit.MINUTES.toMillis(1))
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
            // For overnight periods (e.g., 13:00 to 02:00)
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        } else {
            // For same-day periods (e.g., 09:00 to 21:00)
            currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        }

        if (!isInTimeRange) return false

        // Check if it's time for a reminder based on the interval
        val interval = settingsViewModel.reminderInterval.value
        return currentMinute % interval == 0
    }

    private fun showReminderNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
} 