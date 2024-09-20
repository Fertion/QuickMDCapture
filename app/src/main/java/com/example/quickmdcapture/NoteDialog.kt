package com.example.quickmdcapture

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDialog(private val activity: AppCompatActivity, private val isAutoSaveEnabled: Boolean) : Dialog(activity) {

    private lateinit var speechRecognizerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.note_dialog)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        window?.attributes?.apply {
            width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        }

        window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)

        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnSpeech = findViewById<ImageButton>(R.id.btnSpeech)

        etNote.requestFocus()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        btnSave.setOnClickListener {
            val note = etNote.text.toString()
            if (note.isNotEmpty()) {
                saveNote(note)
            }
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        speechRecognizerLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                    val results: ArrayList<String>? =
                        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = results?.get(0) ?: ""
                    updateNoteText(spokenText)
                }
            }

        btnSpeech.setOnClickListener {
            startSpeechRecognition()
        }
    }

    private fun updateNoteText(text: String) {
        val etNote = findViewById<EditText>(R.id.etNote)
        val currentText = etNote.text.toString()
        etNote.setText("$currentText $text")

        if (isAutoSaveEnabled) {
            saveNote(etNote.text.toString())
            dismiss() // Закрываем диалог после автосохранения
        }
    }

    fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        speechRecognizerLauncher.launch(intent)
    }

    private fun saveNote(note: String) {
        val folderUriString = context.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)
            .getString("FOLDER_URI", null)
        val propertyName = context.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)
            .getString("PROPERTY_NAME", "created")

        val noteTitleTemplate = context.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)
            .getString("NOTE_TITLE_TEMPLATE", "yyyy.MM.dd HH_mm_ss")

        val isDateCreatedEnabled = context.getSharedPreferences("QuickMDCapture", Context.MODE_PRIVATE)
            .getBoolean("SAVE_DATE_CREATED", false)

        if (folderUriString == null) {
            Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val folderUri = Uri.parse(folderUriString)
            val contentResolver = context.contentResolver

            val timeStamp = SimpleDateFormat(noteTitleTemplate, Locale.getDefault()).format(Date())
            val fullTimeStamp = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
            val fileName = "${timeStamp.replace(":", "_")}.md"

            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            if (documentFile == null || !documentFile.canWrite()) {
                Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
                return
            }

            val existingFile = documentFile.findFile(fileName)
            if (existingFile != null) {
                contentResolver.openOutputStream(existingFile.uri, "wa")?.use { outputStream ->
                    outputStream.write("\n$note".toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.note_appended), Toast.LENGTH_SHORT).show()
            } else {
                val fileDoc = documentFile.createFile("text/markdown", fileName)
                if (fileDoc != null) {
                    contentResolver.openOutputStream(fileDoc.uri)?.use { outputStream ->
                        val dataToWrite = if (isDateCreatedEnabled) {
                            "---\n$propertyName: ${fullTimeStamp}\n---\n$note"
                        } else {
                            note
                        }
                        outputStream.write(dataToWrite.toByteArray())
                    }
                    Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
        }
    }
}