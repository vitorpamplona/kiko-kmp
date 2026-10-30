/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.kiko.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.vitorpamplona.kiko.runtime.Buttons

/**
 * The keyboard and gamepad for [input]: arrows, Z/C/N for O, X/V/M for X, Enter or P to pause,
 * and every key typed for games that read the keyboard. Escape (and a key-event Back) calls
 * [onBack]. Keys arrive only while the element has focus: put this before a `focusable()` that
 * the screen focuses.
 */
public fun Modifier.kikoKeys(
    input: KikoInput,
    onBack: () -> Unit,
): Modifier =
    onKeyEvent { event ->
        if (event.key == Key.Escape || event.key == Key.Back) {
            if (event.type == KeyEventType.KeyUp) onBack()
            return@onKeyEvent true
        }
        // Games that read the keyboard get every key typed, buttons included, as on the original.
        if (event.type == KeyEventType.KeyDown) typedText(event)?.let(input.player::type)
        val bit = KEY_BITS[event.key] ?: return@onKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> input.keys = input.keys or bit
            KeyEventType.KeyUp -> input.keys = input.keys and bit.inv()
        }
        true
    }

/** Keyboard and gamepad keys and the button each holds down. */
internal val KEY_BITS: Map<Key, Int> =
    mapOf(
        Key.DirectionLeft to Buttons.LEFT,
        Key.DirectionRight to Buttons.RIGHT,
        Key.DirectionUp to Buttons.UP,
        Key.DirectionDown to Buttons.DOWN,
        Key.Z to Buttons.O,
        Key.C to Buttons.O,
        Key.N to Buttons.O,
        Key.ButtonA to Buttons.O,
        Key.ButtonY to Buttons.O,
        Key.X to Buttons.X,
        Key.V to Buttons.X,
        Key.M to Buttons.X,
        Key.ButtonB to Buttons.X,
        Key.ButtonX to Buttons.X,
        Key.Enter to Buttons.PAUSE,
        Key.P to Buttons.PAUSE,
        Key.ButtonStart to Buttons.PAUSE,
    )

/** What a key types for games that read the keyboard: Enter is a carriage return and Backspace `\b`, as on the original. */
internal fun typedText(event: KeyEvent): String? =
    when {
        event.key == Key.Enter -> "\r"
        event.key == Key.Backspace -> "\b"
        event.utf16CodePoint >= ' '.code -> event.utf16CodePoint.toChar().toString()
        else -> null
    }
