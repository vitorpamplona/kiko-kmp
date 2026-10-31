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

import com.vitorpamplona.kiko.cart.CartLoader
import com.vitorpamplona.kiko.runtime.Buttons
import com.vitorpamplona.kiko.runtime.ConsoleHost
import com.vitorpamplona.kiko.runtime.FantasyConsole

/**
 * kiko alone, for an app that runs its own loop: play [frames] frames of [cart] holding right,
 * hand each frame's sound to [play], and return the last frame as 128x128 ARGB pixels. Call it on
 * one thread with a [FantasyConsole.RECOMMENDED_STACK_BYTES] stack, and step once every
 * 1/console.fps s when a person is watching; this one runs as fast as it can.
 */
fun runFrames(
    cart: ByteArray,
    frames: Int,
    saves: MutableMap<String, IntArray>,
    play: (samples: FloatArray, count: Int) -> Unit,
): IntArray {
    // .p8.png, .p8 or .p8.rom: the format is read from the bytes, not the file name.
    val console =
        FantasyConsole(
            CartLoader.load(cart),
            object : ConsoleHost {
                override fun log(message: String) = println(message) // printh

                override fun loadCartData(id: String): IntArray? = saves[id] // cartdata, 64 numbers

                override fun saveCartData(
                    id: String,
                    values: IntArray,
                ) {
                    saves[id] = values
                }
            },
        )

    val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
    for (frame in 0 until frames) {
        console.setButtons(0, Buttons.RIGHT) // player 0..7; LEFT, RIGHT, UP, DOWN, O, X, PAUSE
        console.step()
        play(console.audioBuffer, console.audioSamples) // FantasyConsole.SAMPLE_RATE Hz mono
        // console.state and console.error say when the cart stops; console.abort() stops it
        // from any thread, even in the middle of a frame.
        if (console.state != FantasyConsole.State.RUNNING) break
    }
    console.render(pixels) // with the pause menu over it when open
    return pixels
}
