package com.example.quickmdcapture

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)

    private val _isShowNotificationEnabled =
        MutableStateFlow(sharedPreferences.getBoolean("SHOW_NOTIFICATION", true))
    val isShowNotificationEnabled: StateFlow<Boolean> = _isShowNotificationEnabled

    private val _isShowOverlockScreenDialog =
        MutableStateFlow(sharedPreferences.getBoolean("SHOW_OVERLOCK_SCREEN_DIALOG", false))
    val isShowOverlockScreenDialog: StateFlow<Boolean> = _isShowOverlockScreenDialog

    private val _isDateCreatedEnabled =
        MutableStateFlow(sharedPreferences.getBoolean("SAVE_DATE_CREATED", false))
    val isDateCreatedEnabled: StateFlow<Boolean> = _isDateCreatedEnabled

    private val _propertyName =
        MutableStateFlow(sharedPreferences.getString("PROPERTY_NAME", "created") ?: "created")
    val propertyName: StateFlow<String> = _propertyName

    private val _noteTitleTemplate = MutableStateFlow(
        sharedPreferences.getString(
            "NOTE_TITLE_TEMPLATE",
            "yyyy.MM.dd HH_mm_ss"
        ) ?: "yyyy.MM.dd HH_mm_ss"
    )
    val noteTitleTemplate: StateFlow<String> = _noteTitleTemplate

    private val _isAutoSaveEnabled =
        MutableStateFlow(sharedPreferences.getBoolean("AUTO_SAVE_ENABLED", false))
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled

    private val _folderUri = MutableStateFlow(
        sharedPreferences.getString(
            "FOLDER_URI",
            application.getString(R.string.folder_not_selected)
        ) ?: application.getString(R.string.folder_not_selected)
    )
    val folderUri: StateFlow<String> = _folderUri

    private val _notificationStyle = MutableStateFlow(
        sharedPreferences.getString("NOTIFICATION_STYLE", "standard") ?: "standard"
    )
    val notificationStyle: StateFlow<String> = _notificationStyle

    fun updateShowNotification(isEnabled: Boolean) {
        viewModelScope.launch {
            _isShowNotificationEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("SHOW_NOTIFICATION", isEnabled).apply()
        }
    }

    fun updateShowOverlockScreenDialog(isEnabled: Boolean) {
        viewModelScope.launch {
            _isShowOverlockScreenDialog.value = isEnabled
            sharedPreferences.edit().putBoolean("SHOW_OVERLOCK_SCREEN_DIALOG", isEnabled).apply()
        }
    }

    fun updateDateCreatedEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isDateCreatedEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("SAVE_DATE_CREATED", isEnabled).apply()
        }
    }

    fun updatePropertyName(name: String) {
        viewModelScope.launch {
            _propertyName.value = name
            sharedPreferences.edit().putString("PROPERTY_NAME", name).apply()
        }
    }

    fun updateNoteTitleTemplate(template: String) {
        viewModelScope.launch {
            _noteTitleTemplate.value = template
            sharedPreferences.edit().putString("NOTE_TITLE_TEMPLATE", template).apply()
        }
    }

    fun updateAutoSaveEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isAutoSaveEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("AUTO_SAVE_ENABLED", isEnabled).apply()
        }
    }

    fun saveFolderUri(uri: String) {
        viewModelScope.launch {
            _folderUri.value = uri
            sharedPreferences.edit().putString("FOLDER_URI", uri).apply()
        }
    }

    fun updateNotificationStyle(style: String) {
        viewModelScope.launch {
            _notificationStyle.value = style
            sharedPreferences.edit().putString("NOTIFICATION_STYLE", style).apply()
        }
    }
}