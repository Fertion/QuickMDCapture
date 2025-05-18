package com.example.quickmdcapture

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider


class NotificationService : Service() {

    companion object {
        private const val CHANNEL_ID = "QuickMDCaptureChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        settingsViewModel = ViewModelProvider.AndroidViewModelFactory(application).create(SettingsViewModel::class.java)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.add_note_notification_text)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationStyle = settingsViewModel.notificationStyle.value

        val intent = Intent(this, TransparentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val voiceInputPendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent.putExtra("START_VOICE_INPUT", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }


        when (notificationStyle) {
            "standard" -> {
                builder.setContentTitle(getString(R.string.app_name))
                builder.setContentText(getString(R.string.add_note_notification_text))
            }
            "expanded_with_buttons_1" -> {
                val notificationLayout = RemoteViews(packageName, R.layout.new_notification_layout)

                notificationLayout.setOnClickPendingIntent(R.id.note_button, pendingIntent)
                notificationLayout.setOnClickPendingIntent(R.id.voice_input_button, voiceInputPendingIntent)

                builder.setCustomContentView(notificationLayout)
                builder.setCustomBigContentView(notificationLayout) // Для расширенного вида
            }
            // Добавьте здесь обработку других стилей уведомлений
        }

        return builder.build()
    }
}