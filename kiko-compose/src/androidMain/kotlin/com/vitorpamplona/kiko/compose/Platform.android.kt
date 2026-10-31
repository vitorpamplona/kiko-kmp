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

import android.graphics.Bitmap
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vitorpamplona.kiko.runtime.FantasyConsole.Companion.SCREEN_SIZE

/** One bitmap, rewritten in place each frame; Android re-uploads it when its pixels change. */
internal actual fun newScreenBitmap(): ScreenBitmap =
    object : ScreenBitmap {
        private val bitmap = Bitmap.createBitmap(SCREEN_SIZE, SCREEN_SIZE, Bitmap.Config.ARGB_8888)
        private val image = bitmap.asImageBitmap()

        override fun update(pixels: IntArray): ImageBitmap {
            bitmap.setPixels(pixels, 0, SCREEN_SIZE, 0, 0, SCREEN_SIZE, SCREEN_SIZE)
            return image
        }
    }

internal actual fun Modifier.claimSystemGestures(): Modifier = systemGestureExclusion()
