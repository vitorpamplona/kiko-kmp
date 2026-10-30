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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitorpamplona.kiko.player.KikoPlayer

/**
 * What [player] is doing, to lay over its screen: a spinner while it loads, a line when it cannot
 * play or has stopped (the game's last screen stays up under it), nothing while it plays.
 */
@Composable
public fun KikoStatus(
    player: KikoPlayer,
    modifier: Modifier = Modifier,
    labels: KikoLabels = KikoLabels.Default,
) {
    val status by player.status.collectAsState()
    Box(modifier) { StatusText(status, labels) }
}

@Composable
private fun StatusText(
    status: KikoPlayer.Status,
    labels: KikoLabels,
) {
    when (status) {
        KikoPlayer.Status.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spinner()
                    Hint(labels.loading, Modifier.padding(16.dp))
                }
            }
        is KikoPlayer.Status.Failed ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val text = if (status.reason == KikoPlayer.Reason.NOT_A_CART) labels.notACart else labels.noCart
                Hint(text, Modifier.padding(32.dp))
            }
        is KikoPlayer.Status.Stopped -> {
            // The game's last screen (with any error drawn on it) stays up; this only labels it.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Hint(if (status.error == null) labels.gameEnded else labels.gameError, Modifier.padding(16.dp))
            }
        }
        KikoPlayer.Status.Playing -> {}
    }
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier,
) {
    BasicText(text, modifier = modifier, style = TextStyle(color = HintColor, fontSize = 13.sp, textAlign = TextAlign.Center))
}

/** An arc going round, drawn here so the library needs no Material. */
@Composable
private fun Spinner() {
    val turn by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
    )
    Canvas(Modifier.size(40.dp).progressSemantics()) {
        val stroke = 4.dp.toPx()
        drawArc(
            color = Color.White,
            startAngle = turn,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

private val HintColor = Color.White.copy(alpha = 0.7f)
