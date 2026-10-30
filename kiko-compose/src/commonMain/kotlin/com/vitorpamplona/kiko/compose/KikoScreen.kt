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

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.runtime.FantasyConsole.Companion.SCREEN_SIZE
import kotlin.math.floor
import kotlin.math.min

/**
 * The game's 128x128 screen, as large as fits in [modifier]'s area, scaled by a whole number when
 * there is room so pixels stay square. The pointer over it (a mouse, or a finger on a touch
 * screen) goes to the game in game pixels, for games that read it. A new frame redraws it
 * without recomposing.
 */
@Composable
public fun KikoScreen(
    player: KikoPlayer,
    modifier: Modifier = Modifier,
) {
    val frameNumber = player.frameNumber.collectAsState()
    val pixels = remember { IntArray(SCREEN_SIZE * SCREEN_SIZE) }
    val bitmap = remember { newScreenBitmap() }
    Canvas(
        modifier.pointerInput(player) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    if (event.type == PointerEventType.Scroll) {
                        val dy = change.scrollDelta.y
                        if (dy != 0f) player.scroll(if (dy < 0) 1 else -1)
                        continue
                    }
                    val (left, top, side) = screenRect(size.width.toFloat(), size.height.toFloat())
                    val x = ((change.position.x - left) * SCREEN_SIZE / side).toInt().coerceIn(0, SCREEN_SIZE - 1)
                    val y = ((change.position.y - top) * SCREEN_SIZE / side).toInt().coerceIn(0, SCREEN_SIZE - 1)
                    val buttons =
                        if (change.type == PointerType.Mouse) {
                            (if (event.buttons.isPrimaryPressed) 1 else 0) or
                                (if (event.buttons.isSecondaryPressed) 2 else 0) or
                                (if (event.buttons.isTertiaryPressed) 4 else 0)
                        } else {
                            if (change.pressed) 1 else 0
                        }
                    player.setPointer(x, y, buttons)
                }
            }
        },
    ) {
        // Read here, not in composition: a new frame redraws without recomposing.
        if (frameNumber.value == 0L || player.copyFrame(pixels) == 0L) return@Canvas
        val image = bitmap.update(pixels)
        val (left, top, side) = screenRect(size.width, size.height)
        drawImage(
            image = image,
            srcSize = IntSize(SCREEN_SIZE, SCREEN_SIZE),
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(side.toInt(), side.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

/** Where the screen lands in a [width] x [height] area: left, top and side, in pixels. */
private fun screenRect(
    width: Float,
    height: Float,
): Triple<Float, Float, Float> {
    val fit = min(width, height)
    val side = if (fit >= SCREEN_SIZE * 2) floor(fit / SCREEN_SIZE) * SCREEN_SIZE else fit
    return Triple((width - side) / 2, (height - side) / 2, side)
}
