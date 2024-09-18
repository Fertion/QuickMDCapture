package com.example.quickmdcapture

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class TransparentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val dialog = NoteDialog(this) // Передаем this (AppCompatActivity)
        dialog.setOnDismissListener {
            finish()
            overridePendingTransition(0, 0)
        }
        dialog.show()

        // Убираем флаг FLAG_NOT_FOCUSABLE, чтобы диалог мог получить фокус
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }
}
