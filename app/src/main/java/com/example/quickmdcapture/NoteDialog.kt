package com.example.quickmdcapture

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var lastPartialTextLength = 0

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
            } else {
                dismissWithMessage(context.getString(R.string.note_error))
            }
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
                lastPartialTextLength = 0
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
                dismissWithMessage(context.getString(R.string.note_error))
                isListening = false
                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                lastPartialTextLength = 0
            }

            override fun onResults(results: Bundle?) {
                // Получены результаты распознавания речи
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0]
                    updateNoteText(spokenText)
                    if (isAutoSaveEnabled && !isListening) {
                        saveNote(etNote.text.toString())
                    }
                }

                isListening = false

                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                lastPartialTextLength = 0
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Получены частичные результаты распознавания речи
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0]
                    updateNoteText(spokenText)
                    lastPartialTextLength = spokenText.length
                }
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

        val newText = if (currentText.length > lastPartialTextLength) {
            currentText.substring(0, currentText.length - lastPartialTextLength) + text
        } else {
            text
        }


        etNote.setText(newText)
        etNote.setSelection(etNote.text.length)

        if (isAutoSaveEnabled && !isListening) {
            saveNote(etNote.text.toString())
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
        btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic_on))
    }

    private fun stopSpeechRecognition() {
        speechRecognizer.stopListening()
        isListening = false
        btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
        lastPartialTextLength = 0
    }

    private fun saveNote(note: String) {
        val folderUriString = settingsViewModel.folderUri.value
        val propertyName = settingsViewModel.propertyName.value
        val noteTitleTemplate = settingsViewModel.noteTitleTemplate.value
        val isDateCreatedEnabled = settingsViewModel.isDateCreatedEnabled.value

        if (folderUriString == context.getString(R.string.folder_not_selected)) {
            if (isScreenLocked()) {
                dismissWithMessage(context.getString(R.string.folder_not_selected))
            } else {
                Toast.makeText(context, context.getString(R.string.folder_not_selected), Toast.LENGTH_SHORT).show()
                dismiss()
            }
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
                if (isScreenLocked()) {
                    dismissWithMessage(context.getString(R.string.note_error))
                } else {
                    Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                return
            }

            val existingFile = documentFile.findFile(fileName)
            if (existingFile != null) {
                contentResolver.openOutputStream(existingFile.uri, "wa")?.use { outputStream ->
                    outputStream.write("\n$note".toByteArray())
                }
                if (isScreenLocked()) {
                    dismissWithMessage(context.getString(R.string.note_appended))
                } else {
                    Toast.makeText(context, context.getString(R.string.note_appended), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
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
                    if (isScreenLocked()) {
                        dismissWithMessage(context.getString(R.string.note_saved))
                    } else {
                        Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                } else {
                    if (isScreenLocked()) {
                        dismissWithMessage(context.getString(R.string.note_error))
                    } else {
                        Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
        } catch (e: Exception) {
            if (isScreenLocked()) {
                dismissWithMessage(context.getString(R.string.note_error))
            } else {
                Toast.makeText(context, context.getString(R.string.note_error), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun dismissWithMessage(message: String) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etNote.windowToken, 0)
        etNote.setText(message)
        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 1000)
    }

    private fun isScreenLocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            keyguardManager.isKeyguardLocked
        } else {
            keyguardManager.isKeyguardSecure
        }
    }
}