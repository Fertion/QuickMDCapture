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
    private val handler = Handler(Looper.getMainLooper())

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
            stopSpeechRecognition()
            dismiss()
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {

            }

            override fun onBeginningOfSpeech() {
                lastPartialTextLength = 0
                val currentText = etNote.text.toString()
                if (currentText.isNotEmpty() && currentText.last() != ' ') {
                    etNote.append(" ")
                }
            }

            override fun onRmsChanged(rmsdB: Float) {

            }

            override fun onBufferReceived(buffer: ByteArray?) {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(error: Int) {
                isListening = false
                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                lastPartialTextLength = 0
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0]
                    updateNoteText(spokenText)
                    if (isAutoSaveEnabled) {
                        handler.postDelayed({
                            saveNote(etNote.text.toString())
                        }, 1000)
                    }
                }

                isListening = false

                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                lastPartialTextLength = 0
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0]
                    updateNoteText(spokenText)
                    lastPartialTextLength = spokenText.length
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {

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
        val newText = currentText.substring(0, currentText.length - lastPartialTextLength) + text
        etNote.setText(newText)
        etNote.setSelection(etNote.text.length)
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
        val noteDateTemplate = settingsViewModel.noteDateTemplate.value
        val isDateCreatedEnabled = settingsViewModel.isDateCreatedEnabled.value
        val isListItemsEnabled = settingsViewModel.isListItemsEnabled.value
        val isTimestampEnabled = settingsViewModel.isTimestampEnabled.value
        val timestampTemplate = settingsViewModel.timestampTemplate.value
        val dateCreatedTemplate = settingsViewModel.dateCreatedTemplate.value


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

            val fileName = getFileNameWithDate(noteDateTemplate)

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
                    var textToWrite = note
                    if (isListItemsEnabled) {
                        textToWrite = textToWrite.split("\n").joinToString("\n") { "- $it" }
                    }
                    if (isTimestampEnabled) {
                        textToWrite = "${getFormattedTimestamp(timestampTemplate)}\n$textToWrite"
                    }
                    outputStream.write("\n$textToWrite".toByteArray())
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
                        var dataToWrite = note
                        if (isListItemsEnabled) {
                            dataToWrite = dataToWrite.split("\n").joinToString("\n") { "- $it" }
                        }
                        if (isTimestampEnabled) {
                            dataToWrite = "${getFormattedTimestamp(timestampTemplate)}\n$dataToWrite"
                        }
                        if (isDateCreatedEnabled) {
                            val fullTimeStamp = getFormattedTimestamp(dateCreatedTemplate)
                            dataToWrite = "---\n$propertyName: $fullTimeStamp\n---\n$dataToWrite"
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
        handler.postDelayed({
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

    private fun getFormattedTimestamp(template: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            if (template[i] == '{' && i < template.length - 1 && template[i + 1] == '{') {
                i += 2
                val endIndex = template.indexOf("}}", i)
                if (endIndex != -1) {
                    val datePart = template.substring(i, endIndex)
                    val formattedDate = SimpleDateFormat(datePart, Locale.getDefault()).format(Date())
                    sb.append(formattedDate)
                    i = endIndex + 2
                } else {
                    sb.append(template[i])
                    i++
                }
            } else {
                sb.append(template[i])
                i++
            }
        }
        return sb.toString()
    }
}