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
package com.vitorpamplona.kiko.demo

import com.vitorpamplona.kiko.cart.CartLoader
import com.vitorpamplona.kiko.runtime.Buttons
import com.vitorpamplona.kiko.runtime.FantasyConsole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The demo's carts load and keep running while their buttons are pressed. */
class DemoCartTest {
    @Test
    fun theDemoCartRuns() {
        val console = FantasyConsole(CartLoader.load(DEMO_CART.encodeToByteArray()))
        repeat(120) { frame ->
            console.setButtons(0, if (frame % 10 < 5) Buttons.RIGHT or Buttons.X else Buttons.O)
            console.step()
        }
        assertNull(console.error)
        assertEquals(FantasyConsole.State.RUNNING, console.state)
    }

    /** Every game in the list is in the demo's resources, loads, and plays four seconds of input. */
    @Test
    fun everyBundledGameRuns() {
        for (game in DEMO_GAMES) {
            val console = FantasyConsole(CartLoader.load(runBlocking { game.cart() }))
            repeat(120) { frame ->
                console.setButtons(0, if (frame % 30 < 2) Buttons.X else 0)
                console.step()
            }
            assertNull(console.error, game.title)
            assertEquals(FantasyConsole.State.RUNNING, console.state, game.title)
        }
    }
}
