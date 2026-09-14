package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.theme.SoundDeckTheme
import com.sounddeck.ui.SoundDeckScreen
import com.sounddeck.ui.SoundDeckViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SoundDeckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundDeckTheme {
                SoundDeckScreen(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0) {
            val keyFormatted = com.sounddeck.ui.KeyUtils.formatKeyEvent(event)
            if (viewModel.triggerPadByKeyShortcut(keyFormatted)) {
                return true
            }
            val unicodeChar = event.unicodeChar
            if (unicodeChar > 0) {
                val charKey = unicodeChar.toChar().toString().uppercase()
                if (charKey != keyFormatted && viewModel.triggerPadByKeyShortcut(charKey)) {
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

