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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.vitorpamplona.kiko.runtime.Buttons
import kotlin.math.abs
import kotlin.math.min

/**
 * A direction pad for [input]: a cross that reads every finger on it, so a thumb slides between
 * directions and diagonals without lifting. Give it a square size with [modifier]. Each new
 * direction ticks the haptics; letting go does not. Android's edge gestures stay off it.
 */
@Composable
public fun KikoDirectionPad(
    input: KikoInput,
    modifier: Modifier = Modifier,
    description: String = KikoLabels.Default.directionPad,
) {
    val haptics = LocalHapticFeedback.current
    DirectionPad(
        onChange = {
            if (it != input.pad && it != 0) haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            input.pad = it
        },
        description = description,
        modifier = modifier,
    )
}

/**
 * The O and X buttons for [input], staggered like a handheld (O low on the left, X high on the
 * right) and read as one area: a thumb rolls from one to the other, and the strip between them
 * presses both. Give it a square size with [modifier]. A button going down ticks the haptics.
 */
@Composable
public fun KikoActionButtons(
    input: KikoInput,
    modifier: Modifier = Modifier,
    description: String = KikoLabels.Default.actionButtons,
) {
    val haptics = LocalHapticFeedback.current
    ActionButtons(
        onChange = {
            if (it and input.actions.inv() != 0) haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            input.actions = it
        },
        description = description,
        modifier = modifier,
    )
}

private val ControlColor = Color.White.copy(alpha = 0.22f)
private val PressedColor = Color.White.copy(alpha = 0.55f)

/** A cross that reads every finger on it, so sliding between directions and diagonals work. */
@Composable
internal fun DirectionPad(
    onChange: (Int) -> Unit,
    description: String,
    modifier: Modifier,
) {
    var held by remember { mutableIntStateOf(0) }
    // The touch loop below lives as long as the control; a new game brings a new onChange.
    val currentOnChange by rememberUpdatedState(onChange)
    Canvas(
        modifier
            .claimSystemGestures()
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        var bits = 0
                        for (change in event.changes) {
                            if (!change.pressed) continue
                            bits = bits or directionOf(change.position, size)
                            change.consume()
                        }
                        if (bits != held) {
                            held = bits
                            currentOnChange(bits)
                        }
                    }
                }
            },
    ) {
        val arm = size.width / 3
        val radius = CornerRadius(arm / 4)
        drawRoundRect(ControlColor, Offset(0f, arm), Size(size.width, arm), radius)
        drawRoundRect(ControlColor, Offset(arm, 0f), Size(arm, size.height), radius)

        fun press(
            bit: Int,
            x: Float,
            y: Float,
        ) {
            if (held and bit != 0) drawRoundRect(PressedColor, Offset(x, y), Size(arm, arm), radius)
        }
        press(Buttons.LEFT, 0f, arm)
        press(Buttons.RIGHT, arm * 2, arm)
        press(Buttons.UP, arm, 0f)
        press(Buttons.DOWN, arm, arm * 2)
    }
}

/** Which directions a touch at [position] holds: each axis counts within 67.5° of it, so corners give diagonals. */
private fun directionOf(
    position: Offset,
    size: IntSize,
): Int {
    val dx = position.x - size.width / 2f
    val dy = position.y - size.height / 2f
    val dead = size.width * 0.1f
    var bits = 0
    if (abs(dx) > dead && abs(dx) >= abs(dy) * TAN_22_5) bits = bits or if (dx < 0) Buttons.LEFT else Buttons.RIGHT
    if (abs(dy) > dead && abs(dy) >= abs(dx) * TAN_22_5) bits = bits or if (dy < 0) Buttons.UP else Buttons.DOWN
    return bits
}

private const val TAN_22_5 = 0.4142f

/**
 * O and X, staggered like a handheld (O low on the left, X high on the right), read as one area
 * like the direction pad: a thumb rolls from one to the other without lifting, the strip between
 * them presses both, and every finger on it counts.
 */
@Composable
internal fun ActionButtons(
    onChange: (Int) -> Unit,
    description: String,
    modifier: Modifier,
) {
    var held by remember { mutableIntStateOf(0) }
    // The touch loop below lives as long as the control; a new game brings a new onChange.
    val currentOnChange by rememberUpdatedState(onChange)
    Canvas(
        modifier
            .claimSystemGestures()
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        var bits = 0
                        for (change in event.changes) {
                            if (!change.pressed) continue
                            bits = bits or buttonsAt(change.position, this.size.width.toFloat())
                            change.consume()
                        }
                        if (bits != held) {
                            held = bits
                            currentOnChange(bits)
                        }
                    }
                }
            },
    ) {
        val radius = this.size.minDimension * BUTTON_RADIUS
        drawActionButton(Offset(radius, this.size.height - radius), radius, isX = false, pressed = held and Buttons.O != 0)
        drawActionButton(Offset(this.size.width - radius, radius), radius, isX = true, pressed = held and Buttons.X != 0)
    }
}

private fun DrawScope.drawActionButton(
    center: Offset,
    radius: Float,
    isX: Boolean,
    pressed: Boolean,
) {
    drawCircle(if (pressed) PressedColor else ControlColor, radius, center)
    val mark = radius * 0.49f
    val stroke = radius * 0.155f
    if (isX) {
        drawLine(Color.White, center - Offset(mark, mark), center + Offset(mark, mark), stroke, StrokeCap.Round)
        drawLine(Color.White, center - Offset(mark, -mark), center + Offset(mark, -mark), stroke, StrokeCap.Round)
    } else {
        drawCircle(Color.White, mark, center, style = Stroke(stroke))
    }
}

/**
 * Which buttons a touch at [position] holds in an area [side] wide: the nearer one, a little past
 * its edge, or both in the strip halfway between them.
 */
private fun buttonsAt(
    position: Offset,
    side: Float,
): Int {
    val radius = side * BUTTON_RADIUS
    val toO = (position - Offset(radius, side - radius)).getDistance()
    val toX = (position - Offset(side - radius, radius)).getDistance()
    if (abs(toO - toX) < radius * BOTH_STRIP && maxOf(toO, toX) < radius * BOTH_REACH) return Buttons.O or Buttons.X
    val nearest = min(toO, toX)
    if (nearest > radius * REACH) return 0
    return if (toO <= toX) Buttons.O else Buttons.X
}

/** Each button's radius, as a share of the area's side. */
private const val BUTTON_RADIUS = 0.225f

/** How far past its edge a button still answers, in radii. */
private const val REACH = 1.6f

/** The strip that presses both: within this many radii of halfway, and this close to each. */
private const val BOTH_STRIP = 0.6f
private const val BOTH_REACH = 2.2f
