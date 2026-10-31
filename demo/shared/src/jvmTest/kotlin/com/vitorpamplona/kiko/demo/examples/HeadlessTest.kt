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

import com.vitorpamplona.kiko.demo.DEMO_CART
import com.vitorpamplona.kiko.runtime.FantasyConsole.Companion.SCREEN_SIZE
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Headless example does what it says: a second of the demo cart, holding right. */
class HeadlessTest {
    @Test
    fun holdingRightMovesTheBallRight() {
        val pixels = runBlocking { playOneSecond(DEMO_CART.encodeToByteArray()) }
        // The ball starts in the middle (x 64) on row 64, drawn in red; a second of right moves it.
        val red = (0 until SCREEN_SIZE).filter { x -> pixels[64 * SCREEN_SIZE + x] == RED }
        assertTrue(red.isNotEmpty() && red.min() > 80, "red on row 64 at $red")
    }

    private companion object {
        const val RED = 0xFFFF004D.toInt()
    }
}
