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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

class NoteDialog(
    private val activity: AppCompatActivity, 
    private val isAutoSaveEnabled: Boolean,
    private val isFromReminder: Boolean
) : Dialog(activity) {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private val etNote by lazy { findViewById<EditText>(R.id.etNote) }
    private val btnSpeech by lazy { findViewById<ImageButton>(R.id.btnSpeech) }
    private val btnRestore by lazy { findViewById<ImageButton>(R.id.btnRestore) }
    private val templateSpinner by lazy { findViewById<Spinner>(R.id.templateSpinner) }
    private var lastPartialTextLength = 0
    private val handler = Handler(Looper.getMainLooper())

    private inner class TemplateAdapter(
        context: Context,
        private val items: List<String>,
        private val textColor: Int
    ) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            (view as TextView).apply {
                setTextColor(textColor)
                gravity = android.view.Gravity.CENTER
            }
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            (view as TextView).apply {
                setTextColor(textColor)
                gravity = android.view.Gravity.CENTER
                setPadding(paddingLeft, 24, paddingRight, 24)
            }
            return view
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(activity)[SettingsViewModel::class.java]

        if (settingsViewModel.currentText.value.isNotEmpty()) {
            settingsViewModel.updatePreviousText(settingsViewModel.currentText.value)
            settingsViewModel.clearCurrentText()
        }

        setContentView(R.layout.note_dialog)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        window?.attributes?.apply {
            width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        }

        window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Setup template spinner
        val templates = settingsViewModel.templates.value
        
        // Apply theme
        val theme = settingsViewModel.theme.value
        val dialogLayout = findViewById<LinearLayout>(R.id.noteDialogLayout)
        val buttonBackground = ContextCompat.getDrawable(context, R.drawable.rounded_button_background)
        when (theme) {
            "light" -> {
                dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background)
                etNote.setTextColor(ContextCompat.getColor(context, R.color.black))
                etNote.setHintTextColor(ContextCompat.getColor(context, R.color.black))
                btnSpeech.background = buttonBackground
                btnRestore.background = buttonBackground
                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background)
                templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background)
                templateSpinner.adapter = TemplateAdapter(
                    context,
                    templates.map { it.name },
                    ContextCompat.getColor(context, R.color.black)
                )
            }
            "dark" -> {
                dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                etNote.setTextColor(ContextCompat.getColor(context, R.color.light_gray))
                etNote.setHintTextColor(ContextCompat.getColor(context, R.color.light_gray))
                btnSpeech.background = buttonBackground
                btnRestore.background = buttonBackground
                val micDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mic)
                DrawableCompat.setTint(micDrawable!!, ContextCompat.getColor(context, R.color.light_gray))
                btnSpeech.setImageDrawable(micDrawable)
                templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background_dark)
                templateSpinner.adapter = TemplateAdapter(
                    context,
                    templates.map { it.name },
                    ContextCompat.getColor(context, R.color.white)
                )

                // Изменение цвета каемки кнопки
                if (buttonBackground != null) {
                    val strokeColor = ContextCompat.getColor(context, R.color.dark_gray)
                    DrawableCompat.setTint(DrawableCompat.wrap(buttonBackground).mutate(), strokeColor)
                    btnSpeech.background = buttonBackground
                    btnRestore.background = buttonBackground
                }
            }
            else -> {
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> {
                        dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        etNote.setTextColor(ContextCompat.getColor(context, R.color.light_gray))
                        etNote.setHintTextColor(ContextCompat.getColor(context, R.color.light_gray))
                        btnSpeech.background = buttonBackground
                        btnRestore.background = buttonBackground
                        val micDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mic)
                        DrawableCompat.setTint(micDrawable!!, ContextCompat.getColor(context, R.color.light_gray))
                        btnSpeech.setImageDrawable(micDrawable)
                        templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        templateSpinner.adapter = TemplateAdapter(
                            context,
                            templates.map { it.name },
                            ContextCompat.getColor(context, R.color.white)
                        )

                        // Изменение цвета каемки кнопки
                        if (buttonBackground != null) {
                            val strokeColor = ContextCompat.getColor(context, R.color.dark_gray)
                            DrawableCompat.setTint(DrawableCompat.wrap(buttonBackground).mutate(), strokeColor)
                            btnSpeech.background = buttonBackground
                            btnRestore.background = buttonBackground
                        }
                    }
                    else -> {
                        dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background)
                        etNote.setTextColor(ContextCompat.getColor(context, R.color.black))
                        etNote.setHintTextColor(ContextCompat.getColor(context, R.color.black))
                        btnSpeech.background = buttonBackground
                        btnRestore.background = buttonBackground
                        btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                        templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background)
                        templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background)
                        templateSpinner.adapter = TemplateAdapter(
                            context,
                            templates.map { it.name },
                            ContextCompat.getColor(context, R.color.black)
                        )
                    }
                }
            }
        }
        
        // Set initial selection based on source
        val templateIndex = if (isFromReminder) {
            // Use reminder template if available, otherwise fall back to default
            val reminderTemplateId = settingsViewModel.selectedReminderTemplateId.value
            if (reminderTemplateId != null) {
                templates.indexOfFirst { it.id == reminderTemplateId }
            } else {
                templates.indexOfFirst { it.isDefault }
            }
        } else {
            // Use default template for regular notifications
            templates.indexOfFirst { it.isDefault }
        }
        
        if (templateIndex != -1) {
            templateSpinner.setSelection(templateIndex)
            settingsViewModel.selectTemplate(templates[templateIndex].id)
        }

        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTemplate = templates[position]
                settingsViewModel.selectTemplate(selectedTemplate.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnRestore.visibility = if (settingsViewModel.previousText.value.isNotEmpty()) {
            ImageButton.VISIBLE
        } else {
            ImageButton.GONE
        }

        btnRestore.setOnClickListener {
            etNote.setText(settingsViewModel.previousText.value)
            etNote.setSelection(etNote.text.length)
            settingsViewModel.clearPreviousText()
            btnRestore.visibility = ImageButton.GONE
        }

        etNote.setText("")

        etNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                settingsViewModel.updateCurrentText(text)
                if (text.length >= 10 && settingsViewModel.previousText.value.isNotEmpty()) {
                    settingsViewModel.clearPreviousText()
                    btnRestore.visibility = ImageButton.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                lastPartialTextLength = 0
                val currentText = etNote.text.toString()
                if (currentText.isNotEmpty() && currentText.last() != ' ') {
                    etNote.append(" ")
                }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

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

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        btnSpeech.setOnClickListener {
            if (isListening) {
                stopSpeechRecognition()
            } else {
                (activity as? TransparentActivity)?.startSpeechRecognition()
            }
        }

        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun updateNoteText(text: String) {
        val currentText = etNote.text.toString()
        val newText = if (lastPartialTextLength <= currentText.length) {
            currentText.substring(0, currentText.length - lastPartialTextLength) + text
        } else {
            text
        }
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
        if (note.isBlank()) {
            Toast.makeText(context, R.string.note_error, Toast.LENGTH_SHORT).show()
            return
        }

        val folderUri = settingsViewModel.folderUri.value
        if (folderUri == context.getString(R.string.folder_not_selected)) {
            Toast.makeText(context, R.string.folder_not_selected, Toast.LENGTH_SHORT).show()
            return
        }

        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
        if (folder == null || !folder.exists() || !folder.canWrite()) {
            Toast.makeText(context, R.string.error_selecting_folder, Toast.LENGTH_SHORT).show()
            return
        }

        // Формируем имя файла
        var filename = getFileNameWithDate(settingsViewModel.noteDateTemplate.value)

        // Добавляем начало текста заметки в имя файла, если включено
        if (settingsViewModel.isNoteTextInFilenameEnabled.value) {
            val noteText = note.take(settingsViewModel.noteTextInFilenameLength.value)
                .replace(Regex("[<>:\"/\\|?*]"), "_") // Заменяем недопустимые символы
                .trim()
            if (noteText.isNotEmpty()) {
                filename = "$filename - $noteText"
            }
        }

        filename = "$filename.md"

        // Проверяем существование файла
        var file = folder.findFile(filename)
        val isNewFile = file == null

        if (isNewFile) {
            file = folder.createFile("text/markdown", filename)
        }

        if (file != null) {
            try {
                val content = StringBuilder()

                // Форматируем текст заметки
                val formattedNote = if (settingsViewModel.isListItemsEnabled.value) {
                    val indent = "\t".repeat(settingsViewModel.listItemIndentLevel.value)
                    note.lines().joinToString("\n") { line ->
                        "$indent- $line"
                    }
                } else {
                    note
                }

                // Добавляем временную метку перед текстом, если включено
                if (settingsViewModel.isTimestampEnabled.value) {
                    val timestamp = getFormattedTimestamp(settingsViewModel.timestampTemplate.value)
                    content.append(timestamp).append("\n")
                }

                // Добавляем отформатированный текст
                content.append(formattedNote)

                // Открываем файл для записи
                context.contentResolver.openOutputStream(file.uri, if (isNewFile) "w" else "wa")?.use { outputStream ->
                    if (isNewFile) {
                        // Для нового файла добавляем YAML заголовок, если включено
                        if (settingsViewModel.isDateCreatedEnabled.value) {
                            val fullTimeStamp = getFormattedTimestamp(settingsViewModel.dateCreatedTemplate.value)
                            val yamlHeader = "---\n${settingsViewModel.propertyName.value}: $fullTimeStamp\n---\n"
                            outputStream.write(yamlHeader.toByteArray())
                        }
                        outputStream.write(content.toString().toByteArray())
                    } else {
                        // Для существующего файла добавляем перенос строки и новый текст
                        outputStream.write("\n".toByteArray())
                        outputStream.write(content.toString().toByteArray())
                    }
                }

                settingsViewModel.clearCurrentText()
                settingsViewModel.clearPreviousText()
                Toast.makeText(
                    context,
                    if (isNewFile) R.string.note_saved else R.string.note_appended,
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(context, R.string.note_error, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.note_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismissWithMessage(message: String) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etNote.windowToken, 0)
        etNote.setText(message)
        handler.postDelayed({
            if (message == context.getString(R.string.note_saved) || message == context.getString(R.string.note_appended)) {
                settingsViewModel.clearCurrentText()
                settingsViewModel.clearPreviousText()
            } else {
                settingsViewModel.updateCurrentText(settingsViewModel.tempText.value)
            }

            settingsViewModel.clearTempText()
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

        return result.replace(":", "_")
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