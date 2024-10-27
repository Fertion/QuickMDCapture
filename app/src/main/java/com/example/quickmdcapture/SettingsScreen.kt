package com.example.quickmdcapture

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSelectFolder: () -> Unit,
    settingsViewModel: SettingsViewModel,
    checkNotificationPermission: () -> Unit,
    showOverlayPermissionWarningDialog: () -> Unit
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
    val theme by settingsViewModel.theme.collectAsState()

    var showAddNotesMethodsInfoDialog by remember { mutableStateOf(false) }
    var showSaveSettingsInfoDialog by remember { mutableStateOf(false) }
    var showOverlaySettingsInfoDialog by remember { mutableStateOf(false) }
    var expandedNotificationStyle by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }

    val textColor = if (theme == "dark") Color.LightGray else Color.Black
    val cardColors = if (theme == "dark") CardDefaults.cardColors(containerColor = Color(0xFF424242)) else CardDefaults.cardColors()

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
                    onExpandedChange = { expandedTheme = it }
                ) {
                    TextField(
                        value = when (theme) {
                            "light" -> stringResource(id = R.string.theme_light)
                            "dark" -> stringResource(id = R.string.theme_dark)
                            else -> stringResource(id = R.string.theme_system)
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTheme) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = textColor,
                            containerColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTheme,
                        onDismissRequest = { expandedTheme = false }
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

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.notification_style),
                    modifier = Modifier.fillMaxWidth(),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedNotificationStyle,
                    onExpandedChange = { expandedNotificationStyle = it }
                ) {
                    TextField(
                        value = when (notificationStyle) {
                            "standard" -> stringResource(id = R.string.notification_style_standard)
                            "expanded_with_buttons_1" -> stringResource(id = R.string.notification_style_expanded_with_buttons_1)
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNotificationStyle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = textColor,
                            containerColor = Color.Transparent
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedNotificationStyle,
                        onDismissRequest = { expandedNotificationStyle = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.notification_style_standard), color = textColor) },
                            onClick = {
                                settingsViewModel.updateNotificationStyle("standard")
                                expandedNotificationStyle = false
                                context.stopService(Intent(context, NotificationService::class.java))
                                context.startService(Intent(context, NotificationService::class.java))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.notification_style_expanded_with_buttons_1), color = textColor) },
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
                    },
                    enabled = isShowNotificationEnabled
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.save_settings_title),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = textColor
        )
        IconButton(onClick = { showSaveSettingsInfoDialog = true }) {
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
            Spacer(modifier = Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(16.dp))

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

            TextField(
                value = timestampTemplate,
                onValueChange = {
                    settingsViewModel.updateTimestampTemplate(it)
                },
                label = { Text(stringResource(id = R.string.timestamp_template_hint), color = textColor) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isTimestampEnabled,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    containerColor = Color.Transparent
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.yaml_settings_title),
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
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = propertyName,
                onValueChange = {
                    settingsViewModel.updatePropertyName(it)
                },
                enabled = isDateCreatedEnabled,
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
                enabled = isDateCreatedEnabled,
                label = { Text(stringResource(id = R.string.date_format_hint), color = textColor) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    containerColor = Color.Transparent
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
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
}

@Composable
fun ClickableText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.Blue,
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
        containerColor = MaterialTheme.colorScheme.surface
    )
}