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

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Bitmap
import kotlin.test.assertTrue

/** Renders [content] until a quarter of a 400x700 scene is [color], for up to 10 s. */
fun assertShows(
    color: Int,
    content: @Composable () -> Unit,
) {
    val scene = ImageComposeScene(width = 400, height = 700, density = Density(1f), content = content)
    try {
        val deadline = System.nanoTime() + 10_000_000_000L
        var matching = 0
        var time = 0L
        while (System.nanoTime() < deadline) {
            time += 16_000_000
            val bitmap = Bitmap.makeFromImage(scene.render(time))
            matching = (0 until 400 step 4).sumOf { x -> (0 until 700 step 4).count { y -> bitmap.getColor(x, y) == color } }
            if (matching > 400 / 4 * 700 / 4 / 4) return
            Thread.sleep(20)
        }
        assertTrue(false, "only $matching sampled pixels were the color wanted")
    } finally {
        scene.close()
    }
}
