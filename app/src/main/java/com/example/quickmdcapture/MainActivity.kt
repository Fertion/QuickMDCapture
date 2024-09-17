package com.example.quickmdcapture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.res.stringResource

class MainActivity : AppCompatActivity() {

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)

                val documentFile = DocumentFile.fromTreeUri(this, it)
                if (documentFile != null && documentFile.canWrite()) {
                    getSharedPreferences("QuickMDCapture", MODE_PRIVATE).edit()
                        .putString("FOLDER_URI", it.toString())
                        .apply()
                    Toast.makeText(this, getString(R.string.folder_selected, it), Toast.LENGTH_LONG).show()
                    currentFolderUri = it.toString() // Обновляем состояние
                } else {
                    Toast.makeText(this, getString(R.string.error_selecting_folder, "Folder is not writable"), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_selecting_folder, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startNotificationService()
        } else {
            Toast.makeText(this, getString(R.string.error_selecting_folder, "Notification permission not granted"), Toast.LENGTH_LONG).show()
        }
    }

    var currentFolderUri by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentFolderUri = getString(R.string.folder_not_selected) // Установите начальное значение

        currentFolderUri = getSelectedFolder() // Инициализация с сохраненной папкой

        setContent {
            MaterialTheme {
                MainScreen(
                    onSelectFolder = { folderPicker.launch(null) },
                    currentFolderUri = currentFolderUri
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startNotificationService()
        }
    }

    private fun getSelectedFolder(): String {
        return getSharedPreferences("QuickMDCapture", MODE_PRIVATE)
            .getString("FOLDER_URI", getString(R.string.folder_not_selected)) ?: getString(R.string.folder_not_selected)
    }

    private fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java)
        startForegroundService(serviceIntent)
        Toast.makeText(this, getString(R.string.notification_service_started), Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSelectFolder: () -> Unit,
    currentFolderUri: String
) {
    var isDateCreatedEnabled by remember { mutableStateOf(false) }
    var propertyName by remember { mutableStateOf("created") }
    var noteTitleTemplate by remember { mutableStateOf("yyyy.MM.dd HH_mm_ss") } // Изменено на "yyyy.MM.dd HH_mm_ss"

    val sharedPreferences = LocalContext.current.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        isDateCreatedEnabled = sharedPreferences.getBoolean("SAVE_DATE_CREATED", false)
        propertyName = sharedPreferences.getString("PROPERTY_NAME", "created") ?: "created"
        noteTitleTemplate = sharedPreferences.getString("NOTE_TITLE_TEMPLATE", "yyyy.MM.dd HH_mm_ss") ?: "yyyy.MM.dd HH_mm_ss" // Инициализация
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF9E7CB2)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.select_folder))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.folder_selected, getFolderDisplayName(currentFolderUri)),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Visible,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                        isDateCreatedEnabled = it
                        sharedPreferences.edit().putBoolean("SAVE_DATE_CREATED", it).apply()
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Поле для ввода названия свойства
            TextField(
                value = propertyName,
                onValueChange = {
                    propertyName = it
                    // Сохраняем новое значение в SharedPreferences
                    sharedPreferences.edit().putString("PROPERTY_NAME", it).apply()
                },
                enabled = isDateCreatedEnabled,
                label = { Text(stringResource(id = R.string.property_name_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Поле для ввода шаблона заголовка заметки
            TextField(
                value = noteTitleTemplate,
                onValueChange = {
                    noteTitleTemplate = it
                    // Сохраняем новое значение в SharedPreferences
                    sharedPreferences.edit().putString("NOTE_TITLE_TEMPLATE", it).apply()
                },
                label = { Text(stringResource(id = R.string.note_title_template_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Функция для извлечения понятного названия папки из URI
fun getFolderDisplayName(uri: String): String {
    val parsedUri = Uri.parse(uri)
    val lastSegment = parsedUri.lastPathSegment ?: "Unknown Folder"
    return lastSegment.replace("primary:", "") // Убираем "primary:"
}
