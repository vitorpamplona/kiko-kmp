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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.vitorpamplona.kiko.runtime.FantasyConsole.Companion.SCREEN_SIZE
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * One Skia bitmap, its pixels replaced each frame. Compose makes a short-lived image from it for
 * each draw and closes it at once, so a frame leaves nothing behind for the garbage collector (a
 * new image per frame was 64 KiB of native memory, 60 times a second, freed only by finalizers).
 */
internal actual fun newScreenBitmap(): ScreenBitmap =
    object : ScreenBitmap {
        private val info = ImageInfo(SCREEN_SIZE, SCREEN_SIZE, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        private val bytes = ByteArray(SCREEN_SIZE * SCREEN_SIZE * 4)
        private val bitmap = Bitmap().apply { allocPixels(info) }
        private val image = bitmap.asComposeImageBitmap()

        override fun update(pixels: IntArray): ImageBitmap {
            for (i in pixels.indices) {
                val p = pixels[i]
                val o = i * 4
                bytes[o] = p.toByte()
                bytes[o + 1] = (p shr 8).toByte()
                bytes[o + 2] = (p shr 16).toByte()
                bytes[o + 3] = (p ushr 24).toByte()
            }
            bitmap.installPixels(bytes)
            return image
        }
    }

/** No system edge gestures to keep off on the desktop, iOS (the app owns its swipes) or the web. */
internal actual fun Modifier.claimSystemGestures(): Modifier = this
