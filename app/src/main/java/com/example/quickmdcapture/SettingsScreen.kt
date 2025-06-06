package com.example.quickmdcapture

import android.app.TimePickerDialog
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSelectFolder: () -> Unit,
    settingsViewModel: SettingsViewModel,
    checkNotificationPermission: () -> Unit,
    showOverlayPermissionWarningDialog: () -> Unit,
    isScrolling: Boolean
) {
    val context = LocalContext.current

    val isShowNotificationEnabled by settingsViewModel.isShowNotificationEnabled.collectAsState()
    val isShowOverlockScreenDialog by settingsViewModel.isShowOverlockScreenDialog.collectAsState()
    val isDateCreatedEnabled by settingsViewModel.isDateCreatedEnabled.collectAsState()
    val propertyName by settingsViewModel.propertyName.collectAsState()
    val noteDateTemplate by settingsViewModel.noteDateTemplate.collectAsState()
    val isAutoSaveEnabled by settingsViewModel.isAutoSaveEnabled.collectAsState()
    val currentFolderUri by settingsViewModel.folderUri.collectAsState()
    val notificationStyle by settingsViewModel.notificationStyle.collectAsState()
    val isListItemsEnabled by settingsViewModel.isListItemsEnabled.collectAsState()
    val isTimestampEnabled by settingsViewModel.isTimestampEnabled.collectAsState()
    val timestampTemplate by settingsViewModel.timestampTemplate.collectAsState()
    val dateCreatedTemplate by settingsViewModel.dateCreatedTemplate.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val theme by settingsViewModel.theme.collectAsState()
    val isNoteTextInFilenameEnabled by settingsViewModel.isNoteTextInFilenameEnabled.collectAsState()
    val noteTextInFilenameLength by settingsViewModel.noteTextInFilenameLength.collectAsState()

    // Reminder settings state
    val isReminderEnabled by settingsViewModel.isReminderEnabled.collectAsState()
    val reminderText by settingsViewModel.reminderText.collectAsState()
    val reminderInterval by settingsViewModel.reminderInterval.collectAsState()
    val reminderStartTime by settingsViewModel.reminderStartTime.collectAsState()
    val reminderEndTime by settingsViewModel.reminderEndTime.collectAsState()

    // Function to restart reminder service
    fun restartReminderService() {
        context.stopService(Intent(context, NotificationService::class.java))
        if (isShowNotificationEnabled) {
            context.startService(Intent(context, NotificationService::class.java))
        }
    }

    // Template management state
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showRenameTemplateDialog by remember { mutableStateOf(false) }
    var showDeleteTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplateForRename by remember { mutableStateOf<SaveTemplate?>(null) }
    var selectedTemplateForDelete by remember { mutableStateOf<SaveTemplate?>(null) }

    val templates by settingsViewModel.templates.collectAsState()
    val selectedTemplateId by settingsViewModel.selectedTemplateId.collectAsState()
    val selectedReminderTemplateId by settingsViewModel.selectedReminderTemplateId.collectAsState()
    var expandedTemplates by remember { mutableStateOf(false) }

    var showAddNotesMethodsInfoDialog by remember { mutableStateOf(false) }
    var showSaveSettingsInfoDialog by remember { mutableStateOf(false) }
    var showOverlaySettingsInfoDialog by remember { mutableStateOf(false) }
    var expandedNotificationStyle by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }

    val textColor = if (theme == "dark") Color.LightGray else Color.Black
    val cardColors =
        if (theme == "dark") CardDefaults.cardColors(containerColor = Color(0xFF424242)) else CardDefaults.cardColors()
    val dropdownMenuBackgroundColor = if (theme == "dark") Color(0xFF2D2D2D) else Color.White

    // Add debounced scrolling state
    var debouncedIsScrolling by remember { mutableStateOf(false) }
    
    // Update debounced scrolling state with delay
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            debouncedIsScrolling = true
        } else {
            delay(300) // Increased delay to prevent accidental taps
            debouncedIsScrolling = false
        }
    }

    // Общие настройки
    Text(
        text = stringResource(id = R.string.general_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = textColor
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Настройка темы
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.theme_setting),
                    modifier = Modifier.fillMaxWidth(),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedTheme,
                    onExpandedChange = { newExpanded ->
                        if (!debouncedIsScrolling) {
                            expandedTheme = newExpanded
                        }
                    }
                ) {
                    TextField(
                        value = when (selectedTheme) {
                            "light" -> stringResource(id = R.string.theme_light)
                            "dark" -> stringResource(id = R.string.theme_dark)
                            else -> stringResource(id = R.string.theme_system)
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTheme)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = textColor,
                            containerColor = Color.Transparent
                        )
                    )
                    // Устанавливаем фон для ExposedDropdownMenu
                    ExposedDropdownMenu(
                        expanded = expandedTheme,
                        onDismissRequest = { expandedTheme = false },
                        modifier = Modifier.background(dropdownMenuBackgroundColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.theme_light), color = textColor) },
                            onClick = {
                                settingsViewModel.updateTheme("light")
                                expandedTheme = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.theme_dark), color = textColor) },
                            onClick = {
                                settingsViewModel.updateTheme("dark")
                                expandedTheme = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.theme_system), color = textColor) },
                            onClick = {
                                settingsViewModel.updateTheme("system")
                                expandedTheme = false
                            }
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.add_notes_methods_title),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = textColor
        )
        IconButton(onClick = { showAddNotesMethodsInfoDialog = true }) {
            Icon(Icons.Filled.Info, contentDescription = "Info", tint = textColor)
        }
    }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    stringResource(id = R.string.add_notes_via_notification),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isShowNotificationEnabled,
                    onCheckedChange = { isChecked ->
                        settingsViewModel.updateShowNotification(isChecked)
                        if (isChecked) {
                            checkNotificationPermission()
                        } else {
                            context.stopService(Intent(context, NotificationService::class.java))
                        }
                    }
                )
            }

            if (isShowNotificationEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.notification_style),
                        modifier = Modifier.fillMaxWidth(),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedNotificationStyle,
                        onExpandedChange = { newExpanded ->
                            if (!debouncedIsScrolling) {
                                expandedNotificationStyle = newExpanded
                            }
                        }
                    ) {
                        TextField(
                            value = when (notificationStyle) {
                                "standard" -> stringResource(id = R.string.notification_style_standard)
                                "expanded_with_buttons_1" -> stringResource(id = R.string.notification_style_expanded_with_buttons_1)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNotificationStyle)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.textFieldColors(
                                textColor = textColor,
                                containerColor = Color.Transparent
                            )
                        )
                        // Устанавливаем фон для ExposedDropdownMenu
                        ExposedDropdownMenu(
                            expanded = expandedNotificationStyle,
                            onDismissRequest = { expandedNotificationStyle = false },
                            modifier = Modifier.background(dropdownMenuBackgroundColor)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = R.string.notification_style_standard),
                                        color = textColor
                                    )
                                },
                                onClick = {
                                    settingsViewModel.updateNotificationStyle("standard")
                                    expandedNotificationStyle = false
                                    context.stopService(Intent(context, NotificationService::class.java))
                                    context.startService(Intent(context, NotificationService::class.java))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = R.string.notification_style_expanded_with_buttons_1),
                                        color = textColor
                                    )
                                },
                                onClick = {
                                    settingsViewModel.updateNotificationStyle("expanded_with_buttons_1")
                                    expandedNotificationStyle = false
                                    context.stopService(Intent(context, NotificationService::class.java))
                                    context.startService(Intent(context, NotificationService::class.java))
                                }
                            )
                        }
                    }
                }

                // Remove divider after notification style dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.show_overlock_screen_dialog),
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { showOverlaySettingsInfoDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isShowOverlockScreenDialog,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                showOverlayPermissionWarningDialog()
                            } else {
                                settingsViewModel.updateShowOverlockScreenDialog(isChecked)
                            }
                        }
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.save_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = textColor
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Templates Section
            Text(
                text = stringResource(id = R.string.templates_title),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expandedTemplates,
                onExpandedChange = { newExpanded ->
                    if (!debouncedIsScrolling) {
                        expandedTemplates = newExpanded
                    }
                }
            ) {
                TextField(
                    value = templates.find { it.id == selectedTemplateId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTemplates)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedTemplates,
                    onDismissRequest = { expandedTemplates = false },
                    modifier = Modifier.background(dropdownMenuBackgroundColor)
                ) {
                    templates.forEach { template ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (template.isDefault) Icons.Default.Star else Icons.Default.FavoriteBorder,
                                        contentDescription = if (template.isDefault) stringResource(id = R.string.default_template) else stringResource(id = R.string.set_as_default),
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(template.name, color = textColor)
                                }
                            },
                            onClick = {
                                settingsViewModel.selectTemplate(template.id)
                                expandedTemplates = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Default Template Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = templates.find { it.id == selectedTemplateId }?.isDefault == true,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            settingsViewModel.setTemplateAsDefault(selectedTemplateId ?: return@Checkbox)
                        }
                    },
                    enabled = templates.find { it.id == selectedTemplateId }?.isDefault != true
                )
                Text(
                    text = stringResource(id = R.string.set_as_default),
                    color = textColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Template Management Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showAddTemplateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_template))
                }

                Button(
                    onClick = {
                        selectedTemplateForRename = templates.find { it.id == selectedTemplateId }
                        newTemplateName = TextFieldValue(selectedTemplateForRename?.name ?: "")
                        showRenameTemplateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.rename_template))
                }

                Button(
                    onClick = {
                        selectedTemplateForDelete = templates.find { it.id == selectedTemplateId }
                        showDeleteTemplateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    ),
                    enabled = templates.find { it.id == selectedTemplateId }?.isDefault != true,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_template))
                }
            }

            // Add divider after template management buttons
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                thickness = 2.dp,
                color = if (theme == "dark") Color.LightGray else Color.DarkGray
            )

            // Save Location Section
            Text(
                text = stringResource(id = R.string.save_location_section_title),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                    contentColor = if (theme == "dark") Color.LightGray else Color.White
                )
            ) {
                Text(stringResource(id = R.string.select_folder))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = R.string.folder_selected,
                    getFolderDisplayName(currentFolderUri)
                ),
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Visible,
                maxLines = 2
            )

            // Add divider after folder selection
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                thickness = 2.dp,
                color = if (theme == "dark") Color.LightGray else Color.DarkGray
            )

            // File Format Section
            Text(
                text = stringResource(id = R.string.file_format_section_title),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = noteDateTemplate,
                onValueChange = {
                    settingsViewModel.updateNoteDateTemplate(it)
                },
                label = { Text(stringResource(id = R.string.filename_template_hint), color = textColor) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    containerColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.use_note_text_in_filename),
                    modifier = Modifier.weight(1f),
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isNoteTextInFilenameEnabled,
                    onCheckedChange = { settingsViewModel.setNoteTextInFilenameEnabled(it) }
                )
            }

            if (isNoteTextInFilenameEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.note_text_in_filename_length),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                var noteTextLengthInput by remember { mutableStateOf(noteTextInFilenameLength.toString()) }
                TextField(
                    value = noteTextLengthInput,
                    onValueChange = { value ->
                        noteTextLengthInput = value
                        when {
                            value.isEmpty() -> {
                                settingsViewModel.setNoteTextInFilenameLength(1)
                            }
                            value.toIntOrNull() != null -> {
                                val number = value.toInt()
                                if (number > 0) {
                                    settingsViewModel.setNoteTextInFilenameLength(number)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    ),
                    placeholder = {
                        Text(
                            text = "1",
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                )
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                thickness = 2.dp,
                color = if (theme == "dark") Color.LightGray else Color.DarkGray
            )

            Text(
                text = stringResource(id = R.string.text_format_section_title),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.save_as_list_items),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isListItemsEnabled,
                    onCheckedChange = {
                        settingsViewModel.updateListItemsEnabled(it)
                    }
                )
            }

            if (isListItemsEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.list_item_indent_level),
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val indentLevels = listOf(0, 1, 2, 3, 4, 5)
                    val currentIndentLevel = settingsViewModel.listItemIndentLevel.collectAsState().value

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textColor
                            )
                        ) {
                            Text(
                                when (currentIndentLevel) {
                                    0 -> stringResource(id = R.string.no_indent)
                                    else -> stringResource(id = R.string.indent_level, currentIndentLevel)
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(dropdownMenuBackgroundColor)
                        ) {
                            indentLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (level) {
                                                0 -> stringResource(id = R.string.no_indent)
                                                else -> stringResource(id = R.string.indent_level, level)
                                            },
                                            color = textColor,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    },
                                    onClick = {
                                        settingsViewModel.updateListItemIndentLevel(level)
                                        expanded = false
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.add_timestamp),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isTimestampEnabled,
                    onCheckedChange = {
                        settingsViewModel.updateTimestampEnabled(it)
                    }
                )
            }

            if (isTimestampEnabled) {
                TextField(
                    value = timestampTemplate,
                    onValueChange = {
                        settingsViewModel.updateTimestampTemplate(it)
                    },
                    label = { Text(stringResource(id = R.string.timestamp_template_hint), color = textColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                thickness = 2.dp,
                color = if (theme == "dark") Color.LightGray else Color.DarkGray
            )


            Text(
                text = stringResource(id = R.string.yaml_settings_title),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.save_date_created),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isDateCreatedEnabled,
                    onCheckedChange = {
                        settingsViewModel.updateDateCreatedEnabled(it)
                    }
                )
            }

            if (isDateCreatedEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = propertyName,
                    onValueChange = {
                        settingsViewModel.updatePropertyName(it)
                    },
                    label = { Text(stringResource(id = R.string.property_name_hint), color = textColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = dateCreatedTemplate,
                    onValueChange = {
                        settingsViewModel.updateDateCreatedTemplate(it)
                    },
                    label = { Text(stringResource(id = R.string.date_format_hint), color = textColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Input Settings Section
    Text(
        text = stringResource(id = R.string.input_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = textColor
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.auto_save_setting),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isAutoSaveEnabled,
                    onCheckedChange = {
                        settingsViewModel.updateAutoSaveEnabled(it)
                    }
                )
            }
        }
    }

    // Reminder Settings Section
    Text(
        text = stringResource(id = R.string.reminder_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = textColor
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.enable_reminders),
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { isChecked ->
                        settingsViewModel.updateReminderEnabled(isChecked)
                        // Restart NotificationService to handle reminder service
                        restartReminderService()
                    }
                )
            }

            if (isReminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = reminderText,
                    onValueChange = { settingsViewModel.updateReminderText(it) },
                    label = { Text(stringResource(id = R.string.reminder_text), color = textColor) },
                    placeholder = { Text(stringResource(id = R.string.reminder_text_hint), color = textColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                var reminderIntervalInput by remember { mutableStateOf(reminderInterval.toString()) }
                TextField(
                    value = reminderIntervalInput,
                    onValueChange = { value ->
                        reminderIntervalInput = value
                        when {
                            value.isEmpty() -> {
                                settingsViewModel.updateReminderInterval(1)
                                restartReminderService()
                            }
                            value.toIntOrNull() != null -> {
                                val number = value.toInt()
                                if (number > 0) {
                                    settingsViewModel.updateReminderInterval(number)
                                    restartReminderService()
                                }
                            }
                        }
                    },
                    label = { Text(stringResource(id = R.string.reminder_interval), color = textColor) },
                    placeholder = { Text("1", color = textColor.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(id = R.string.reminder_start_time),
                            color = textColor
                        )
                        OutlinedButton(
                            onClick = {
                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    TimePickerDialog.OnTimeSetListener { _, hour: Int, minute: Int ->
                                        val time = String.format("%02d:%02d", hour, minute)
                                        settingsViewModel.updateReminderStartTime(time)
                                        restartReminderService()
                                    },
                                    reminderStartTime.split(":")[0].toInt(),
                                    reminderStartTime.split(":")[1].toInt(),
                                    true
                                )
                                timePickerDialog.show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textColor
                            )
                        ) {
                            Text(reminderStartTime)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(id = R.string.reminder_end_time),
                            color = textColor
                        )
                        OutlinedButton(
                            onClick = {
                                val timePickerDialog = TimePickerDialog(
                                    context,
                                    TimePickerDialog.OnTimeSetListener { _, hour: Int, minute: Int ->
                                        val time = String.format("%02d:%02d", hour, minute)
                                        settingsViewModel.updateReminderEndTime(time)
                                        restartReminderService()
                                    },
                                    reminderEndTime.split(":")[0].toInt(),
                                    reminderEndTime.split(":")[1].toInt(),
                                    true
                                )
                                timePickerDialog.show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textColor
                            )
                        ) {
                            Text(reminderEndTime)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default template dropdown
                var expandedReminderTemplate by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedReminderTemplate,
                    onExpandedChange = { expandedReminderTemplate = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = templates.find { it.id == selectedReminderTemplateId }?.name 
                            ?: templates.find { it.isDefault }?.name 
                            ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.reminder_default_template), color = textColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReminderTemplate) },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = textColor,
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedReminderTemplate,
                        onDismissRequest = { expandedReminderTemplate = false },
                        modifier = Modifier
                            .exposedDropdownSize()
                            .background(dropdownMenuBackgroundColor)
                    ) {
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name, color = textColor) },
                                onClick = {
                                    settingsViewModel.selectReminderTemplate(template.id)
                                    expandedReminderTemplate = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddNotesMethodsInfoDialog) {
        ShowInfoDialog(stringResource(id = R.string.add_notes_methods_info), theme) {
            showAddNotesMethodsInfoDialog = false
        }
    }

    if (showSaveSettingsInfoDialog) {
        ShowInfoDialog(stringResource(id = R.string.save_settings_info), theme) {
            showSaveSettingsInfoDialog = false
        }
    }

    if (showOverlaySettingsInfoDialog) {
        ShowInfoDialog(stringResource(id = R.string.overlay_permission_info), theme) {
            showOverlaySettingsInfoDialog = false
        }
    }

    // Template Management Dialogs
    if (showAddTemplateDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { showAddTemplateDialog = false },
            title = { Text(stringResource(id = R.string.add_template_dialog_title), color = textColor) },
            text = {
                TextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text(stringResource(id = R.string.template_name_hint), color = textColor) },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateName.text.isNotBlank()) {
                            settingsViewModel.addTemplate(
                                SaveTemplate(
                                    name = newTemplateName.text,
                                    isDefault = templates.isEmpty()
                                )
                            )
                            newTemplateName = TextFieldValue("")
                            showAddTemplateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.add))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        newTemplateName = TextFieldValue("")
                        showAddTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            containerColor = if (theme == "dark") Color(0xFF424242) else Color.White
        )
    }

    if (showRenameTemplateDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { showRenameTemplateDialog = false },
            title = { Text(stringResource(id = R.string.rename_template_dialog_title), color = textColor) },
            text = {
                TextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text(stringResource(id = R.string.template_name_hint), color = textColor) },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = textColor,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTemplateName.text.isNotBlank()) {
                            selectedTemplateForRename?.let { template ->
                                settingsViewModel.updateTemplate(
                                    template.copy(name = newTemplateName.text)
                                )
                            }
                            newTemplateName = TextFieldValue("")
                            showRenameTemplateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.rename))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        newTemplateName = TextFieldValue("")
                        showRenameTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            containerColor = if (theme == "dark") Color(0xFF424242) else Color.White
        )
    }

    if (showDeleteTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTemplateDialog = false },
            title = { Text(stringResource(id = R.string.delete_template_dialog_title), color = textColor) },
            text = {
                Text(
                    stringResource(
                        id = R.string.delete_template_dialog_message,
                        selectedTemplateForDelete?.name ?: ""
                    ),
                    color = textColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTemplateForDelete?.let { template ->
                            settingsViewModel.deleteTemplate(template.id)
                        }
                        showDeleteTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteTemplateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                        contentColor = if (theme == "dark") Color.LightGray else Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            containerColor = if (theme == "dark") Color(0xFF424242) else Color.White
        )
    }
}

@Composable
fun ClickableText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFFD235D2),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun ShowInfoDialog(message: String, theme: String, onDismiss: () -> Unit) {
    val textColor = if (theme == "dark") Color.LightGray else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.info_dialog_title), color = textColor) },
        text = { Text(message, color = textColor) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme == "dark") Color(0xFF616161) else Color(0xFF9E7CB2),
                    contentColor = if (theme == "dark") Color.LightGray else Color.White
                )
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        containerColor = if (theme == "dark") Color(0xFF424242) else Color.White
    )
}