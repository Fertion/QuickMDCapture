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

    private val _noteDateTemplate = MutableStateFlow(
        sharedPreferences.getString(
            "NOTE_DATE_TEMPLATE",
            "{{yyyy.MM.dd HH_mm_ss}}"
        ) ?: "{{yyyy.MM.dd HH_mm_ss}}"
    )
    val noteDateTemplate: StateFlow<String> = _noteDateTemplate

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

    private val _isListItemsEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("LIST_ITEMS_ENABLED", false)
    )
    val isListItemsEnabled: StateFlow<Boolean> = _isListItemsEnabled

    private val _isTimestampEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("TIMESTAMP_ENABLED", false)
    )
    val isTimestampEnabled: StateFlow<Boolean> = _isTimestampEnabled

    private val _timestampTemplate = MutableStateFlow(
        sharedPreferences.getString(
            "TIMESTAMP_TEMPLATE",
            "# {{yyyy.MM.dd HH:mm:ss}}"
        ) ?: "# {{yyyy.MM.dd HH:mm:ss}}"
    )
    val timestampTemplate: StateFlow<String> = _timestampTemplate

    private val _dateCreatedTemplate = MutableStateFlow(
        sharedPreferences.getString(
            "DATE_CREATED_TEMPLATE",
            "{{yyyy.MM.dd}}T{{HH:mm:ssZ}}"
        ) ?: "{{yyyy.MM.dd}}T{{HH:mm:ssZ}}"
    )
    val dateCreatedTemplate: StateFlow<String> = _dateCreatedTemplate

    private val _currentText = MutableStateFlow(sharedPreferences.getString("CURRENT_TEXT", "") ?: "")
    val currentText: StateFlow<String> = _currentText

    private val _previousText = MutableStateFlow(sharedPreferences.getString("PREVIOUS_TEXT", "") ?: "")
    val previousText: StateFlow<String> = _previousText

    private val _tempText = MutableStateFlow("")
    val tempText: StateFlow<String> = _tempText


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

    fun updateNoteDateTemplate(template: String) {
        viewModelScope.launch {
            _noteDateTemplate.value = template
            sharedPreferences.edit().putString("NOTE_DATE_TEMPLATE", template).apply()
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

    fun updateListItemsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isListItemsEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("LIST_ITEMS_ENABLED", isEnabled).apply()
        }
    }

    fun updateTimestampEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isTimestampEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("TIMESTAMP_ENABLED", isEnabled).apply()
        }
    }

    fun updateTimestampTemplate(template: String) {
        viewModelScope.launch {
            _timestampTemplate.value = template
            sharedPreferences.edit().putString("TIMESTAMP_TEMPLATE", template).apply()
        }
    }

    fun updateDateCreatedTemplate(template: String) {
        viewModelScope.launch {
            _dateCreatedTemplate.value = template
            sharedPreferences.edit().putString("DATE_CREATED_TEMPLATE", template).apply()
        }
    }

    fun updateCurrentText(text: String) {
        viewModelScope.launch {
            _currentText.value = text
            sharedPreferences.edit().putString("CURRENT_TEXT", text).apply()
        }
    }

    fun updatePreviousText(text: String) {
        viewModelScope.launch {
            _previousText.value = text
            sharedPreferences.edit().putString("PREVIOUS_TEXT", text).apply()
        }
    }

    fun updateTempText(text: String) {
        viewModelScope.launch {
            _tempText.value = text
        }
    }

    fun clearCurrentText() {
        updateCurrentText("")
    }

    fun clearPreviousText() {
        updatePreviousText("")
    }

    fun clearTempText() {
        updateTempText("")
    }
}