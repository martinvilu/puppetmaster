package com.sounddeck.ui

import android.view.KeyEvent

object KeyUtils {
    fun formatKeyEvent(event: KeyEvent): String {
        val keyCode = event.keyCode
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> "SPACE"
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "ENTER"
            KeyEvent.KEYCODE_TAB -> "TAB"
            KeyEvent.KEYCODE_ESCAPE -> "ESC"
            KeyEvent.KEYCODE_DEL -> "BACKSPACE"
            KeyEvent.KEYCODE_FORWARD_DEL -> "DELETE"
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_PAGE_UP -> "PAGE_UP"
            KeyEvent.KEYCODE_PAGE_DOWN -> "PAGE_DOWN"
            KeyEvent.KEYCODE_VOLUME_UP -> "VOL_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "VOL_DOWN"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "PLAY_PAUSE"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "NEXT"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "PREV"
            KeyEvent.KEYCODE_BUTTON_A -> "BUTTON_A"
            KeyEvent.KEYCODE_BUTTON_B -> "BUTTON_B"
            KeyEvent.KEYCODE_BUTTON_X -> "BUTTON_X"
            KeyEvent.KEYCODE_BUTTON_Y -> "BUTTON_Y"
            KeyEvent.KEYCODE_BUTTON_L1 -> "BUTTON_L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "BUTTON_R1"
            else -> {
                val unicode = event.unicodeChar
                if (unicode > 32 && !Character.isISOControl(unicode)) {
                    unicode.toChar().toString().uppercase()
                } else {
                    val label = event.displayLabel
                    if (label != '\u0000' && !label.isWhitespace()) {
                        label.toString().uppercase()
                    } else {
                        KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
                    }
                }
            }
        }
    }
}
