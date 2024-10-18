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

    var showAddNotesMethodsInfoDialog by remember { mutableStateOf(false) }
    var showSaveSettingsInfoDialog by remember { mutableStateOf(false) }
    var showOverlaySettingsInfoDialog by remember { mutableStateOf(false) }
    var expandedNotificationStyle by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.add_notes_methods_title),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        IconButton(onClick = { showAddNotesMethodsInfoDialog = true }) {
            Icon(Icons.Filled.Info, contentDescription = "Info")
        }
    }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
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
                    color = Color.Black,
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
                    color = Color.Black
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
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedNotificationStyle,
                        onDismissRequest = { expandedNotificationStyle = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.notification_style_standard)) },
                            onClick = {
                                settingsViewModel.updateNotificationStyle("standard")
                                expandedNotificationStyle = false
                                context.stopService(Intent(context, NotificationService::class.java))
                                context.startService(Intent(context, NotificationService::class.java))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.notification_style_expanded_with_buttons_1)) },
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
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { showOverlaySettingsInfoDialog = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Info")
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
            color = Color.Black
        )
        IconButton(onClick = { showSaveSettingsInfoDialog = true }) {
            Icon(Icons.Filled.Info, contentDescription = "Info")
        }
    }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.select_folder))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    id = R.string.folder_selected,
                    getFolderDisplayName(currentFolderUri)
                ),
                color = Color.Black,
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
                label = { Text(stringResource(id = R.string.filename_template_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.yaml_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.save_date_created),
                    color = Color.Black,
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
                label = { Text(stringResource(id = R.string.property_name_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.input_settings_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.auto_save_setting),
                    color = Color.Black,
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
        ShowInfoDialog(stringResource(id = R.string.add_notes_methods_info)) {
            showAddNotesMethodsInfoDialog = false
        }
    }

    if (showSaveSettingsInfoDialog) {
        ShowInfoDialog(stringResource(id = R.string.save_settings_info)) {
            showSaveSettingsInfoDialog = false
        }
    }

    if (showOverlaySettingsInfoDialog) {
        ShowInfoDialog(stringResource(id = R.string.overlay_permission_info)) {
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
fun ShowInfoDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.info_dialog_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.ok))
            }
        }
    )
}