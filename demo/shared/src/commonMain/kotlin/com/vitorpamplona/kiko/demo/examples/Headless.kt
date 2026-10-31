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
package com.vitorpamplona.kiko.demo.examples

import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.runtime.Buttons
import com.vitorpamplona.kiko.runtime.FantasyConsole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * kiko-player without Compose, for any other UI (or none): play one second of [cart] holding
 * right, and return the last frame as 128x128 ARGB pixels.
 */
suspend fun playOneSecond(cart: ByteArray): IntArray {
    val player = KikoPlayer(source = { cart }, openAudio = { null })
    try {
        player.setButtons(Buttons.RIGHT)
        player.resume() // loads the cart, then runs it on its own thread at the game's frame rate
        withTimeout(5.seconds) { player.frameNumber.first { it >= 30 } }

        val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
        player.copyFrame(pixels)
        return pixels
    } finally {
        player.close()
    }
}
