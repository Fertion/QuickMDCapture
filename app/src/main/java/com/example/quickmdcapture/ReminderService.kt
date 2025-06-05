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
import java.util.concurrent.atomic.AtomicBoolean

class ReminderService : Service() {

    companion object {
        private const val REMINDER_CHANNEL_ID = "ReminderChannel"
        private const val REMINDER_NOTIFICATION_ID = 100
        private const val PREFS_NAME = "ReminderPrefs"
        private const val KEY_LAST_REMINDER_TIME = "last_reminder_time"
        private const val KEY_NEXT_SCHEDULED_TIME = "next_scheduled_time"
    }

    private lateinit var settingsViewModel: SettingsViewModel
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var reminderJob: Job? = null
    private val isReminderRunning = AtomicBoolean(false)
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

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

    private fun startReminderJob() {
        // If a reminder cycle is already running, don't start a new one
        if (!isReminderRunning.compareAndSet(false, true)) {
            return
        }

        reminderJob?.cancel()
        reminderJob = serviceScope.launch {
            try {
                while (isActive) {
                    if (!settingsViewModel.isReminderEnabled.value) {
                        delay(TimeUnit.MINUTES.toMillis(1))
                        continue
                    }

                    val currentTime = System.currentTimeMillis()
                    val lastReminderTime = prefs.getLong(KEY_LAST_REMINDER_TIME, 0)
                    val nextScheduledTime = prefs.getLong(KEY_NEXT_SCHEDULED_TIME, 0)

                    if (isTimeInRange(currentTime)) {
                        val shouldShowReminder = when {
                            // Don't show if we already showed a reminder in this minute
                            lastReminderTime > 0 && isSameMinute(currentTime, lastReminderTime) -> false
                            // Show if we're at a scheduled interval
                            isAtScheduledInterval(currentTime) -> true
                            // Show if we missed a scheduled reminder
                            nextScheduledTime > 0 && currentTime > nextScheduledTime -> true
                            else -> false
                        }

                        if (shouldShowReminder) {
                            showReminderNotification()
                            prefs.edit().putLong(KEY_LAST_REMINDER_TIME, currentTime).apply()
                        }
                    }

                    // Calculate next scheduled time
                    val nextTime = calculateNextScheduledTime(currentTime)
                    prefs.edit().putLong(KEY_NEXT_SCHEDULED_TIME, nextTime).apply()

                    // Calculate delay until next check
                    // Always align to the start of the next minute
                    val nextMinute = ((currentTime / 60000) + 1) * 60000
                    val delay = minOf(
                        nextMinute - currentTime, // Delay until start of next minute
                        nextTime - currentTime // Or until next scheduled time
                    )
                    delay(delay)
                }
            } finally {
                isReminderRunning.set(false)
            }
        }
    }

    private fun isTimeInRange(timeMillis: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startTime = settingsViewModel.reminderStartTime.value.split(":")
        val endTime = settingsViewModel.reminderEndTime.value.split(":")
        
        val startTimeInMinutes = startTime[0].toInt() * 60 + startTime[1].toInt()
        val endTimeInMinutes = endTime[0].toInt() * 60 + endTime[1].toInt()

        return if (endTimeInMinutes < startTimeInMinutes) {
            // Handle overnight range (e.g., 23:00 to 07:00)
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        } else {
            currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        }
    }

    private fun isAtScheduledInterval(timeMillis: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startTime = settingsViewModel.reminderStartTime.value.split(":")
        val startTimeInMinutes = startTime[0].toInt() * 60 + startTime[1].toInt()
        val interval = settingsViewModel.reminderInterval.value

        val minutesSinceStart = (currentTimeInMinutes - startTimeInMinutes) % interval
        return minutesSinceStart == 0
    }

    private fun isSameMinute(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
               cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
               cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE)
    }

    private fun calculateNextScheduledTime(currentTime: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startTime = settingsViewModel.reminderStartTime.value.split(":")
        val endTime = settingsViewModel.reminderEndTime.value.split(":")
        val interval = settingsViewModel.reminderInterval.value
        
        val startTimeInMinutes = startTime[0].toInt() * 60 + startTime[1].toInt()
        val endTimeInMinutes = endTime[0].toInt() * 60 + endTime[1].toInt()

        // If current time is not in range, return start of next range
        if (!isTimeInRange(currentTime)) {
            val nextStartTime = if (currentTimeInMinutes >= endTimeInMinutes) {
                // If we're past end time, next start is tomorrow
                startTimeInMinutes + 24 * 60
            } else {
                startTimeInMinutes
            }
            return calendar.apply {
                set(Calendar.HOUR_OF_DAY, nextStartTime / 60)
                set(Calendar.MINUTE, nextStartTime % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (nextStartTime >= 24 * 60) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }.timeInMillis
        }

        // Calculate next interval time
        val minutesSinceStart = (currentTimeInMinutes - startTimeInMinutes) % interval
        val nextIntervalTime = if (minutesSinceStart == 0) {
            currentTimeInMinutes + interval
        } else {
            currentTimeInMinutes + (interval - minutesSinceStart)
        }

        // Check if next interval is within range
        if (isTimeInRange(calendar.apply {
            set(Calendar.HOUR_OF_DAY, nextIntervalTime / 60)
            set(Calendar.MINUTE, nextIntervalTime % 60)
        }.timeInMillis)) {
            return calendar.timeInMillis
        }

        // If next interval is outside range, return start of next range
        val nextStartTime = if (endTimeInMinutes < startTimeInMinutes) {
            // If range is overnight, next start is tomorrow
            startTimeInMinutes + 24 * 60
        } else {
            startTimeInMinutes
        }
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, nextStartTime / 60)
            set(Calendar.MINUTE, nextStartTime % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextStartTime >= 24 * 60) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun showReminderNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, TransparentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("FROM_REMINDER", true)
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