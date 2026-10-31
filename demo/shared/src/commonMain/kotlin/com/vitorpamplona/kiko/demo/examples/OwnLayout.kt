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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vitorpamplona.kiko.compose.KikoActionButtons
import com.vitorpamplona.kiko.compose.KikoDirectionPad
import com.vitorpamplona.kiko.compose.KikoScreen
import com.vitorpamplona.kiko.compose.KikoStatus
import com.vitorpamplona.kiko.compose.kikoKeys
import com.vitorpamplona.kiko.compose.rememberKikoInput
import com.vitorpamplona.kiko.compose.rememberKikoPlayer

/** The pieces [com.vitorpamplona.kiko.compose.KikoGame] is made of, in a layout of your own. */
@Composable
fun OwnLayout(cart: ByteArray) {
    val player = rememberKikoPlayer(key = cart) { cart }
    DisposableEffect(player) {
        player.resume()
        onDispose { player.pause() }
    }
    // One player's buttons, from the touch controls and the keyboard together.
    val input = rememberKikoInput(player)
    val keyboard = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboard.requestFocus() }

    Column(
        Modifier
            .background(Color.Black)
            .focusRequester(keyboard)
            .kikoKeys(input, onBack = player::tapPause)
            .focusable(),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            KikoScreen(player)
            KikoStatus(player)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            KikoDirectionPad(input, Modifier.size(140.dp))
            BasicText("Pause", Modifier.clickable(onClick = player::tapPause))
            KikoActionButtons(input, Modifier.size(140.dp))
        }
    }
}
