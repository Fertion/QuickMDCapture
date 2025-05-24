package com.example.quickmdcapture

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Template-related state
    private val _templates = MutableStateFlow<List<SaveTemplate>>(loadTemplates())
    val templates: StateFlow<List<SaveTemplate>> = _templates

    private val _selectedTemplateId = MutableStateFlow(
        sharedPreferences.getString("SELECTED_TEMPLATE_ID", null)
    )
    val selectedTemplateId: StateFlow<String?> = _selectedTemplateId

    private val selectedTemplate: SaveTemplate?
        get() = templates.value.find { it.id == selectedTemplateId.value }

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

    private val _listItemIndentLevel = MutableStateFlow(
        sharedPreferences.getInt("LIST_ITEM_INDENT_LEVEL", 0)
    )
    val listItemIndentLevel: StateFlow<Int> = _listItemIndentLevel

    private val _isTimestampEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("TIMESTAMP_ENABLED", false)
    )
    val isTimestampEnabled: StateFlow<Boolean> = _isTimestampEnabled

    private val _isNoteTextInFilenameEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("NOTE_TEXT_IN_FILENAME_ENABLED", false)
    )
    val isNoteTextInFilenameEnabled: StateFlow<Boolean> = _isNoteTextInFilenameEnabled

    private val _noteTextInFilenameLength = MutableStateFlow(
        sharedPreferences.getInt("NOTE_TEXT_IN_FILENAME_LENGTH", 30)
    )
    val noteTextInFilenameLength: StateFlow<Int> = _noteTextInFilenameLength

    // Reminder settings
    private val _isReminderEnabled = MutableStateFlow(
        sharedPreferences.getBoolean("REMINDER_ENABLED", false)
    )
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled

    private val _reminderText = MutableStateFlow(
        sharedPreferences.getString(
            "REMINDER_TEXT",
            application.getString(R.string.reminder_default_text)
        ) ?: application.getString(R.string.reminder_default_text)
    )
    val reminderText: StateFlow<String> = _reminderText

    private val _reminderInterval = MutableStateFlow(
        sharedPreferences.getInt("REMINDER_INTERVAL", 60)
    )
    val reminderInterval: StateFlow<Int> = _reminderInterval

    private val _reminderStartTime = MutableStateFlow(
        sharedPreferences.getString("REMINDER_START_TIME", "09:00") ?: "09:00"
    )
    val reminderStartTime: StateFlow<String> = _reminderStartTime

    private val _reminderEndTime = MutableStateFlow(
        sharedPreferences.getString("REMINDER_END_TIME", "21:00") ?: "21:00"
    )
    val reminderEndTime: StateFlow<String> = _reminderEndTime

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

    private val _selectedTheme =
        MutableStateFlow(sharedPreferences.getString("SELECTED_THEME", "system") ?: "system")
    val selectedTheme: StateFlow<String> = _selectedTheme

    private val _theme = MutableStateFlow(
        when (_selectedTheme.value) {
            "light" -> "light"
            "dark" -> "dark"
            else -> if (isSystemInDarkTheme(application)) "dark" else "light"
        }
    )
    val theme: StateFlow<String> = _theme

    init {
        // Initialize with default template if none exists
        if (templates.value.isEmpty()) {
            val defaultTemplate = SaveTemplate(
                name = "Default",
                isDefault = true
            )
            addTemplate(defaultTemplate)
            selectTemplate(defaultTemplate.id)
        }

        // Load selected template settings
        loadSelectedTemplateSettings()

        viewModelScope.launch {
            _selectedTheme.collect {
                _theme.value = when (it) {
                    "light" -> "light"
                    "dark" -> "dark"
                    else -> if (isSystemInDarkTheme(application)) "dark" else "light"
                }
            }
        }
    }

    private fun isSystemInDarkTheme(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
    }

    private fun loadTemplates(): List<SaveTemplate> {
        val templatesJson = sharedPreferences.getString("TEMPLATES", null)
        return if (templatesJson != null) {
            val type = object : TypeToken<List<SaveTemplate>>() {}.type
            gson.fromJson(templatesJson, type)
        } else {
            emptyList()
        }
    }

    private fun saveTemplates(templates: List<SaveTemplate>) {
        sharedPreferences.edit()
            .putString("TEMPLATES", gson.toJson(templates))
            .apply()
    }

    private fun loadSelectedTemplateSettings() {
        selectedTemplate?.let { template ->
            _folderUri.value = template.folderUri
            _noteDateTemplate.value = template.noteDateTemplate
            _isListItemsEnabled.value = template.isListItemsEnabled
            _listItemIndentLevel.value = template.listItemIndentLevel
            _isTimestampEnabled.value = template.isTimestampEnabled
            _timestampTemplate.value = template.timestampTemplate
            _isDateCreatedEnabled.value = template.isDateCreatedEnabled
            _propertyName.value = template.propertyName
            _dateCreatedTemplate.value = template.dateCreatedTemplate
            _isNoteTextInFilenameEnabled.value = template.isNoteTextInFilenameEnabled
            _noteTextInFilenameLength.value = template.noteTextInFilenameLength
        }
    }

    fun addTemplate(template: SaveTemplate) {
        viewModelScope.launch {
            val updatedTemplates = templates.value.toMutableList()
            if (template.isDefault) {
                // Remove default flag from other templates
                updatedTemplates.forEach { it.copy(isDefault = false) }
            }
            updatedTemplates.add(template)
            _templates.value = updatedTemplates
            saveTemplates(updatedTemplates)
        }
    }

    fun updateTemplate(template: SaveTemplate) {
        viewModelScope.launch {
            val updatedTemplates = templates.value.map {
                if (it.id == template.id) {
                    if (template.isDefault) {
                        // Remove default flag from other templates
                        templates.value.forEach { other ->
                            if (other.id != template.id) {
                                other.copy(isDefault = false)
                            }
                        }
                    }
                    template
                } else {
                    if (template.isDefault) {
                        it.copy(isDefault = false)
                    } else {
                        it
                    }
                }
            }
            _templates.value = updatedTemplates
            saveTemplates(updatedTemplates)
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            val templateToDelete = templates.value.find { it.id == templateId }
            if (templateToDelete?.isDefault == true) {
                // Don't allow deleting the default template
                return@launch
            }

            val updatedTemplates = templates.value.filter { it.id != templateId }
            _templates.value = updatedTemplates
            saveTemplates(updatedTemplates)

            if (selectedTemplateId.value == templateId) {
                // Select the default template if the deleted template was selected
                val defaultTemplate = updatedTemplates.find { it.isDefault }
                defaultTemplate?.let { selectTemplate(it.id) }
            }
        }
    }

    fun selectTemplate(templateId: String) {
        viewModelScope.launch {
            _selectedTemplateId.value = templateId
            sharedPreferences.edit()
                .putString("SELECTED_TEMPLATE_ID", templateId)
                .apply()
            loadSelectedTemplateSettings()
        }
    }

    fun setTemplateAsDefault(templateId: String) {
        viewModelScope.launch {
            val template = templates.value.find { it.id == templateId } ?: return@launch
            updateTemplate(template.copy(isDefault = true))
        }
    }

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
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(isDateCreatedEnabled = isEnabled))
            }
        }
    }

    fun updatePropertyName(name: String) {
        viewModelScope.launch {
            _propertyName.value = name
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(propertyName = name))
            }
        }
    }

    fun updateNoteDateTemplate(template: String) {
        viewModelScope.launch {
            _noteDateTemplate.value = template
            selectedTemplate?.let { currentTemplate ->
                updateTemplate(currentTemplate.copy(noteDateTemplate = template))
            }
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
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(folderUri = uri))
            }
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
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(isListItemsEnabled = isEnabled))
            }
        }
    }

    fun updateListItemIndentLevel(level: Int) {
        viewModelScope.launch {
            _listItemIndentLevel.value = level
            sharedPreferences.edit().putInt("LIST_ITEM_INDENT_LEVEL", level).apply()
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(listItemIndentLevel = level))
            }
        }
    }

    fun updateTimestampEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isTimestampEnabled.value = isEnabled
            selectedTemplate?.let { template ->
                updateTemplate(template.copy(isTimestampEnabled = isEnabled))
            }
        }
    }

    fun updateTimestampTemplate(template: String) {
        viewModelScope.launch {
            _timestampTemplate.value = template
            selectedTemplate?.let { currentTemplate ->
                updateTemplate(currentTemplate.copy(timestampTemplate = template))
            }
        }
    }

    fun updateDateCreatedTemplate(template: String) {
        viewModelScope.launch {
            _dateCreatedTemplate.value = template
            selectedTemplate?.let { currentTemplate ->
                updateTemplate(currentTemplate.copy(dateCreatedTemplate = template))
            }
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
        _currentText.value = ""
        sharedPreferences.edit().remove("CURRENT_TEXT").apply()
    }

    fun clearPreviousText() {
        _previousText.value = ""
        sharedPreferences.edit().remove("PREVIOUS_TEXT").apply()
    }

    fun clearTempText() {
        _tempText.value = ""
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            _selectedTheme.value = theme
            sharedPreferences.edit().putString("SELECTED_THEME", theme).apply()
        }
    }

    fun getCurrentTheme(): Int {
        return when (theme.value) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    fun setNoteTextInFilenameEnabled(enabled: Boolean) {
        _isNoteTextInFilenameEnabled.value = enabled
        sharedPreferences.edit().putBoolean("NOTE_TEXT_IN_FILENAME_ENABLED", enabled).apply()
        updateSelectedTemplate()
    }

    fun setNoteTextInFilenameLength(length: Int) {
        _noteTextInFilenameLength.value = length
        sharedPreferences.edit().putInt("NOTE_TEXT_IN_FILENAME_LENGTH", length).apply()
        updateSelectedTemplate()
    }

    private fun updateSelectedTemplate() {
        selectedTemplate?.let { template ->
            updateTemplate(template.copy(
                isNoteTextInFilenameEnabled = _isNoteTextInFilenameEnabled.value,
                noteTextInFilenameLength = _noteTextInFilenameLength.value
            ))
        }
    }

    // Reminder settings update functions
    fun updateReminderEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _isReminderEnabled.value = isEnabled
            sharedPreferences.edit().putBoolean("REMINDER_ENABLED", isEnabled).apply()
        }
    }

    fun updateReminderText(text: String) {
        viewModelScope.launch {
            _reminderText.value = text
            sharedPreferences.edit().putString("REMINDER_TEXT", text).apply()
        }
    }

    fun updateReminderInterval(interval: Int) {
        viewModelScope.launch {
            _reminderInterval.value = interval
            sharedPreferences.edit().putInt("REMINDER_INTERVAL", interval).apply()
        }
    }

    fun updateReminderStartTime(time: String) {
        viewModelScope.launch {
            _reminderStartTime.value = time
            sharedPreferences.edit().putString("REMINDER_START_TIME", time).apply()
        }
    }

    fun updateReminderEndTime(time: String) {
        viewModelScope.launch {
            _reminderEndTime.value = time
            sharedPreferences.edit().putString("REMINDER_END_TIME", time).apply()
        }
    }
}