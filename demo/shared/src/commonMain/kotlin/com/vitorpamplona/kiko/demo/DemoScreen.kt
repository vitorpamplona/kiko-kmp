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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitorpamplona.kiko.compose.KikoGame

/**
 * The demo on every platform: [KikoGame] playing what [source] gives (the demo's own cart when
 * it gives null), reloading when [key] changes, with an "Open cart" button when the platform can
 * pick a file ([onOpen]).
 */
@Composable
fun DemoScreen(
    key: Any?,
    source: suspend () -> ByteArray?,
    onOpen: (() -> Unit)?,
    onExit: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        KikoGame(
            key = key,
            source = { source() ?: DEMO_CART.encodeToByteArray() },
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            onExit = onExit,
        )
        if (onOpen != null) {
            BasicText(
                text = "Open cart",
                style = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
