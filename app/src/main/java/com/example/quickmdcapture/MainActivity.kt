package com.example.quickmdcapture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*

class MainActivity : AppCompatActivity() {

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        contentResolver.takePersistableUriPermission(it, takeFlags)
                    }

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
                    settingsViewModel.updateShowOverlockScreenDialog(true)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.overlay_permission_denied),
                        Toast.LENGTH_LONG
                    ).show()
                    settingsViewModel.updateShowOverlockScreenDialog(false)
                }
            }
        }

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val currentTheme = settingsViewModel.getCurrentTheme()
            AppCompatDelegate.setDefaultNightMode(currentTheme)

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

        // Start reminder service if enabled
        if (settingsViewModel.isReminderEnabled.value) {
            startReminderService()
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

    fun startReminderService() {
        val serviceIntent = Intent(this, ReminderService::class.java)
        startService(serviceIntent)
    }

    fun stopReminderService() {
        val serviceIntent = Intent(this, ReminderService::class.java)
        stopService(serviceIntent)
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
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
    val coroutineScope = rememberCoroutineScope()
    var latestRelease by remember { mutableStateOf<Release?>(null) }
    var currentVersion by remember { mutableStateOf<String?>(null) }
    val theme by settingsViewModel.theme.collectAsState()

    // Add scroll state for the entire screen
    val scrollState = rememberLazyListState()
    
    // Track scrolling state with debounce
    var isScrolling by remember { mutableStateOf(false) }
    
    // Update scrolling state based on LazyColumn scroll with debounce
    LaunchedEffect(scrollState.isScrollInProgress) {
        isScrolling = scrollState.isScrollInProgress
        if (!scrollState.isScrollInProgress) {
            // Add 100ms debounce after scrolling ends
            delay(100)
            isScrolling = false
        }
    }

    fun checkLatestRelease() {
        coroutineScope.launch {
            currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val versionedPackage = VersionedPackage(context.packageName, 0)
                val packageInfoFlags = PackageManager.PackageInfoFlags.of(0L)
                context.packageManager.getPackageInfo(versionedPackage, packageInfoFlags).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val gitHubDataService = retrofit.create(GitHubDataService::class.java)
            gitHubDataService.getLatestRelease().enqueue(object : Callback<Release?> {
                override fun onResponse(call: Call<Release?>, response: Response<Release?>) {
                    if (response.isSuccessful) {
                        latestRelease = response.body()
                    }
                }

                override fun onFailure(call: Call<Release?>, t: Throwable) {
                    // Handle error
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        checkLatestRelease()
    }

    val backgroundColor = when (theme) {
        "light" -> Color(0xFF9E7CB2)
        "dark" -> Color(0xFF303030)
        else -> Color(0xFF9E7CB2)
    }

    val textColor = if (theme == "dark") Color.LightGray else Color.Black
    val cardColors =
        if (theme == "dark") CardDefaults.cardColors(containerColor = Color(0xFF424242)) else CardDefaults.cardColors()

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            state = scrollState
        ) {
            item {
                SettingsScreen(
                    onSelectFolder = onSelectFolder,
                    settingsViewModel = settingsViewModel,
                    checkNotificationPermission = checkNotificationPermission,
                    showOverlayPermissionWarningDialog = showOverlayPermissionWarningDialog,
                    isScrolling = isScrolling
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.source_code_title),
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
                        Text(
                            stringResource(id = R.string.current_version, currentVersion ?: "Unknown"),
                            color = textColor
                        )
                        Text(
                            stringResource(
                                id = R.string.latest_version,
                                latestRelease?.tag_name ?: "Unknown"
                            ),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ClickableText(
                            text = stringResource(id = R.string.github_link),
                            onClick = {
                                openLink(
                                    context,
                                    "https://github.com/Fertion/QuickMDCapture"
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (Locale.getDefault().language == "ru") {
                            ClickableText(
                                text = stringResource(id = R.string.telegram_link),
                                onClick = { openLink(context, "https://t.me/for_obsidian") }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getFolderDisplayName(uri: String): String {
    val parsedUri = Uri.parse(uri)
    val lastSegment = parsedUri.lastPathSegment ?: "Unknown Folder"
    return lastSegment.replace("primary:", "")
}

fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

interface GitHubDataService {
    @GET("repos/Fertion/QuickMDCapture/releases/latest")
    fun getLatestRelease(): Call<Release?>
}

data class Release(
    val tag_name: String,
    val assets: List<Asset>
)

data class Asset(
    val name: String
)