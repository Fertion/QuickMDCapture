package com.example.quickmdcapture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(it, takeFlags)

                    val documentFile = DocumentFile.fromTreeUri(this, it)
                    if (documentFile != null && documentFile.canWrite()) {
                        settingsViewModel.saveFolderUri(it.toString())
                        Toast.makeText(
                            this,
                            getString(R.string.folder_selected, it),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error_selecting_folder, "Folder is not writable"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_selecting_folder, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (settingsViewModel.isShowNotificationEnabled.value) {
                    startNotificationService()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.notification_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                settingsViewModel.updateShowNotification(false)
            }
        }

    private val requestOverlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    settingsViewModel.updateShowOverlockScreenDialog(true) // Включаем переключатель после получения разрешения
                } else {
                    // Разрешение не получено
                    Toast.makeText(this, getString(R.string.overlay_permission_denied), Toast.LENGTH_LONG).show()
                    settingsViewModel.updateShowOverlockScreenDialog(false)
                }
            }
        }

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            MaterialTheme {
                MainScreen(
                    onSelectFolder = { folderPicker.launch(null) },
                    settingsViewModel = settingsViewModel,
                    checkNotificationPermission = { checkNotificationPermission() },
                    showOverlayPermissionWarningDialog = { showOverlayPermissionWarningDialog() }
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (settingsViewModel.isShowNotificationEnabled.value) {
                checkNotificationPermission()
            }
        } else {
            if (settingsViewModel.isShowNotificationEnabled.value) {
                startNotificationService()
            }
        }

        if (settingsViewModel.isShowOverlockScreenDialog.value) {
            checkOverlayPermission()
        }
    }


    fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, getString(R.string.notification_service_started), Toast.LENGTH_SHORT)
            .show()
    }

    fun stopNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, getString(R.string.notification_service_stopped), Toast.LENGTH_SHORT)
            .show()
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startNotificationService()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationPermissionExplanationDialog()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                    showOverlayPermissionExplanationDialog()
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    requestOverlayPermissionLauncher.launch(intent)
                }
            }
        }
    }

    private fun showNotificationPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_needed))
            .setMessage(getString(R.string.notification_permission_explanation))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                settingsViewModel.updateShowNotification(false)
            }
            .show()
    }

    private fun showOverlayPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.overlay_permission_needed))
            .setMessage(getString(R.string.overlay_permission_explanation))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                requestOverlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                settingsViewModel.updateShowOverlockScreenDialog(false)
            }
            .show()
    }

    private fun showOverlayPermissionWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.warning))
            .setMessage(getString(R.string.overlay_permission_warning))
            .setPositiveButton(getString(R.string.im_sure)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    settingsViewModel.updateShowOverlockScreenDialog(true)
                } else {
                    checkOverlayPermission()
                }
            }
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                settingsViewModel.updateShowOverlockScreenDialog(false)
            }
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
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
    val noteTitleTemplate by settingsViewModel.noteTitleTemplate.collectAsState()
    val isAutoSaveEnabled by settingsViewModel.isAutoSaveEnabled.collectAsState()
    val currentFolderUri by settingsViewModel.folderUri.collectAsState()

    var showAddNotesMethodsInfoDialog by remember { mutableStateOf(false) }
    var showSaveSettingsInfoDialog by remember { mutableStateOf(false) }
    var showOverlaySettingsInfoDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF9E7CB2)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                // Настройки постоянного уведомления
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
                            modifier = Modifier.fillMaxWidth()
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
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            checkNotificationPermission()
                                        } else {
                                            (context as MainActivity).startNotificationService()
                                        }
                                    } else {
                                        (context as MainActivity).stopNotificationService()
                                    }
                                }
                            )
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
                                enabled = isShowNotificationEnabled // Переключатель доступен только если уведомления включены
                            )
                        }
                    }
                }
                // Куда и как сохранять
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
                            value = noteTitleTemplate,
                            onValueChange = {
                                settingsViewModel.updateNoteTitleTemplate(it)
                            },
                            label = { Text(stringResource(id = R.string.note_title_template_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // Настройки YAML
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
                // Настройки ввода
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
                // Исходный код
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.source_code_title),
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
                        if (Locale.getDefault().language == "ru") {
                            ClickableText(
                                text = stringResource(id = R.string.github_link),
                                onClick = { openLink(context, "https://github.com/Fertion/QuickMDCapture") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ClickableText(
                                text = stringResource(id = R.string.telegram_link),
                                onClick = { openLink(context, "https://t.me/for_obsidian") }
                            )
                        } else {
                            ClickableText(
                                text = stringResource(id = R.string.github_link),
                                onClick = { openLink(context, "https://github.com/Fertion/QuickMDCapture") }
                            )
                        }
                    }
                }
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

fun getFolderDisplayName(uri: String): String {
    val parsedUri = Uri.parse(uri)
    val lastSegment = parsedUri.lastPathSegment ?: "Unknown Folder"
    return lastSegment.replace("primary:", "")
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

@Composable
fun ClickableText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.Blue,
        modifier = Modifier.clickable { onClick() }
    )
}

fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}