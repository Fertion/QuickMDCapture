package com.example.quickmdcapture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Create SettingsViewModel to check if reminders are enabled
            val settingsViewModel = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as android.app.Application)
                .create(SettingsViewModel::class.java)

            // Start NotificationService if reminders are enabled
            if (settingsViewModel.isReminderEnabled.value) {
                val serviceIntent = Intent(context, NotificationService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
} 