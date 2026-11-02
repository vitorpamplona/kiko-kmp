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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vitorpamplona.kiko.demo.assertShows
import kotlin.test.Test

/** Each Compose example, drawn off screen, shows a game: a cart that paints its screen red. */
class ExamplesTest {
    private val red = "__lua__\nfunction _draw() cls(8) end\n".encodeToByteArray()

    @Test
    fun oneCallShowsTheGame() = assertShows(RED) { OneCall("red.p8") { red } }

    @Test
    fun keepTheGameShowsTheGame() =
        assertShows(RED) {
            // A window or an Activity provides this; an off-screen scene does not.
            val owner =
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                KeepTheGame("red.p8", { red }, MemorySaves(), onBack = {})
            }
        }

    @Test
    fun ownLayoutShowsTheGame() = assertShows(RED) { OwnLayout(red) }

    private companion object {
        const val RED = 0xFFFF004D.toInt()
    }
}
