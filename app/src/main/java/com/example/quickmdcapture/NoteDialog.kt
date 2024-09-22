package com.example.quickmdcapture

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*


class NoteDialog(private val activity: AppCompatActivity, private val isAutoSaveEnabled: Boolean) :
    Dialog(activity) {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private val etNote by lazy { findViewById<EditText>(R.id.etNote) }
    private val btnSpeech by lazy { findViewById<ImageButton>(R.id.btnSpeech) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.note_dialog)

        settingsViewModel = ViewModelProvider(activity)[SettingsViewModel::class.java]

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        window?.attributes?.apply {
            width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        }

        window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)


        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

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


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Готов к распознаванию речи
            }

            override fun onBeginningOfSpeech() {
                // Начало распознавания речи
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Изменение уровня громкости звука
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Получен буфер аудиоданных
            }

            override fun onEndOfSpeech() {
                // Конец распознавания речи
            }

            override fun onError(error: Int) {
                // Произошла ошибка при распознавании речи
                Toast.makeText(context, "Ошибка распознавания речи", Toast.LENGTH_SHORT).show()
                isListening = false
                btnSpeech.setImageResource(R.drawable.ic_mic)
            }

            override fun onResults(results: Bundle?) {
                // Получены результаты распознавания речи
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0]
                    updateNoteText(spokenText)
                }
                isListening = false
                btnSpeech.setImageResource(R.drawable.ic_mic)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Получены частичные результаты распознавания речи
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Произошло событие
            }
        })

        btnSpeech.setOnClickListener {
            if (isListening) {
                stopSpeechRecognition()
            } else {
                (activity as? TransparentActivity)?.startSpeechRecognition()
            }
        }
    }


    private fun updateNoteText(text: String) {
        val currentText = etNote.text.toString()

        val newText = if (currentText.isNotEmpty()) {
            "$currentText $text"
        } else {
            text
        }

        etNote.setText(newText)
        etNote.setSelection(etNote.text.length)

        if (isAutoSaveEnabled) {
            saveNote(etNote.text.toString())
            dismiss()
        }
    }


    fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer.startListening(intent)
        isListening = true
        btnSpeech.setImageResource(R.drawable.ic_mic_off)
    }

    private fun stopSpeechRecognition() {
        speechRecognizer.stopListening()
        isListening = false
        btnSpeech.setImageResource(R.drawable.ic_mic)
    }

    private fun saveNote(note: String) {
        val folderUriString = settingsViewModel.folderUri.value
        val propertyName = settingsViewModel.propertyName.value
        val noteTitleTemplate = settingsViewModel.noteTitleTemplate.value
        val isDateCreatedEnabled = settingsViewModel.isDateCreatedEnabled.value

        if (folderUriString == context.getString(R.string.folder_not_selected)) {
            Toast.makeText(context, context.getString(R.string.folder_not_selected), Toast.LENGTH_SHORT)
                .show()
            return
        }

        try {
            val folderUri = Uri.parse(folderUriString)
            val contentResolver = context.contentResolver

            val timeStamp = SimpleDateFormat(noteTitleTemplate, Locale.getDefault()).format(Date())
            val fullTimeStamp =
                SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
            val fileName = "${timeStamp.replace(":", "_")}.md"

            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            if (documentFile == null || !documentFile.canWrite()) {
                Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT)
                    .show()
                return
            }

            val existingFile = documentFile.findFile(fileName)
            if (existingFile != null) {
                contentResolver.openOutputStream(existingFile.uri, "wa")?.use { outputStream ->
                    outputStream.write("\n$note".toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.note_appended), Toast.LENGTH_SHORT)
                    .show()
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
                    Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
        }
    }
}