package com.example.quickmdcapture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class TransparentActivity : AppCompatActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Разрешение на использование микрофона получено, запускаем распознавание речи
                dialog.startSpeechRecognition()
            } else {
                // Разрешение на использование микрофона не получено
                Toast.makeText(
                    this,
                    getString(R.string.microphone_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private lateinit var dialog: NoteDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        if (settingsViewModel.isShowOverlockScreenDialog.value) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val isAutoSaveEnabled = settingsViewModel.isAutoSaveEnabled.value
        val isFromReminder = intent.getBooleanExtra("FROM_REMINDER", false)
        dialog = NoteDialog(this, isAutoSaveEnabled, isFromReminder)
        dialog.setOnDismissListener {
            finish()
            overridePendingTransition(0, 0)
        }
        dialog.show()

        if (intent.getBooleanExtra("START_VOICE_INPUT", false)) {
            startSpeechRecognition()
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    fun startSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            dialog.startSpeechRecognition()
        }
    }
}