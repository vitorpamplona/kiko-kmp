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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vitorpamplona.kiko.compose.KikoGame
import com.vitorpamplona.kiko.compose.rememberKikoPlayer
import kotlinx.coroutines.launch

/**
 * The demo on every platform: one [KikoGame] (the whole integration; see `examples/` for more
 * ways to use kiko) under the demo's own [GameMenu]. Plays [opened], a cart the user picked
 * with [onOpen], or one of [DEMO_GAMES].
 */
@Composable
fun DemoScreen(
    opened: ByteArray?,
    onOpen: (() -> Unit)?,
    onExit: (() -> Unit)? = null,
) {
    // The cart playing now, and the bundled game it is (null for the user's own).
    var cart by remember { mutableStateOf<ByteArray?>(null) }
    var playing by remember { mutableStateOf<DemoGame?>(null) }
    val scope = rememberCoroutineScope()

    fun play(game: DemoGame) =
        scope.launch {
            cart = game.cart()
            playing = game
        }
    // The demo's own game, unless the app started with a cart of the user's.
    LaunchedEffect(Unit) { if (cart == null && opened == null) play(DEMO_GAMES.first()) }
    LaunchedEffect(opened) {
        if (opened != null) {
            cart = opened
            playing = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        cart?.let { bytes ->
            // The whole integration: one KikoGame call, here with the player held by the demo so
            // the system Back gesture can open the game's pause menu first.
            val player = rememberKikoPlayer(key = bytes) { bytes }
            KikoGame(player, Modifier.fillMaxSize().safeDrawingPadding(), onExit = onExit)
            SystemBack { if (!player.openPauseMenu()) onExit?.invoke() }
        }
        GameMenu(playing = playing, onPick = ::play, onOpen = onOpen)
    }
}
