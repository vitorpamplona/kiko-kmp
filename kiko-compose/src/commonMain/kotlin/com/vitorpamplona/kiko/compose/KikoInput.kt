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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vitorpamplona.kiko.player.KikoPlayer

/**
 * One player's buttons, gathered from every control that sets them: the touch pad, the touch
 * buttons and the keyboard each hold their own, and the game sees them together. Make one with
 * [rememberKikoInput] and hand it to [KikoDirectionPad], [KikoActionButtons] and [kikoKeys].
 */
public class KikoInput internal constructor(
    internal val player: KikoPlayer,
    private val playerIndex: Int,
) {
    // Plain fields, sent on every change: an effect keyed on them would merge a quick tap's
    // press and release into one recomposition and the game would never see the press.
    internal var pad = 0
        set(value) {
            field = value
            send()
        }
    internal var actions = 0
        set(value) {
            field = value
            send()
        }
    internal var keys = 0
        set(value) {
            field = value
            send()
        }

    private fun send() = player.setButtons(pad or actions or keys, playerIndex)
}

/** The [KikoInput] for [playerIndex] (0..7) of [player]. */
@Composable
public fun rememberKikoInput(
    player: KikoPlayer,
    playerIndex: Int = 0,
): KikoInput = remember(player, playerIndex) { KikoInput(player, playerIndex) }
