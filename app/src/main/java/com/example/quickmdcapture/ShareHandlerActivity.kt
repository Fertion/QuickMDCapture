package com.example.quickmdcapture

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

class ShareHandlerActivity : AppCompatActivity() {

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        val folderUriString = settingsViewModel.folderUri.value
        if (folderUriString == getString(R.string.folder_not_selected)) {
            Toast.makeText(this, getString(R.string.folder_not_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val folderUri = Uri.parse(folderUriString)
        val documentFile = DocumentFile.fromTreeUri(this, folderUri)
        if (documentFile == null || !documentFile.canWrite()) {
            Toast.makeText(this, getString(R.string.note_error), Toast.LENGTH_SHORT).show()
            return
        }

        when (action) {
            Intent.ACTION_SEND -> {
                handleActionSend(intent, type, documentFile)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                handleActionSendMultiple(intent, type, documentFile)
            }
            Intent.ACTION_PROCESS_TEXT -> {
                handleActionProcessText(intent, documentFile)
            }
            else -> {
                // Unsupported действие
            }
        }
    }

    private fun handleActionSend(intent: Intent, type: String?, folder: DocumentFile) {
        when {
            type?.startsWith("text/") == true -> {
                val sharedText = getSharedText(intent)
                if (sharedText != null) {
                    saveTextAsNote(sharedText, folder)
                }
            }
            type?.startsWith("image/") == true || type?.startsWith("application/") == true ||
                    type?.startsWith("audio/") == true || type?.startsWith("video/") == true -> {
                val sharedUri = getSharedUri(intent)
                if (sharedUri != null) {
                    saveFile(sharedUri, folder)
                }
            }
            else -> {
                val sharedUri = getSharedUri(intent)
                if (sharedUri != null) {
                    saveFile(sharedUri, folder)
                }
            }
        }
    }

    private fun handleActionSendMultiple(intent: Intent, type: String?, folder: DocumentFile) {
        getSharedUris(intent)?.let { uris ->
            uris.forEach { uri ->
                if (type?.startsWith("text/") == true) {
                    val sharedText = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                    if (sharedText != null) {
                        saveTextAsNote(sharedText, folder)
                    }
                } else {
                    saveFile(uri, folder)
                }
            }
        }
    }

    private fun handleActionProcessText(intent: Intent, folder: DocumentFile) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
        if (sharedText != null) {
            saveTextAsNote(sharedText, folder)
        }
    }


    private fun getSharedUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun getSharedUris(intent: Intent): ArrayList<Uri>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun getSharedText(intent: Intent): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            @Suppress("DEPRECATION")
            intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    private fun saveTextAsNote(text: String, folder: DocumentFile) {
        val isDateCreatedEnabled = settingsViewModel.isDateCreatedEnabled.value
        val propertyName = settingsViewModel.propertyName.value
        val noteDateTemplate = settingsViewModel.noteDateTemplate.value


        val fileName = getFileNameWithDate(noteDateTemplate)

        val existingFile = folder.findFile(fileName)
        if (existingFile != null) {
            contentResolver.openOutputStream(existingFile.uri, "wa")?.use { outputStream ->
                outputStream.write("\n$text".toByteArray())
            }
            Toast.makeText(this, getString(R.string.note_appended), Toast.LENGTH_SHORT).show()
        } else {
            val newFile = folder.createFile("text/markdown", fileName)
            if (newFile != null) {
                contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                    val dataToWrite = if (isDateCreatedEnabled) {
                        val fullTimeStamp = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
                        "---\n$propertyName: ${fullTimeStamp}\n---\n$text"
                    } else {
                        text
                    }
                    outputStream.write(dataToWrite.toByteArray())
                }
                Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.note_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFile(       sourceUri: Uri, folder: DocumentFile) {
        val sourceDocument = DocumentFile.fromSingleUri(this, sourceUri) ?: return
        val fileName = sourceDocument.name ?: "shared_file_${System.currentTimeMillis()}"
        val destinationFile = folder.createFile(sourceDocument.type ?: "application/octet-stream", fileName)
        if (destinationFile != null) {
            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    contentResolver.openOutputStream(destinationFile.uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(this, getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.note_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.note_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameWithDate(template: String): String {
        var result = template

        var startIndex = result.indexOf("{{")
        while (startIndex != -1) {
            val endIndex = result.indexOf("}}", startIndex + 2)
            if (endIndex != -1) {
                val datePart = result.substring(startIndex + 2, endIndex)
                val formattedDate = SimpleDateFormat(datePart, Locale.getDefault()).format(Date())
                result = result.replaceRange(startIndex, endIndex + 2, formattedDate)
                startIndex = result.indexOf("{{", endIndex)
            } else {
                startIndex = -1
            }
        }


        return "${result.replace(":", "_")}.md"
    }
}