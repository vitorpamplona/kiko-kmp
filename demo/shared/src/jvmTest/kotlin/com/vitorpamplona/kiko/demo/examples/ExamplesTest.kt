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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertTrue

/** Each Compose example, drawn off screen, shows a game: a cart that paints its screen red. */
class ExamplesTest {
    private val red = "__lua__\nfunction _draw() cls(8) end\n".encodeToByteArray()

    @Test
    fun oneCallShowsTheGame() = assertShowsRed { OneCall("red.p8") { red } }

    @Test
    fun keepTheGameShowsTheGame() =
        assertShowsRed {
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
    fun ownLayoutShowsTheGame() = assertShowsRed { OwnLayout(red) }

    /** Renders [content] until a quarter of a 400x700 scene is the cart's red, for up to 10 s. */
    private fun assertShowsRed(content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 400, height = 700, density = Density(1f), content = content)
        try {
            val deadline = System.nanoTime() + 10_000_000_000L
            var redPixels = 0
            var time = 0L
            while (System.nanoTime() < deadline) {
                time += 16_000_000
                val bitmap = Bitmap.makeFromImage(scene.render(time))
                redPixels = (0 until 400 step 4).sumOf { x -> (0 until 700 step 4).count { y -> bitmap.getColor(x, y) == RED } }
                if (redPixels > 400 / 4 * 700 / 4 / 4) return
                Thread.sleep(20)
            }
            assertTrue(false, "only $redPixels sampled pixels were the game's red")
        } finally {
            scene.close()
        }
    }

    private companion object {
        const val RED = 0xFFFF004D.toInt()
    }
}
